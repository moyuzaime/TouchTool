package top.bogey.touch_tool.ui.blueprint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.widget.Toast;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.utils.DisplayUtil;

// 蓝图画布方向盘：四向跳边界或平移画布，点中心切换模式
public class SteeringWheelView extends android.view.View {
    public interface Listener {
        void onDirection(int direction, boolean jumpToEdge);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private final int containerColor;
    private final int primaryColor;
    private final int selectedColor;
    private boolean jumpToEdge = true;
    private Listener listener;

    public SteeringWheelView(Context context) {
        super(context);
        containerColor = DisplayUtil.getAttrColor(context, R.attr.canvasControlContainer);
        primaryColor = DisplayUtil.getAttrColor(context, R.attr.canvasControlPrimary);
        selectedColor = DisplayUtil.getAttrColor(context, R.attr.canvasControlSelected);
        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setColor(primaryColor);
        setClickable(true);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public boolean isJumpToEdge() {
        return jumpToEdge;
    }

    public void setJumpToEdge(boolean jumpToEdge) {
        this.jumpToEdge = jumpToEdge;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = Math.min(cx, cy);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(containerColor);
        canvas.drawCircle(cx, cy, r, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, DisplayUtil.dp2px(getContext(), 1)));
        paint.setColor(primaryColor);
        canvas.drawCircle(cx, cy, r - paint.getStrokeWidth(), paint);

        // 中心圆点：跳边界模式为实心，平移模式为空心
        paint.setStyle(jumpToEdge ? Paint.Style.FILL : Paint.Style.STROKE);
        paint.setColor(jumpToEdge ? selectedColor : primaryColor);
        canvas.drawCircle(cx, cy, r * 0.18f, paint);

        drawArrow(canvas, cx, cy, r * 0.62f, 0);
        drawArrow(canvas, cx, cy, r * 0.62f, 1);
        drawArrow(canvas, cx, cy, r * 0.62f, 2);
        drawArrow(canvas, cx, cy, r * 0.62f, 3);
    }

    // direction: 0 左, 1 上, 2 右, 3 下
    private void drawArrow(Canvas canvas, float cx, float cy, float distance, int direction) {
        float size = DisplayUtil.dp2px(getContext(), 7);
        canvas.save();
        canvas.rotate(direction * 90f, cx, cy);
        path.reset();
        path.moveTo(cx, cy - distance - size / 2f);
        path.lineTo(cx - size / 2f, cy - distance + size / 2f);
        path.lineTo(cx + size / 2f, cy - distance + size / 2f);
        path.close();
        arrowPaint.setColor(direction == 1 ? selectedColor : primaryColor);
        canvas.drawPath(path, arrowPaint);
        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN) return super.onTouchEvent(event);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float dx = event.getX() - cx;
        float dy = event.getY() - cy;
        float distance = (float) Math.hypot(dx, dy);
        float r = Math.min(cx, cy);

        // 点中心切换模式
        if (distance <= r * 0.35f) {
            setJumpToEdge(!jumpToEdge);
            Toast.makeText(getContext(), jumpToEdge ? R.string.blueprint_steering_wheel_on : R.string.blueprint_steering_wheel_off, Toast.LENGTH_SHORT).show();
            performClick();
            return true;
        }
        if (distance > r) return true;

        int direction;
        if (Math.abs(dx) > Math.abs(dy)) direction = dx < 0 ? 0 : 2;
        else direction = dy < 0 ? 1 : 3;
        if (listener != null) listener.onDirection(direction, jumpToEdge);
        performClick();
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
