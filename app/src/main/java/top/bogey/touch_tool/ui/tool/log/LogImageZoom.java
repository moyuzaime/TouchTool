package top.bogey.touch_tool.ui.tool.log;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindowHelper;

// 日志里的图片点开后全屏（80%×60%）查看，点任意处或按返回键关闭
// 依据 261114_1909 版 APK 反编译重建（LogImageZoom）
public class LogImageZoom {
    private final static String TAG = LogImageZoom.class.getName();

    private static FrameLayout layer;
    private static ImageView image;

    public static void show(Bitmap bitmap) {
        MainAccessibilityService service;
        if (bitmap == null || bitmap.isRecycled()) return;
        service = MainApplication.getInstance().getService();
        if (service == null) return;

        hide();

        FrameLayout frameLayout = new FrameLayout(service);
        frameLayout.setBackgroundColor(Color.argb(220, 0, 0, 0));
        frameLayout.setFocusableInTouchMode(true);
        frameLayout.setOnClickListener(v -> hide());
        frameLayout.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
                hide();
                return true;
            }
            return false;
        });

        ImageView imageView = new ImageView(service);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setImageBitmap(bitmap);
        DisplayMetrics metrics = service.getResources().getDisplayMetrics();
        frameLayout.addView(imageView, new FrameLayout.LayoutParams(
                (int) (metrics.widthPixels * 0.8f),
                (int) (metrics.heightPixels * 0.6f),
                Gravity.CENTER));

        boolean shown = FloatWindow.with(service)
                .setLayout(frameLayout)
                .setTag(TAG)
                .setSpecial(true)
                .setDragAble(false)
                .setSize(-1, -1)
                .show();
        if (!shown) return;

        FloatWindowHelper helper = FloatWindow.getHelper(TAG);
        if (helper != null) helper.setFocusable(true, false);
        frameLayout.requestFocus();

        layer = frameLayout;
        image = imageView;
    }

    public static void hide() {
        if (layer == null) return;
        FloatWindow.dismiss(TAG);
        layer = null;
        image = null;
    }
}
