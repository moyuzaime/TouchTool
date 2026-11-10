package top.bogey.touch_tool.ui.blueprint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

import androidx.core.view.ViewCompat;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.utils.DisplayUtil;

// 蓝图缩放条：竖向圆角条，拖动改变画布缩放比例
public class ZoomBarView extends android.view.View {
    public interface Listener {
        void onPercentChanged(float percent);
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final int containerColor;
    private final int primaryColor;
    private final int outlineColor;
    private final int trackColor;
    private float percent = 0.28f;
    private Listener listener;

    public ZoomBarView(Context context) {
        super(context);
        containerColor = DisplayUtil.getAttrColor(context, R.attr.canvasControlContainer);
        primaryColor = DisplayUtil.getAttrColor(context, R.attr.canvasControlPrimary);
        outlineColor = blendColor(containerColor, primaryColor, 0.12f);
        trackColor = blendColor(containerColor, -1, 0.35f);
        setClickable(true);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setPercent(float percent) {
        this.percent = Math.max(0f, Math.min(1f, percent));
        invalidate();
    }

    // 颜色按比例混合
    public static int blendColor(int from, int to, float ratio) {
        float inverse = 1f - ratio;
        return android.graphics.Color.argb(
                (int) (android.graphics.Color.alpha(from) * inverse + android.graphics.Color.alpha(to) * ratio),
                (int) (android.graphics.Color.red(from) * inverse + android.graphics.Color.red(to) * ratio),
                (int) (android.graphics.Color.green(from) * inverse + android.graphics.Color.green(to) * ratio),
                (int) (android.graphics.Color.blue(from) * inverse + android.graphics.Color.blue(to) * ratio));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        float inset = DisplayUtil.dp2px(getContext(), 3);
        rect.set(inset, inset, width - inset, height - inset);
        float radius = rect.width() / 2f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(trackColor);
        canvas.drawRoundRect(rect, radius, radius, paint);

        float top = rect.bottom - rect.height() * percent;
        canvas.save();
        canvas.clipRect(rect.left, top, rect.right, rect.bottom);
        paint.setColor(primaryColor);
        canvas.drawRoundRect(rect, radius, radius, paint);
        canvas.restore();

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(1f, DisplayUtil.dp2px(getContext(), 1)));
        paint.setColor(outlineColor);
        canvas.drawRoundRect(rect, radius, radius, paint);

        float dotY = Math.min(rect.bottom - 4f, DisplayUtil.dp2px(getContext(), 23));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(blendColor(containerColor, ViewCompat.MEASURED_STATE_MASK, 0.65f));
        canvas.drawCircle(width / 2f, dotY, Math.max(2f, DisplayUtil.dp2px(getContext(), 2)), paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            setPercent(1f - event.getY() / Math.max(1f, getHeight()));
            if (listener != null) listener.onPercentChanged(percent);
            return true;
        }
        return super.onTouchEvent(event);
    }
}
