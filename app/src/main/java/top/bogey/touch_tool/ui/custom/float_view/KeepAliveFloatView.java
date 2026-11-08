package top.bogey.touch_tool.ui.custom.float_view;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.DynamicColors;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.bean.save.setting.SettingSaver;
import top.bogey.touch_tool.service.ITaskListener;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.BaseActivity;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.EAnchor;
import top.bogey.touch_tool.utils.float_window_manager.FloatInterface;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

public class KeepAliveFloatView extends FrameLayout implements FloatInterface, ITaskListener {
    private final Handler handler;

    private final boolean darkMode;

    public KeepAliveFloatView(@NonNull Context context) {
        super(context);
        handler = new Handler(Looper.getMainLooper());
        MainAccessibilityService service = MainApplication.getInstance().getService();
        service.addListener(this);

        MaterialCardView cardView = new MaterialCardView(context);
        float px = DisplayUtil.dp2px(context, 4);
        DisplayUtil.setViewWidth(cardView, (int) px);
        DisplayUtil.setViewHeight(cardView, (int) px);
        cardView.setRadius(px / 2);
        cardView.setStrokeWidth(0);
        cardView.setCardBackgroundColor(DisplayUtil.getAttrColor(context, com.google.android.material.R.attr.colorSurfaceVariant));
        addView(cardView);

        setAlpha(0);

        darkMode = (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private void showMe() {
        boolean startTips = SettingSaver.TASK_RUNNING_TIPS.get();
        if (startTips) {
            post(() -> {
                animate().alpha(0.5f);
                handler.removeCallbacksAndMessages(null);
                handler.postDelayed(this::hideMe, 1500);
            });
        }
    }

    private void hideMe() {
        animate().alpha(0);
    }

    @Override
    public void onStart(TaskRunnable runnable) {
        showMe();
    }

    @Override
    public void show() {
        MainAccessibilityService service = MainApplication.getInstance().getService();

        FloatWindow.with(service)
                .setLayout(this)
                .setTag(KeepAliveFloatView.class.getName())
                .setSpecial(true)
                .setDragAble(false)
                .setAnimator(null)
                .setLocation(EAnchor.TOP_CENTER, 0, (int) DisplayUtil.dp2px(getContext(), 2))
                .show();
        showMe();
    }

    @Override
    public void dismiss() {
        // 先停掉延时任务，避免窗口关闭后回调仍在跑
        handler.removeCallbacksAndMessages(null);
        FloatWindow.dismiss(KeepAliveFloatView.class.getName());
    }

    public Context getThemeContext() {
        Context context = getContext();
        if (context instanceof BaseActivity || context instanceof ContextThemeWrapper) return context;
        return DynamicColors.wrapContextIfAvailable(context);
    }

    public boolean isDarkMode() {
        return darkMode;
    }
}
