package top.bogey.touch_tool.ui.blueprint;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import top.bogey.touch_tool.utils.DisplayUtil;

// 在图标右上角绘制数量角标的 Drawable（不占按钮宽度，也不受父容器裁剪影响）
public class BadgeIconDrawable extends Drawable {
    private final Drawable base;
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int count;

    public BadgeIconDrawable(Context context, @NonNull Drawable base) {
        this.base = base;
        bgPaint.setColor(DisplayUtil.getAttrColor(context, com.google.android.material.R.attr.colorTertiary));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(DisplayUtil.dp2px(context, 9));
        textPaint.setFakeBoldText(true);
    }

    public void setCount(int count) {
        if (this.count == count) return;
        this.count = count;
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        base.setBounds(bounds);
        base.draw(canvas);
        if (count <= 0) return;

        float radius = Math.max(DisplayUtil.dp2px(getContextHack(), 7), bounds.width() * 0.28f);
        float cx = bounds.right - radius * 0.65f;
        float cy = bounds.top + radius * 0.65f;
        canvas.drawCircle(cx, cy, radius, bgPaint);

        String text = count > 99 ? "99+" : String.valueOf(count);
        float size = textPaint.getTextSize() * (text.length() > 1 ? 0.78f : 1f);
        textPaint.setTextSize(size);
        float baseline = cy - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(text, cx, baseline, textPaint);
    }

    // 拿不到 context 时的兜底尺寸（1dp≈2px，仅用于圆半径下限）
    private Context hackContext;
    private Context getContextHack() {
        return hackContext;
    }

    public void setContext(Context context) {
        hackContext = context;
        textPaint.setTextSize(DisplayUtil.dp2px(context, 9));
    }

    @Override
    public void setAlpha(int alpha) {
        base.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        base.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public int getIntrinsicWidth() {
        return base.getIntrinsicWidth();
    }

    @Override
    public int getIntrinsicHeight() {
        return base.getIntrinsicHeight();
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        base.setBounds(left, top, right, bottom);
    }
}
