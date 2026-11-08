package top.bogey.touch_tool.ui.blueprint.picker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.ListPopupWindow;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.databinding.FloatPickerImagePreviewBinding;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.ui.custom.float_view.TouchPathFloatView;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.MatchResult;
import top.bogey.touch_tool.utils.callback.ResultCallback;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

@SuppressLint("ViewConstructor")
public class ImagePickerPreview extends BasePicker<Bitmap> {
    private final FloatPickerImagePreviewBinding binding;
    private Bitmap template;
    private boolean test;
    private int scale = 1;

    public ImagePickerPreview(@NonNull Context context, ResultCallback<Bitmap> callback, Bitmap image) {
        super(context, callback);
        binding = FloatPickerImagePreviewBinding.inflate(LayoutInflater.from(context), this, true);
        template = image;
        binding.current.setImageBitmap(image);

        binding.switchButton.setVisibility(VISIBLE);
        binding.switchButton.setOnClickListener(v -> {
            test = !test;
            binding.title.setText(test ? R.string.picker_test_title : R.string.picker_image_title);
            binding.current.setVisibility(test ? GONE : VISIBLE);
            binding.testBox.setVisibility(test ? VISIBLE : GONE);
            if (test) {
                FloatWindow.hide(tag);
                postDelayed(() -> {
                    matchImage();
                    FloatWindow.show(tag);
                }, 100);
            }
        });

        binding.backButton.setOnClickListener(v -> dismiss());

        binding.saveButton.setOnClickListener(v -> {
            if (callback != null) callback.onResult(template);
            dismiss();
        });

        binding.pickerButton.setOnClickListener(v -> new ImagePicker(context, result -> {
            if (result == null) return;
            template = result;
            binding.current.setImageBitmap(template);
        }, template).show());

        String[] array = getResources().getStringArray(R.array.match_image_scale);
        binding.spinner.setOnClickListener(v -> {
            ListPopupWindow popup = new ListPopupWindow(getContext());
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.widget_textview_item);
            adapter.addAll(array);
            popup.setAdapter(adapter);
            popup.setAnchorView(binding.spinner);
            popup.setModal(true);
            popup.setWidth(DisplayUtil.measureArrayAdapterContentWidth(getContext(), adapter));
            popup.setOnItemClickListener((parent, view, position, id) -> {
                scale = position;
                binding.spinner.setText(array[scale]);
                popup.dismiss();
            });
            popup.show();
        });
        binding.spinner.setText(array[scale]);

        binding.matchButton.setOnClickListener(v -> {
            FloatWindow.hide(tag);
            postDelayed(() -> {
                matchImage();
                FloatWindow.show(tag);
            }, 100);
        });

        binding.touchButton.setOnClickListener(v -> {
            MainAccessibilityService service = MainApplication.getInstance().getService();
            if (service != null && service.isEnabled()) {
                FloatWindow.hide(tag);
                postDelayed(() -> {
                    Bitmap bitmap = service.tryGetScreenShot();
                    FloatWindow.show(tag);
                    if (bitmap != null) {
                        Rect rect = DisplayUtil.matchTemplate(bitmap, template, null, 0, scale, binding.cannySwitch.isChecked());
                        if (rect == null || rect.isEmpty()) return;
                        int x = rect.left + rect.width() / 2;
                        int y = rect.top + rect.height() / 2;
                        service.runGesture(x, y, 50, null);

                        TouchPathFloatView.showGesture(x, y);
                    }
                }, 100);
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void matchImage() {
        MainAccessibilityService service = MainApplication.getInstance().getService();
        if (service == null || !service.isEnabled()) return;
        Bitmap bitmap = service.tryGetScreenShot();
        if (bitmap != null) {
            MatchResult matchResult = DisplayUtil.matchTemplateResult(bitmap, template, null, 0, scale, binding.cannySwitch.isChecked());
            if (matchResult == null) binding.matchedImage.setImageDrawable(null);
            else {
                Rect rect = matchResult.area;
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
                binding.similarity.setText(service.getString(R.string.picker_image_offset, (int) (matchResult.value * 100)));
            }
        }
    }
}
