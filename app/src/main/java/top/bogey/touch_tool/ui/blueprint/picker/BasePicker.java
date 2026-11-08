package top.bogey.touch_tool.ui.blueprint.picker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.bean.save.setting.SettingSaver;
import top.bogey.touch_tool.utils.callback.ResultCallback;
import top.bogey.touch_tool.utils.float_window_manager.FloatCallback;
import top.bogey.touch_tool.utils.float_window_manager.FloatInterface;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

@SuppressLint("ViewConstructor")
public class BasePicker<T> extends FrameLayout implements FloatInterface {
    protected final ResultCallback<T> callback;
    protected String tag;
    protected boolean dragAble = true;
    protected boolean editable = false;
    protected FloatCallback floatCallback;

    public BasePicker(@NonNull Context context, ResultCallback<T> callback) {
        super(context);
        this.callback = callback;
        tag = getClass().getName();
        floatCallback = new FloatBaseCallback();
    }

    public void setFloatCallback(FloatCallback floatCallback) {
        this.floatCallback = floatCallback;
    }

    @Override
    public void show() {
        FloatWindow.with(MainApplication.getInstance().getService())
                .setLayout(this)
                .setTag(tag)
                .setDragAble(dragAble)
                .setExistEditText(editable)
                .setHideByScreenshot(SettingSaver.MANUAL_PLAY_VIEW_HIDE_WHEN_SCREENSHOT.get())
                .setCallback(floatCallback)
                .show();
    }

    @Override
    public void dismiss() {
        FloatWindow.dismiss(tag);
    }

    @Override
    public String getTag() {
        return tag;
    }
}
