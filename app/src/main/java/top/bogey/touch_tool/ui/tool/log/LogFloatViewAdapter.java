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

    public void setLogSave(LogSave logSave) {
        debugLogSave = logSave;
        task = TaskSaver.getInstance().getTask(logSave.getKey());
        nodeMap.clear();

        // 按 order 遍历，按 children（日志 uid）还原层级
        java.util.Map<String, LogInfo> byUid = new java.util.LinkedHashMap<>();
        for (int i = 0; i < logSave.getLogCount(); i++) {
            String uid = logSave.getUidAt(i);
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
            nodeList.add(buildNode(logSave, uid, byUid, 0));
        }
        setTreeNodes(nodeList);
    }

    private LazyTreeNode buildNode(LogSave logSave, String uid, java.util.Map<String, LogInfo> byUid, int depth) {
        LazyTreeNode node = new LazyTreeNode(logSave, uid);
        node.setDepth(depth);
        nodeMap.put(uid, node);
        LogInfo info = byUid.get(uid);
        if (info == null) return node;
        java.util.Set<String> childSigs = new java.util.HashSet<>();
        for (Object flag : info.getChildrenFlags()) {
            String childUid = String.valueOf(flag);
            if (!byUid.containsKey(childUid)) continue;
            LogInfo childInfo = byUid.get(childUid);
            // 同一动作同一时刻的重复记录只保留一条（历史数据里可能存了两份）
            String sig = childInfo.getTime() + "|" + (childInfo.getLogObject()
                    instanceof top.bogey.touch_tool.bean.other.log.ActionLog a ? a.getActionId() : childUid);
            if (!childSigs.add(sig)) continue;
            node.addChild(buildNode(logSave, childUid, byUid, depth + 1));
        }
        return node;
    }

    // 增量同步：只补新出现的节点，不重建整棵树，已展开的节点不会被打回原状
    public synchronized void syncLog(LogSave logSave, LogInfo info) {
        if (info == null || info.getUid() == null) return;

        String uid = info.getUid();
        String dbgParent = info.getParentLogUid();
        if (dbgParent == null && info.getLogObject() instanceof top.bogey.touch_tool.bean.other.log.ActionLog al0) {
            dbgParent = al0.getParentLogUid();
        }
        LazyTreeNode node = nodeMap.get(uid);
        if (node == null) {
            // 增量：带父 uid 的日志（分支/子动作/内容行）直接挂到父节点下
            String parentUid = dbgParent;
            if (parentUid != null && !parentUid.isEmpty()) {
                LazyTreeNode parentNode = nodeMap.get(parentUid);
                if (parentNode == null) {
                    // 父节点还没上屏（子日志先到）：先把父节点补成根行，再挂子
                    LogInfo parentLog = logSave.getLog(parentUid);
                    if (parentLog != null) {
                        parentNode = new LazyTreeNode(logSave, parentUid);
                        nodeMap.put(parentUid, parentNode);
                        addTreeNode(parentNode);
                    }
                }
                if (parentNode != null) {
                    LogInfo parentInfo = logSave.getLog(parentUid);
                    if (parentInfo != null) {
                        // 父节点下已有同动作同毫秒的子行就不再重复挂
                        for (TreeNode existNode : parentNode.getChildren()) {
                            Object existData = existNode.getData();
                            if (existData instanceof LogInfo existInfo
                                    && existInfo.getTime() == info.getTime()
                                    && existInfo.getLogObject() instanceof top.bogey.touch_tool.bean.other.log.ActionLog ea
                                    && info.getLogObject() instanceof top.bogey.touch_tool.bean.other.log.ActionLog na
                                    && ea.getActionId().equals(na.getActionId())) {
                                return;
                            }
                        }
                        boolean has = false;
                        for (Object flag : parentInfo.getChildrenFlags()) {
                            if (String.valueOf(flag).equals(uid)) {
                                has = true;
                                break;
                            }
                        }
                        if (!has) {
                            parentInfo.addChild(uid);
                            top.bogey.touch_tool.bean.save.log.LogSaver.getInstance()
                                    .updateLog(logSave.getKey(), parentInfo);
                        }
                        syncChildren(logSave, parentNode, parentInfo);
                        return;
                    }
                }
            }

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
