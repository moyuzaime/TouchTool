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
    // uid -> 鑺傜偣锛屽閲忔彃鍏ユ椂鎸?uid 瀹氫綅锛岄伩鍏嶆暣琛ㄩ噸寤?
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

    // 鍒嗛〉绐楀彛锛氫竴娆″彧鍔犺浇鏈€鍚?N 鏉℃牴鏃ュ織锛屾洿鏃╃殑璧?loadMore()
    private final static int PAGE_SIZE = 200;

    private String storeTaskId;
    private LogStoreNodeLoader storeLoader;
    private int pageCount = 1;
    // 灞曞紑鐘舵€佹寜 uid 璁颁綇锛氬垎椤?閲嶅缓鍚庢仮澶?
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

    public void setTask(String taskId) {
        pageCount = 1;
        reload(taskId);
    }

    // 鍔犺浇鏇村锛氱獥鍙ｆ墿澶т竴鍊嶅悗閲嶈浇锛堟棩蹇楁寜鏃堕棿閫掑锛屽巻鍙查」鍦ㄥ垪琛ㄥご閮級
    public synchronized void loadMore() {
        if (storeTaskId == null) return;
        pageCount++;
        reload(storeTaskId);
    }

    private void reload(String taskId) {
        storeTaskId = taskId;
        storeLoader = new LogStoreNodeLoader(taskId);
        task = TaskSaver.getInstance().getTask(taskId);
        // 璁颁綇褰撳墠灞曞紑鐨勮妭鐐癸紝閲嶅缓鍚庢仮澶?
        for (TreeNode node : new ArrayList<>(treeNodes)) {
            if (node instanceof LazyTreeNode lazy && node.isExpanded()) {
                expandedUids.add(String.valueOf(lazy.getFlag()));
            }
        }
        nodeMap.clear();
        List<TreeNode> nodeList = new ArrayList<>();
        for (String uid : topbogeyLogStore().rootUids(taskId, PAGE_SIZE * pageCount)) {
            LazyTreeNode node = new LazyTreeNode(storeLoader, uid);
            node.setDepth(0);
            nodeMap.put(uid, node);
            nodeList.add(node);
        }
        applyTreeNodes(nodeList);
        restoreExpanded();
    }

    private top.bogey.touch_tool.bean.save.log.LogStore topbogeyLogStore() {
        return top.bogey.touch_tool.bean.save.log.LogStore.getInstance();
    }

    // 鏁磋〃鏇挎崲鐢ㄤ竴娆″埛鏂帮細TreeAdapter.setTreeNodes 鏄€屽厛鍒犵┖鍐嶆彃鍏ャ€嶏紝
    // 楂橀閲嶅缓鏃朵細闂嚭涓€甯х┖鍒楄〃
    private void applyTreeNodes(List<TreeNode> nodes) {
        treeNodes.clear();
        treeNodes.addAll(nodes);
        notifyDataSetChanged();
    }

    // 閲嶅缓鍚庢寜 uid 鎭㈠灞曞紑鐘舵€侊紱expandNode 浼氭妸瀛愯妭鐐规彃鍏ュ垪琛紝鎵€浠ョ敤鎸夌储寮曟帹杩涚殑寰幆
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

    // ===== 澧為噺杩藉姞锛氭柊鏃ュ織涓嶅啀鏁磋〃閲嶅缓锛屽彧鍦ㄦ湯灏炬彃鍏ヤ竴琛岋紝婊氬姩浣嶇疆鐢?RecyclerView 鑷淇濇寔 =====

    // 鏂版牴鏃ュ織锛氱洿鎺ヨ拷鍔犲埌鍒楄〃鏈熬
    // 父节点还没上屏时先挂起，等父落库后再挂进去（以前是降级成根行，导致跑任务时层级错乱）
    // ===== 方案 B：增量只做「按库取结构 + 差额合并」，与重开窗口吃同一条还原路径 =====
    // 通知里只关心"有新日志"，结构一律回库里查（parent_uid + id 顺序），不再手工算插入位置

    public synchronized void syncFromStore(String taskId) {
        if (storeLoader == null || taskId == null) return;
        java.util.List<TreeNode> desired = buildDesiredRows(taskId);
        int prefix = 0;
        int min = Math.min(desired.size(), treeNodes.size());
        while (prefix < min && sameRow(treeNodes.get(prefix), desired.get(prefix))) prefix++;
        if (prefix == treeNodes.size() && prefix == desired.size()) return;   // 没有变化，不动列表

        if (prefix < treeNodes.size()) {
            int removed = treeNodes.size() - prefix;
            treeNodes.subList(prefix, treeNodes.size()).clear();
            notifyItemRangeRemoved(prefix, removed);
        }
        java.util.List<TreeNode> tail = desired.subList(prefix, desired.size());
        if (!tail.isEmpty()) {
            treeNodes.addAll(tail);
            notifyItemRangeInserted(prefix, tail.size());
        }
    }

    // 拼出"应该显示的可见行"：根日志 + 已展开节点的子树（顺序来自数据库 id）
    private java.util.List<TreeNode> buildDesiredRows(String taskId) {
        java.util.List<TreeNode> rows = new java.util.ArrayList<>();
        for (String uid : topbogeyLogStore().rootUids(taskId, PAGE_SIZE * pageCount)) {
            if (uid == null) continue;
            rows.add(nodeFor(uid, 0));
            if (expandedUids.contains(uid)) appendChildRows(rows, taskId, uid, 1);
        }
        return rows;
    }

    private void appendChildRows(java.util.List<TreeNode> rows, String taskId, String parentUid, int depth) {
        for (String childUid : topbogeyLogStore().childUids(taskId, parentUid)) {
            if (childUid == null) continue;
            rows.add(nodeFor(childUid, depth));
            if (expandedUids.contains(childUid)) appendChildRows(rows, taskId, childUid, depth + 1);
        }
    }

    // 复用已有节点：保住展开状态，也不会重复注册
    private LazyTreeNode nodeFor(String uid, int depth) {
        LazyTreeNode node = nodeMap.get(uid);
        if (node == null) {
            node = new LazyTreeNode(storeLoader, uid);
            nodeMap.put(uid, node);
        }
        node.setDepth(depth);
        return node;
    }

    private boolean sameRow(TreeNode a, TreeNode b) {
        if (!(a instanceof LazyTreeNode la) || !(b instanceof LazyTreeNode lb)) return false;
        return String.valueOf(la.getFlag()).equals(String.valueOf(lb.getFlag()))
                && la.getDepth() == lb.getDepth();
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
            // 鐢ㄦ埛鎵嬪姩灞曞紑/鎶樺彔鍚庨€氱煡澶栭儴锛屾棩蹇楃獥鍙ｆ嵁姝ゆ殏鍋滆嚜鍔ㄨ窡闅?
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
    // ===== 导航/导出辅助（保留原行为，数据源改为 SQLite 加载器）=====

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

}
