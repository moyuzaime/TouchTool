package top.bogey.touch_tool.ui.blueprint;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposePathEffect;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.LightingColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionInfo;
import top.bogey.touch_tool.bean.action.parent.SyncAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.save.SavedCardGroup;
import top.bogey.touch_tool.bean.save.SavedGroupSaver;
import top.bogey.touch_tool.bean.save.task.TaskSaveListener;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.save.variable.VariableSaveListener;
import top.bogey.touch_tool.bean.save.variable.VariableSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.bean.task.Variable;
import top.bogey.touch_tool.ui.blueprint.card.ActionCard;
import top.bogey.touch_tool.ui.blueprint.card.ShowTextActionCard;
import top.bogey.touch_tool.ui.blueprint.pin.PinCachedView;
import top.bogey.touch_tool.ui.blueprint.pin.PinView;
import top.bogey.touch_tool.ui.blueprint.selecter.select_action.SelectActionByPinDialog;
import top.bogey.touch_tool.ui.blueprint.selecter.select_action.SelectActionDialog;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.PngStreamWriter;

public class CardLayoutView extends FrameLayout implements TaskSaveListener, VariableSaveListener {
    public static final int GRID_DP_SIZE = 12;

    public enum TouchState {
        TOUCH_NONE,
        TOUCH_BACKGROUND,
        TOUCH_CARD,
        TOUCH_PIN,

        TOUCH_SCALE,
        TOUCH_SELECT_AREA,

        TOUCH_DRAG_BACKGROUND,
        TOUCH_DRAG_CARD,
        TOUCH_DRAG_PIN,
        TOUCH_DRAG_LINK
    }

    private static final long LONG_TOUCH_TIME = 300L;

    public interface OnScaleChangedListener {
        void onScaleChanged(float scale);
    }

    private OnScaleChangedListener scaleChangedListener;

    // 卡片组暂存：每组是一批动作 id，只存内存不落盘
    private final List<SavedCardGroup> savedCardGroups = new ArrayList<>();
    // 框选等批量改选中时先跳过工具栏刷新
    private boolean batchSelecting = false;
    private final List<ActionCard> savedGroupHighlightCards = new ArrayList<>();
    private int highlightedGroup = -1;
    private boolean savedGroupPanelOpen = false;
    private View savedGroupPanel;
    private ViewGroup savedGroupListBox;
    private View savedGroupButton;
    // 顶栏入口：有暂存组时常驻，用来在没有选中卡片时也能打开面板
    private View savedGroupBarButton;

    // 连线跳转：记住跳转前的视图位置，供「跳回」还原
    private PinView focusLinkPin;
    private PinView focusLinkOpposite;
    private View jumpBackButton;
    private boolean hasSavedOffset = false;
    private float savedOffsetX;
    private float savedOffsetY;

    private final Handler longTouchHandler;
    private final Handler doubleTouchHandler;

    private final float gridSize;
    private final Paint gridPaint;
    private final Paint linkPaint;

    private final ScaleGestureDetector detector;

    private TouchState touchState = TouchState.TOUCH_NONE;
    final Set<ActionCard> selectedCards = new HashSet<>();
    private ActionCard touchedCard;

    private final Map<String, String> selectedLinks = new HashMap<>();
    private PinView touchedPin;
    private PinView lastTouchedPin;
    private PinView dragOnPin;

    private RectF selectArea = new RectF();
    private SelectActionDialog actionDialog = null;

    private ViewGroup cacheBox = null;
    private RectF cacheBoxArea = new RectF();
    private final List<CachedPin> cachedPins = new ArrayList<>();

    private float startX, startY, realStartX, realStartY;
    private float lastX, lastY, realLastX, realLastY;
    private boolean movingTouch = false;

    private float offsetX, offsetY, scale;
    private final Map<Task, TaskEditLayoutInfo> layoutInfoMap = new HashMap<>();

    private final Map<String, ActionCard> cards = new HashMap<>();
    private Task task;
    private List<Action> actions;

    private boolean editable = true;

    private boolean loaded = false;

    public CardLayoutView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        setSaveEnabled(false);
        setSaveFromParentEnabled(false);

        longTouchHandler = new Handler(Looper.getMainLooper());
        doubleTouchHandler = new Handler(Looper.getMainLooper());

        gridSize = DisplayUtil.dp2px(context, GRID_DP_SIZE);

        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStrokeWidth(1);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setColor(DisplayUtil.getAttrColor(context, com.google.android.material.R.attr.colorPrimaryVariant));
        gridPaint.setAlpha(50);

        linkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linkPaint.setStyle(Paint.Style.STROKE);
        linkPaint.setStrokeCap(Paint.Cap.ROUND);
        linkPaint.setStrokeJoin(Paint.Join.ROUND);

        detector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                float oldScale = scale;
                scale *= detector.getScaleFactor();
                scale = Math.clamp(scale, 0.2f, 2f);

                float v = 1 - scale / oldScale;
                float focusX = detector.getFocusX() - offsetX;
                float focusY = detector.getFocusY() - offsetY;

                offsetX += focusX * v;
                offsetY += focusY * v;

