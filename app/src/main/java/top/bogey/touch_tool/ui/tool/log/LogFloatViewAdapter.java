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
import java.util.List;
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

    public void setLogSave(LogSave logSave) {
        task = TaskSaver.getInstance().getTask(logSave.getKey());

        List<TreeNode> nodeList = new ArrayList<>();
        for (int i = 1; i < logSave.getLogCount() + 1; i++) {
            TreeNode node = new LazyTreeNode(logSave, i);
            nodeList.add(node);
        }
        setTreeNodes(nodeList);
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
}
