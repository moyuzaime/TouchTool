package top.bogey.touch_tool.ui.custom.float_view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.other.NodeInfo;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinNodePathString;
import top.bogey.touch_tool.databinding.FloatNodeInfoBinding;
import top.bogey.touch_tool.databinding.FloatNodeInfoItemBinding;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.EAnchor;
import top.bogey.touch_tool.utils.callback.ResultCallback;
import top.bogey.touch_tool.utils.float_window_manager.FloatInterface;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

@SuppressLint("ViewConstructor")
public class NodeInfoFloatView extends FrameLayout implements FloatInterface {
    private final ResultCallback<NodeInfo> callback;
    private final FloatNodeInfoBinding binding;
    private final NodeInfoFloatViewAdapter adapter;

    private NodeInfo nodeInfo;

    private int width = 0, height = 0;
    private boolean expanded = true;

    public static void showInfo(NodeInfo nodeInfo, ResultCallback<NodeInfo> callback) {
        KeepAliveFloatView keepView = (KeepAliveFloatView) FloatWindow.getView(KeepAliveFloatView.class.getName());
        if (keepView == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            NodeInfoFloatView nodeInfoView = (NodeInfoFloatView) FloatWindow.getView(NodeInfoFloatView.class.getName());
            if (nodeInfoView == null) {
                nodeInfoView = new NodeInfoFloatView(keepView.getThemeContext(), callback);
                nodeInfoView.show();
            }
            nodeInfoView.innerShowToast(nodeInfo);
        });
    }

    private NodeInfoFloatView(@NonNull Context context, ResultCallback<NodeInfo> callback) {
        super(context);
        this.callback = callback;
        binding = FloatNodeInfoBinding.inflate(LayoutInflater.from(context), this, true);
        adapter = new NodeInfoFloatViewAdapter();
        binding.recyclerView.setAdapter(adapter);

        binding.parentButton.setOnClickListener(v -> {
            NodeInfo parent = nodeInfo.getParent();
            if (parent == null) return;
            innerShowToast(parent);
        });
        binding.childButton.setOnClickListener(v -> {
            if (nodeInfo.getChildCount() == 0) return;
            innerShowToast(nodeInfo.getChild(0));
        });
        binding.preButton.setOnClickListener(v -> {
            NodeInfo parent = nodeInfo.getParent();
            if (parent == null || nodeInfo.index <= 1) return;
            innerShowToast(parent.getChild(nodeInfo.index - 2));
        });
        binding.nextButton.setOnClickListener(v -> {
            NodeInfo parent = nodeInfo.getParent();
            if (parent == null || nodeInfo.index == parent.getChildCount()) return;
            innerShowToast(parent.getChild(nodeInfo.index));
        });

        binding.closeButton.setOnClickListener(v -> dismiss());

        binding.expandButton.setOnClickListener(v -> {
            expanded = !expanded;
            if (expanded) {
                binding.contentBox.setVisibility(VISIBLE);
                int margin = (int) DisplayUtil.dp2px(context, 8);
                DisplayUtil.setViewMargin(binding.expandButton, margin, 0, 0, 0);
                ViewGroup.LayoutParams params = binding.getRoot().getLayoutParams();
                params.width = width;
                params.height = height;
                binding.getRoot().setLayoutParams(params);
                binding.expandButton.setIconResource(R.drawable.icon_zoom_in_map);
            } else {
                binding.contentBox.setVisibility(GONE);
                DisplayUtil.setViewMargin(binding.expandButton, 0, 0, 0, 0);
                ViewGroup.LayoutParams params = binding.getRoot().getLayoutParams();
                width = params.width;
                height = params.height;
                params.width = binding.expandButton.getWidth();
                params.height = binding.expandButton.getHeight();
                binding.getRoot().setLayoutParams(params);
                binding.expandButton.setIconResource(R.drawable.icon_zoom_out_map);

            }
            FloatWindow.updateLayoutParam(NodeInfoFloatView.class.getName());
        });
    }

    // 节点层级深度：从当前节点往上数父节点
    private int getNodeDepth(NodeInfo nodeInfo) {
        int depth = 0;
        NodeInfo parent = nodeInfo.getParent();
        while (parent != null) {
            depth++;
            parent = parent.getParent();
        }
        return depth;
    }
    private void innerShowToast(NodeInfo nodeInfo) {
        this.nodeInfo = nodeInfo;
        if (nodeInfo == null) return;
        NodeInfo parent = nodeInfo.getParent();
        binding.parentButton.setVisibility(parent == null ? INVISIBLE : VISIBLE);
        binding.childButton.setVisibility(nodeInfo.getChildCount() == 0 ? INVISIBLE : VISIBLE);
        binding.preButton.setVisibility(parent == null || nodeInfo.index <= 1 ? INVISIBLE : VISIBLE);
        binding.nextButton.setVisibility(parent == null || nodeInfo.index == parent.getChildCount() ? INVISIBLE : VISIBLE);

        List<NodeInfoContent> contents = new ArrayList<>();
        String[] array = getResources().getStringArray(R.array.node_info_type);
        for (int i = 0; i < array.length; i++) {
            String content = switch (i) {
                case 0 -> nodeInfo.clazz;
                case 1 -> nodeInfo.id != null ? nodeInfo.id : "";
                case 2 -> nodeInfo.toString();
                case 3 -> nodeInfo.text != null ? nodeInfo.text : "";
                case 4 -> nodeInfo.desc != null ? nodeInfo.desc : "";
                case 5 -> String.valueOf(nodeInfo.index);
                case 6 -> String.valueOf(nodeInfo.usable);
                case 7 -> String.valueOf(nodeInfo.visible);
                case 8 -> getContext().getString(R.string.area_left) + ": " + nodeInfo.area.left + ", "
                        + getContext().getString(R.string.area_top) + ": " + nodeInfo.area.top + ", "
                        + getContext().getString(R.string.area_right) + ": " + nodeInfo.area.right + ", "
                        + getContext().getString(R.string.area_bottom) + ": " + nodeInfo.area.bottom;
                case 9 -> nodeInfo.area.width() + " × " + nodeInfo.area.height();
                case 10 -> parent == null ? "" : parent.toString();
                case 11 -> String.valueOf(nodeInfo.getChildCount() != 0);
                case 12 -> nodeInfo.node == null ? "" : String.valueOf(nodeInfo.node.getPackageName());
                case 13 -> String.valueOf(nodeInfo.node != null && nodeInfo.node.isLongClickable());
                case 14 -> String.valueOf(nodeInfo.node != null && nodeInfo.node.isScrollable());
                case 15 -> String.valueOf(nodeInfo.node != null && nodeInfo.node.isFocusable());
                case 16 -> String.valueOf(nodeInfo.node != null && nodeInfo.node.isEditable());
                case 17 -> String.valueOf(nodeInfo.node != null && nodeInfo.node.isSelected());
                case 18 -> String.valueOf(nodeInfo.node != null && nodeInfo.node.isChecked());
                case 19 -> String.valueOf(nodeInfo.node != null && nodeInfo.node.isFocused());
                case 20 -> String.valueOf(nodeInfo.node != null && nodeInfo.node.isEnabled());
                case 21 -> nodeInfo.node == null || nodeInfo.node.getHintText() == null ? "" : nodeInfo.node.getHintText().toString();
                case 22 -> String.valueOf(getNodeDepth(nodeInfo));
                case 23 -> {
                    if (nodeInfo.node == null) yield "";
                    java.util.List<android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction> actions = nodeInfo.node.getActionList();
                    yield String.valueOf(actions == null ? 0 : actions.size());
                }
                case 24 -> nodeInfo.node == null ? "" : String.valueOf(nodeInfo.node.getWindowId());
                case 25 -> nodeInfo.node == null ? "" : String.valueOf(nodeInfo.node.getInputType());
                default -> "";
            };
            String copyValue = switch (i) {
                case 2 -> {
                    PinNodePathString pathString = new PinNodePathString();
                    pathString.setValue(nodeInfo);
                    yield pathString.getValue();
                }
                case 7 -> nodeInfo.area.toString();
                case 10 -> {
                    if (parent == null) yield "";
                    PinNodePathString pathString = new PinNodePathString();
                    pathString.setValue(parent);
                    yield pathString.getValue();
                }
                default -> content;
            };
            contents.add(new NodeInfoContent(array[i], content, copyValue));
        }
        adapter.setContents(contents);
        callback.onResult(nodeInfo);
    }

    @Override
    public void show() {
        FloatWindow.with(MainApplication.getInstance().getService())
                .setLayout(this)
                .setTag(NodeInfoFloatView.class.getName())
                .setLocation(EAnchor.CENTER, 0, 0)
                .setSpecial(true)
                .show();
    }

    @Override
    public void dismiss() {
        FloatWindow.dismiss(NodeInfoFloatView.class.getName());
    }

    private static class NodeInfoFloatViewAdapter extends RecyclerView.Adapter<NodeInfoFloatViewViewHolder> {
        private List<NodeInfoContent> contents = new ArrayList<>();

        @NonNull
        @Override
        public NodeInfoFloatViewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new NodeInfoFloatViewViewHolder(FloatNodeInfoItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull NodeInfoFloatViewViewHolder holder, int position) {
            holder.refresh(contents.get(position));
        }

        @Override
        public int getItemCount() {
            return contents.size();
        }

        public void setContents(List<NodeInfoContent> contents) {
            int size = this.contents.size();
            this.contents = contents;
            if (size == 0) notifyItemRangeInserted(0, contents.size());
            else notifyItemRangeChanged(0, size);
        }
    }

    private static class NodeInfoFloatViewViewHolder extends RecyclerView.ViewHolder {
        private final FloatNodeInfoItemBinding binding;
        private NodeInfoContent content;

        public NodeInfoFloatViewViewHolder(@NonNull FloatNodeInfoItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.copyButton.setOnClickListener(v -> AppUtil.copyToClipboard(itemView.getContext(), content.copyValue));
        }

        public void refresh(NodeInfoContent content) {
            this.content = content;
            binding.title.setText(content.key);
            binding.content.setText(content.value);
        }
    }

    private record NodeInfoContent(String key, String value, String copyValue) {
    }
}
