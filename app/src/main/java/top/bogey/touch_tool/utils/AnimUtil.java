package top.bogey.touch_tool.utils;

import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

/**
 * 统一的显隐动画：淡入淡出 + 轻微位移，避免界面元素瞬间出现或消失。
 */
public class AnimUtil {
    public static final long DEFAULT_DURATION = 180L;

    // fromDp 为正表示从下方浮入，为负表示从上方落下来
    public static void show(View view, float fromDp) {
        show(view, fromDp, DEFAULT_DURATION);
    }

    public static void show(View view, float fromDp, long duration) {
        if (view == null) return;
        view.animate().cancel();
        if (view.getVisibility() == View.VISIBLE && view.getAlpha() == 1f
                && view.getTranslationY() == 0f) {
            return;
        }
        view.setVisibility(View.VISIBLE);
        view.setAlpha(0f);
        view.setTranslationY(DisplayUtil.dp2px(view.getContext(), fromDp));
        view.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(duration)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    public static void hide(View view, float toDp) {
        hide(view, toDp, DEFAULT_DURATION, null);
    }

    public static void hide(View view, float toDp, long duration) {
        hide(view, toDp, duration, null);
    }

    public static void hide(View view, float toDp, long duration, Runnable onEnd) {
        if (view == null) {
            if (onEnd != null) onEnd.run();
            return;
        }
        view.animate().cancel();
        if (view.getVisibility() != View.VISIBLE) {
            if (onEnd != null) onEnd.run();
            return;
        }
        view.animate()
                .alpha(0f)
                .translationY(DisplayUtil.dp2px(view.getContext(), toDp))
                .setDuration(duration)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    view.setVisibility(View.GONE);
                    view.setAlpha(1f);
                    view.setTranslationY(0f);
                    if (onEnd != null) onEnd.run();
                })
                .start();
    }

    // 两个视图交替显示：先淡出旧的，再淡入新的
    public static void swap(View hideView, float hideToDp, View showView, float showFromDp) {
        hide(hideView, hideToDp);
        show(showView, showFromDp);
    }

    /**
     * 串行交替：旧的完全收走之后，新的才浮出来。
     * 两条同位置的工具栏用这个，避免交叉淡入淡出时短暂重叠成一块。
     */
    public static void swapSequential(View hideView, float hideToDp, View showView, float showFromDp) {
        hide(hideView, hideToDp, DEFAULT_DURATION, () -> show(showView, showFromDp, DEFAULT_DURATION));
    }
}
