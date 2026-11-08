package top.bogey.touch_tool.ui.custom.float_view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;

import java.util.UUID;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.save.setting.SettingSaver;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.EAnchor;
import top.bogey.touch_tool.utils.float_window_manager.FloatInterface;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

public class MarkTargetFloatView extends AppCompatImageView implements FloatInterface {
    private final static int LINE_WIDTH = 10;

    private final String tag = UUID.randomUUID().toString();

    private final Paint paint;

    private Rect targetArea;

    public static void showTargetArea(Rect targetArea) {
        boolean showTargetArea = SettingSaver.TASK_TARGET_MARK.get();
        if (!showTargetArea) return;

        showMarkArea(targetArea);
    }

    public static void showMarkArea(Rect targetArea) {
        KeepAliveFloatView keepView = (KeepAliveFloatView) FloatWindow.getView(KeepAliveFloatView.class.getName());
        if (keepView == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            MarkTargetFloatView floatView = new MarkTargetFloatView(keepView.getThemeContext());
            int height = DisplayUtil.getStatusBarHeight(null, null);
            targetArea.offset(0, -height);

            floatView.innerShowTargetArea(targetArea);
            floatView.show();
        });
    }

    public MarkTargetFloatView(@NonNull Context context) {
        super(context);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(DisplayUtil.getAttrColor(getContext(), R.attr.colorPrimaryLight));
        paint.setStrokeWidth(LINE_WIDTH);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
    }

    private void innerShowTargetArea(Rect targetArea) {
        this.targetArea = targetArea;
        int width = targetArea.width();
        int height = targetArea.height();
        if (width <= 0 || height <= 0) {
            post(this::dismiss);
            return;
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.translate(-targetArea.left, -targetArea.top);

        canvas.drawRect(new RectF(targetArea), paint);
        setImageBitmap(bitmap);
        postDelayed(() -> animate().alpha(0).withEndAction(this::dismiss), 500);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = targetArea.width();
        int height = targetArea.height();
        setMeasuredDimension(width, height);
    }

    @Override
    public void show() {
        FloatWindow.with(MainApplication.getInstance().getService())
                .setLayout(this)
                .setTag(tag)
                .setLocation(EAnchor.TOP_LEFT, targetArea.left, targetArea.top)
                .setAnchor(EAnchor.TOP_LEFT)
                .setDragAble(false)
                .setSpecial(true)
                .setHideByScreenshot(SettingSaver.MANUAL_PLAY_VIEW_HIDE_WHEN_SCREENSHOT.get())
                .setFlag(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                .show();
    }

    @Override
    public void dismiss() {
        FloatWindow.dismiss(tag);
    }
}
