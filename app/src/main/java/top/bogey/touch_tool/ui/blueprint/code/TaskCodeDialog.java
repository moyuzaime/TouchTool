package top.bogey.touch_tool.ui.blueprint.code;

import android.content.Context;
import android.graphics.Point;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.save.TagSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.databinding.DialogTaskCodeBinding;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.listener.TextChangedListener;

// 任务代码对话框：把整任务以 JSON 文本编辑；工具条按钮也只改文本，保存时统一校验回写
public class TaskCodeDialog {
    public static void show(Context context, Task task, Runnable onApplied) {
        if (task == null) return;

        DialogTaskCodeBinding binding = DialogTaskCodeBinding.inflate(LayoutInflater.from(context));
        String code;
        try {
            code = TaskCode.dump(task);
        } catch (Exception e) {
            Toast.makeText(context, context.getString(R.string.task_code_failed, String.valueOf(e.getMessage())), Toast.LENGTH_SHORT).show();
            return;
        }
        binding.codeEdit.setText(code);
        binding.codeEdit.setSelection(0);

        // 工具条只改编辑框里的文本，真正的回写仍走保存时的 TaskCode.apply
        binding.chipTitle.setOnClickListener(v -> editField(context, binding, "title", R.string.task_code_field_title));
        binding.chipDescription.setOnClickListener(v -> editField(context, binding, "description", R.string.task_code_field_description));
        binding.chipFlag.setOnClickListener(v -> editFlag(context, binding));
        binding.chipTags.setOnClickListener(v -> editTags(context, binding));
        binding.chipVariable.setOnClickListener(v -> editVariables(context, binding));
        binding.chipSubtask.setOnClickListener(v -> editSubtasks(context, binding));
        binding.chipAction.setOnClickListener(v -> editActions(context, binding));

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

    // 标题、描述：普通文本输入
    private static void editField(Context context, DialogTaskCodeBinding binding, String key, int titleRes) {
        JsonObject document = readDocument(context, binding);
        if (document == null) return;

        JsonElement valueElement = document.get(key);
        String value = valueElement != null && valueElement.isJsonPrimitive() ? valueElement.getAsString() : "";
        AppUtil.showEditDialog(context, titleRes, value, result -> {
            document.addProperty(key, result == null ? "" : result);
            applyDocument(binding, document);
        });
    }

    // 标记：目前只有「记录日志细节」这一位
    private static void editFlag(Context context, DialogTaskCodeBinding binding) {
        JsonObject document = readDocument(context, binding);
        if (document == null) return;

        int flag = getInt(document, "flag", 0);
        boolean[] checked = {(flag & Task.FLAG_DEBUG) != 0};
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.task_code_field_flag)
                .setMultiChoiceItems(new CharSequence[]{context.getString(R.string.task_code_flag_debug)},
                        new boolean[]{checked[0]}, (dialog, which, isChecked) -> checked[0] = isChecked)
                .setPositiveButton(R.string.enter, (dialog, which) -> {
                    int newFlag = checked[0] ? (flag | Task.FLAG_DEBUG) : (flag & ~Task.FLAG_DEBUG);
                    document.addProperty("flag", newFlag);
                    applyDocument(binding, document);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // 标签：多选已有标签
    private static void editTags(Context context, DialogTaskCodeBinding binding) {
        JsonObject document = readDocument(context, binding);
        if (document == null) return;

        List<String> allTags = new ArrayList<>(TagSaver.getInstance().getTags());
        if (allTags.isEmpty()) {
            Toast.makeText(context, R.string.task_code_tags_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        Set<String> selected = new HashSet<>();
        JsonElement tagsElement = document.get("tags");
        if (tagsElement != null && tagsElement.isJsonArray()) {
            for (JsonElement item : tagsElement.getAsJsonArray()) {
                if (item.isJsonPrimitive()) selected.add(item.getAsString());
            }
        }
        boolean[] checked = new boolean[allTags.size()];
        for (int i = 0; i < allTags.size(); i++) checked[i] = selected.contains(allTags.get(i));

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.task_code_field_tags)
                .setMultiChoiceItems(allTags.toArray(new CharSequence[0]), checked,
                        (dialog, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(R.string.enter, (dialog, which) -> {
                    JsonArray tags = new JsonArray();
                    for (int i = 0; i < allTags.size(); i++) {
                        if (checked[i]) tags.add(allTags.get(i));
                    }
                    document.add("tags", tags);
                    applyDocument(binding, document);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // 变量：列出文本里的变量，可新建、重命名、删除
    private static void editVariables(Context context, DialogTaskCodeBinding binding) {
        JsonObject document = readDocument(context, binding);
        if (document == null) return;

        JsonArray variables = getArray(document, "variables");
        List<CharSequence> items = new ArrayList<>();
        for (JsonElement item : variables) items.add(variableName(item));
        items.add(context.getString(R.string.task_code_variable_add));

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.task_code_field_variable)
                .setItems(items.toArray(new CharSequence[0]), (dialog, which) -> {
                    if (which == items.size() - 1) addVariable(context, binding, document);
                    else editVariable(context, binding, document, which);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static void addVariable(Context context, DialogTaskCodeBinding binding, JsonObject document) {
        String[] types = context.getResources().getStringArray(R.array.task_code_variable_type);
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.task_code_variable_type)
                .setItems(types, (dialog, which) -> AppUtil.showEditDialog(context, R.string.task_code_variable_title,
                        context.getString(R.string.task_code_variable_name_default), result -> {
                            String title = result == null || result.trim().isEmpty()
                                    ? context.getString(R.string.task_code_variable_name_default) : result.trim();
                            getArray(document, "variables").add(TaskCode.newVariable(title, which));
                            applyDocument(binding, document);
                        }))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static void editVariable(Context context, DialogTaskCodeBinding binding, JsonObject document, int index) {
        JsonArray variables = getArray(document, "variables");
        if (index < 0 || index >= variables.size() || !variables.get(index).isJsonObject()) return;

        JsonObject variable = variables.get(index).getAsJsonObject();
        String name = variableName(variables.get(index));
        new MaterialAlertDialogBuilder(context)
                .setTitle(name)
                .setItems(new CharSequence[]{
                        context.getString(R.string.task_code_variable_rename),
                        context.getString(R.string.task_code_variable_delete)}, (dialog, which) -> {
                    if (which == 0) {
                        AppUtil.showEditDialog(context, R.string.task_code_variable_title, name, result -> {
                            variable.addProperty("title", result == null ? "" : result.trim());
                            applyDocument(binding, document);
                        });
                    } else {
                        variables.remove(index);
                        applyDocument(binding, document);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // 子任务：列出文本里的子任务，可新建、重命名、删除
    private static void editSubtasks(Context context, DialogTaskCodeBinding binding) {
        JsonObject document = readDocument(context, binding);
        if (document == null) return;

        JsonArray tasks = getArray(document, "tasks");
        List<CharSequence> items = new ArrayList<>();
        for (JsonElement item : tasks) items.add(variableName(item));
        items.add(context.getString(R.string.task_code_subtask_add));

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.task_code_field_subtask)
                .setItems(items.toArray(new CharSequence[0]), (dialog, which) -> {
                    if (which == items.size() - 1) addSubtask(context, binding, document);
                    else editSubtask(context, binding, document, which);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static void addSubtask(Context context, DialogTaskCodeBinding binding, JsonObject document) {
        AppUtil.showEditDialog(context, R.string.task_code_subtask_title,
                context.getString(R.string.task_code_subtask_name_default), result -> {
                    String title = result == null || result.trim().isEmpty()
                            ? context.getString(R.string.task_code_subtask_name_default) : result.trim();
                    getArray(document, "tasks").add(TaskCode.newSubtask(title));
                    applyDocument(binding, document);
                });
    }

    private static void editSubtask(Context context, DialogTaskCodeBinding binding, JsonObject document, int index) {
        JsonArray tasks = getArray(document, "tasks");
        if (index < 0 || index >= tasks.size() || !tasks.get(index).isJsonObject()) return;

        JsonObject task = tasks.get(index).getAsJsonObject();
        String name = variableName(tasks.get(index));
        new MaterialAlertDialogBuilder(context)
                .setTitle(name)
                .setItems(new CharSequence[]{
                        context.getString(R.string.task_code_variable_rename),
                        context.getString(R.string.task_code_variable_delete)}, (dialog, which) -> {
                    if (which == 0) {
                        AppUtil.showEditDialog(context, R.string.task_code_subtask_title, name, result -> {
                            task.addProperty("title", result == null ? "" : result.trim());
                            applyDocument(binding, document);
                        });
                    } else {
                        tasks.remove(index);
                        applyDocument(binding, document);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // 动作：选择动作类型后插入一份默认模板，针脚/连线仍由画布或文本维护
    private static void editActions(Context context, DialogTaskCodeBinding binding) {
        JsonObject document = readDocument(context, binding);
        if (document == null) return;

        TaskCodeActionPicker.show(context, type -> {
            try {
                Point pos = nextActionPos(document);
                getArray(document, "actions").add(TaskCode.newAction(type, pos.x, pos.y));
                applyDocument(binding, document);
            } catch (TaskCode.TaskCodeException e) {
                showError(context, binding, e);
            }
        });
    }

    private static Point nextActionPos(JsonObject document) {
        int x = 0;
        int y = 0;
        boolean empty = true;
        JsonElement actions = document.get("actions");
        if (actions != null && actions.isJsonArray()) {
            for (JsonElement item : actions.getAsJsonArray()) {
                if (!item.isJsonObject()) continue;
                empty = false;
                JsonElement pos = item.getAsJsonObject().get("pos");
                if (pos == null || !pos.isJsonObject()) continue;
                x = Math.max(x, getInt(pos.getAsJsonObject(), "x", 0));
                y = Math.max(y, getInt(pos.getAsJsonObject(), "y", 0));
            }
        }
        return empty ? new Point(0, 0) : new Point(x, y + 40);
    }

    // 读取当前文本并解析成任务对象，失败时红字提示
    private static JsonObject readDocument(Context context, DialogTaskCodeBinding binding) {
        CharSequence text = binding.codeEdit.getText();
        try {
            return TaskCode.parseDocument(text == null ? "" : text.toString());
        } catch (TaskCode.TaskCodeException e) {
            showError(context, binding, e);
            return null;
        }
    }

    // 把改完的对象写回编辑框，尽量保留滚动位置
    private static void applyDocument(DialogTaskCodeBinding binding, JsonObject document) {
        int scrollY = binding.codeEdit.getScrollY();
        binding.codeEdit.setText(TaskCode.writeDocument(document));
        binding.codeEdit.post(() -> binding.codeEdit.scrollTo(0, scrollY));
    }

    private static JsonArray getArray(JsonObject document, String key) {
        JsonElement element = document.get(key);
        if (element != null && element.isJsonArray()) return element.getAsJsonArray();
        JsonArray array = new JsonArray();
        document.add(key, array);
        return array;
    }

    private static int getInt(JsonObject object, String key, int defaultValue) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) return defaultValue;
        try {
            return element.getAsInt();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String variableName(JsonElement element) {
        if (element == null || !element.isJsonObject()) return "?";
        JsonObject object = element.getAsJsonObject();
        String title = getString(object, "title");
        if (title != null && !title.isEmpty()) return title;
        String id = getString(object, "id");
        return id == null || id.isEmpty() ? "?" : id;
    }

    private static String getString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) return null;
        try {
            return element.getAsString();
        } catch (Exception e) {
            return null;
        }
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
