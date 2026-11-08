package top.bogey.touch_tool.ui.tool.app_info;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.RectF;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.databinding.FloatAppInfoBinding;
import top.bogey.touch_tool.service.TaskInfoSummary;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.EAnchor;
import top.bogey.touch_tool.utils.callback.ResultCallback;
import top.bogey.touch_tool.utils.float_window_manager.FloatInterface;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

@SuppressLint("ViewConstructor")
public class AppInfoFloatView extends FrameLayout implements FloatInterface, ResultCallback<TaskInfoSummary.PackageActivity> {
    // 复用同一实例，避免重复 inflate
    private static java.lang.ref.WeakReference<AppInfoFloatView> cached = new java.lang.ref.WeakReference<>(null);

    public static void showTool(android.content.Context context) {
        AppInfoFloatView view = cached.get();
        if (view == null || view.getContext() != context) {
            view = new AppInfoFloatView(context);
            cached = new java.lang.ref.WeakReference<>(view);
        }
        view.show();
    }

    private final FloatAppInfoBinding binding;
    private final AppInfoFloatViewAdapter adapter;
    private final String tag = AppInfoFloatView.class.getName();

    private final int minWidth, minHeight;
    private final int maxWidth, maxHeight;
    private int originWidth = 0, originHeight = 0;
    private int width = 0, height = 0;

    private float lastX = 0, lastY = 0;
    private boolean canDrag = false;
    private boolean dragging = false;
    private boolean expanded = true;

    @SuppressLint("ClickableViewAccessibility")
    public AppInfoFloatView(@NonNull Context context) {
        super(context);

        minWidth = (int) DisplayUtil.dp2px(context, 168);
        minHeight = (int) DisplayUtil.dp2px(context, 128);
        Size size = DisplayUtil.getScreenSize(context);
        maxWidth = size.getWidth();
        maxHeight = (int) (size.getHeight() * 0.8f);

        binding = FloatAppInfoBinding.inflate(LayoutInflater.from(context), this, true);

        binding.closeButton.setOnClickListener(v -> dismiss());

        binding.expandButton.setOnClickListener(v -> {
            expanded = !expanded;
            if (expanded) {
                binding.contentBox.setVisibility(VISIBLE);
                int margin = (int) DisplayUtil.dp2px(context, 8);
                DisplayUtil.setViewMargin(binding.expandButton, margin, 0, 0, 0);
                ViewGroup.LayoutParams params = binding.getRoot().getLayoutParams();
                params.width = width;
                params.height = height;
                binding.getRoot().setLayoutParams(params);
                binding.expandButton.setIconResource(R.drawable.icon_zoom_in_map);
            } else {
                binding.contentBox.setVisibility(GONE);
                DisplayUtil.setViewMargin(binding.expandButton, 0, 0, 0, 0);
                ViewGroup.LayoutParams params = binding.getRoot().getLayoutParams();
                width = params.width;
                height = params.height;
                params.width = binding.expandButton.getWidth();
                params.height = binding.expandButton.getHeight();
                binding.getRoot().setLayoutParams(params);
                binding.expandButton.setIconResource(R.drawable.icon_zoom_out_map);

            }
            FloatWindow.updateLayoutParam(tag);
        });

        adapter = new AppInfoFloatViewAdapter();
        binding.recyclerView.setAdapter(adapter);
        // 打开时先填充一条当前所处界面信息
        TaskInfoSummary.PackageActivity packageActivity = TaskInfoSummary.getInstance().getPackageActivity();
        if (packageActivity != null && !packageActivity.packageName().isEmpty() && !packageActivity.activityName().isEmpty()) {
            adapter.addPackageActivity(packageActivity);
        }
        if (adapter.getItemCount() > 0) binding.recyclerView.scrollToPosition(adapter.getItemCount() - 1);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (originWidth == 0 || originHeight == 0) {
            originWidth = binding.getRoot().getWidth();
            originHeight = binding.getRoot().getHeight();
            originWidth = Math.max(1, originWidth);
            originHeight = Math.max(1, originHeight);

            width = binding.expandButton.getWidth();
            height = binding.expandButton.getHeight();
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        float x = event.getRawX();
        float y = event.getRawY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            canDrag = false;
            int[] location = new int[2];
            binding.recyclerView.getLocationOnScreen(location);
            if (new RectF(location[0], location[1], location[0] + binding.recyclerView.getWidth(), location[1] + binding.recyclerView.getHeight()).contains(x, y)) {
                FloatWindow.setDragAble(tag, false);
                return super.onInterceptTouchEvent(event);
            }

            location = new int[2];
            binding.dragImage.getLocationOnScreen(location);
            if (new RectF(location[0], location[1], location[0] + binding.dragImage.getWidth(), location[1] + binding.dragImage.getHeight()).contains(x, y)) {
                FloatWindow.setDragAble(tag, false);
                lastX = 0;
                lastY = 0;
                canDrag = true;
                return super.onInterceptTouchEvent(event);
            }
            FloatWindow.setDragAble(tag, true);
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (!canDrag) return false;
            if ((lastX == 0 && lastY == 0) || (lastX == x && lastY == y)) {
                lastX = x;
                lastY = y;
                return false;
            }
            dragging = true;
            return true;
        }
        return super.onInterceptTouchEvent(event);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getRawX();
        float y = event.getRawY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN -> {
                lastX = x;
                lastY = y;
                return true;
            }
            case MotionEvent.ACTION_MOVE -> {
                float dx = x - lastX;
                float dy = y - lastY;
                ViewGroup.LayoutParams params = binding.getRoot().getLayoutParams();
                if (params.width <= 0) params.width = originWidth;
                if (params.height <= 0) params.height = originHeight;
                params.width += (int) dx;
                params.height += (int) dy;
                params.width = Math.clamp(params.width, minWidth, maxWidth);
                params.height = Math.clamp(params.height, minHeight, maxHeight);
                binding.getRoot().setLayoutParams(params);
                FloatWindow.updateLayoutParam(tag);
                lastX = x;
                lastY = y;
                return true;
            }

            case MotionEvent.ACTION_UP -> {
                if (dragging) {
                    dragging = false;
                    FloatWindow.setDragAble(tag, true);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void show() {
        FloatWindow.with(MainApplication.getInstance().getService())
                .setLayout(this)
                .setTag(tag)
                .setSpecial(true)
                .setLocation(EAnchor.CENTER, 0, 0)
                .setExistEditText(true)
                .show();
        TaskInfoSummary.getInstance().addPackageActivityListener(this);
    }

    @Override
    public void dismiss() {
        TaskInfoSummary.getInstance().removePackageActivityListener(this);
        FloatWindow.dismiss(tag);
    }

    @Override
    public void onResult(TaskInfoSummary.PackageActivity packageActivity) {
        adapter.addPackageActivity(packageActivity);
        binding.recyclerView.scrollToPosition(adapter.getItemCount() - 1);
    }
}
