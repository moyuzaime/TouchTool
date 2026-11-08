package top.bogey.touch_tool.utils.float_window_manager;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

@SuppressLint("ViewConstructor")
public class FloatViewParent extends FrameLayout {
    private final FloatWindowHelper helper;
    private final FloatConfig config;

    private float lastX = 0, lastY = 0;
    private Rect showArea = new Rect();

    private boolean isCreated = false;

    FloatViewParent(@NonNull Context context, FloatWindowHelper helper) {
        super(context);
        this.helper = helper;
        config = helper.config;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!isCreated) {
            isCreated = true;
            helper.onLayoutCreated();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (config.callback != null) {
            if (config.callback.onTouch(event)) return true;
        }

        if (!config.dragAble || config.animating) {
            config.dragging = false;
            return super.onTouchEvent(event);
        }

        float x = event.getRawX();
        float y = event.getRawY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN -> {
                config.dragging = false;
                showArea = helper.getShowArea();
                lastX = x;
                lastY = y;
            }

            case MotionEvent.ACTION_MOVE -> {
                float dx = x - lastX;
                float dy = y - lastY;
                if (dx * dx + dy * dy < 81) return false;
                config.dragging = true;

                helper.params.x = (int) Math.clamp(helper.params.x + dx, showArea.left, showArea.right);
                helper.params.y = (int) Math.clamp(helper.params.y + dy, showArea.top, showArea.bottom);
                helper.manager.updateViewLayout(this, helper.params);

                if (config.callback != null) config.callback.onDrag();

                lastX = x;
                lastY = y;
            }

            case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!config.dragging) return false;
                config.dragging = false;

                toDockSide();
            }
        }

        return config.dragging || super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (!config.dragAble) return super.onInterceptTouchEvent(event);

        float x = event.getRawX();
        float y = event.getRawY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN -> {
                config.dragging = false;
                showArea = helper.getShowArea();
                lastX = x;
                lastY = y;
            }
            case MotionEvent.ACTION_MOVE -> {
                float dx = x - lastX;
                float dy = y - lastY;
                if (dx * dx + dy * dy < 81) return false;
                lastX = x;
                lastY = y;
                config.dragging = true;
            }
        }

        return config.dragging || super.onInterceptTouchEvent(event);
    }

    public void toDockSide() {
        if (!isCreated) return;

        if (config.side != FloatDockSide.DEFAULT) {
            showArea = helper.getShowArea();

            int leftDistance = Math.abs(helper.params.x - showArea.left);
            int rightDistance = Math.abs(helper.params.x - showArea.right);
            int topDistance = Math.abs(helper.params.y - showArea.top);
            int bottomDistance = Math.abs(helper.params.y - showArea.bottom);

            boolean isPortrait = false;
            int endValue = 0;
            switch (config.side) {
                case LEFT -> endValue = showArea.left;
                case RIGHT -> endValue = showArea.right - getWidth();
                case TOP -> {
                    endValue = showArea.top;
                    isPortrait = true;
                }
                case BOTTOM -> {
                    endValue = showArea.bottom - getHeight();
                    isPortrait = true;
                }
                case HORIZONTAL -> endValue = leftDistance < rightDistance ? showArea.left : showArea.right;
                case VERTICAL -> {
                    endValue = topDistance < bottomDistance ? showArea.top : showArea.bottom;
                    isPortrait = true;
                }
                case SIDE -> {
                    int minHorizontalDistance = Math.min(leftDistance, rightDistance);
                    int minVerticalDistance = Math.min(topDistance, bottomDistance);

                    if (minHorizontalDistance < minVerticalDistance) {
                        endValue = leftDistance < rightDistance ? showArea.left : showArea.right;
                    } else {
                        endValue = topDistance < bottomDistance ? showArea.top : showArea.bottom;
                        isPortrait = true;
                    }
                }
            }

            ValueAnimator animator = ValueAnimator.ofInt(isPortrait ? helper.params.y : helper.params.x, endValue);
            boolean finalIsPortrait = isPortrait;
            animator.addUpdateListener(animation -> {
                if (finalIsPortrait) {
                    helper.params.y = (int) animation.getAnimatedValue();
                } else {
                    helper.params.x = (int) animation.getAnimatedValue();
                }
                try {
                    helper.manager.updateViewLayout(this, helper.params);
                } catch (Exception ignored) {
                    animator.cancel();
                }
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationCancel(Animator animation) {
                    super.onAnimationCancel(animation);
                    config.animating = false;
                    if (config.callback != null) config.callback.onDragEnd();
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    config.animating = false;
                    if (config.callback != null) config.callback.onDragEnd();
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    config.animating = true;
                }
            });
            animator.start();
        } else {
            if (config.callback != null) config.callback.onDragEnd();
        }
    }

    @Override
    public boolean dispatchKeyEventPreIme(KeyEvent event) {
        if (config.existEditText) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                helper.params.flags = FloatWindow.NOT_FOCUSABLE | config.flag;
                helper.manager.updateViewLayout(this, helper.params);
            }
        }
        return super.dispatchKeyEventPreIme(event);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (config.callback != null) config.callback.onDismiss();
    }
}
