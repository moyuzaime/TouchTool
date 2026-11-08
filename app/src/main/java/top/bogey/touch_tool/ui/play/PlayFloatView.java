package top.bogey.touch_tool.ui.play;

import android.content.Context;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.ShapeAppearanceModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.start.ManualStartAction;
import top.bogey.touch_tool.bean.action.start.StartAction;
import top.bogey.touch_tool.bean.save.setting.SettingSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.databinding.FloatPlayBinding;
import top.bogey.touch_tool.service.ITaskListener;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskInfoSummary;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.blueprint.picker.FloatBaseCallback;
import top.bogey.touch_tool.ui.custom.float_view.KeepAliveFloatView;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.EAnchor;
import top.bogey.touch_tool.utils.float_window_manager.FloatDockSide;
import top.bogey.touch_tool.utils.float_window_manager.FloatInterface;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindowHelper;

public class PlayFloatView extends FrameLayout implements FloatInterface, ITaskListener {
    public static final int UNIT_DP_SIZE = 2;
    public static final int BUTTON_DP_SIZE = 32;
    public static final int UNIT_GROW_DP_SIZE = 8;


    private static String HIDE_PACKAGE = null;
    private static long HIDE_TIME = 0;
    private static boolean NOT_PLAY_HIDE = false;

    private final FloatPlayBinding binding;

    private final Handler handler;
    private final int padding = SettingSaver.MANUAL_PLAY_VIEW_PADDING.get() * UNIT_DP_SIZE;

    private int runningTaskCount = 0;

