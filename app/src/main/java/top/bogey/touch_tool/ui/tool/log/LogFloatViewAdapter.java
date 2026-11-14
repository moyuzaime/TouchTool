package top.bogey.touch_tool.ui.tool.log;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.other.log.ActionLog;
import top.bogey.touch_tool.bean.other.log.DateTimeLog;
import top.bogey.touch_tool.bean.other.log.Log;
import top.bogey.touch_tool.bean.other.log.LogInfo;
import top.bogey.touch_tool.bean.other.log.NormalLog;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.save.log.LogSave;
import top.bogey.touch_tool.bean.save.setting.SettingSaver;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.databinding.FloatLogActionItemBinding;
import top.bogey.touch_tool.databinding.FloatLogActionValueItemBinding;
import top.bogey.touch_tool.databinding.FloatLogDateTimeItemBinding;
import top.bogey.touch_tool.databinding.FloatLogNormalItemBinding;
import top.bogey.touch_tool.ui.InstantActivity;
import top.bogey.touch_tool.ui.MainActivity;
import top.bogey.touch_tool.ui.blueprint.BlueprintView;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.tree.LazyTreeNode;
import top.bogey.touch_tool.utils.tree.TreeAdapter;
import top.bogey.touch_tool.utils.tree.TreeNode;

public class LogFloatViewAdapter extends TreeAdapter {
    private Task task;
    int searchIndex = -1;
    // uid -> 节点，增量插入时按 uid 定位，避免整表重建
    private final Map<String, LazyTreeNode> nodeMap = new HashMap<>();
    private Runnable onNodeToggled;

    public void setOnNodeToggled(Runnable onNodeToggled) {
        this.onNodeToggled = onNodeToggled;
    }

