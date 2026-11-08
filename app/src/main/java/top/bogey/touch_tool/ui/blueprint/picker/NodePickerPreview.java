package top.bogey.touch_tool.ui.blueprint.picker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.other.NodeInfo;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinNodePathString;
import top.bogey.touch_tool.databinding.FloatPickerNodePreviewBinding;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.ui.custom.float_view.MarkTargetFloatView;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.callback.StringResultCallback;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

@SuppressLint("ViewConstructor")
public class NodePickerPreview extends BasePicker<String> {
    private final FloatPickerNodePreviewBinding binding;
    private final PinNodePathString nodePath;
    private boolean test;

    @SuppressLint("DefaultLocale")
    public NodePickerPreview(@NonNull Context context, StringResultCallback callback, String path) {
        super(context, callback);
        binding = FloatPickerNodePreviewBinding.inflate(LayoutInflater.from(context), this, true);
        nodePath = new PinNodePathString(path);

        binding.switchButton.setVisibility(VISIBLE);
        binding.switchButton.setOnClickListener(v -> {
            test = !test;
            binding.title.setText(test ? R.string.picker_test_title : R.string.picker_node_title);
            binding.pathText.setVisibility(test ? GONE : VISIBLE);
            binding.testBox.setVisibility(test ? VISIBLE : GONE);
            if (test) matchImage();
        });

        binding.pathText.setText(path);

        binding.backButton.setOnClickListener(v -> dismiss());

        binding.saveButton.setOnClickListener(v -> {
            if (callback != null) callback.onResult(nodePath.getValue());
            dismiss();
        });

        binding.pickerButton.setOnClickListener(v -> new NodePicker(context, result -> {
            nodePath.setValue(result);
            binding.pathText.setText(nodePath.getSimpleValue(10, true));
        }, nodePath.getValue()).show());

        binding.copyButton.setOnClickListener(v -> AppUtil.copyToClipboard(getContext(), nodePath.getValue()));

        binding.matchButton.setOnClickListener(v -> {
            FloatWindow.hide(tag);
            postDelayed(() -> {
                matchImage();
                FloatWindow.show(tag);
            }, 100);
        });

        binding.touchButton.setOnClickListener(v -> {
            NodeInfo nodeInfo = nodePath.findNode(NodeInfo.getWindows(), true);
            if (nodeInfo == null) return;
            MarkTargetFloatView.showTargetArea(nodeInfo.area);
            nodeInfo.node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        });
    }

    private void matchImage() {
        MainAccessibilityService service = MainApplication.getInstance().getService();
        if (service == null || !service.isEnabled()) return;
        Bitmap bitmap = service.tryGetScreenShot();
        if (bitmap != null) {
            NodeInfo nodeInfo = nodePath.findNode(NodeInfo.getWindows(), true);
            // 控件不存在或区域为空（宽高为零，如已移除的控件）时无法截取，清空预览
            if (nodeInfo == null || nodeInfo.area == null || nodeInfo.area.isEmpty()) {
                binding.matchedImage.setImageDrawable(null);
                return;
            }
            Rect rect = nodeInfo.area;
            int px = (int) DisplayUtil.dp2px(getContext(), 16);
            Rect area = DisplayUtil.safeClipBitmapArea(bitmap, rect.left - px, rect.top - px, rect.width() + px * 2, rect.height() + px * 2);
            if (area == null) return;
            Bitmap clipBitmap = DisplayUtil.safeClipBitmap(bitmap, area.left, area.top, area.width(), area.height());
            if (clipBitmap == null) return;
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStrokeWidth(2);
            paint.setStyle(Paint.Style.STROKE);
            Canvas canvas = new Canvas(clipBitmap);
            canvas.translate(rect.left - area.left, rect.top - area.top);
            canvas.drawRect(new Rect(0, 0, rect.width(), rect.height()), paint);
            binding.matchedImage.setImageBitmap(clipBitmap);
        }
    }
}
