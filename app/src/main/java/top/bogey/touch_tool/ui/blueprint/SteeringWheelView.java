package top.bogey.touch_tool.ui.blueprint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;

import androidx.core.graphics.ColorUtils;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.utils.DisplayUtil;

// 蓝图画布方向盘：四向跳边界/平移画布，点中心切换模式（按 mod 实现）
public class SteeringWheelView extends android.view.View {
    public interface Listener {
        void onCenterToggled(boolean jumpEnabled);

        void onDirection(int direction);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final int containerColor;
    private final int primaryColor;
    private final int outlineColor;
    private boolean jumpEnabled = false;
    private Listener listener;

    public SteeringWheelView(Context context) {
        super(context);
        containerColor = DisplayUtil.getAttrColor(context, R.attr.canvasControlContainer);
        primaryColor = DisplayUtil.getAttrColor(context, R.attr.canvasControlPrimary);
        outlineColor = ColorUtils.blendARGB(containerColor, primaryColor, 0.35f);
        setClickable(true);
        updateContentDescription(false);
    }

    public boolean isJumpEnabled() {
        return jumpEnabled;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    private void updateContentDescription(boolean announce) {
        setContentDescription(getContext().getString(jumpEnabled
                ? R.string.blueprint_steering_wheel_on : R.string.blueprint_steering_wheel_off));
        if (announce) sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) / 2f;

        // 四条对角连接线
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, 0.045f * radius));
        paint.setColor(outlineColor);
        for (double angle : new double[]{45d, 135d, 225d, 315d}) {
            double radians = Math.toRadians(angle);
            double inner = 0.34f * radius;
            double outer = 0.82f * radius;
            canvas.drawLine((float) (Math.cos(radians) * inner) + cx, (float) (inner * Math.sin(radians)) + cy,
                    (float) (outer * Math.cos(radians)) + cx, (float) (outer * Math.sin(radians)) + cy, paint);
        }

        // 四向箭头
        float distance = radius * 0.59f;
        float size = radius * 0.15f;
        paint.setStrokeWidth(Math.max(2f, 0.07f * radius));
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(primaryColor);
        drawChevron(canvas, cx, cy - distance, 0, size);
        drawChevron(canvas, cx + distance, cy, 1, size);
        drawChevron(canvas, cx, cy + distance, 2, size);
        drawChevron(canvas, cx - distance, cy, 3, size);

        // 中心圆：跳边界模式实心，平移模式空心
        float centerRadius = radius * 0.3f;
        paint.setStyle(Paint.Style.FILL);
        if (jumpEnabled) {
            paint.setColor(primaryColor);
            canvas.drawCircle(cx, cy, centerRadius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(-1);
            paint.setStrokeWidth(Math.max(2f, radius * 0.05f));
            canvas.drawCircle(cx, cy, centerRadius * 0.58f, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy, centerRadius * 0.16f, paint);
            return;
        }
        paint.setColor(-1);
        canvas.drawCircle(cx, cy, centerRadius, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(primaryColor);
        paint.setStrokeWidth(Math.max(2f, radius * 0.06f));
        canvas.drawCircle(cx, cy, centerRadius - radius * 0.02f, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, centerRadius * 0.28f, paint);
    }

    // direction: 0 上, 1 右, 2 下, 3 左
    private void drawChevron(Canvas canvas, float x, float y, int direction, float size) {
        canvas.save();
        canvas.rotate(direction * 90f, x, y);
        path.reset();
        path.moveTo(x - size, y + size * 0.55f);
        path.lineTo(x, y - size * 0.55f);
        path.lineTo(x + size, y + size * 0.55f);
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float x = event.getX() - cx;
        float y = event.getY() - cy;
        float centerLimit = Math.min(getWidth(), getHeight()) / 2f * 0.4f;

        if (x * x + y * y <= centerLimit * centerLimit) {
            jumpEnabled = !jumpEnabled;
            updateContentDescription(true);
            invalidate();
            if (listener != null) listener.onCenterToggled(jumpEnabled);
            return true;
        }

        double angle = Math.atan2(y, x);
        int direction;
        if (angle >= -0.7853981633974483d && angle < 0.7853981633974483d) {
            direction = 2;      // 右
        } else if (angle >= 0.7853981633974483d && angle < 2.356194490192345d) {
            direction = 3;      // 下
        } else if (angle >= -2.356194490192345d && angle < -0.7853981633974483d) {
            direction = 1;      // 上
        } else {
            direction = 0;      // 左
        }
        if (listener != null) listener.onDirection(direction);
        return true;
    }
}
