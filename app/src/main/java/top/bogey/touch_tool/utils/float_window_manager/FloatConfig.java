package top.bogey.touch_tool.utils.float_window_manager;

import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;
import android.view.WindowManager;

import top.bogey.touch_tool.utils.EAnchor;

class FloatConfig {
    // 唯一标签
    String tag = null;

    // 布局id
    int layoutId = 0;
    // 布局view
    View layoutView = null;

    // 特殊悬浮窗，不受批量悬浮窗操作影响
    boolean special = false;

    // 是否能拖动
    boolean dragAble = true;
    // 是否正在拖动
    boolean dragging = false;

    // 悬浮窗进出动画
    FloatAnimator animator = null;
    // 动画是否正在执行
    boolean animating = false;

    // 是否存在输入框
    boolean existEditText = false;
    // 悬浮窗额外flag
    int flag = 0;

    // 悬浮窗停靠类型
    FloatDockSide side = FloatDockSide.DEFAULT;
    // 悬浮窗停靠边距
    Rect border = new Rect();

    // 悬浮窗锚点
    EAnchor anchor = EAnchor.CENTER;

    // 悬浮窗屏幕位置锚点
    EAnchor gravity = EAnchor.CENTER;
    // 悬浮窗屏幕锚点位置偏移
    Point location = new Point();

    // 悬浮窗大小
    int width = WindowManager.LayoutParams.WRAP_CONTENT;
    int height = WindowManager.LayoutParams.WRAP_CONTENT;

    // 悬浮窗回调
    FloatCallback callback = null;

    // 对截图或录屏隐藏
    boolean hideByScreenshot = false;
}
