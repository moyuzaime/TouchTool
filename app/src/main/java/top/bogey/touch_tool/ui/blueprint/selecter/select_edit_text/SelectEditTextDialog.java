package top.bogey.touch_tool.ui.blueprint.selecter.select_edit_text;

import android.content.Context;
import android.graphics.Rect;
import android.text.Editable;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputEditText;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.databinding.DialogFullScreenEditBinding;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.callback.StringResultCallback;
import top.bogey.touch_tool.utils.listener.TextChangedListener;

public class SelectEditTextDialog extends BottomSheetDialog {
    private final DialogFullScreenEditBinding binding;
    private final Rect visibleFrame = new Rect();
    private final float heightRatio;
    private int lastVisibleHeight = -1;
    private ViewTreeObserver.OnGlobalLayoutListener windowLayoutListener;

    public SelectEditTextDialog(@NonNull Context context, TextInputEditText from, StringResultCallback callback) {
        this(context, from, callback, null);
    }

    /**
     * @param functions 传入时按公式编辑处理：多行、去掉换行符，并展示可点击插入的常用函数
     */
    public SelectEditTextDialog(@NonNull Context context, TextInputEditText from, StringResultCallback callback, String[] functions) {
        super(context);
        binding = DialogFullScreenEditBinding.inflate(LayoutInflater.from(context));
        setContentView(binding.getRoot());

        boolean formulaMode = functions != null && functions.length > 0;
        binding.editText.setText(from.getText());
        binding.editText.setEnabled(from.isEnabled());
        if (formulaMode) {
            binding.editText.setInputType(from.getInputType() | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE);
            binding.editText.setSingleLine(false);
            binding.editText.setMaxLines(12);
            binding.editText.setMinLines(5);
        } else {
            binding.editText.setSingleLine(from.getMaxLines() <= 1);
            binding.editText.setInputType(from.getInputType());
            binding.editText.setMinLines(1);
        }
        binding.editText.setNestedScrollingEnabled(false);

        binding.editText.addTextChangedListener(new TextChangedListener() {
            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                // 公式模式下换行会被直接抹掉，避免多个变量名被换行连成一个
                if (formulaMode && text != null) text = text.replace("\n", "").replace("\r", "");
                callback.onResult(text);
            }
        });
        binding.editText.requestFocus();

        if (formulaMode) initFunctionTips(functions);

        boolean portrait = DisplayUtil.isPortrait(context);
        heightRatio = formulaMode ? 0.9f : (portrait ? 0.7f : 0.8f);

        BottomSheetBehavior<FrameLayout> behavior = getBehavior();
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        if (!portrait) {
            View decorView = MainApplication.getInstance().getActivity().getWindow().getDecorView();
            behavior.setMaxWidth(decorView.getWidth());
        }

        Window window = getWindow();
        if (window == null) {
            applyPanelHeight(DisplayUtil.getScreenSize(context).getHeight());
            return;
        }
        // 软键盘弹出时跟随可见区域高度，避免面板被键盘顶出屏幕
        window.setSoftInputMode((window.getAttributes().softInputMode & ~WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        View decorView = window.getDecorView();
        windowLayoutListener = this::onWindowLayoutChanged;
        decorView.getViewTreeObserver().addOnGlobalLayoutListener(windowLayoutListener);
        decorView.getWindowVisibleDisplayFrame(visibleFrame);
        applyPanelHeight(visibleFrame.height());
    }

    private void onWindowLayoutChanged() {
        Window window = getWindow();
        if (window == null) return;
        window.getDecorView().getWindowVisibleDisplayFrame(visibleFrame);
        int height = visibleFrame.height();
        if (height > 0 && Math.abs(height - lastVisibleHeight) >= 8) applyPanelHeight(height);
    }

    private void applyPanelHeight(int height) {
        if (height <= 0) return;
        lastVisibleHeight = height;
        DisplayUtil.setViewHeight(binding.getRoot(), (int) (height * heightRatio));
        DisplayUtil.setViewWidth(binding.getRoot(), ViewGroup.LayoutParams.MATCH_PARENT);
        binding.getRoot().removeCallbacks(settleRunnable);
        binding.getRoot().post(settleRunnable);
    }

    private final Runnable settleRunnable = () -> {
        if (!isShowing()) return;
        BottomSheetBehavior<FrameLayout> behavior = getBehavior();
        if (behavior != null) behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    };

    private void initFunctionTips(String[] functions) {
        binding.helpTitle.setVisibility(View.VISIBLE);
        binding.helpIntro.setVisibility(View.VISIBLE);
        binding.helpIntro.setText(R.string.math_expression_operators);

        int padding = (int) DisplayUtil.dp2px(getContext(), 5);
        for (String function : functions) {
            String[] parts = function.split("\\|", 2);
            TextView item = new TextView(getContext());
            item.setText(parts[0] + "\u3000" + (parts.length > 1 ? parts[1] : ""));
            item.setTextSize(14);
            item.setPadding(0, padding, 0, padding);
            item.setOnClickListener(v -> insertFunction(parts[0]));
            binding.helpList.addView(item);
        }
    }

    // 在光标处插入函数名并补上括号，光标停在括号内
    private void insertFunction(String name) {
        int start = binding.editText.getSelectionStart();
        if (start < 0) start = binding.editText.length();
        binding.editText.getText().insert(start, name + "()");
        binding.editText.setSelection(start + name.length() + 1);
        binding.editText.requestFocus();
    }

    @Override
    public void dismiss() {
        Window window = getWindow();
        if (window != null && windowLayoutListener != null) {
            window.getDecorView().getViewTreeObserver().removeOnGlobalLayoutListener(windowLayoutListener);
            windowLayoutListener = null;
        }
        super.dismiss();
    }
}
