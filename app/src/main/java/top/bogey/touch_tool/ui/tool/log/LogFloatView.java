package top.bogey.touch_tool.ui.tool.log;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.RectF;
import android.text.Editable;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.other.log.ActionLog;
import top.bogey.touch_tool.bean.other.log.LogInfo;
import top.bogey.touch_tool.bean.save.log.LogSave;
import top.bogey.touch_tool.bean.save.log.LogSaveListener;
import top.bogey.touch_tool.bean.save.log.LogSaver;
import top.bogey.touch_tool.bean.save.setting.SettingSaver;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.databinding.FloatLogBinding;
import top.bogey.touch_tool.utils.AnimUtil;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.EAnchor;
import top.bogey.touch_tool.utils.float_window_manager.FloatInterface;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;
import top.bogey.touch_tool.utils.listener.TextChangedListener;

@SuppressLint("ViewConstructor")
public class LogFloatView extends FrameLayout implements FloatInterface, LogSaveListener {
    // 复用同一个实例：关掉后再次打开不必重新 inflate 与绑定
    private static java.lang.ref.WeakReference<LogFloatView> cached = new java.lang.ref.WeakReference<>(null);

    public static void showTool(android.content.Context context) {
        LogFloatView view = cached.get();
        if (view == null || view.getContext() != context) {
            view = new LogFloatView(context);
            cached = new java.lang.ref.WeakReference<>(view);
        }
        view.show();
    }

    private final FloatLogBinding binding;
    private final LogFloatViewAdapter adapter;
    private final String tag = LogFloatView.class.getName();
    private int lastRootCount = 0;
    // 是否跟随最新日志滚动；用户翻看或展开节点后暂停
    private boolean autoFollow = true;

    private final int minWidth, minHeight;
    private final int maxWidth, maxHeight;

    private Task task;
    private int originWidth = 0, originHeight = 0;
    private int width = 0, height = 0;

    private float lastX = 0, lastY = 0;
    private boolean canDrag = false;
    private boolean dragging = false;
    private boolean expanded = true;

    @SuppressLint("ClickableViewAccessibility")
    public LogFloatView(@NonNull Context context) {
        this(context, null);
    }