    @NonNull
    @Override
    public TreeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return switch (viewType) {
            case 1 -> new ViewHolder(FloatLogActionItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            case 2 -> new ViewHolder(FloatLogDateTimeItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            default -> new ViewHolder(FloatLogNormalItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        };
    }

    @Override
    public int getItemViewType(int position) {
        TreeNode treeNode = treeNodes.get(position);
        LogInfo logInfo = (LogInfo) treeNode.getData();
        if (logInfo == null) return 0;
        if (logInfo.getLogObject() instanceof ActionLog) return 1;
        if (logInfo.getLogObject() instanceof DateTimeLog) return 2;
        return 0;
    }

    private LogSave debugLogSave;
    // 渲染去重：同一动作同一毫秒只显示一条
    private final java.util.Set<String> renderSigs = new java.util.HashSet<>();
    // 展开状态按 uid 记住：整表重建后恢复，否则边跑边看子日志时每次重建都会被打回折叠
    private final java.util.Set<String> expandedUids = new java.util.HashSet<>();

    @Override
    public void switchNodeExpand(TreeNode node) {
        super.switchNodeExpand(node);
        if (node instanceof LazyTreeNode lazy) {
            String uid = String.valueOf(lazy.getFlag());
            if (node.isExpanded()) expandedUids.add(uid);
            else expandedUids.remove(uid);
        }
    }

    public void setLogSave(LogSave logSave) {
        debugLogSave = logSave;
        task = TaskSaver.getInstance().getTask(logSave.getKey());
        // 记住当前展开的节点，重建后恢复
        for (TreeNode node : new ArrayList<>(treeNodes)) {
            if (node instanceof LazyTreeNode lazy && node.isExpanded()) {
                expandedUids.add(String.valueOf(lazy.getFlag()));
            }
        }
        nodeMap.clear();
        renderSigs.clear();

        // 按 order 遍历，按 children（日志 uid）还原层级；顺序表只取一次，避免逐条重复解析
        java.util.List<String> order = logSave.getOrder();
        java.util.Map<String, LogInfo> byUid = new java.util.LinkedHashMap<>();
        for (String uid : order) {
            if (uid == null) continue;
            LogInfo info = logSave.getLog(uid);
            if (info == null) continue;
            byUid.put(uid, info);
        }
        java.util.Set<String> childUids = new java.util.HashSet<>();
        for (LogInfo info : byUid.values()) {
            for (Object flag : info.getChildrenFlags()) childUids.add(String.valueOf(flag));
        }
        List<TreeNode> nodeList = new ArrayList<>();
        for (String uid : byUid.keySet()) {
            if (childUids.contains(uid)) continue;
            LogInfo rootInfo = byUid.get(uid);
            if (rootInfo != null) {
                // 根节点也按「时间+actionId」去重，避免已在父节点下渲染的条目又以根行出现
                String sig = rootInfo.getTime() + "|" + (rootInfo.getLogObject()
                        instanceof top.bogey.touch_tool.bean.other.log.ActionLog a ? a.getActionId() : uid);
                if (!renderSigs.add(sig)) continue;
            }
            nodeList.add(buildNode(logSave, uid, byUid, 0));
        }
        applyTreeNodes(nodeList);
        restoreExpanded();
    }

    // 整表替换用一次刷新：TreeAdapter.setTreeNodes 是「先删空再插入」，
    // 高频重建时会闪出一帧空列表
    private void applyTreeNodes(List<TreeNode> nodes) {
        treeNodes.clear();
        treeNodes.addAll(nodes);
        notifyDataSetChanged();
    }

    // 重建后按 uid 恢复展开状态；expandNode 会把子节点插入列表，所以用按索引推进的循环
    private void restoreExpanded() {
        if (expandedUids.isEmpty()) return;
        int index = 0;
        while (index < treeNodes.size()) {
            TreeNode node = treeNodes.get(index);
            if (node instanceof LazyTreeNode lazy && !node.isExpanded()
                    && expandedUids.contains(String.valueOf(lazy.getFlag()))) {
                expandNode(node);
            }
            index++;
        }
    }

    private LazyTreeNode buildNode(LogSave logSave, String uid, java.util.Map<String, LogInfo> byUid, int depth) {
        LazyTreeNode node = new LazyTreeNode(logSave, uid);
        node.setDepth(depth);
        nodeMap.put(uid, node);
        // 子节点交给 LazyTreeNode.getData() 按 children 自动加载，
        // 这里再手工 addChild 会让同一个 uid 被注册两次、展开时出现重复行
        return node;
    }

    // ===== 增量追加：新日志不再整表重建，只在末尾插入一行，滚动位置由 RecyclerView 自行保持 =====

    // 新根日志：直接追加到列表末尾
    public synchronized void appendRoot(LogSave logSave, LogInfo info) {
        if (info == null || info.getUid() == null) return;
        String uid = info.getUid();
        LazyTreeNode exist = nodeMap.get(uid);
        if (exist != null) {
            // 已在屏上：只同步它的子节点（例如容器节点后来才有 children）
            syncChildren(logSave, exist, info);
            return;
        }
        LazyTreeNode root = new LazyTreeNode(logSave, uid);
        root.setDepth(0);
        nodeMap.put(uid, root);
        int index = treeNodes.size();
        treeNodes.add(index, root);
        notifyItemInserted(index);
        // 该节点可能已带 children（如并行执行容器），把它名下已在根层显示的行收进来
        syncChildren(logSave, root, info);
    }

    // 父节点下新增子日志：父已展开则插到子树末尾，父折叠时只刷新父行
    public synchronized void appendChild(LogSave logSave, String parentUid, LogInfo info) {
        if (info == null || info.getUid() == null) return;
        String uid = info.getUid();
        if (nodeMap.containsKey(uid)) return;

        LazyTreeNode parent = parentUid == null ? null : nodeMap.get(parentUid);
        if (parent == null || treeNodes.indexOf(parent) < 0) {
            // 父节点还没上屏（容器日志后落盘）：先当根显示，父节点上来后 syncChildren 会把它移进去
            appendRoot(logSave, info);
            return;
        }

        int parentIndex = treeNodes.indexOf(parent);
        if (!parent.isExpanded()) {
            notifyItemChanged(parentIndex);   // 折叠状态：只更新"有子项"的状态，不动滚动
            return;
        }

        // 复用 LazyTreeNode 已有的同名子节点，避免同一 uid 注册两次
        LazyTreeNode child = null;
        for (TreeNode treeNode : parent.getChildren()) {
            if (treeNode instanceof LazyTreeNode lazy
                    && uid.equals(String.valueOf(lazy.getFlag()))) {
                child = lazy;
                break;
            }
        }
        if (child == null) {
            child = new LazyTreeNode(logSave, uid);
            child.setDepth(parent.getDepth() + 1);
            parent.addChild(child);
        }
        nodeMap.put(uid, child);

        int insertAt = parentIndex + parent.getExpandedChildren().size();
        treeNodes.add(insertAt, child);
        notifyItemInserted(insertAt);
    }

    // 轮询兜底：把 order 里还没上屏的日志补上（漏通知时用，不做整表重建）
    public synchronized void appendMissing(LogSave logSave) {
        java.util.List<String> order = logSave.getOrder();
        for (String uid : order) {
            if (uid == null || nodeMap.containsKey(uid)) continue;
            LogInfo info = logSave.getLog(uid);
            if (info != null) appendRoot(logSave, info);
        }
    }

    // 增量同步：只补新出现的节点，不重建整棵树，已展开的节点不会被打回原状
    public synchronized void syncLog(LogSave logSave, LogInfo info) {
        if (info == null || info.getUid() == null) return;

        String uid = info.getUid();
        LazyTreeNode node = nodeMap.get(uid);
        if (node == null) {
            // 没见过的根节点，追加到末尾
            if (!isRootIndex(uid, logSave)) return;
            // 同一个 uid 只能上屏一次：已存在节点（无论根还是子）就不再建根
            if (nodeMap.containsKey(uid)) return;
            LazyTreeNode root = new LazyTreeNode(logSave, uid);
            nodeMap.put(uid, root);
            Object rootData = root.getData();
            long rootTime = rootData instanceof LogInfo rootInfo ? rootInfo.getTime() : 0L;
            int insertIndex = treeNodes.size();
            for (int i = treeNodes.size() - 1; i >= 0; i--) {
                Object data = treeNodes.get(i).getData();
                long time = data instanceof LogInfo info2 ? info2.getTime() : 0L;
                if (time <= rootTime) break;
                insertIndex = i;
            }
            treeNodes.add(insertIndex, root);
            notifyItemInserted(insertIndex);
            if (insertIndex + 1 < treeNodes.size()) {
                notifyItemRangeChanged(insertIndex + 1, treeNodes.size() - insertIndex - 1);
            }
            // 首次上屏的节点可能已经带 children（如并行执行容器先被写入），立即补齐
            syncChildren(logSave, root, info);
            return;
        }
        syncChildren(logSave, node, info);
    }

    // 补齐某个节点下新增的子节点
    private void syncChildren(LogSave logSave, LazyTreeNode node, LogInfo info) {
        List<?> flags = info.getChildrenFlags();
        List<TreeNode> children = node.getChildren();
        if (flags.size() <= children.size()) return;

        List<TreeNode> added = new ArrayList<>();
        for (int i = children.size(); i < flags.size(); i++) {
            Object flag = flags.get(i);
            String childUid0 = String.valueOf(flag);
            // 该子节点若已作为根行显示，先移出根层，再挂到父节点下（否则会内外各一条）
            LazyTreeNode existChild = nodeMap.get(childUid0);
            if (existChild != null) {
                int existIndex = treeNodes.indexOf(existChild);
                if (existIndex >= 0) {
                    treeNodes.remove(existIndex);
                    notifyItemRemoved(existIndex);
                }
                existChild.setDepth(node.getDepth() + 1);
                node.addChild(existChild);
                continue;
            }
            LazyTreeNode child = new LazyTreeNode(logSave, flag);
            child.setDepth(node.getDepth() + 1);
            node.addChild(child);
            added.add(child);
            nodeMap.put(String.valueOf(flag), child);
        }

        int index = treeNodes.indexOf(node);
        if (index < 0) return;
        // 折叠状态下子项不显示，只刷新这一行
        if (!node.isExpanded()) {
            notifyItemChanged(index);
            return;
        }

        // 插到该节点已展开子树的末尾，保持层级顺序
        int insertIndex = index + 1;
        for (int i = index + 1; i < treeNodes.size(); i++) {
            if (treeNodes.get(i).getDepth() <= node.getDepth()) break;
            insertIndex = i + 1;
        }
        treeNodes.addAll(insertIndex, added);
        notifyItemRangeInserted(insertIndex, added.size());
    }

    // 兜底同步根节点：只追加没见过的，不重建
    public void syncRoots(LogSave logSave) {
        // 复用 syncLog 的落点逻辑：带父 uid 的挂到父节点，其余才当根
        int count = logSave.getLogCount();
        for (int i = 0; i < count; i++) {
            String uid = logSave.getUidAt(i);
            if (uid == null || nodeMap.containsKey(uid)) continue;
            LogInfo info = logSave.getLog(uid);
            if (info == null) continue;
            syncLog(logSave, info);
        }
    }

    // 没有搜索词时上下箭头的目标：以当前视口所在行为锚点，取它所在展开块的首行 / 末行
    public int findExpandedEdge(boolean isNext, int anchorPosition) {
        if (treeNodes.isEmpty()) return -1;
        int anchor = Math.max(0, Math.min(anchorPosition, treeNodes.size() - 1));

        TreeNode node = findExpandedBlock(anchor);
        if (node == null) return -1;
        int index = treeNodes.indexOf(node);
        if (index < 0) return -1;

        if (!isNext) {
            // 直接跳到展开节点本身那一行
            return index;
        }

        // 展开块的最后一行：顺着 depth 走完这棵子树
        int end = index;
        for (int i = index + 1; i < treeNodes.size(); i++) {
            if (treeNodes.get(i).getDepth() <= node.getDepth()) break;
            end = i;
        }
        return end > index ? end : -1;
    }

    // 从锚点行往上找它所属的展开块头：自身展开就取自身，否则找最近的已展开祖先
    private TreeNode findExpandedBlock(int position) {
        TreeNode current = treeNodes.get(position);
        if (current.isExpanded()) return current;

        int depth = current.getDepth();
        for (int i = position - 1; i >= 0 && depth > 0; i--) {
            TreeNode candidate = treeNodes.get(i);
            if (candidate.getDepth() >= depth) continue;
            if (candidate.isExpanded()) return candidate;
            depth = candidate.getDepth();
        }
        return null;
    }

    private boolean isRootIndex(String uid, LogSave logSave) {
        // key 已是 GUID，只确认这条日志存在
        return uid != null && logSave.getLog(uid) != null;
    }

    public List<LogInfo> getLogs() {
        return getLogs(treeNodes);
    }

    private List<LogInfo> getLogs(List<TreeNode> treeNodes) {
        List<LogInfo> logs = new ArrayList<>();
        for (TreeNode treeNode : treeNodes) {
            LazyTreeNode node = (LazyTreeNode) treeNode;
            logs.add((LogInfo) node.getData());
            logs.addAll(getLogs(node.getChildren()));
        }
        return logs;
    }

    public void addLog(LogSave logSave, LogInfo log) {
        TreeNode node = new LazyTreeNode(logSave, log.getUid());
        addTreeNode(node);
    }

    public int searchLog(String text, Boolean isNext) {
        if (text.isEmpty()) {
            searchIndex = -1;
            return searchIndex;
        }
        if (isNext == null) {
            searchIndex = -1;
            isNext = true;
        }
        List<TreeNode> subList;
        if (searchIndex < 1 || searchIndex >= treeNodes.size()) {
            subList = treeNodes;
        } else {
            if (isNext) {
                subList = treeNodes.subList(searchIndex + 1, treeNodes.size());
            } else {
                subList = treeNodes.subList(0, searchIndex - 1);
            }
        }

        if (searchIndex >= 0 && searchIndex < treeNodes.size() - 1) {
            int index = searchIndex;
            searchIndex = -1;
            notifyItemChanged(index);
        }

        TreeNode treeNode = findTreeNode(subList, text, isNext);
        if (treeNode != null) {
            expandNode(treeNode);
            searchIndex = treeNodes.indexOf(treeNode);
            notifyItemChanged(searchIndex);
        }
        return searchIndex;
    }

    private TreeNode findTreeNode(List<TreeNode> treeNodes, String text, boolean isNext) {
        boolean pinyinMatch = SettingSaver.BLUEPRINT_SEARCH_WITH_PINYIN.get();
        boolean posMatch = SettingSaver.BLUEPRINT_SEARCH_WITH_POSITION.get();
        boolean caseMatch = SettingSaver.BLUEPRINT_SEARCH_WITH_CASE.get();
        boolean regexMatch = SettingSaver.BLUEPRINT_SEARCH_WITH_REGEX.get();

        if (!caseMatch) text = text.toLowerCase();
        Pattern pattern = null;
        if (regexMatch) pattern = AppUtil.getPattern(text);

        if (isNext) {
            for (int i = 0; i < treeNodes.size(); i++) {
                TreeNode treeNode = treeNodes.get(i);

                LogInfo logInfo = (LogInfo) treeNode.getData();
                if (logInfo == null) continue;
                String logString = logInfo.getLog();
                if (!caseMatch) logString = logString.toLowerCase();
                if (pattern == null) {
                    if (logString.contains(text)) return treeNode;
                } else {
                    if (pattern.matcher(logString).find()) return treeNode;
                }
                if (pinyinMatch) {
                    if (AppUtil.isPinyinContains(logString, text)) return treeNode;
                }

                Log log = logInfo.getLogObject();
                if (log instanceof ActionLog actionLog) {
                    Task actionTask = TaskSaver.getInstance().downFindTask(task, actionLog.getTaskId());
                    if (actionTask == null) continue;
                    Action action = actionTask.getAction(actionLog.getActionId());
                    if (action == null) continue;

                    if (posMatch) {
                        Point pos = action.getPos();
                        String posString = pos.x + "," + pos.y;
                        if (pattern == null) {
                            if (posString.contains(text)) return treeNode;
                        } else {
                            if (pattern.matcher(posString).find()) return treeNode;
                        }
                    }

                    String fullDescription = action.getFullDescription();
                    if (!caseMatch) fullDescription = fullDescription.toLowerCase();
                    if (pattern == null) {
                        if (fullDescription.contains(text)) return treeNode;
                    } else {
                        if (pattern.matcher(fullDescription).find()) return treeNode;
                    }
                }

                if (!treeNode.isExpanded()) {
                    TreeNode tree = findTreeNode(treeNode.getChildren(), text, true);
                    if (tree != null) return tree;
                }
            }
        } else {
            for (int i = treeNodes.size() - 1; i >= 0; i--) {
                TreeNode treeNode = treeNodes.get(i);

                if (!treeNode.isExpanded()) {
                    TreeNode tree = findTreeNode(treeNode.getChildren(), text, false);
                    if (tree != null) return tree;
                }

                LogInfo logInfo = (LogInfo) treeNode.getData();
                if (logInfo == null) continue;
                String logString = logInfo.getLog();
                if (!caseMatch) logString = logString.toLowerCase();
                if (pattern == null) {
                    if (logString.contains(text)) return treeNode;
                } else {
                    if (pattern.matcher(logString).find()) return treeNode;
                }
                if (pinyinMatch) {
                    if (AppUtil.isPinyinContains(logString, text)) return treeNode;
                }

                Log log = logInfo.getLogObject();
                if (log instanceof ActionLog actionLog) {
                    Task actionTask = TaskSaver.getInstance().downFindTask(task, actionLog.getTaskId());
                    if (actionTask == null) continue;
                    Action action = actionTask.getAction(actionLog.getActionId());
                    if (action == null) continue;

                    if (posMatch) {
                        Point pos = action.getPos();
                        String posString = pos.x + "," + pos.y;
                        if (pattern == null) {
                            if (posString.contains(text)) return treeNode;
                        } else {
                            if (pattern.matcher(posString).find()) return treeNode;
                        }
                    }

                    String fullDescription = action.getFullDescription();
                    if (!caseMatch) fullDescription = fullDescription.toLowerCase();
                    if (pattern == null) {
                        if (fullDescription.contains(text)) return treeNode;
                    } else {
                        if (pattern.matcher(fullDescription).find()) return treeNode;
                    }
                }
            }
        }

        return null;
    }

    public class ViewHolder extends TreeAdapter.ViewHolder {
        private final Context context;
        private FloatLogNormalItemBinding normalBinding;
        private FloatLogActionItemBinding actionBinding;
        private FloatLogDateTimeItemBinding dateTimeBinding;

        public ViewHolder(@NonNull FloatLogNormalItemBinding binding) {
            super(LogFloatViewAdapter.this, binding.getRoot());
            context = binding.getRoot().getContext();
            this.normalBinding = binding;

            binding.copyButton.setOnClickListener(v -> {
                LogInfo logInfo = (LogInfo) node.getData();
                if (logInfo == null) return;
                AppUtil.copyToClipboard(context, logInfo.getLog());
            });
        }

        public ViewHolder(@NonNull FloatLogActionItemBinding binding) {
            super(LogFloatViewAdapter.this, binding.getRoot());
            context = binding.getRoot().getContext();
            this.actionBinding = binding;

            binding.gotoButton.setOnClickListener(v -> {
                if (node == null) return;
                LogInfo logInfo = (LogInfo) node.getData();
                if (logInfo == null) return;
                Log log = logInfo.getLogObject();
                if (log instanceof ActionLog actionLog) {
                    Task topTask = TaskSaver.getInstance().getTask(actionLog.getTopTaskId());
                    if (topTask != null) {
                        Task currTask = topTask.downFindTask(actionLog.getTaskId());
                        if (currTask != null) {
                            Action action = currTask.getAction(actionLog.getActionId());
                            if (action != null) {
                                if (!BlueprintView.tryFocusAction(currTask, action)) {
                                    Intent intent = new Intent(context, MainActivity.class);
                                    intent.setAction(MainActivity.INTENT_KEY_OPEN_TASK);
                                    intent.putExtra(InstantActivity.TASK_ID, task.getId());
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    context.startActivity(intent);
                                }
                            }
                        }
                    }
                    if (searchIndex != -1) {
                        int index = searchIndex;
                        searchIndex = -1;
                        notifyItemChanged(index);
                    }
                    searchIndex = getBindingAdapterPosition();
                    notifyItemChanged(searchIndex);
                }
            });

            binding.expandButton.setOnClickListener(v -> switchNodeExpand(node));
        }

        public ViewHolder(@NonNull FloatLogDateTimeItemBinding binding) {
            super(LogFloatViewAdapter.this, binding.getRoot());
            context = binding.getRoot().getContext();
            this.dateTimeBinding = binding;
        }

        @Override
        public void onClicked(View view) {
            super.onClicked(view);
            // 用户手动展开/折叠后通知外部，日志窗口据此暂停自动跟随
            if (onNodeToggled != null) onNodeToggled.run();
            if (searchIndex != -1) {
                int index = searchIndex;
                searchIndex = -1;
                notifyItemChanged(index);
            }
            searchIndex = -1;
            notifyItemChanged(getBindingAdapterPosition());
        }

        @SuppressLint("SetTextI18n")
        public void refresh(TreeNode node) {
            super.refresh(node);

            LogInfo logInfo = (LogInfo) node.getData();
            if (logInfo == null) {
                // 排查：为什么这条日志加载失败（key / 原始长度 / 头部内容）
                try {
                    String dbgUid = node instanceof LazyTreeNode lazyNode ? String.valueOf(lazyNode.getFlag()) : "?";
                    String raw = debugLogSave == null ? null : debugLogSave.debugRaw(dbgUid);
                } catch (Throwable ignored) {
                }
                String error = context.getString(R.string.task_running_log_load_error);

                if (normalBinding != null) {
                    normalBinding.title.setText(error);
                    normalBinding.time.setText("");
                }
                if (actionBinding != null) {
                    actionBinding.title.setText(error);
                    actionBinding.time.setText("");
                }
                if (dateTimeBinding != null) {
                    dateTimeBinding.title.setText("");
                }
                return;
            }

            Log log = logInfo.getLogObject();
            this.node = node;

            if (log instanceof NormalLog normalLog && normalBinding != null) {
                normalBinding.title.setText(normalLog.getLog());
                normalBinding.time.setText(logInfo.getTime(context));
            }

            if (log instanceof ActionLog actionLog && actionBinding != null) {
                Action action = null;
                Task topTask = TaskSaver.getInstance().getTask(actionLog.getTopTaskId());
                if (topTask != null) {
                    Task currTask = topTask.downFindTask(actionLog.getTaskId());
                    if (currTask != null) {
                        action = currTask.getAction(actionLog.getActionId());
                    }
                }
                actionBinding.gotoButton.setVisibility(action != null ? View.VISIBLE : View.GONE);
                int size = logInfo.getChildrenFlags().size();
                actionBinding.expandButton.setVisibility(size > 0 ? View.VISIBLE : View.INVISIBLE);
                actionBinding.expandButton.setIconResource(node.isExpanded() ? R.drawable.icon_keyboard_arrow_down : R.drawable.icon_keyboard_arrow_right);
                actionBinding.title.setText(actionLog.getLog());
                actionBinding.time.setText(logInfo.getTime(context));
                actionBinding.icon.setImageResource(actionLog.isExecute() ? R.drawable.icon_shuffle : R.drawable.icon_equal);

                if (action != null) {
                    actionBinding.valueBox.removeAllViews();
                    Point pos = action.getPos();
                    actionBinding.time.setText(logInfo.getTime(context) + " " + pos.x + "," + pos.y);
                    Action currentAction = action;
                    actionLog.getValues().forEach((key, value) -> {
                        Pin pinById = currentAction.getPinById(key);
                        if (pinById == null) return;
                        FloatLogActionValueItemBinding itemBinding = FloatLogActionValueItemBinding.inflate(LayoutInflater.from(context), actionBinding.valueBox, true);
                        itemBinding.title.setText(pinById.getTitle());
                        if (value instanceof PinImage pinImage) {
                            itemBinding.image.setImageBitmap(pinImage.getImage());
                        } else {
                            itemBinding.content.setText(value.toString());
                        }
                    });
                }
                actionBinding.valueCard.setVisibility(searchIndex == getBindingAdapterPosition() ? View.VISIBLE : View.GONE);
            }

            if (log instanceof DateTimeLog dateTimeLog && dateTimeBinding != null) {
                dateTimeBinding.title.setText(dateTimeLog.getLog());
            }

            if (itemView instanceof MaterialCardView cardView) {
                if (searchIndex == getBindingAdapterPosition()) {
                    cardView.setCardBackgroundColor(DisplayUtil.getAttrColor(context, com.google.android.material.R.attr.colorTertiaryContainer));
                } else {
                    cardView.setCardBackgroundColor(DisplayUtil.getAttrColor(context, com.google.android.material.R.attr.colorSurfaceContainerHighest));
                }
            }
        }
    }
}
