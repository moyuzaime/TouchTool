package top.bogey.touch_tool.ui.blueprint.picker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinColor;
import top.bogey.touch_tool.databinding.FloatPickerColorBinding;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.callback.ResultCallback;

@SuppressLint("ViewConstructor")
public class ColorPicker extends FullScreenPicker<PinColor.ColorInfo> {
    private final FloatPickerColorBinding binding;
    private final PinColor.ColorInfo colorInfo;
    private final Paint bitmapPaint;
    private final Paint markPaint;

    private List<Rect> pickList = null;
    private float currentX, currentY;
    private float lastX, lastY;
    private boolean picking = false;

    public ColorPicker(@NonNull Context context, ResultCallback<PinColor.ColorInfo> callback, PinColor.ColorInfo color) {
        super(context, callback);
        binding = FloatPickerColorBinding.inflate(LayoutInflater.from(context), this, true);
        colorInfo = new PinColor.ColorInfo(color.getColor(), color.getMinArea(), color.getMaxArea());

        bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bitmapPaint.setFilterBitmap(true);
        bitmapPaint.setDither(true);

        markPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markPaint.setStyle(Paint.Style.FILL);
        markPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        binding.backButton.setOnClickListener(v -> dismiss());

        binding.saveButton.setOnClickListener(v -> {
            if (callback != null) callback.onResult(colorInfo);
            dismiss();
        });

        binding.lastColor.setBackgroundColor(color.getColor());

        binding.slider.setLabelFormatter(value -> String.valueOf(Math.round(value)));
        binding.slider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            if (values.size() == 2) {
                colorInfo.setMinArea(values.get(0).intValue());
                colorInfo.setMaxArea(values.get(1).intValue());
            }
            invalidate();
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            if (currentX == 0 && currentY == 0) {
                currentX = x;
                currentY = y;
            }
            picking = false;
            lastX = x;
            lastY = y;
        }

        if (action == MotionEvent.ACTION_MOVE) {
            float dx = x - lastX;
            float dy = y - lastY;
            if (dx == 0 && dy == 0) return true;
            currentX += dx / 5;
            currentY += dy / 5;
            currentX = Math.clamp(currentX, 0, getWidth() - 1);
            currentY = Math.clamp(currentY, 0, getHeight() - 1);
            lastX = x;
            lastY = y;
            picking = true;
        }

        if (action == MotionEvent.ACTION_UP) {
            if (picking) {
                picking = false;
            } else {
                currentX = x;
                currentY = y;
            }
            matchColor((int) currentX, (int) currentY);
        }
        refreshUI();
        return true;
    }

    private void matchColor(int x, int y) {
        Bitmap screenShot = screenInfo.getScreenShot();
        if (screenShot != null) {
            int pixel = screenShot.getPixel(x, y);
            matchColor(pixel, 0, Integer.MAX_VALUE);
        }
    }

    private void matchColor(int color, int minArea, int maxArea) {
        if (screenInfo.getScreenShot() == null) return;

        pickList = DisplayUtil.matchColor(screenInfo.getScreenShot(), color, new Rect(), 80);

        if (pickList != null && !pickList.isEmpty()) {
            colorInfo.setColor(color);

            Rect maxRect = pickList.get(0);
            int max = maxRect.width() * maxRect.height();
            maxArea = Math.min(max, maxArea);
            Rect minRect = pickList.get(pickList.size() - 1);
            int min = minRect.width() * minRect.height();
            minArea = Math.max(min, minArea);

            int[] ints = {minArea, maxArea, min, max};
            Arrays.sort(ints);
            if (ints[0] == ints[3]) ints[3] += 1;

            binding.slider.setValueFrom(0);
            binding.slider.setValueTo(ints[3]);
            binding.slider.setValueFrom(ints[0]);
            binding.slider.setValues((float) ints[1], (float) ints[2]);
        }
    }

    @Override
    protected void realShow() {
        matchColor(colorInfo.getColor(), colorInfo.getMinArea(), colorInfo.getMaxArea());
        if (pickList != null && !pickList.isEmpty()) {
            for (Rect rect : pickList) {
                int size = rect.width() * rect.height();
                if (size >= colorInfo.getMinArea() && size <= colorInfo.getMaxArea()) {
                    currentX = rect.centerX();
                    currentY = rect.centerY();
                    break;
                }
            }
        }
        refreshUI();
    }

    private void refreshUI() {
        binding.slider.setVisibility(pickList == null || pickList.isEmpty() ? GONE : VISIBLE);
        binding.colorBox.setVisibility(currentX == 0 && currentY == 0 ? GONE : VISIBLE);
        binding.colorBox.setX(currentX - binding.colorBox.getWidth() / 2f);
        float px = 3;
        if (currentY + binding.colorBox.getHeight() > getHeight()) {
            binding.colorBox.setScaleY(-1f);
            binding.colorBox.setY(currentY - binding.colorBox.getHeight() + px);
        } else {
            binding.colorBox.setScaleY(1f);
            binding.colorBox.setY(currentY - px);
        }
        Bitmap screenShot = screenInfo.getScreenShot();
        if (screenShot != null && screenShot.getWidth() > currentX && screenShot.getHeight() > currentY) {
            int pixel = screenShot.getPixel((int) currentX, (int) currentY);
            binding.colorPreview.setBackgroundColor(pixel);
            binding.currentColor.setBackgroundColor(pixel);
            colorInfo.setColor(pixel);
        }

        invalidate();
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (picking) {
            float scale = 8;
            int width = (int) (getWidth() / scale);
            int height = (int) (getHeight() / scale);
            Rect area = new Rect((int) (currentX - width / 2f), (int) (currentY - height / 2f), (int) (currentX + width / 2f), (int) (currentY + height / 2f));
            float x = getWidth() / 2f - area.width() * scale / 2 - area.left * scale;
            float y = getHeight() / 2f - area.height() * scale / 2 - area.top * scale;
            canvas.translate(x, y);
            canvas.scale(scale, scale);
        }

        Bitmap screenShot = screenInfo.getScreenShot();
        if (screenShot != null) canvas.drawBitmap(screenShot, 0, 0, bitmapPaint);

        if (!picking) {
            canvas.saveLayer(0, 0, getWidth(), getHeight(), bitmapPaint);
            super.dispatchDraw(canvas);
            if (pickList != null && !pickList.isEmpty()) {
                for (Rect rect : pickList) {
                    int size = rect.width() * rect.height();
                    if (size >= colorInfo.getMinArea() && size <= colorInfo.getMaxArea()) {
                        canvas.drawRect(rect, markPaint);
                    }
                }
            }
            canvas.restore();
            drawChild(canvas, binding.slider, getDrawingTime());
            drawChild(canvas, binding.buttonBox, getDrawingTime());
        }

        drawChild(canvas, binding.colorBox, getDrawingTime());
    }
}