    public static void showActions(List<TaskInfoSummary.ManualExecuteInfo> actions) {
        TaskInfoSummary.PackageActivity packageActivity = TaskInfoSummary.getInstance().getPackageActivity();
        if (packageActivity != null && packageActivity.packageName().equals(HIDE_PACKAGE)) return;
        if (System.currentTimeMillis() < HIDE_TIME) return;
        HIDE_PACKAGE = null;
        HIDE_TIME = 0;

        KeepAliveFloatView keepView = (KeepAliveFloatView) FloatWindow.getView(KeepAliveFloatView.class.getName());
        if (keepView == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            View playFloatView = FloatWindow.getView(PlayFloatView.class.getName());
            if (playFloatView == null) {
                if (!actions.isEmpty()) new PlayFloatView(keepView.getThemeContext(), actions).show();
            } else {
                ((PlayFloatView) playFloatView).setActions(actions);
            }
        });
    }

    public static void addRunningAction(TaskRunnable runnable) {
        TaskInfoSummary.PackageActivity packageActivity = TaskInfoSummary.getInstance().getPackageActivity();
        if (packageActivity != null && packageActivity.packageName().equals(HIDE_PACKAGE)) return;
        if (System.currentTimeMillis() < HIDE_TIME) return;
        HIDE_PACKAGE = null;
        HIDE_TIME = 0;

        KeepAliveFloatView keepView = (KeepAliveFloatView) FloatWindow.getView(KeepAliveFloatView.class.getName());
        if (keepView == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            PlayFloatView playFloatView = (PlayFloatView) FloatWindow.getView(PlayFloatView.class.getName());
            if (playFloatView == null) {
                playFloatView = new PlayFloatView(keepView.getThemeContext());
                playFloatView.show();
            }
            playFloatView.addAction(runnable);
        });
    }

    public PlayFloatView(@NonNull Context context) {
        super(context);
        binding = FloatPlayBinding.inflate(LayoutInflater.from(context), this, true);
        DisplayUtil.setViewMargin(binding.playButtonBox, padding, 0, padding, 0);

        int alpha = SettingSaver.MANUAL_PLAY_VIEW_NOT_USED_FADE_LEVEL.get();
        setAlpha(NOT_PLAY_HIDE ? alpha / 100f : 1f);

        handler = new Handler(Looper.getMainLooper());

        int size = SettingSaver.MANUAL_PLAY_VIEW_ZOOM_SIZE.get();
        int buttonDpSize = BUTTON_DP_SIZE * 2 / 3;
        int growDpSize = (BUTTON_DP_SIZE - buttonDpSize) / 2;
        int px = (int) DisplayUtil.dp2px(context, buttonDpSize + growDpSize * (size - 1));
        DisplayUtil.setViewWidth(binding.dragSpaceButton, px);

        binding.dragSpaceButton.setOnClickListener(v -> {
            if (NOT_PLAY_HIDE) {
                playHide(false);
                playHide(true);
            } else {
                refreshExpand(true);
                refreshCorner(false);
                toDockSide();
            }
        });

        binding.dragSpaceButton.setOnLongClickListener(v -> {
            hide(SettingSaver.MANUAL_PLAY_VIEW_HIDE_TYPE.get());
            return true;
        });

        binding.closeButton.setOnClickListener(v -> {
            if (NOT_PLAY_HIDE) {
                playHide(false);
                playHide(true);
            } else {
                refreshExpand(false);
                refreshCorner(false);
                toDockSide();
            }
        });

        binding.closeButton.setOnLongClickListener(v -> {
            hide(SettingSaver.MANUAL_PLAY_VIEW_HIDE_TYPE.get());
            return true;
        });

        binding.buttonBox.setOnHierarchyChangeListener(new OnHierarchyChangeListener() {
            @Override
            public void onChildViewAdded(View parent, View child) {

            }

            @Override
            public void onChildViewRemoved(View parent, View child) {
                int childCount = binding.buttonBox.getChildCount();
                if (childCount == 0) dismiss();
            }
        });

        MainAccessibilityService service = MainApplication.getInstance().getService();
        if (service != null) service.addListener(this);

        playHide(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        MainAccessibilityService service = MainApplication.getInstance().getService();
        if (service != null) service.removeListener(this);
    }

    public PlayFloatView(Context context, List<TaskInfoSummary.ManualExecuteInfo> actions) {
        this(context);
        setActions(actions);
    }

    public void addAction(TaskRunnable runnable) {
        Task task = runnable.getStartTask();
        StartAction action = runnable.getStartAction();
        PlayFloatItemView itemView = new PlayFloatItemView(getContext(), task, action);
        binding.buttonBox.addView(itemView);
        itemView.setNeedRemove(true);
        itemView.onStart(runnable);
        runnable.addListener(itemView);

        if (runnable.isInterrupt()) itemView.onFinish(runnable);
    }

    public void setActions(List<TaskInfoSummary.ManualExecuteInfo> actions) {
        Set<ManualStartAction> already = new HashSet<>();
        Set<PlayFloatItemView> needRemove = new HashSet<>();

        int childCount = binding.buttonBox.getChildCount();
        for (int index = childCount - 1; index >= 0; index--) {
            PlayFloatItemView itemView = (PlayFloatItemView) binding.buttonBox.getChildAt(index);
            // 如果itemView不在actions中，则移除
            boolean flag = true;
            for (TaskInfoSummary.ManualExecuteInfo info : actions) {
                Task task = info.task();
                ManualStartAction action = info.action();
                if (itemView.check(task, action)) {
                    already.add(action);
                    itemView.setNeedRemove(false);
                    flag = false;
                    break;
                }
            }
            if (flag) needRemove.add(itemView);
        }

        boolean expand = false;
        for (TaskInfoSummary.ManualExecuteInfo info : actions) {
            Task task = info.task();
            ManualStartAction action = info.action();
            if (action.isExpand()) expand = true;
            if (already.contains(action)) continue;
            PlayFloatItemView itemView = new PlayFloatItemView(getContext(), task, action);
            itemView.setVisibility(isHideNotRunningPlayItem() ? View.GONE : View.VISIBLE);
            binding.buttonBox.addView(itemView);
        }
        if (expand) refreshExpand(true);

        needRemove.forEach(PlayFloatItemView::tryRemoveFromParent);
    }

    private void refreshExpand(boolean expand) {
        SettingSaver.MANUAL_PLAY_VIEW_EXPAND_STATE.set(expand);
        binding.playButtonBox.setVisibility(expand ? VISIBLE : GONE);
        binding.dragSpace.setVisibility(expand ? GONE : VISIBLE);
        binding.dragSpaceButton.setIconResource(inLeft() ? R.drawable.icon_keyboard_arrow_right : R.drawable.icon_keyboard_arrow_left);
    }

    private void refreshCorner(boolean dragging) {
        boolean expand = SettingSaver.MANUAL_PLAY_VIEW_EXPAND_STATE.get();
        int buttonDpSize = BUTTON_DP_SIZE * 2 / 3;
        float cornerSize = DisplayUtil.dp2px(getContext(), expand ? BUTTON_DP_SIZE / 2f : buttonDpSize / 2f);
        if (dragging) {
            ShapeAppearanceModel appearanceModel = ShapeAppearanceModel.builder()
                    .setTopLeftCorner(CornerFamily.ROUNDED, cornerSize)
                    .setTopRightCorner(CornerFamily.ROUNDED, cornerSize)
                    .setBottomLeftCorner(CornerFamily.ROUNDED, cornerSize)
                    .setBottomRightCorner(CornerFamily.ROUNDED, cornerSize)
                    .build();

            if (padding == 0) binding.playButtonBox.setShapeAppearanceModel(appearanceModel);
            binding.dragSpace.setShapeAppearanceModel(appearanceModel);
        } else {
            if (inLeft()) {
                ShapeAppearanceModel appearanceModel = ShapeAppearanceModel.builder()
                        .setTopLeftCorner(CornerFamily.CUT, 0)
                        .setTopRightCorner(CornerFamily.ROUNDED, cornerSize)
                        .setBottomLeftCorner(CornerFamily.CUT, 0)
                        .setBottomRightCorner(CornerFamily.ROUNDED, cornerSize)
                        .build();
                if (padding == 0) binding.playButtonBox.setShapeAppearanceModel(appearanceModel);
                binding.dragSpace.setShapeAppearanceModel(appearanceModel);
            } else {
                ShapeAppearanceModel appearanceModel = ShapeAppearanceModel.builder()
                        .setTopLeftCorner(CornerFamily.ROUNDED, cornerSize)
                        .setTopRightCorner(CornerFamily.CUT, 0)
                        .setBottomLeftCorner(CornerFamily.ROUNDED, cornerSize)
                        .setBottomRightCorner(CornerFamily.CUT, 0)
                        .build();
                if (padding == 0) binding.playButtonBox.setShapeAppearanceModel(appearanceModel);
                binding.dragSpace.setShapeAppearanceModel(appearanceModel);
            }
        }
    }

    private void toDockSide() {
        FloatWindowHelper helper = FloatWindow.getHelper(PlayFloatView.class.getName());
        if (helper != null) helper.viewParent.toDockSide();
    }

    private void refreshPlayButton() {
        post(() -> {
            MainAccessibilityService service = MainApplication.getInstance().getService();
            if (service != null && service.isEnabled()) {
                int count = binding.buttonBox.getChildCount();
                for (int i = 0; i < count; i++) {
                    PlayFloatItemView view = (PlayFloatItemView) binding.buttonBox.getChildAt(i);
                    if (service.isTaskRunning(view.task, view.startAction)) {
                        view.setVisibility(VISIBLE);
                    } else {
                        view.setVisibility(isHideNotRunningPlayItem() ? GONE : VISIBLE);
                    }
                }
            }
        });
    }

    private boolean isHideNotRunningPlayItem() {
        return SettingSaver.MANUAL_PLAY_VIEW_BUTTON_HIDE_WHEN_EXECUTE.get() && runningTaskCount > 0;
    }

    private boolean inLeft() {
        int[] location = new int[2];
        getLocationOnScreen(location);
        Size size = DisplayUtil.getScreenSize(getContext());
        return location[0] < (size.getWidth() - getWidth()) / 2;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            refreshExpand(SettingSaver.MANUAL_PLAY_VIEW_EXPAND_STATE.get());
            toDockSide();
        }
    }

    @Override
    public void show() {
        Point pos = SettingSaver.MANUAL_PLAY_VIEW_POS.get();
        FloatWindow.with(MainApplication.getInstance().getService())
                .setTag(PlayFloatView.class.getName())
                .setLayout(this)
                .setDockSide(FloatDockSide.HORIZONTAL)
                .setAnchor(EAnchor.TOP_LEFT)
                .setLocation(EAnchor.CENTER_RIGHT, pos.x, pos.y)
                .setCallback(new PlayFloatCallback())
                .setSpecial(true)
                .setHideByScreenshot(SettingSaver.MANUAL_PLAY_VIEW_HIDE_WHEN_SCREENSHOT.get())
                .show();
    }

    @Override
    public void dismiss() {
        // 先停掉延时任务，避免窗口关闭后回调仍在跑
        handler.removeCallbacksAndMessages(null);
        FloatWindow.dismiss(PlayFloatView.class.getName());
    }

    public void hide(int hideType) {
        switch (hideType) {
            case 1 -> {
                TaskInfoSummary.PackageActivity packageActivity = TaskInfoSummary.getInstance().getPackageActivity();
                if (packageActivity != null) HIDE_PACKAGE = packageActivity.packageName();
                dismiss();
            }
            case 2 -> {
                HIDE_TIME = System.currentTimeMillis() + 3 * 50 * 1000;
                dismiss();
            }

            case 3 -> new PlayFloatHideChoiceView(getContext(), this::hide).show();

            default -> dismiss();
        }
    }

    private void playHide(boolean hide) {
        handler.removeCallbacksAndMessages(null);
        if (hide) {
            if (runningTaskCount == 0 && SettingSaver.MANUAL_PLAY_VIEW_NOT_USED_FADE.get()) {
                handler.postDelayed(() -> {
                    int alpha = SettingSaver.MANUAL_PLAY_VIEW_NOT_USED_FADE_LEVEL.get();
                    animate().alpha(alpha / 100f);
                    NOT_PLAY_HIDE = true;
                }, 10000);
            }
        } else {
            post(() -> animate().alpha(1f));
            NOT_PLAY_HIDE = false;
        }
    }

    @Override
    public void onStart(TaskRunnable runnable) {
        StartAction startAction = runnable.getStartAction();
        if (startAction instanceof ManualStartAction manualStartAction) {
            if (!manualStartAction.isSingleShow()) {
                runningTaskCount++;
                refreshPlayButton();
            }
        }

        handler.removeCallbacksAndMessages(null);
        post(() -> animate().alpha(1f));
        NOT_PLAY_HIDE = false;
    }

    @Override
    public void onFinish(TaskRunnable runnable) {
        StartAction startAction = runnable.getStartAction();
        if (startAction instanceof ManualStartAction manualStartAction) {
            if (!manualStartAction.isSingleShow()) {
                runningTaskCount--;
                refreshPlayButton();
            }
        }

        playHide(true);
    }

    private static class PlayFloatCallback extends FloatBaseCallback {

        @Override
        public void onShow(String tag) {

        }

        @Override
        public void onDismiss() {

        }

        @Override
        public void onDrag() {
            super.onDrag();
            View view = FloatWindow.getView(PlayFloatView.class.getName());
            if (view instanceof PlayFloatView playFloatView) {
                playFloatView.refreshCorner(true);
                playFloatView.handler.removeCallbacksAndMessages(null);
                playFloatView.playHide(false);
            }
        }

        @Override
        public void onDragEnd() {
            super.onDragEnd();
            FloatWindowHelper helper = FloatWindow.getHelper(PlayFloatView.class.getName());
            if (helper != null) {
                Point point = helper.getRelativePoint();
                SettingSaver.MANUAL_PLAY_VIEW_POS.set(point);
            }

            View view = FloatWindow.getView(PlayFloatView.class.getName());
            if (view instanceof PlayFloatView playFloatView) {
                playFloatView.refreshExpand(SettingSaver.MANUAL_PLAY_VIEW_EXPAND_STATE.get());
                playFloatView.refreshCorner(false);
                playFloatView.playHide(true);
            }
        }

        @Override
        public void onRotate() {
            super.onRotate();
            View view = FloatWindow.getView(PlayFloatView.class.getName());
            if (view instanceof PlayFloatView playFloatView) {
                playFloatView.postDelayed(() -> playFloatView.refreshCorner(false), 200);
            }
        }
    }
}