    @SuppressLint("ClickableViewAccessibility")
    public LogFloatView(@NonNull Context context, @Nullable Task task) {
        super(context);
        this.task = task;

        minWidth = (int) DisplayUtil.dp2px(context, 168);
        minHeight = (int) DisplayUtil.dp2px(context, 128);
        Size size = DisplayUtil.getScreenSize(context);
        maxWidth = size.getWidth();
        maxHeight = (int) (size.getHeight() * 0.8f);

        binding = FloatLogBinding.inflate(LayoutInflater.from(context), this, true);
        adapter = new LogFloatViewAdapter();
        // 手动展开/折叠后暂停自动跟随，避免正在看的分支被新日志顶走
        adapter.setOnNodeToggled(() -> autoFollow = false);
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) autoFollow = false;
                // 手动拖回底部后恢复跟随
                if (newState == RecyclerView.SCROLL_STATE_IDLE && isNearBottom()) autoFollow = true;
            }
        });
        initResizeHandle();

        binding.title.setOnClickListener(v -> {
            ListPopupWindow popup = new ListPopupWindow(context);
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, R.layout.widget_textview_item);
            List<Task> tasks = TaskSaver.getInstance().getTasks();
            List<Task> validTasks = new ArrayList<>();
            tasks.forEach(t -> {
                if (t.isEnable()) {
                    validTasks.add(t);
                    arrayAdapter.add(t.getTitle());
                }
            });
            popup.setAdapter(arrayAdapter);
            popup.setAnchorView(binding.title);
            popup.setOnItemClickListener((parent, view, position, id) -> {
                this.task = validTasks.get(position);
                binding.title.setText(this.task.getTitle());
                adapter.setLogSave(LogSaver.getInstance().getLogSave(this.task.getId()));
                popup.dismiss();
            });
            popup.setModal(true);
            popup.show();

            post(() -> DisplayUtil.setListPopupWindowAutoHeight(popup));
        });

        binding.closeButton.setOnClickListener(v -> dismiss());

        binding.exportButton.setOnClickListener(v -> {
            StringBuilder builder = new StringBuilder();
            for (LogInfo log : adapter.getLogs()) {
                builder.append("[").append(AppUtil.formatDateTime(context, log.getTime(), false, false)).append("] ");
                if (log.getLogObject() instanceof ActionLog actionLog) {
                    builder.append("[").append(actionLog.isExecute() ? "->" : "<-").append("] ");
                }
                builder.append(log.getLog());
                if (log.getLogObject() instanceof ActionLog actionLog) {
                    builder.append(" ").append(actionLog.getValues().values());
                }
                builder.append("\n");
            }
            AppUtil.shareText(context, builder.toString());
        });

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
                // 收起时显示本功能的图标，方便在桌面上认出是哪个窗口
                binding.expandButton.setIconResource(R.drawable.icon_note_stack);

            }
            FloatWindow.updateLayoutParam(tag);
        });

        binding.recyclerView.setAdapter(adapter);

        if (task != null) {
            binding.title.setText(task.getTitle());
            adapter.setLogSave(LogSaver.getInstance().getLogSave(task.getId()));
            binding.recyclerView.scrollToPosition(adapter.getItemCount() - 1);
        }

        binding.searchEdit.addTextChangedListener(new TextChangedListener() {
            @Override
            public void afterTextChanged(Editable s) {
                if (adapter.searchIndex >= 0 && adapter.searchIndex < adapter.getItemCount() - 1) {
                    int index = adapter.searchIndex;
                    adapter.searchIndex = -1;
                    adapter.notifyItemChanged(index);
                }
            }
        });

        binding.preButton.setOnClickListener(v -> searchLog(false));
        binding.nextButton.setOnClickListener(v -> searchLog(true));
        // 搜索选项平时收起，点右侧按钮在窗口内展开，避免悬浮窗里的弹窗被裁到屏幕外
        binding.menuButton.setOnClickListener(v -> toggleSearchOptions());
        binding.pinyinCheck.addOnCheckedChangeListener((buttonView, isChecked) -> SettingSaver.BLUEPRINT_SEARCH_WITH_PINYIN.set(isChecked));
        binding.pinyinCheck.setChecked(SettingSaver.BLUEPRINT_SEARCH_WITH_PINYIN.get());
        binding.posCheck.addOnCheckedChangeListener((buttonView, isChecked) -> SettingSaver.BLUEPRINT_SEARCH_WITH_POSITION.set(isChecked));
        binding.posCheck.setChecked(SettingSaver.BLUEPRINT_SEARCH_WITH_POSITION.get());
        binding.caseCheck.addOnCheckedChangeListener((buttonView, isChecked) -> SettingSaver.BLUEPRINT_SEARCH_WITH_CASE.set(isChecked));
        binding.caseCheck.setChecked(SettingSaver.BLUEPRINT_SEARCH_WITH_CASE.get());
        binding.regCheck.addOnCheckedChangeListener((buttonView, isChecked) -> SettingSaver.BLUEPRINT_SEARCH_WITH_REGEX.set(isChecked));
        binding.regCheck.setChecked(SettingSaver.BLUEPRINT_SEARCH_WITH_REGEX.get());

        binding.clearButton.setOnClickListener(v -> {
            if (this.task == null) return;
            LogSaver.getInstance().clearLog(this.task.getId());
            refreshLogs();
        });
    }

    // 搜索选项在窗口内展开/收起
    private void toggleSearchOptions() {
        boolean show = binding.searchOptionsBox.getVisibility() != View.VISIBLE;
        if (show) {
            AnimUtil.show(binding.searchOptionsBox, -8f);
        } else {
            AnimUtil.hide(binding.searchOptionsBox, -8f);
        }
    }

    private void searchLog(boolean isNext) {
        String s = "";
        Editable text = binding.searchEdit.getText();
        if (text != null) s = text.toString();

        // 没有搜索词时，上下箭头在当前展开块里跳：上箭头到首行，下箭头到末行
        if (s.isEmpty()) {
            int count = adapter.getItemCount();
            if (count <= 0) return;
            int edge = adapter.findExpandedEdge(isNext, getAnchorPosition());
            if (edge < 0) edge = isNext ? count - 1 : 0;
            binding.recyclerView.scrollToPosition(edge);
            return;
        }

        int index = adapter.searchLog(s, isNext);
        if (index >= 0) binding.recyclerView.scrollToPosition(index);
        else binding.recyclerView.scrollToPosition(adapter.getItemCount() - 1);
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

            FloatWindow.setDragAble(tag, true);
        }
        return super.onInterceptTouchEvent(event);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    // 缩放交给手柄自己处理：手柄贴在窗口右下角，父容器的拦截容易和子控件抢事件
    private void initResizeHandle() {
        binding.dragImage.setOnTouchListener((view, event) -> {
            float x = event.getRawX();
            float y = event.getRawY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN -> {
                    lastX = x;
                    lastY = y;
                    dragging = true;
                    FloatWindow.setDragAble(tag, false);
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
                case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    dragging = false;
                    FloatWindow.setDragAble(tag, true);
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    public void show() {
        // 无障碍服务没起来时加窗会抛 BadToken，还可能留下关不掉的幽灵窗口，直接提示
        if (MainApplication.getInstance().getService() == null) {
            Toast.makeText(getContext(), R.string.app_setting_enable_error, Toast.LENGTH_SHORT).show();
            return;
        }

        Size size = DisplayUtil.getScreenSize(getContext());
        FloatWindow.with(MainApplication.getInstance().getService())
                .setLayout(this)
                .setTag(tag)
                .setSpecial(true)
                .setLocation(EAnchor.BOTTOM_CENTER, 0, -size.getHeight() / 4)
                .setExistEditText(true)
                .show();
        LogSaver.getInstance().addListener(this);
        postDelayed(pollRunnable, 500);
    }

    @Override
    public void dismiss() {
        removeCallbacks(pollRunnable);
        LogSaver.getInstance().removeListener(this);
        FloatWindow.dismiss(tag);
    }

    // 兜底轮询：只看根节点数量，漏了通知也能补上，且不做整表重建
    private final Runnable pollRunnable = this::pollLogs;

    private void pollLogs() {
        if (task == null) return;
        LogSave logSave = LogSaver.getInstance().getLogSave(task.getId());
        if (logSave.getLogCount() != lastRootCount) {
            boolean follow = autoFollow && isNearBottom();
            lastRootCount = logSave.getLogCount();
            adapter.syncRoots(logSave);
            if (follow) scrollToBottom();
            updateEmptyTips();
        }
        postDelayed(pollRunnable, 500);
    }

    // 首次加载/切换任务时重建列表
    private void refreshLogs() {
        if (task == null) return;
        LogSave logSave = LogSaver.getInstance().getLogSave(task.getId());
        lastRootCount = logSave.getLogCount();
        adapter.setLogSave(logSave);
        // 首次打开/切换任务时回到最新一条，并恢复跟随
        autoFollow = true;
        scrollToBottom();
    }

    // 当前视口的第一行，用作"所在层"的锚点
    private int getAnchorPosition() {
        RecyclerView.LayoutManager manager = binding.recyclerView.getLayoutManager();
        if (manager instanceof LinearLayoutManager linearManager) {
            int first = linearManager.findFirstVisibleItemPosition();
            if (first != RecyclerView.NO_POSITION) return first;
        }
        return 0;
    }

    // 只有用户本来就在底部时才自动跟随；正在往上翻看时不要把人顶走
    private boolean isNearBottom() {
        RecyclerView recyclerView = binding.recyclerView;
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if (manager == null || manager.getItemCount() == 0) return true;
        if (manager instanceof LinearLayoutManager linearManager) {
            int last = linearManager.findLastVisibleItemPosition();
            if (last == RecyclerView.NO_POSITION) return true;
            return last >= manager.getItemCount() - 2;
        }
        return true;
    }

    private void scrollToBottom() {
        int count = adapter.getItemCount();
        if (count > 0) binding.recyclerView.scrollToPosition(count - 1);
        updateEmptyTips();
    }

    // 没有日志时给一句提示，避免窗口中间一片空白
    private void updateEmptyTips() {
        boolean empty = adapter.getItemCount() == 0;
        binding.emptyTips.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onNewLog(LogSave logSave, LogInfo log) {
        if (task == null) return;
        if (!logSave.getKey().equals(task.getId())) return;
        // 逐条插入，不重建列表；只在本来就在底部时跟随滚动
        post(() -> {
            boolean follow = autoFollow && isNearBottom();
            adapter.syncLog(logSave, log);
            lastRootCount = logSave.getLogCount();
            if (follow) scrollToBottom();
            updateEmptyTips();
        });
    }
}
