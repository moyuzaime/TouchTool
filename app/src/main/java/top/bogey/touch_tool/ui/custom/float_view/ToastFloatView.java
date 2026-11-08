package top.bogey.touch_tool.ui.custom.float_view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.databinding.FloatToastBinding;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.EAnchor;
import top.bogey.touch_tool.utils.float_window_manager.FloatInterface;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;


@SuppressLint("ViewConstructor")
public class ToastFloatView extends FrameLayout implements FloatInterface {
    private final FloatToastBinding binding;
    private final Handler handler;

    public static void showToast(String msg, EAnchor anchor, EAnchor gravity, Point pos) {
        KeepAliveFloatView keepView = (KeepAliveFloatView) FloatWindow.getView(KeepAliveFloatView.class.getName());
        if (keepView == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            ToastFloatView toastView = (ToastFloatView) FloatWindow.getView(ToastFloatView.class.getName());
            if (toastView == null) {
                toastView = new ToastFloatView(keepView.getThemeContext());
                toastView.show();
            }
            toastView.innerShowToast(msg, anchor, gravity, pos);
        });
    }

    public static void showToast(String msg) {
        Size size = DisplayUtil.getScreenSize(MainApplication.getInstance());
        showToast(msg, EAnchor.BOTTOM_CENTER, EAnchor.BOTTOM_CENTER, new Point(0, -size.getHeight() / 5));
    }

    private ToastFloatView(@NonNull Context context) {
        super(context);
        binding = FloatToastBinding.inflate(LayoutInflater.from(context), this, true);
        handler = new Handler(Looper.getMainLooper());
    }

    private void innerShowToast(String msg, EAnchor anchor, EAnchor gravity, Point pos) {
        binding.title.setText(msg);
        post(() -> FloatWindow.setLocation(ToastFloatView.class.getName(), anchor, gravity, pos));
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::dismiss, 1500);
    }

    @Override
    public void show() {
        Size screenSize = DisplayUtil.getScreenSize(getContext());
        FloatWindow.with(MainApplication.getInstance().getService())
                .setLayout(this)
                .setTag(ToastFloatView.class.getName())
                .setLocation(EAnchor.BOTTOM_CENTER, 0, -screenSize.getHeight() / 5)
                .setSpecial(true)
                .setDragAble(false)
                .show();
    }

    @Override
    public void dismiss() {
        // 先停掉延时任务，避免窗口关闭后回调仍在跑
        handler.removeCallbacksAndMessages(null);
        FloatWindow.dismiss(ToastFloatView.class.getName());
    }
}