                updateCardsPos();
                invalidate();
                if (scaleChangedListener != null) scaleChangedListener.onScaleChanged(scale);
                return true;
            }

            @Override
            public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
                touchState = TouchState.TOUCH_SCALE;
                return true;
            }

            @Override
            public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
                touchState = TouchState.TOUCH_NONE;
            }
        });
        detector.setQuickScaleEnabled(true);

        TaskSaver.getInstance().addListener(this);
        VariableSaver.getInstance().addListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        TaskSaver.getInstance().removeListener(this);
        VariableSaver.getInstance().removeListener(this);
    }

    public float getScaleValue() {
        return scale;
    }

    // 以视图中心为锚点缩放，供缩放条调用
    public void setScaleValue(float value) {
        float newScale = Math.clamp(value, 0.2f, 2f);
        float ratio = 1 - newScale / scale;
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        offsetX += (centerX - offsetX) * ratio;
        offsetY += (centerY - offsetY) * ratio;
        scale = newScale;
        updateCardsPos();
        invalidate();
        if (scaleChangedListener != null) scaleChangedListener.onScaleChanged(scale);
    }

    public void setOnScaleChangedListener(OnScaleChangedListener listener) {
        scaleChangedListener = listener;
        if (listener != null) listener.onScaleChanged(scale);
    }

    // 跳到指定方向上最远的卡片：0 左、1 上、2 右、3 下
    public void jumpToEdge(int direction) {
        if (cards.isEmpty()) return;

        ActionCard target = null;
        int edgeValue = 0;
        for (ActionCard card : cards.values()) {
            Point pos = card.getAction().getPos();
            if (pos == null) continue;
            int value = (direction == 0 || direction == 2) ? pos.x : pos.y;
            if (target == null || ((direction == 0 || direction == 1) ? value < edgeValue : value > edgeValue)) {
                target = card;
                edgeValue = value;
            }
        }
        if (target == null) return;

        Point pos = target.getAction().getPos();
        float grid = getScaleGridSize();
        if (direction == 0 || direction == 2) {
            animateOffsetTo(-pos.x * grid + (getWidth() - target.getWidth() * scale) / 2f, offsetY);
        } else {
            animateOffsetTo(offsetX, -pos.y * grid + (getHeight() - target.getHeight() * scale) / 3f);
        }

        if (selectedCards.isEmpty()) {
            cleanSelectedCards();
            addSelectedCard(target);
        }
        target.bringToFront();
    }

    private void animateOffsetTo(float toX, float toY) {
        animateOffsetTo(toX, toY, null);
    }

    private void animateOffsetTo(float toX, float toY, Runnable endAction) {
        float fromX = offsetX;
        float fromY = offsetY;
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.addUpdateListener(a -> {
            float fraction = (float) a.getAnimatedValue();
            offsetX = fromX + (toX - fromX) * fraction;
            offsetY = fromY + (toY - fromY) * fraction;
            updateCardsPos();
        });
        if (endAction != null) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    endAction.run();
                }
            });
        }
        animator.start();
    }

    // ===== 卡片组暂存 =====

    public void initSavedGroupViews(View panel, ViewGroup listBox, View button) {
        savedGroupPanel = panel;
        savedGroupListBox = listBox;
        savedGroupButton = button;
        button.setOnClickListener(v -> onSavedGroupButtonClick());
        // 长按清空全部暂存组，先确认，避免误触把整组配置清掉
        button.setOnLongClickListener(v -> {
            confirmClearSavedGroups();
            return true;
        });
        refreshSavedGroupViews();
    }

    // 清空全部暂存组：先弹确认框
    private void confirmClearSavedGroups() {
        if (savedCardGroups.isEmpty()) return;
        if (isSavedGroupAddMode()) return;
        new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.blueprint_saved_group_clear)
                .setMessage(R.string.blueprint_saved_group_clear_confirm)
                .setPositiveButton(R.string.remove_task, (dialog, which) -> {
                    savedCardGroups.clear();
                    highlightedGroup = -1;
                    savedGroupPanelOpen = false;
                    clearSavedGroupHighlight();
                    saveSavedGroups();
                    refreshSavedGroupViews();
                    Toast.makeText(getContext(), R.string.blueprint_saved_group_cleared, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // 顶栏常驻入口：只要存在暂存组就显示，没有选中卡片时也能打开面板
    public void setSavedGroupBarButton(View button) {
        savedGroupBarButton = button;
        button.setOnClickListener(v -> {
            if (savedCardGroups.isEmpty()) return;
            savedGroupPanelOpen = !savedGroupPanelOpen;
            refreshSavedGroupViews();
        });
        // 与选中工具栏里的暂存按钮一致：长按可清空
        button.setOnLongClickListener(v -> {
            confirmClearSavedGroups();
            return true;
        });
        refreshSavedGroupViews();
    }

    private Set<String> currentSelectionIds() {
        Set<String> ids = new HashSet<>();
        for (ActionCard card : selectedCards) {
            if (card != null && card.getAction() != null) ids.add(card.getAction().getId());
        }
        return ids;
    }

    private boolean isSavedGroupAddMode() {
        Set<String> ids = currentSelectionIds();
        if (ids.isEmpty()) return false;
        for (SavedCardGroup group : savedCardGroups) {
            if (group.getIds().containsAll(ids)) return false;
        }
        return true;
    }

    private void onSavedGroupButtonClick() {
        if (isSavedGroupAddMode()) {
            Set<String> ids = currentSelectionIds();
            savedCardGroups.add(new SavedCardGroup(buildDefaultGroupName(ids), ids));
            highlightedGroup = savedCardGroups.size() - 1;
            savedGroupPanelOpen = true;
            saveSavedGroups();
        } else {
            savedGroupPanelOpen = !savedGroupPanelOpen;
        }
        refreshSavedGroupViews();
    }

    // 新建组默认用卡片备注命名；都没有备注时用卡片名
    private String buildDefaultGroupName(Set<String> ids) {
        List<ActionCard> groupCards = new ArrayList<>();
        for (String id : ids) {
            ActionCard card = cards.get(id);
            if (card != null) groupCards.add(card);
        }
        // 按位置排序，保证「第一张」每次都是同一张
        groupCards.sort((o1, o2) -> {
            Point p1 = o1.getAction().getPos();
            Point p2 = o2.getAction().getPos();
            int byY = Integer.compare(p1.y, p2.y);
            return byY != 0 ? byY : Integer.compare(p1.x, p2.x);
        });

        for (ActionCard card : groupCards) {
            String description = card.getAction().getDescription();
            if (description != null && !description.trim().isEmpty()) return trimGroupName(description);
        }
        if (!groupCards.isEmpty()) return trimGroupName(groupCards.get(0).getAction().getTitle());
        return "";
    }

    private String trimGroupName(String name) {
        if (name == null) return "";
        String result = name.trim().replace('\n', ' ');
        return result.length() > 16 ? result.substring(0, 16) + "…" : result;
    }

    // 组内容变化后写回本地，切换任务或重启后仍能恢复
    private void saveSavedGroups() {
        if (task == null) return;
        SavedGroupSaver.getInstance().saveGroups(task.getId(), savedCardGroups);
    }

    private List<ActionCard> resolveSavedGroup(int index) {
        List<ActionCard> result = new ArrayList<>();
        if (index >= 0 && index < savedCardGroups.size()) {
            for (String id : savedCardGroups.get(index).getIds()) {
                ActionCard card = cards.get(id);
                if (card != null) result.add(card);
            }
        }
        return result;
    }

    private void clearSavedGroupHighlight() {
        for (ActionCard card : savedGroupHighlightCards) {
            if (card != null) {
                card.setSavedGroupHighlight(false);
                if (selectedCards.contains(card)) card.setSelected(true);
            }
        }
        savedGroupHighlightCards.clear();
    }

    private void applySavedGroupHighlight(List<ActionCard> groupCards) {
        clearSavedGroupHighlight();
        if (groupCards.isEmpty()) return;
        for (ActionCard card : groupCards) {
            if (card != null && card.getAction() != null && cards.get(card.getAction().getId()) == card) {
                card.setSavedGroupHighlight(true);
                savedGroupHighlightCards.add(card);
            }
        }
    }

    public boolean focusSavedGroup(int index) {
        List<ActionCard> groupCards = resolveSavedGroup(index);
        if (groupCards.isEmpty()) return false;
        highlightedGroup = index;
        // 跳转的同时把组内卡片选中，方便接着拖动或复制
        cleanSelectedCards();
        for (ActionCard card : groupCards) addSelectedCard(card);
        RectF area = calculateCardsArea(groupCards);
        animateOffsetTo(getWidth() / 2f - area.centerX(), getHeight() / 2f - area.centerY(),
                () -> applySavedGroupHighlight(groupCards));
        invalidate();
        refreshSavedGroupViews();
        return true;
    }

    public void moveSavedGroupToViewCenter(int index) {
        List<ActionCard> groupCards = resolveSavedGroup(index);
        if (groupCards.isEmpty()) return;
        highlightedGroup = index;
        if (!moveCardsToViewCenter(groupCards)) {
            Toast.makeText(getContext(), R.string.blueprint_move_selected_here_noop, Toast.LENGTH_SHORT).show();
        }
        applySavedGroupHighlight(groupCards);
        refreshSavedGroupViews();
    }

    public void removeSavedGroup(int index) {
        if (index < 0 || index >= savedCardGroups.size()) return;
        savedCardGroups.remove(index);
        if (savedCardGroups.isEmpty()) {
            highlightedGroup = -1;
            savedGroupPanelOpen = false;
            clearSavedGroupHighlight();
        } else if (highlightedGroup == index) {
            highlightedGroup = -1;
            clearSavedGroupHighlight();
        } else if (highlightedGroup > index) {
            highlightedGroup--;
            applySavedGroupHighlight(resolveSavedGroup(highlightedGroup));
        }
        saveSavedGroups();
        refreshSavedGroupViews();
    }

    private void refreshSavedGroupViews() {
        if (savedGroupPanel == null || savedGroupListBox == null || savedGroupButton == null) return;

        boolean addMode = isSavedGroupAddMode();
        boolean empty = savedCardGroups.isEmpty();
        // 按钮本身挂在底部工具栏里，只切换它自己的可见性，不能动整个按钮组
        savedGroupButton.setVisibility(!empty || addMode ? View.VISIBLE : View.GONE);
        if (savedGroupBarButton != null) savedGroupBarButton.setVisibility(empty ? View.GONE : View.VISIBLE);

        if (savedGroupButton instanceof MaterialButton button) {
            button.setIconResource(addMode ? R.drawable.icon_radio_button_unchecked : R.drawable.icon_radio_button_checked);
        }

        boolean open = savedGroupPanelOpen && !empty;
        setSavedGroupPanelOpen(open);
        if (!open) return;

        int rowHeight = (int) DisplayUtil.dp2px(getContext(), 36);
        int padding = (int) DisplayUtil.dp2px(getContext(), 8);
        ViewGroup.LayoutParams params = savedGroupPanel.getLayoutParams();
        if (params != null) {
            params.height = Math.min(rowHeight * 4 + padding, savedCardGroups.size() * rowHeight + padding);
            savedGroupPanel.requestLayout();
        }

        savedGroupListBox.removeAllViews();
        int buttonSize = (int) DisplayUtil.dp2px(getContext(), 32);
        for (int i = 0; i < savedCardGroups.size(); i++) {
            final int index = i;
            LinearLayout row = new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowHeight));

            SavedCardGroup group = savedCardGroups.get(i);
            TextView indexView = new TextView(getContext());
            String name = group.getName();
            if (name.isEmpty()) name = getContext().getString(R.string.blueprint_saved_group_default, i + 1);
            indexView.setText(name + " · " + group.getIds().size());
            indexView.setMaxLines(1);
            indexView.setEllipsize(TextUtils.TruncateAt.END);
            indexView.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            indexView.setTextSize(12);
            indexView.setTextColor(DisplayUtil.getAttrColor(getContext(), androidx.appcompat.R.attr.colorPrimary));
            indexView.setPadding((int) DisplayUtil.dp2px(getContext(), 6), 0, (int) DisplayUtil.dp2px(getContext(), 6), 0);
            indexView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
            // 当前组用圆角底色标出，避免整块方块贴边的生硬感
            GradientDrawable rowBackground = new GradientDrawable();
            rowBackground.setCornerRadius(DisplayUtil.dp2px(getContext(), 8));
            rowBackground.setColor(i == highlightedGroup
                    ? DisplayUtil.getAttrColor(getContext(), com.google.android.material.R.attr.colorTertiaryContainer)
                    : Color.TRANSPARENT);
            indexView.setBackground(rowBackground);
            indexView.setOnClickListener(v -> focusSavedGroup(index));
            // 长按编号给这一组改名
            indexView.setOnLongClickListener(v -> {
                AppUtil.showEditDialog(getContext(), R.string.blueprint_saved_group_name, group.getName(), newName -> {
                    group.setName(newName == null ? "" : newName);
                    saveSavedGroups();
                    refreshSavedGroupViews();
                });
                return true;
            });
            row.addView(indexView);

            row.addView(makeSavedGroupActionButton(buttonSize, R.drawable.icon_delete, R.string.remove_task,
                    v -> removeSavedGroup(index)));
            row.addView(makeSavedGroupActionButton(buttonSize, R.drawable.icon_my_location, R.string.blueprint_move_selected_here,
                    v -> moveSavedGroupToViewCenter(index)));
            savedGroupListBox.addView(row);
        }
    }

    // 面板带动画展开/收起，避免硬切造成的突兀感
    private void setSavedGroupPanelOpen(boolean open) {
        if (savedGroupPanel == null) return;
        float shift = DisplayUtil.dp2px(getContext(), 8);
        savedGroupPanel.animate().cancel();
        if (open) {
            if (savedGroupPanel.getVisibility() != View.VISIBLE) {
                savedGroupPanel.setVisibility(View.VISIBLE);
                savedGroupPanel.setAlpha(0f);
                savedGroupPanel.setTranslationY(shift);
            }
            savedGroupPanel.animate().alpha(1f).translationY(0f).setDuration(160).start();
        } else if (savedGroupPanel.getVisibility() == View.VISIBLE) {
            savedGroupPanel.animate().alpha(0f).translationY(shift).setDuration(140)
                    .withEndAction(() -> savedGroupPanel.setVisibility(View.GONE)).start();
        }
    }

    private View makeSavedGroupActionButton(int size, int iconRes, int descRes, OnClickListener listener) {
        MaterialButton button = new MaterialButton(getContext(), null, com.google.android.material.R.attr.materialIconButtonStyle);
        button.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        button.setIconResource(iconRes);
        button.setIconTint(ColorStateList.valueOf(DisplayUtil.getAttrColor(getContext(), androidx.appcompat.R.attr.colorPrimary)));
        button.setInsetTop(0);
        button.setInsetBottom(0);
        button.setPadding(0, 0, 0, 0);
        button.setContentDescription(getContext().getString(descRes));
        button.setOnClickListener(listener);
        return button;
    }

    // 卡片被删除时，把它的 id 从所有暂存组里剔除，空了就整组移除
    private boolean pruneSavedGroupsOnCardRemoved(ActionCard card) {
        if (card == null || card.getAction() == null || savedCardGroups.isEmpty()) return false;
        String id = card.getAction().getId();
        if (id == null) return false;

        boolean changed = false;
        boolean clearHighlight = false;
        int shift = 0;
        for (int i = savedCardGroups.size() - 1; i >= 0; i--) {
            Set<String> group = savedCardGroups.get(i).getIds();
            if (!group.remove(id)) continue;
            if (group.isEmpty()) {
                savedCardGroups.remove(i);
                if (i == highlightedGroup) {
                    changed = true;
                    clearHighlight = true;
                } else {
                    if (i < highlightedGroup) shift++;
                    changed = true;
                }
            } else {
                changed = true;
            }
        }
        if (!changed) return false;

        if (clearHighlight) {
            highlightedGroup = -1;
            clearSavedGroupHighlight();
        } else {
            highlightedGroup -= shift;
        }
        saveSavedGroups();
        refreshSavedGroupViews();
        return true;
    }

    private boolean moveCardsToViewCenter(Collection<ActionCard> collection) {
        if (collection.isEmpty()) return false;
        List<ActionCard> movable = new ArrayList<>();
        for (ActionCard card : new HashSet<>(collection)) {
            if (card == null || card.getAction() == null) continue;
            if (card.getAction().isLocked()) continue;
            if (cards.get(card.getAction().getId()) != card) continue;
            movable.add(card);
        }
        if (movable.isEmpty()) return false;

        float grid = getScaleGridSize();
        if (grid <= 0 || getWidth() <= 0 || getHeight() <= 0) return false;

        RectF area = calculateCardsArea(movable);
        int dx = Math.round((getWidth() / 2f - offsetX - area.centerX()) / grid);
        int dy = Math.round((getHeight() / 2f - offsetY - area.centerY()) / grid);
        if (dx == 0 && dy == 0) return false;

        for (ActionCard card : movable) {
            Point pos = card.getAction().getPos();
            if (pos == null) continue;
            pos.x += dx;
            pos.y += dy;
            updateCardPos(card);
        }
        return true;
    }

    // ===== 连线跳转 / 跳回 =====

    public void setJumpBackButton(View button) {
        jumpBackButton = button;
        updateJumpBackButton();
    }

    private void updateJumpBackButton() {
        if (jumpBackButton != null) jumpBackButton.setVisibility(focusLinkPin != null ? View.VISIBLE : View.GONE);
    }

    // 点击针脚：有多条连线时先选一条，再跳到对面那张卡片
    private void focusPinLink(PinView pinView) {
        if (pinView == null) return;
        Map<String, String> links = pinView.getPin().getLinks();
        if (links.isEmpty()) return;
        if (links.size() > 1) {
            showBranchSelector(pinView);
            return;
        }
        Map.Entry<String, String> entry = links.entrySet().iterator().next();
        PinView opposite = findPinView(entry.getValue(), entry.getKey());
        if (opposite != null) showFocusLink(pinView, opposite);
    }

    private PinView findPinView(String ownerId, String pinId) {
        ActionCard card = cards.get(ownerId);
        return card == null ? null : card.getPinView(pinId);
    }

    private void showBranchSelector(PinView pinView) {
        List<Map.Entry<String, String>> entries = new ArrayList<>(pinView.getPin().getLinks().entrySet());
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) labels.add(buildLinkLabel(i, entries.get(i)));

        new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.blueprint_focus_link_title)
                .setAdapter(new ArrayAdapter<>(getContext(), R.layout.widget_textview_item, labels), (dialog, which) -> {
                    Map.Entry<String, String> entry = entries.get(which);
                    PinView opposite = findPinView(entry.getValue(), entry.getKey());
                    if (opposite != null) showFocusLink(pinView, opposite);
                })
                .show();
    }

    private String buildLinkLabel(int index, Map.Entry<String, String> entry) {
        Action action = task == null ? null : task.getAction(entry.getValue());
        if (action == null) return (index + 1) + ". " + entry.getKey();
        Pin pin = action.getPinById(entry.getKey());
        return (index + 1) + ". " + action.getTitle() + " · " + (pin == null ? "" : pin.getTitle());
    }

    private void showFocusLink(PinView pin, PinView opposite) {
        if (pin == null || opposite == null) return;
        if (!hasSavedOffset) {
            savedOffsetX = offsetX;
            savedOffsetY = offsetY;
            hasSavedOffset = true;
        }
        focusLinkPin = pin;
        focusLinkOpposite = opposite;
        updateJumpBackButton();
        focusLinkedCard(opposite);
    }

    private void focusLinkedCard(PinView opposite) {
        ActionCard card = cards.get(opposite.getPin().getOwnerId());
        if (card == null) return;
        offsetX += getWidth() / 2f - (card.getX() + card.getWidth() / 2f);
        offsetY += getHeight() / 2f - (card.getY() + card.getHeight() / 2f);
        updateCardsPos();

        cleanSelectedCards();
        addSelectedCard(card);
        card.bringToFront();
        invalidate();
    }

    public void jumpBack() {
        if (!hasSavedOffset) return;
        hasSavedOffset = false;
        focusLinkPin = null;
        focusLinkOpposite = null;
        updateJumpBackButton();
        animateOffsetTo(savedOffsetX, savedOffsetY);
    }

    public void setTask(Task task) {
        if (this.task != null) {
            TaskEditLayoutInfo layoutInfo = layoutInfoMap.computeIfAbsent(this.task, t -> new TaskEditLayoutInfo());
            layoutInfo.setOffsetX(offsetX);
            layoutInfo.setOffsetY(offsetY);
            layoutInfo.setScale(scale);
        }
        TaskEditLayoutInfo layoutInfo = layoutInfoMap.computeIfAbsent(task, t -> new TaskEditLayoutInfo());

        this.task = task;
        loaded = false;
        offsetX = layoutInfo.getOffsetX();
        offsetY = layoutInfo.getOffsetY();
        scale = layoutInfo.getScale();
        cleanSelectedCards();
        clearCachePins();

        // 切换任务时载入该任务自己的卡片组
        savedCardGroups.clear();
        savedCardGroups.addAll(SavedGroupSaver.getInstance().getGroups(task.getId()));
        highlightedGroup = -1;
        savedGroupPanelOpen = false;
        clearSavedGroupHighlight();
        refreshSavedGroupViews();

        cards.values().forEach(this::removeView);
        cards.clear();

        actions = task.getActions();
        actions.sort((o1, o2) -> {
            Point pos1 = o1.getPos();
            Point pos2 = o2.getPos();
            return (pos1.x + pos1.y) - (pos2.x + pos2.y);
        });
        loadCards(0);
    }

    private void loadCards(int index) {
        Map<String, ActionCard> cardMap = new HashMap<>();
        for (int i = index; i < index + 5; i++) {
            if (i >= actions.size()) {
                updateCardsPos(cardMap);
                checkCards();
                invalidate();
                loaded = true;
                return;
            }
            Action action = actions.get(i);
            if (action instanceof SyncAction syncAction) syncAction.sync(task);
            ActionCard card = newCard(actions.get(i));
            cards.put(action.getId(), card);
            cardMap.put(action.getId(), card);
            addView(card);
        }
        updateCardsPos(cardMap);
        postDelayed(() -> loadCards(index + 5), 100);
    }

    public ActionCard newCard(Action action) {
        ActionInfo info = ActionInfo.getActionInfo(action.getType());
        try {
            assert info != null;
            Constructor<? extends ActionCard> constructor = info.getCardClass().getConstructor(Context.class, Task.class, Action.class);
            return constructor.newInstance(getContext(), task, action);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 添加卡片
    public ActionCard addCard(Action action) {
        // 单个任务内只能有一个
        if (action.hasFlag(Action.SINGLE_IN_TASK)) {
            List<Action> actions = task.getActions(action.getClass());
            if (!actions.isEmpty()) {
                Toast.makeText(getContext(), R.string.custom_action_single_error, Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        if (action instanceof SyncAction syncAction) {
            // 任务中这个动作互相同步数据
            if (action.hasFlag(Action.SYNC_IN_TASK)) {
                List<Action> actions = task.getActions(action.getClass());
                if (!actions.isEmpty()) {
                    Action first = actions.get(0);
                    Action newCopy = first.newCopy();
                    newCopy.setPos(action.getPos());
                    syncAction = (SyncAction) newCopy;
                    action = newCopy;
                }
            }
            syncAction.sync(task);
        }

        task.addAction(action);
        ActionCard card = newCard(action);
        cards.put(action.getId(), card);
        addView(card);
        updateCardPos(card);

        // 新卡片淡入并轻微放大，避免凭空出现
        card.setAlpha(0f);
        card.setScaleX(scale * 0.92f);
        card.setScaleY(scale * 0.92f);
        card.animate()
                .alpha(1f)
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(180)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        return card;
    }

    public void removeCard(ActionCard card) {
        Action action = card.getAction();
        action.getPins().forEach(pin -> pin.clearLinks(task));
        task.removeAction(action.getId());
        cards.remove(action.getId());
        pruneSavedGroupsOnCardRemoved(card);

        if (selectedCards.remove(card)) refreshEditView();

        // 淡出后再移除视图，避免卡片直接消失
        card.animate()
                .alpha(0f)
                .scaleX(scale * 0.92f)
                .scaleY(scale * 0.92f)
                .setDuration(120)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    removeView(card);
                    card.setAlpha(1f);
                })
                .start();
    }

    public void initCardPos(ActionCard card) {
        card.measure(0, 0);
        float width = card.getMeasuredWidth() * scale;
        float height = card.getMeasuredHeight() * scale;
        float x, y;
        if (lastX <= 0 && lastY <= 0) {
            x = (getWidth() - width) / 2f;
            y = (getHeight() - height) / 5f;
        } else {
            x = lastX - width / 2f;
            y = lastY - height / 2f;
        }
        card.getAction().setPos((int) ((x - offsetX) / getScaleGridSize()), (int) ((y - offsetY) / getScaleGridSize()));
        updateCardPos(card);
    }

    public void updateCardPos(ActionCard card) {
        card.setScaleX(scale);
        card.setScaleY(scale);
        Action action = card.getAction();
        Point pos = action.getPos();
        float x = pos.x * getScaleGridSize() + offsetX;
        float y = pos.y * getScaleGridSize() + offsetY;
        card.updateCardPos(x, y);

        RectF area = getCardArea(card);
        area.offset(offsetX, offsetY);
        RectF windowSize = new RectF(0, 0, getWidth(), getHeight());
        boolean intersects = RectF.intersects(windowSize, area);
        boolean contains = windowSize.contains(area);
        card.setNeedDraw(intersects || contains);
    }

    public void updateCardsPos() {
        updateCardsPos(cards);
    }

    public void updateCardsPos(Map<String, ActionCard> cards) {
        cards.values().forEach(card -> {
            updateCardPos(card);
            if (card instanceof ShowTextActionCard) card.bringToFront();
        });
    }

    public ActionCard getActionCard(Action action) {
        return cards.get(action.getId());
    }

    private ActionCard getActionCard(float x, float y, boolean checkLock) {
        List<ActionCard> cards = new ArrayList<>(this.cards.values());
        cards.sort((o1, o2) -> indexOfChild(o2) - indexOfChild(o1));
        for (ActionCard card : cards) {
            if (checkLock && card.getAction().isLocked()) continue;
            RectF area = getCardArea(card);
            if (area.contains(x, y)) return card;
        }
        return null;
    }

    public void focusCard(String actionId) {
        focusCard(cards.get(actionId));
    }

    public void focusCard(ActionCard card) {
        if (card == null) return;

        cleanSelectedCards();
        addSelectedCard(card);
        card.bringToFront();
        Action action = card.getAction();
        Point pos = action.getPos();
        float x = -pos.x * getScaleGridSize() + (getWidth() - card.getWidth() * scale) / 2f;
        float y = -pos.y * getScaleGridSize() + (getHeight() - card.getHeight() * scale) / 3f;
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            offsetX += (x - offsetX) * value;
            offsetY += (y - offsetY) * value;
            updateCardsPos();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                card.startFocusAnim();
            }
        });
        animator.start();
    }

    public void addSelectedCard(Action action) {
        ActionCard card = getActionCard(action);
        if (card != null) addSelectedCard(card);
    }

    private void addSelectedCard(ActionCard card) {
        selectedCards.add(card);
        card.setSelected(true);
        refreshEditView();
    }

    public boolean removeSelectedCard(Action action) {
        ActionCard card = getActionCard(action);
        if (card != null) return removeSelectedCard(card);
        return false;
    }

    private boolean removeSelectedCard(ActionCard card) {
        boolean removed = selectedCards.remove(card);
        card.setSelected(false);
        refreshEditView();
        return removed;
    }

    public void cleanSelectedCards() {
        selectedCards.forEach(card -> card.setSelected(false));
        selectedCards.clear();
        refreshEditView();
    }

    private void refreshEditView() {
        // 框选等批量改选中时先不刷新，等整批改完再统一刷新
        if (batchSelecting) return;
        BlueprintView.tryShowFloatingToolBar(!selectedCards.isEmpty());
        refreshSavedGroupViews();
    }

    public void refreshPinView() {
        cards.forEach((id, card) -> card.getPinViews().forEach((pinId, pinView) -> pinView.refreshCopyButton()));
    }

    private RectF getCardArea(ActionCard card) {
        float gridSize = getScaleGridSize();
        Point cardPos = card.getAction().getPos();
        float cardX = cardPos.x * gridSize;
        float cardY = cardPos.y * gridSize;
        float cardWidth = card.getWidth() * scale;
        float cardHeight = card.getHeight() * scale;
        return new RectF(cardX, cardY, cardX + cardWidth, cardY + cardHeight);
    }

    private RectF calculateCardsArea(Collection<ActionCard> cards) {
        List<PointF> points = new ArrayList<>();
        cards.forEach(card -> {
            RectF cardArea = getCardArea(card);
            points.add(new PointF(cardArea.left, cardArea.top));
            points.add(new PointF(cardArea.right, cardArea.bottom));
        });
        return DisplayUtil.getPointFsArea(points);
    }

    public float getScaleGridSize() {
        return gridSize * scale;
    }

    public float getGridSize() {
        return gridSize;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        float realX = x - offsetX;
        float realY = y - offsetY;

        if (!editable) return true;

        ActionCard card = getActionCard(realX, realY, false);
        if (card != null) {
            if (card.isEmptyPosition(x - card.getX(), y - card.getY())) {
                return true;
            }
        }
        return super.onInterceptTouchEvent(event);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        detector.onTouchEvent(event);
        if (touchState == TouchState.TOUCH_SCALE) {
            longTouchHandler.removeCallbacksAndMessages(null);
            doubleTouchHandler.removeCallbacksAndMessages(null);
            return true;
        }

        float gridSize = getScaleGridSize();
        float x = event.getX();
        float y = event.getY();
        float realX = x - offsetX;
        float realY = y - offsetY;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN -> {
                // 点画布任意位置收起暂存面板
                if (savedGroupPanelOpen) {
                    savedGroupPanelOpen = false;
                    refreshSavedGroupViews();
                }
                startX = x;
                startY = y;
                realStartX = realX;
                realStartY = realY;

                lastX = x;
                lastY = y;
                realLastX = realX;
                realLastY = realY;

                touchState = TouchState.TOUCH_BACKGROUND;
                touchedCard = null;
                touchedPin = null;
                selectedLinks.clear();
                movingTouch = false;

                longTouchHandler.removeCallbacksAndMessages(null);

                if (editable) {
                    CachedPin cachedPin = findCachedPin(x, y);
                    if (cachedPin != null) {
                        if (cachedPin.isDragPin()) {
                            touchState = TouchState.TOUCH_PIN;
                            touchedPin = cachedPin.getTouchedPin();
                        } else {
                            touchState = TouchState.TOUCH_DRAG_LINK;
                            touchedPin = cachedPin.getTouchedPin();
                            selectedLinks.putAll(cachedPin.getSelectedLinks());
                        }
                        removeCachePin(cachedPin);
                    } else {
                        ActionCard card = getActionCard(realX, realY, true);
                        if (card != null) {
                            touchState = TouchState.TOUCH_CARD;
                            touchedCard = card;

                            card.bringToFront();
                            PinView pinView = card.getLinkAblePinView(x - card.getX(), y - card.getY());
                            if (pinView != null) {
                                touchState = TouchState.TOUCH_PIN;
                                touchedPin = pinView;
                            }
                        }
                    }
                }

                switch (touchState) {
                    case TOUCH_BACKGROUND -> {
                        if (!editable) break;
                        longTouchHandler.postDelayed(() -> {
                            touchState = TouchState.TOUCH_SELECT_AREA;
                            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        }, LONG_TOUCH_TIME);
                    }

                    case TOUCH_PIN -> longTouchHandler.postDelayed(() -> {
                        Pin pin = touchedPin.getPin();
                        if (pin.isLinked()) {
                            touchState = TouchState.TOUCH_DRAG_LINK;
                            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                            selectedLinks.putAll(pin.getLinks());
                            pin.clearLinks(task);
                        }

                        Pin linkedPin = pin.getLinkedPin(task);
                        if (linkedPin == null) return;
                        Action action = task.getAction(linkedPin.getOwnerId());
                        if (action == null) return;

                        touchState = TouchState.TOUCH_NONE;
                        focusCard(action.getId());
                    }, LONG_TOUCH_TIME);
                }
            }
            case MotionEvent.ACTION_MOVE -> {
                if (Math.abs(x - startX) > getScaleGridSize() || Math.abs(y - startY) > getScaleGridSize()) movingTouch = true;

                if (movingTouch) {
                    longTouchHandler.removeCallbacksAndMessages(null);

                    switch (touchState) {
                        case TOUCH_BACKGROUND -> touchState = TouchState.TOUCH_DRAG_BACKGROUND;
                        case TOUCH_CARD -> {
                            touchState = TouchState.TOUCH_DRAG_CARD;
                            if (!selectedCards.contains(touchedCard)) {
                                cleanSelectedCards();
                                addSelectedCard(touchedCard);
                            }
                        }
                        case TOUCH_PIN -> {
                            touchState = TouchState.TOUCH_DRAG_PIN;
                            cleanSelectedCards();
                        }
                    }
                }

                // 边界移动
                if (touchState != TouchState.TOUCH_DRAG_BACKGROUND) sideMove(x, y);

                switch (touchState) {
                    case TOUCH_DRAG_BACKGROUND -> {
                        offsetX += x - lastX;
                        offsetY += y - lastY;
                        lastX = x;
                        lastY = y;
                        updateCardsPos();
                    }

                    case TOUCH_SELECT_AREA -> {
                        // 框选过程中成批改选中，结束后只刷新一次工具栏，避免动画被反复打断
                        batchSelecting = true;
                        cleanSelectedCards();
                        selectArea = new RectF(realStartX, realStartY, x - offsetX, y - offsetY);
                        selectArea.sort();
                        cards.values().forEach(card -> {
                            RectF cardArea = getCardArea(card);
                            if (cardArea.intersect(selectArea)) addSelectedCard(card);
                        });
                        batchSelecting = false;
                        refreshEditView();
                    }

                    case TOUCH_DRAG_CARD -> {
                        int dx = (int) ((x - offsetX - realLastX) / gridSize);
                        int dy = (int) ((y - offsetY - realLastY) / gridSize);

                        selectedCards.forEach(card -> {
                            Action action = card.getAction();
                            if (action.isLocked()) return;
                            Point pos = action.getPos();
                            if (dx != 0) pos.x += dx;
                            if (dy != 0) pos.y += dy;
                            updateCardPos(card);
                        });

                        realLastX += dx * gridSize;
                        realLastY += dy * gridSize;
                    }

                    case TOUCH_DRAG_PIN, TOUCH_DRAG_LINK -> {
                        lastX = x;
                        lastY = y;
                        PinView tmpDragOnPin = dragOnPin;
                        dragOnPin = null;
                        ActionCard card = getActionCard(lastX - offsetX, lastY - offsetY, true);
                        if (card != null) {
                            PinView currPosPinView = card.getLinkAblePinView(lastX - card.getX(), lastY - card.getY());
                            if (currPosPinView != null) {
                                if (isTouchLinkablePin(touchState, currPosPinView.getPin())) {
                                    dragOnPin = currPosPinView;
                                }
                            }
                        }
                        if (dragOnPin != null && dragOnPin != tmpDragOnPin) {
                            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        }
                    }
                }
            }

            case MotionEvent.ACTION_UP -> {
                longTouchHandler.removeCallbacksAndMessages(null);

                switch (touchState) {
                    case TOUCH_BACKGROUND -> cleanSelectedCards();

                    case TOUCH_CARD -> {
                        if (!removeSelectedCard(touchedCard)) {
                            if (selectedCards.size() == 1) {
                                cleanSelectedCards();
                            }
                            addSelectedCard(touchedCard);
                        }
                    }

                    case TOUCH_PIN -> {
                        doubleTouchHandler.removeCallbacksAndMessages(null);
                        if (lastTouchedPin == touchedPin) {
                            Pin pin = touchedPin.getPin();
                            pin.clearLinks(task);
                            lastTouchedPin = null;
                        } else {
                            lastTouchedPin = touchedPin;
                            doubleTouchHandler.postDelayed(() -> {
                                if (lastTouchedPin != null) focusPinLink(lastTouchedPin);
                                lastTouchedPin = null;
                            }, LONG_TOUCH_TIME);
                        }
                    }

                    case TOUCH_DRAG_PIN, TOUCH_DRAG_LINK -> {
                        // 拖动到缓存面板上
                        if (cacheBoxArea.contains(x, y)) {
                            addCachePin();
                        } else {
                            ActionCard card = getActionCard(realX, realY, false);
                            if (card != null) {
                                boolean handled = false;
                                PinView pinView = card.getLinkAblePinView(x - card.getX(), y - card.getY());
                                if (pinView != null) {
                                    Pin pin = pinView.getPin();
                                    if (touchState == TouchState.TOUCH_DRAG_PIN) {
                                        if (pin.linkAble(touchedPin.getPin())) {
                                            pin.mutualAddLink(task, touchedPin.getPin());
                                            handled = true;
                                        }
                                    } else {
                                        handled = pin.addLinks(task, selectedLinks);
                                    }
                                }

                                if (!handled) tryLinkTouchPin(card.getAction());
                            } else {
                                showActionSelector();
                            }
                        }
                    }
                }
                touchState = TouchState.TOUCH_NONE;
                lastX = 0;
                lastY = 0;
            }
        }
        invalidate();
        return true;
    }

    private void sideMove(float x, float y) {
        float triggerWidth = gridSize * 3;
        float sideMoveX = 0, sideMoveY = 0;
        if (x <= triggerWidth) {
            sideMoveX = 20;
        } else if (x >= getWidth() - triggerWidth) {
            sideMoveX = -20;
        }

        if (y <= triggerWidth) {
            sideMoveY = 20;
        } else if (y >= getHeight() - triggerWidth) {
            sideMoveY = -20;
        }

        offsetX += sideMoveX;
        offsetY += sideMoveY;
        updateCardsPos();
    }

    public boolean isTouchLinkablePin(TouchState touchState, Pin pin) {
        if (!pin.linkAble(task)) return false;

        if (touchState == TouchState.TOUCH_DRAG_PIN) {
            if (touchedPin == null) return false;
            Pin touchPin = touchedPin.getPin();
            return pin.linkAble(task) && pin.linkAble(touchPin);
        } else if (touchState == TouchState.TOUCH_DRAG_LINK) {
            if (selectedLinks.isEmpty()) return false;
            boolean flag = true;
            for (Map.Entry<String, String> entry : selectedLinks.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                ActionCard actionCard = cards.get(value);
                if (actionCard == null) continue;
                PinView pinView = actionCard.getPinView(key);
                if (pinView == null) continue;
                if (!pinView.getPin().linkAble(pin)) {
                    flag = false;
                    break;
                }
            }
            return flag;
        }
        return false;
    }

    public Pin getTouchLinkblePin(TouchState touchState, Action action) {
        for (Pin pin : action.getPins()) {
            if (isTouchLinkablePin(touchState, pin)) {
                return pin;
            }
        }
        return null;
    }

    private void tryLinkTouchPin(Action action) {
        tryLinkTouchPin(touchState, action);
    }

    public void tryLinkTouchPin(TouchState touchState, Action action) {
        Pin linkblePin = getTouchLinkblePin(touchState, action);
        if (linkblePin == null) return;

        Pin p = touchedPin.getPin();

        if (touchState == TouchState.TOUCH_DRAG_PIN) {
            linkblePin.mutualAddLink(task, p);
        } else if (touchState == TouchState.TOUCH_DRAG_LINK) {
            linkblePin.addLinks(task, selectedLinks);
        }
    }

    private void showActionSelector() {
        TouchState touchState = this.touchState;
        float lastX = this.lastX;
        float lastY = this.lastY;

        Pin pin = touchedPin.getPin();
        if (touchState == TouchState.TOUCH_DRAG_PIN) {
            pin = new Pin(pin.getValue(), 0, pin.isOut());
        } else {
            pin = new Pin(pin.getValue(), 0, !pin.isOut());
        }

        actionDialog = new SelectActionByPinDialog(getContext(), task, pin, action -> {
            ActionCard card = addCard(action);
            if (card != null) {
                tryLinkTouchPin(touchState, action);
                action.setPos((int) ((lastX - offsetX) / getScaleGridSize()), (int) ((lastY - offsetY) / getScaleGridSize()));
                updateCardPos(card);
            }
            if (actionDialog != null) actionDialog.dismiss();
        });
        actionDialog.show();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            updateCardsPos();
        }
    }

    // 导出区域与还原用的现场
    private float captureScale, captureOffsetX, captureOffsetY;

    public static class CaptureContext {
        public float left, top;
        public int width, height;
    }

    /**
     * 准备导出：切到原始比例并算出整张图的区域。
     * 会改动卡片位置，必须在 UI 线程调用；之后用 {@link #drawCaptureBand} 在后台逐带绘制。
     */
    public CaptureContext prepareTaskCapture() {
        cleanSelectedCards();

        captureScale = scale;
        captureOffsetX = offsetX;
        captureOffsetY = offsetY;

        // 还原为未缩放大小，按真实尺寸导出
        scale = 1;
        offsetX = 0;
        offsetY = 0;
        updateCardsPos();
        cards.values().forEach(card -> card.setNeedDraw(true));

        // 计算所有卡片的绘制区域；留出比卡片本身更大的边距，
        // 否则连线的贝塞尔曲线弯出卡片范围时会被裁掉
        RectF area = calculateCardsArea(cards.values());
        float padding = gridSize * (CardLayoutHelper.CORNER_GRID_COUNT + 6);
        area.inset(-padding, -padding);

        offsetX = -area.left;
        offsetY = -area.top;
        updateCardsPos();
        cards.values().forEach(card -> card.setNeedDraw(true));

        CaptureContext context = new CaptureContext();
        context.left = area.left;
        context.top = area.top;
        context.width = Math.max(1, (int) Math.ceil(area.width()));
        context.height = Math.max(1, (int) Math.ceil(area.height()));
        return context;
    }

    // 导出结束后恢复原来的缩放与偏移，必须在 UI 线程调用
    public void restoreTaskCapture() {
        scale = captureScale;
        offsetX = captureOffsetX;
        offsetY = captureOffsetY;
        updateCardsPos();
    }

    // 绘制其中一条横带
    public void drawCaptureBand(Canvas canvas, CaptureContext context, int bandTop) {
        canvas.drawColor(DisplayUtil.getAttrColor(getContext(), com.google.android.material.R.attr.colorSurface));
        canvas.translate(-context.left, -(context.top + bandTop));
        dispatchDraw(canvas);
    }

    /**
     * 绘制背景网格
     *
     * @param canvas   画布
     * @param startX   绝对起始位置
     * @param startY   绝对起始位置
     * @param gridSize 网格大小
     */
    private void drawBackground(Canvas canvas, float startX, float startY, float gridSize) {
        canvas.save();
        float offsetX = startX % gridSize;
        float offsetY = startY % gridSize;
        canvas.translate(offsetX, offsetY);

        startX = startX - offsetX;
        startY = startY - offsetY;

        float row = canvas.getHeight() / gridSize;
        float col = canvas.getWidth() / gridSize;

        float bigGridSize = gridSize * 10;
        for (int i = 0; i < row; i++) {
            float y = i * gridSize;
            if (startY == y) {
                gridPaint.setStrokeWidth(5);
            } else {
                float offset = Math.abs((startY - y) % bigGridSize);
                boolean isBig = offset < 1 || offset > bigGridSize - 1;
                gridPaint.setStrokeWidth(isBig ? 2 : 0.5f);
            }
            canvas.drawLine(-gridSize, y, canvas.getWidth() + gridSize, y, gridPaint);
        }

        for (int i = 0; i < col; i++) {
            float x = i * gridSize;
            if (startX == x) {
                gridPaint.setStrokeWidth(5);
            } else {
                float offset = Math.abs((startX - x) % bigGridSize);
                boolean isBig = offset < 1 || offset > bigGridSize - 1;
                gridPaint.setStrokeWidth(isBig ? 2 : 0.5f);
            }
            canvas.drawLine(x, -gridSize, x, canvas.getHeight() + gridSize, gridPaint);
        }

        canvas.restore();
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        float gridSize = getScaleGridSize();
        drawBackground(canvas, offsetX, offsetY, gridSize);


        CornerPathEffect cornerPathEffect = new CornerPathEffect(gridSize * CardLayoutHelper.CORNER_GRID_COUNT / 2);
        linkPaint.setPathEffect(cornerPathEffect);

        linkPaint.setStrokeWidth(gridSize / 4);
        // 所有卡片的连线，不包括选中的卡，因为选中的卡的连线要置顶
        cards.values().forEach(card -> {
            if (selectedCards.contains(card)) return;

            Action action = card.getAction();
            action.getPins().forEach(pin -> {
                // 输出针脚才需要画线
                if (!pin.isOut()) return;
                PinView cardPinView = card.getPinView(pin.getId());
                if (cardPinView == null) return;

                pin.getLinks().forEach((key, value) -> {
                    ActionCard actionCard = cards.get(value);

                    if (actionCard == null) return;

                    if (selectedCards.contains(actionCard)) return;

                    PinView pinView = actionCard.getPinView(key);
                    if (pinView == null) return;

                    if (pinView.getPin().isSameClass(pin)) {
                        linkPaint.setShader(null);
                        linkPaint.setColor(pinView.getPinColor());
                    } else {
                        PointF startPoint = cardPinView.getSlotPosInLayout(scale);
                        PointF endPoint = pinView.getSlotPosInLayout(scale);
                        linkPaint.setShader(new LinearGradient(startPoint.x, startPoint.y, endPoint.x, endPoint.y, cardPinView.getPinColor(), pinView.getPinColor(), Shader.TileMode.CLAMP));
                        linkPaint.setColor(Color.WHITE);
                    }

                    canvas.drawPath(calculateLinkPath(cardPinView, pinView), linkPaint);
                });
            });
        });

        linkPaint.setStrokeWidth(gridSize / 2);
        linkPaint.setColorFilter(new LightingColorFilter(getResources().getColor(R.color.SelectedPinMul, null), getResources().getColor(R.color.SelectedPinAdd, null)));
        // 选中的卡片
        selectedCards.forEach(card -> {
            Action action = card.getAction();
            action.getPins().forEach(pin -> {
                PinView cardPinView = card.getPinView(pin.getId());
                if (cardPinView == null) return;

                pin.getLinks().forEach((key, value) -> {
                    ActionCard actionCard = cards.get(value);

                    if (actionCard == null) return;

                    PinView pinView = actionCard.getPinView(key);
                    if (pinView == null) return;

                    linkPaint.setColor(pinView.getPinColor());

                    if (pinView.getPin().isSameClass(pin)) {
                        linkPaint.setShader(null);
                        linkPaint.setColor(pinView.getPinColor());
                    } else {
                        PointF startPoint = cardPinView.getSlotPosInLayout(scale);
                        PointF endPoint = pinView.getSlotPosInLayout(scale);
                        linkPaint.setShader(new LinearGradient(startPoint.x, startPoint.y, endPoint.x, endPoint.y, cardPinView.getPinColor(), pinView.getPinColor(), Shader.TileMode.CLAMP));
                        linkPaint.setColor(Color.WHITE);
                    }

                    if (pinView.getPin().isOut()) {
                        canvas.drawPath(calculateLinkPath(pinView, cardPinView), linkPaint);
                    } else {
                        canvas.drawPath(calculateLinkPath(cardPinView, pinView), linkPaint);
                    }
                });
            });
        });

        linkPaint.setShader(null);
        linkPaint.setColorFilter(null);
        linkPaint.setStrokeWidth(gridSize / 4);

        // 拖动针脚，要么连线，要么挪线
        if (touchedPin != null && (touchState == TouchState.TOUCH_DRAG_PIN || touchState == TouchState.TOUCH_DRAG_LINK)) {
            linkPaint.setColor(DisplayUtil.getAttrColor(getContext(), com.google.android.material.R.attr.colorPrimaryInverse));

            if (dragOnPin != null) {
                Pin pin = dragOnPin.getPin();
                if (touchedPin.getPin().isSameClass(pin)) {
                    linkPaint.setShader(null);
                    linkPaint.setColor(touchedPin.getPinColor());
                } else {
                    PointF startPoint = touchedPin.getSlotPosInLayout(scale);
                    PointF endPoint = dragOnPin.getSlotPosInLayout(scale);
                    linkPaint.setShader(new LinearGradient(startPoint.x, startPoint.y, endPoint.x, endPoint.y, touchedPin.getPinColor(), dragOnPin.getPinColor(), Shader.TileMode.CLAMP));
                    linkPaint.setColor(Color.WHITE);
                }
            }

            if (touchState == TouchState.TOUCH_DRAG_PIN) {
                canvas.drawPath(calculateLinkPath(touchedPin), linkPaint);
            } else {
                for (Map.Entry<String, String> entry : selectedLinks.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();
                    ActionCard actionCard = cards.get(value);
                    if (actionCard == null) continue;
                    PinView pinView = actionCard.getPinView(key);
                    if (pinView == null) continue;

                    canvas.drawPath(calculateLinkPath(pinView), linkPaint);
                }
            }
        }

        super.dispatchDraw(canvas);

        // 框选
        if (touchState == TouchState.TOUCH_SELECT_AREA || !selectedCards.isEmpty()) {
            DashPathEffect dashPathEffect = new DashPathEffect(new float[]{gridSize, gridSize}, 0);
            linkPaint.setPathEffect(new ComposePathEffect(cornerPathEffect, dashPathEffect));
            linkPaint.setColor(DisplayUtil.getAttrColor(getContext(), com.google.android.material.R.attr.colorPrimaryVariant));
            RectF area = new RectF();
            if (touchState == TouchState.TOUCH_SELECT_AREA) {
                area.set(selectArea);
            } else {
                area.set(calculateCardsArea(selectedCards));
                area.left -= gridSize;
                area.top -= gridSize;
                area.right += gridSize;
                area.bottom += gridSize;
            }
            area.offset(offsetX, offsetY);
            canvas.drawRect(area, linkPaint);
        }
    }

    private Path calculateLinkPath(PinView start, PinView end) {
        PointF startPoint = start.getSlotPosInLayout(scale);
        PointF endPoint = end.getSlotPosInLayout(scale);
        return CardLayoutHelper.calculateLinkPath(getScaleGridSize(), startPoint, endPoint, start.getPin().isVertical());
    }

    private Path calculateLinkPath(PinView pinView) {
        PointF pos = pinView.getSlotPosInLayout(scale);
        if (pinView.getPin().isOut()) {
            return CardLayoutHelper.calculateLinkPath(getScaleGridSize(), pos, new PointF(lastX, lastY), pinView.getPin().isVertical());
        } else {
            return CardLayoutHelper.calculateLinkPath(getScaleGridSize(), new PointF(lastX, lastY), pos, pinView.getPin().isVertical());
        }
    }

    public void checkCards() {
        if (!Looper.getMainLooper().isCurrentThread()) return;

        int count = 0;
        for (ActionCard card : cards.values()) {
            boolean result = card.check();
            if (!result) count++;
        }
        if (count == 0) return;
        Toast.makeText(getContext(), getContext().getString(R.string.card_check_error_tips, count), Toast.LENGTH_SHORT).show();
    }

    public void syncCards() {
        if (!Looper.getMainLooper().isCurrentThread()) return;

        for (ActionCard card : cards.values()) {
            Action action = card.getAction();
            if (action instanceof SyncAction syncAction) {
                syncAction.sync(task);
                card.refreshCardInfo();
            }
        }
    }

    public void initCacheBoxArea(View areaView, ViewGroup cacheBox) {
        this.cacheBox = cacheBox;

        int[] viewLocation = new int[2];
        int[] layoutLocation = new int[2];

        areaView.getLocationInWindow(viewLocation);
        getLocationInWindow(layoutLocation);

        int x = viewLocation[0] - layoutLocation[0];
        int y = viewLocation[1] - layoutLocation[1];
        cacheBoxArea = new RectF(x, y, x + areaView.getWidth(), y + areaView.getHeight());
    }

    private CachedPin findCachedPin(float x, float y) {
        for (CachedPin cachedPin : cachedPins) {
            View slotDragBox = cachedPin.getPinView().getSlotDragBox();
            int[] pinLocation = new int[2];
            slotDragBox.getLocationInWindow(pinLocation);
            if (new RectF(pinLocation[0], pinLocation[1], pinLocation[0] + slotDragBox.getWidth(), pinLocation[1] + slotDragBox.getHeight()).contains(x, y)) {
                return cachedPin;
            }
        }
        return null;
    }

    private void addCachePin() {
        if (cachedPins.size() >= 4) return;

        PinCachedView pinView = new PinCachedView(getContext(), touchedPin.getPin());
        DisplayUtil.setViewHeight(pinView, (int) DisplayUtil.dp2px(getContext(), 102));
        CachedPin cachedPin = new CachedPin(pinView, touchedPin, selectedLinks);
        cachedPins.add(cachedPin);

        cacheBox.addView(pinView);
    }

    private void removeCachePin(CachedPin cachedPin) {
        cachedPins.remove(cachedPin);
        cacheBox.removeView(cachedPin.getPinView());
    }

    private void clearCachePins() {
        for (CachedPin cachedPin : cachedPins) {
            cacheBox.removeView(cachedPin.getPinView());
        }
        cachedPins.clear();
    }

    public Task getTask() {
        return task;
    }

    public float getScale() {
        return scale;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public List<Action> getSelectedActions() {
        List<Action> actions = new ArrayList<>();
        for (ActionCard card : selectedCards) {
            actions.add(card.getAction());
        }
        return actions;
    }

    public List<Action> getSelectedActionsCopy() {
        Map<String, Action> actionMap = new HashMap<>();
        Map<String, Action> copiedActionMap = new HashMap<>();

        for (ActionCard card : selectedCards) {
            Action action = card.getAction();
            actionMap.put(action.getId(), action);
            Action copiedAction = action.newCopy();
            copiedActionMap.put(copiedAction.getUid(), copiedAction);
        }

        for (ActionCard card : selectedCards) {
            Action action = card.getAction();
            Action copiedAction = copiedActionMap.get(action.getUid());
            if (copiedAction == null) continue;
            for (Pin pin : action.getPins()) {
                if (!pin.isLinked()) continue;
                Pin copiedPin = copiedAction.getPinByUid(pin.getUid());
                if (copiedPin == null) continue;
                for (Map.Entry<String, String> entry : pin.getLinks().entrySet()) {
                    String pinId = entry.getKey();
                    String actionId = entry.getValue();
                    Action linkedAction = actionMap.get(actionId);
                    if (linkedAction == null) continue;
                    Pin linkedPin = linkedAction.getPinById(pinId);
                    if (linkedPin == null) continue;
                    Action linkedCopiedAction = copiedActionMap.get(linkedAction.getUid());
                    if (linkedCopiedAction == null) continue;
                    Pin linkedCopiedPin = linkedCopiedAction.getPinByUid(linkedPin.getUid());
                    if (linkedCopiedPin == null) continue;
                    copiedPin.directAddLink(linkedCopiedPin);
                    linkedCopiedPin.directAddLink(copiedPin);
                }
            }
        }

        List<Action> actions = new ArrayList<>();
        for (Action action : copiedActionMap.values()) {
            action.setUid(UUID.randomUUID().toString());
            actions.add(action);
        }
        return actions;
    }

    @Override
    public void onCreate(Task task) {

    }

    @Override
    public void onUpdate(Task task) {
        syncCards();
        checkCards();
    }

    @Override
    public void onRemove(Task task) {
        checkCards();
    }

    @Override
    public void onCreate(Variable var) {

    }

    @Override
    public void onUpdate(Variable var) {
        syncCards();
    }

    @Override
    public void onRemove(Variable var) {
        checkCards();
    }

    private static class TaskEditLayoutInfo {
        private static final float DEFAULT_OFFSET = DisplayUtil.dp2px(MainApplication.getInstance(), 5 * GRID_DP_SIZE);
        private float scale = 0.75f;
        private float offsetX = DEFAULT_OFFSET;
        private float offsetY = DEFAULT_OFFSET;

        public float getScale() {
            return scale;
        }

        public void setScale(float scale) {
            this.scale = scale;
        }

        public float getOffsetX() {
            return offsetX;
        }

        public void setOffsetX(float offsetX) {
            this.offsetX = offsetX;
        }

        public float getOffsetY() {
            return offsetY;
        }

        public void setOffsetY(float offsetY) {
            this.offsetY = offsetY;
        }
    }

    private static class CachedPin {
        private final PinCachedView pinView;
        private final PinView touchedPin;
        private final Map<String, String> selectedLinks = new HashMap<>();

        public CachedPin(PinCachedView pinView, PinView touchedPin, Map<String, String> selectedLinks) {
            this.pinView = pinView;
            this.touchedPin = touchedPin;
            if (selectedLinks != null) this.selectedLinks.putAll(selectedLinks);
        }

        public PinCachedView getPinView() {
            return pinView;
        }

        public PinView getTouchedPin() {
            return touchedPin;
        }

        public Map<String, String> getSelectedLinks() {
            return selectedLinks;
        }

        public boolean isDragPin() {
            return selectedLinks.isEmpty();
        }
    }
}
