package top.bogey.touch_tool.ui.blueprint.picker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.bean.other.ScreenInfo;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.callback.ResultCallback;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

@SuppressLint("ViewConstructor")
public abstract class FullScreenPicker<T> extends BasePicker<T> {
    protected final int[] location = new int[2];

    protected MainAccessibilityService service;
    protected ScreenInfo screenInfo;

    public FullScreenPicker(@NonNull Context context, ResultCallback<T> callback) {
        super(context, callback);
        dragAble = false;
        floatCallback = new FullScreenPickerCallback(this);
        service = MainApplication.getInstance().getService();
    }

    protected abstract void realShow();

    protected void onShow() {
        postDelayed(() -> {
            screenInfo = new ScreenInfo(service);
            Bitmap bitmap = screenInfo.getScreenShot();
            if (bitmap != null) {
                screenInfo.setScreenShot(DisplayUtil.safeClipBitmap(bitmap, location[0], location[1], getWidth(), getHeight()));
            }
            FloatWindow.show(tag);
            realShow();
        }, 300);
    }

    @Override
    public void show() {
        FloatWindow.with(MainApplication.getInstance().getService())
                .setLayout(this)
                .setTag(tag)
                .setDragAble(dragAble)
                .setExistEditText(editable)
                .setSize(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                .setCallback(floatCallback)
                .show();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            getLocationOnScreen(location);
        }
    }

    protected static class FullScreenPickerCallback extends FloatBaseCallback {
        private boolean first = true;
        private final FullScreenPicker<?> picker;

        public FullScreenPickerCallback(FullScreenPicker<?> picker) {
            this.picker = picker;
        }

        @Override
        public void onShow(String tag) {
            if (first) {
                first = false;
                super.onShow(null);
                picker.onShow();
            } else {
                super.onShow(tag);
            }
        }
    }
}
