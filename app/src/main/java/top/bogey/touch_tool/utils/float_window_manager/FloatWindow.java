package top.bogey.touch_tool.utils.float_window_manager;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import top.bogey.touch_tool.utils.EAnchor;

public class FloatWindow {
    static final int FOCUSABLE = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
    static final int FOCUSABLE_NOT_IM = WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM | FOCUSABLE;
    static final int NOT_FOCUSABLE = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

    static final String DEFAULT_TAG = "DEFAULT_TAG";

    static final List<String> tags = new ArrayList<>();
    static final Map<String, FloatWindowHelper> views = new HashMap<>();

    public static FloatWindowHelper getHelper(String tag) {
        return views.get(tag);
    }

    public static void dismiss(String tag) {
        tag = checkTag(tag);
        if (tags.contains(tag)) {
            FloatWindowHelper helper = getHelper(tag);
            if (helper != null) helper.dismissFloatWindow();
            tags.remove(tag);
            views.remove(tag);
        }
    }

    public static void dismiss(Class<?> clazz) {
        HashMap<String, FloatWindowHelper> map = new HashMap<>(views);
        map.forEach((tag, helper) -> {
            if (helper.config.layoutView.getClass() == clazz) {
                dismiss(tag);
            }
        });
    }

    public static void hide(String tag) {
        tag = checkTag(tag);
        if (tags.contains(tag)) {
            FloatWindowHelper helper = getHelper(tag);
            if (helper != null) {
                helper.viewParent.setVisibility(View.INVISIBLE);
                if (helper.config.callback != null) helper.config.callback.onHide();
            }
        }
    }

    public static void hideAll(String ignored) {
        for (String tag : tags) {
            if (tag.equals(ignored)) continue;
            FloatWindowHelper helper = getHelper(tag);
            if (helper != null && helper.config.special) continue;
            hide(tag);
        }
    }

    public static void show(String tag) {
        tag = checkTag(tag);
        if (tags.contains(tag)) {
            FloatWindowHelper helper = getHelper(tag);
            if (helper != null) {
                helper.viewParent.setVisibility(View.VISIBLE);
                if (helper.config.callback != null) helper.config.callback.onShow(tag);
            }
        }
    }

    public static boolean showLast() {
        for (int i = tags.size() - 1; i >= 0; i--) {
            String tag = tags.get(i);
            FloatWindowHelper helper = getHelper(tag);
            if (helper != null && helper.config.special) continue;
            show(tag);
            return true;
        }
        return false;
    }

    public static View getView(String tag) {
        tag = checkTag(tag);
        FloatWindowHelper helper = getHelper(tag);
        if (helper != null) {
            return helper.config.layoutView;
        }
        return null;
    }

    public static List<View> getViews(Class<?> viewClass) {
        List<View> result = new ArrayList<>();
        views.forEach((tag, helper) -> {
            if (helper.config.layoutView.getClass() == viewClass) {
                result.add(helper.config.layoutView);
            }
        });
        return result;
    }

    public static void reset() {
        tags.clear();
        views.clear();
    }

    public static Point getLocation(String tag) {
        tag = checkTag(tag);
        FloatWindowHelper helper = getHelper(tag);
        if (helper != null) {
            return helper.getRelativePoint();
        }
        return null;
    }

    public static void setLocation(String tag, EAnchor anchor, EAnchor gravity, Point location) {
        tag = checkTag(tag);
        FloatWindowHelper helper = getHelper(tag);
        if (helper != null) {
            helper.setRelativePoint(anchor, gravity, location);
        }
    }

    public static void setDragAble(String tag, boolean dragAble) {
        tag = checkTag(tag);
        FloatWindowHelper helper = getHelper(tag);
        if (helper != null) {
            helper.config.dragAble = dragAble;
        }
    }

    public static void updateLayoutParam(String tag) {
        tag = checkTag(tag);
        FloatWindowHelper helper = getHelper(tag);
        if (helper != null) {
            helper.manager.updateViewLayout(helper.viewParent, helper.params);
        }
    }

    private static String checkTag(String tag) {
        if (tag == null) {
            return DEFAULT_TAG;
        }
        return tag;
    }

    public static Builder with(Context context) {
        return new Builder(context);
    }

    public static class Builder {
        private final Context context;
        private final FloatConfig config = new FloatConfig();

        public Builder(Context context) {
            this.context = context;
        }

        public Builder setLayout(int layout) {
            config.layoutId = layout;
            return this;
        }

        public Builder setLayout(View layout) {
            config.layoutView = layout;
            return this;
        }

        public Builder setTag(String tag) {
            config.tag = tag;
            return this;
        }

        public Builder setSpecial(boolean special) {
            config.special = special;
            return this;
        }

        public Builder setDragAble(boolean dragAble) {
            config.dragAble = dragAble;
            return this;
        }

        public Builder setAnimator(FloatAnimator animator) {
            config.animator = animator;
            return this;
        }

        public Builder setExistEditText(boolean exist) {
            config.existEditText = exist;
            return this;
        }

        public Builder setFlag(int flag) {
            config.flag = flag;
            return this;
        }

        public Builder setDockSide(FloatDockSide side) {
            config.side = side;
            return this;
        }

        public Builder setBorder(Rect border) {
            config.border = border;
            return this;
        }

        public Builder setBorder(int left, int top, int right, int bottom) {
            config.border = new Rect(left, top, right, bottom);
            return this;
        }

        public Builder setLocation(EAnchor gravity, int x, int y) {
            config.gravity = gravity;
            config.location = new Point(x, y);
            return this;
        }

        public Builder setAnchor(EAnchor anchor) {
            config.anchor = anchor;
            return this;
        }

        public Builder setSize(int width, int height) {
            config.width = width;
            config.height = height;
            return this;
        }

        public Builder setCallback(FloatCallback callback) {
            config.callback = callback;
            return this;
        }

        public Builder setHideByScreenshot(boolean hide) {
            config.hideByScreenshot = hide;
            return this;
        }

        public void show() {
            if (config.layoutId == 0 && config.layoutView == null) {
                return;
            }

            if (config.tag == null) {
                config.tag = DEFAULT_TAG;
            }

            if (tags.contains(config.tag)) {
                return;
            }

            if (context == null) return;

            FloatWindowHelper helper = new FloatWindowHelper(context, config);
            helper.createFloatWindow();
            tags.add(config.tag);
            views.put(config.tag, helper);
        }
    }
}
