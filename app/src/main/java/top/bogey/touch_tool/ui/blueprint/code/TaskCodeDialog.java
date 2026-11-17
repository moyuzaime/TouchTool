package top.bogey.touch_tool.ui.blueprint.code;

import android.content.Context;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.databinding.DialogTaskCodeBinding;
import top.bogey.touch_tool.utils.listener.TextChangedListener;

// 任务代码对话框：把当前任务的动作清单以 JSON 文本编辑，保存后回写画布
public class TaskCodeDialog {
    public static void show(Context context, Task task, Runnable onApplied) {
        if (task == null) return;

        DialogTaskCodeBinding binding = DialogTaskCodeBinding.inflate(LayoutInflater.from(context));
        String code;
        try {
            code = TaskCode.dump(task);
        } catch (Exception e) {
            code = "[]";
        }
        binding.codeEdit.setText(code);
        binding.codeEdit.setSelection(0);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.task_code)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.task_code_format, null)
                .create();

        dialog.setOnShowListener(d -> {
            // 代码编辑需要尽量大的输入区，窗口铺满并在键盘弹出时让出空间
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> save(context, task, binding, dialog, onApplied));
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> format(context, binding));
        });
        // 编辑中的长文本误触外面就丢了，关掉外部点击关闭；取消与返回键仍可退出
        dialog.setCanceledOnTouchOutside(false);

        // 改过内容就把上一次的报错收起来，避免旧错误一直挂着
        binding.codeEdit.addTextChangedListener(new TextChangedListener() {
            @Override
            public void afterTextChanged(Editable s) {
                hideError(binding);
            }
        });

        dialog.show();
    }

    private static void save(Context context, Task task, DialogTaskCodeBinding binding, AlertDialog dialog, Runnable onApplied) {
        CharSequence text = binding.codeEdit.getText();
        try {
            TaskCode.apply(task, text == null ? "" : text.toString());
        } catch (TaskCode.TaskCodeException e) {
            showError(context, binding, e);
            return;
        } catch (Exception e) {
            binding.codeError.setText(context.getString(R.string.task_code_failed, String.valueOf(e.getMessage())));
            binding.codeError.setVisibility(View.VISIBLE);
            return;
        }

        dialog.dismiss();
        if (onApplied != null) onApplied.run();
    }

    private static void format(Context context, DialogTaskCodeBinding binding) {
        CharSequence text = binding.codeEdit.getText();
        try {
            String formatted = TaskCode.format(text == null ? "" : text.toString());
            binding.codeEdit.setText(formatted);
            binding.codeEdit.setSelection(0);
            hideError(binding);
        } catch (TaskCode.TaskCodeException e) {
            showError(context, binding, e);
        }
    }

    private static void showError(Context context, DialogTaskCodeBinding binding, TaskCode.TaskCodeException e) {
        String message = e.getIndex() > 0
                ? context.getString(R.string.task_code_error_action, e.getIndex(), e.getMessage())
                : e.getMessage();
        binding.codeError.setText(message);
        binding.codeError.setVisibility(View.VISIBLE);
    }

    private static void hideError(DialogTaskCodeBinding binding) {
        if (binding.codeError.getVisibility() != View.GONE) binding.codeError.setVisibility(View.GONE);
    }
}
