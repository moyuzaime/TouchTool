package top.bogey.touch_tool.ui.blueprint.code;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.text.Editable;
import android.text.Layout;
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
import java.util.Map;
import java.util.Set;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionInfo;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.save.TagSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.databinding.DialogTaskCodeBinding;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.listener.TextChangedListener;

// 任务代码对话框：整任务以 JSON 文本编辑；工具条按钮只改文本，保存时统一校验回写
public class TaskCodeDialog {
    // 记住上次搜索词，重复点搜索时不用重新输入
    private static String lastSearch = "";

    public interface DocumentListener {
        void onSave(String text) throws TaskCode.TaskCodeException;
    }

    public interface DocumentResult {
        void onResult(JsonObject document);
    }

    // 顶层任务：保存时写回真实任务
    public static void show(Context context, Task task, Runnable onApplied) {
        show(context, task, null, onApplied);
    }

    // focusActionId 不为空时，打开后直接滚到该卡片的代码
    public static void show(Context context, Task task, String focusActionId, Runnable onApplied) {
        if (task == null) return;
        String code;
        try {
            code = TaskCode.dump(task);
        } catch (Exception e) {
            Toast.makeText(context, context.getString(R.string.task_code_failed, String.valueOf(e.getMessage())), Toast.LENGTH_SHORT).show();
            return;
        }
        showText(context, R.string.task_code, code, focusActionId, text -> TaskCode.apply(task, text), onApplied);
    }

    // 先在画布侧选卡片，再打开代码并定位到它
    public static void showCardPicker(Context context, Task task, Runnable onApplied) {
        if (task == null) return;

        List<Action> actions = new ArrayList<>(task.getActions());
        actions.sort((left, right) -> {
            Point leftPos = left.getPos();
            Point rightPos = right.getPos();
            if (leftPos.y != rightPos.y) return Integer.compare(leftPos.y, rightPos.y);
            return Integer.compare(leftPos.x, rightPos.x);
        });
        if (actions.isEmpty()) {
            Toast.makeText(context, R.string.task_code_action_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        List<CharSequence> items = new ArrayList<>();
        for (Action action : actions) items.add(cardLabel(action));
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.task_code_card_pick)
                .setItems(items.toArray(new CharSequence[0]), (dialog, which) -> {
                    if (which < 0 || which >= actions.size()) return;
                    show(context, task, actions.get(which).getId(), onApplied);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static String cardLabel(Action action) {
        ActionInfo info = ActionInfo.getActionInfo(action.getType());
        String typeTitle = info == null ? action.getType().name() : displayTitle(info, action.getType().name());
        String custom = action.getTitle();
        if (custom == null || custom.isEmpty() || custom.equals(typeTitle)) return typeTitle;
        return custom + "（" + typeTitle + "）";
    }

    // 局部编辑：保存时只校验这一段并回吐对象，由调用方合并回上层文本
    public static void showDocument(Context context, JsonObject document, DocumentResult result) {
        showText(context, R.string.task_code_subtask_code, TaskCode.writeDocument(document), null, text -> {
            JsonObject edited = TaskCode.validateDocument(text);
            if (result != null) result.onResult(edited);
        }, null);
    }

    private static void showText(Context context, int titleRes, String code, String focusActionId, DocumentListener listener, Runnable onSaved) {
        DialogTaskCodeBinding binding = DialogTaskCodeBinding.inflate(LayoutInflater.from(context));
        binding.codeTitle.setText(titleRes);
        binding.codeEdit.setText(code);
        binding.codeEdit.setSelection(0);

        // 结构工具：只改编辑框里的文本，真正的回写仍走保存时的 TaskCode.apply
        binding.chipTitle.setOnClickListener(v -> editField(context, binding, "title", R.string.task_code_field_title));
        binding.chipDescription.setOnClickListener(v -> editField(context, binding, "description", R.string.task_code_field_description));
        binding.chipFlag.setOnClickListener(v -> editFlag(context, binding));
        binding.chipTags.setOnClickListener(v -> editTags(context, binding));
        binding.chipVariable.setOnClickListener(v -> editVariables(context, binding));
        binding.chipSubtask.setOnClickListener(v -> editSubtasks(context, binding));
        binding.chipAction.setOnClickListener(v -> editActions(context, binding));
        binding.chipLocate.setOnClickListener(v -> locateCard(context, binding));

        // 文本工具：直接作用于编辑框文本，光标位置保留
        binding.chipSearch.setOnClickListener(v -> searchText(context, binding));
        binding.chipCopy.setOnClickListener(v -> copyText(context, binding));
        binding.chipPaste.setOnClickListener(v -> pasteText(context, binding));
        binding.chipDelete.setOnClickListener(v -> deleteText(context, binding));
        binding.chipFormat.setOnClickListener(v -> format(context, binding));

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(binding.getRoot())
                .create();

        dialog.setOnShowListener(d -> {
            // 全屏：铺满窗口，去掉对话框圆角背景，键盘弹出时让出空间
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                window.setBackgroundDrawable(new ColorDrawable(DisplayUtil.getAttrColor(context, com.google.android.material.R.attr.colorSurface)));
                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
            binding.codeClose.setOnClickListener(v -> dialog.dismiss());
            binding.codeSave.setOnClickListener(v -> saveText(context, binding, dialog, listener, onSaved));
        });
        // 编辑中的长文本误触外面就丢了，关掉外部点击关闭；返回键仍可退出
        dialog.setCanceledOnTouchOutside(false);

        // 改过内容就把上一次的报错收起来，避免旧错误一直挂着
        binding.codeEdit.addTextChangedListener(new TextChangedListener() {
            @Override
            public void afterTextChanged(Editable s) {
                hideError(binding);
            }
        });

        dialog.show();
        if (focusActionId != null) {
            binding.codeEdit.post(() -> jumpToActionId(context, binding, focusActionId));
        }
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

    // 变量：列出文本里的变量，可新建、编辑类型/名称、删除
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
                    else showVariableActions(context, binding, document, which);
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

    private static void showVariableActions(Context context, DialogTaskCodeBinding binding, JsonObject document, int index) {
        JsonArray variables = getArray(document, "variables");
        if (index < 0 || index >= variables.size() || !variables.get(index).isJsonObject()) return;

        String name = variableName(variables.get(index));
        new MaterialAlertDialogBuilder(context)
                .setTitle(name)
                .setItems(new CharSequence[]{
                        context.getString(R.string.task_code_variable_edit),
                        context.getString(R.string.task_code_delete)}, (dialog, which) -> {
                    if (which == 0) {
                        TaskCodeVariableEditor.show(context, variables.get(index), edited -> {
                            variables.set(index, edited);
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

    // 子任务：列出文本里的子任务，可新建、局部编辑、重命名、删除
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
                    else showSubtaskActions(context, binding, document, which);
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

    private static void showSubtaskActions(Context context, DialogTaskCodeBinding binding, JsonObject document, int index) {
        JsonArray tasks = getArray(document, "tasks");
        if (index < 0 || index >= tasks.size() || !tasks.get(index).isJsonObject()) return;

        JsonObject task = tasks.get(index).getAsJsonObject();
        String name = variableName(tasks.get(index));
        new MaterialAlertDialogBuilder(context)
                .setTitle(name)
                .setItems(new CharSequence[]{
                        context.getString(R.string.task_code_subtask_edit),
                        context.getString(R.string.task_code_rename),
                        context.getString(R.string.task_code_delete)}, (dialog, which) -> {
                    if (which == 0) {
                        // 局部编辑：这段子树单独校验后合并回上层文本
                        showDocument(context, task, edited -> {
                            tasks.set(index, edited);
                            applyDocument(binding, document);
                        });
                    } else if (which == 1) {
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

    // 动作：插入模板或管理现有动作
    private static void editActions(Context context, DialogTaskCodeBinding binding) {
        JsonObject document = readDocument(context, binding);
        if (document == null) return;

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.task_code_field_action)
                .setItems(new CharSequence[]{
                        context.getString(R.string.task_code_action_insert),
                        context.getString(R.string.task_code_action_manage)}, (dialog, which) -> {
                    if (which == 0) insertAction(context, binding, document);
                    else manageActions(context, binding, document);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static void insertAction(Context context, DialogTaskCodeBinding binding, JsonObject document) {
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

    private static void manageActions(Context context, DialogTaskCodeBinding binding, JsonObject document) {
        JsonArray actions = getArray(document, "actions");
        if (actions.isEmpty()) {
            Toast.makeText(context, R.string.task_code_action_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        List<Integer> order = sortedActionIndexes(actions);
        List<CharSequence> items = new ArrayList<>();
        for (int index : order) items.add(actionLabel(actions.get(index)));

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.task_code_action_list)
                .setItems(items.toArray(new CharSequence[0]), (dialog, which) -> {
                    if (which < 0 || which >= order.size()) return;
                    showActionActions(context, binding, document, order.get(which));
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private static void showActionActions(Context context, DialogTaskCodeBinding binding, JsonObject document, int index) {
        JsonArray actions = getArray(document, "actions");
        if (index < 0 || index >= actions.size()) return;

        new MaterialAlertDialogBuilder(context)
                .setTitle(actionLabel(actions.get(index)))
                .setItems(new CharSequence[]{
                        context.getString(R.string.task_code_locate_action),
                        context.getString(R.string.task_code_move_up),
                        context.getString(R.string.task_code_move_down),
                        context.getString(R.string.task_code_action_delete)}, (dialog, which) -> {
                    if (which == 0) {
                        String actionId = getString(actions.get(index).getAsJsonObject(), "id");
                        if (actionId != null) jumpToActionId(context, binding, actionId);
                    } else if (which == 1) moveAction(context, binding, document, index, -1);
                    else if (which == 2) moveAction(context, binding, document, index, 1);
                    else deleteAction(context, binding, document, index);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // 上移/下移交换的是卡片坐标，画布本身按坐标排序
    private static void moveAction(Context context, DialogTaskCodeBinding binding, JsonObject document, int index, int direction) {
        JsonArray actions = getArray(document, "actions");
        List<Integer> order = sortedActionIndexes(actions);
        int from = order.indexOf(index);
        int to = from + direction;
        if (from < 0 || to < 0 || to >= order.size()) {
            Toast.makeText(context, direction < 0 ? R.string.task_code_action_move_up : R.string.task_code_action_move_down, Toast.LENGTH_SHORT).show();
            return;
        }

        JsonObject first = actions.get(index).getAsJsonObject();
        JsonObject second = actions.get(order.get(to)).getAsJsonObject();
        JsonObject firstPos = getOrCreatePos(first);
        JsonObject secondPos = getOrCreatePos(second);
        int firstX = getInt(firstPos, "x", 0);
        int firstY = getInt(firstPos, "y", 0);
        int secondX = getInt(secondPos, "x", 0);
        int secondY = getInt(secondPos, "y", 0);
        firstPos.addProperty("x", secondX);
        firstPos.addProperty("y", secondY);
        secondPos.addProperty("x", firstX);
        secondPos.addProperty("y", firstY);
        applyDocument(binding, document);
    }

    // 删除动作时一起清掉指向它的连线，否则保存会被连线校验拦下
    private static void deleteAction(Context context, DialogTaskCodeBinding binding, JsonObject document, int index) {
        JsonArray actions = getArray(document, "actions");
        if (index < 0 || index >= actions.size() || !actions.get(index).isJsonObject()) return;

        JsonObject removed = actions.get(index).getAsJsonObject();
        String removedActionId = getString(removed, "id");
        Set<String> removedPinIds = new HashSet<>();
        JsonElement removedPins = removed.get("pins");
        if (removedPins != null && removedPins.isJsonArray()) {
            for (JsonElement pin : removedPins.getAsJsonArray()) {
                if (!pin.isJsonObject()) continue;
                String pinId = getString(pin.getAsJsonObject(), "id");
                if (pinId != null) removedPinIds.add(pinId);
            }
        }
        actions.remove(index);

        if (removedActionId != null || !removedPinIds.isEmpty()) {
            for (JsonElement element : actions) {
                if (!element.isJsonObject()) continue;
                JsonElement pinsElement = element.getAsJsonObject().get("pins");
                if (pinsElement == null || !pinsElement.isJsonArray()) continue;
                for (JsonElement pin : pinsElement.getAsJsonArray()) {
                    if (!pin.isJsonObject()) continue;
                    JsonElement linksElement = pin.getAsJsonObject().get("links");
                    if (linksElement == null || !linksElement.isJsonObject()) continue;
                    JsonObject links = linksElement.getAsJsonObject();
                    List<String> stale = new ArrayList<>();
                    for (Map.Entry<String, JsonElement> entry : links.entrySet()) {
                        String targetActionId = entry.getValue().isJsonPrimitive() ? entry.getValue().getAsString() : null;
                        if (removedActionId.equals(targetActionId) || removedPinIds.contains(entry.getKey())) {
                            stale.add(entry.getKey());
                        }
                    }
                    for (String pinId : stale) links.remove(pinId);
                }
            }
        }
        applyDocument(binding, document);
    }

    // 定位：先列出现有卡片，选中后滚到它的代码
    private static void locateCard(Context context, DialogTaskCodeBinding binding) {
        JsonObject document = readDocument(context, binding);
        if (document == null) return;

        JsonArray actions = getArray(document, "actions");
        if (actions.isEmpty()) {
            Toast.makeText(context, R.string.task_code_action_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        List<Integer> order = sortedActionIndexes(actions);
        List<CharSequence> items = new ArrayList<>();
        for (int index : order) items.add(actionLabel(actions.get(index)));

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.task_code_locate)
                .setItems(items.toArray(new CharSequence[0]), (dialog, which) -> {
                    if (which < 0 || which >= order.size()) return;
                    JsonObject action = actions.get(order.get(which)).getAsJsonObject();
                    String actionId = getString(action, "id");
                    if (actionId != null) jumpToActionId(context, binding, actionId);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    // 按动作 id 找到它在文本里的位置，选中并滚到整段代码的开头
    private static void jumpToActionId(Context context, DialogTaskCodeBinding binding, String actionId) {
        CharSequence textValue = binding.codeEdit.getText();
        String text = textValue == null ? "" : textValue.toString();
        int index = findIdFieldIndex(text, actionId);
        if (index < 0) {
            Toast.makeText(context, R.string.task_code_locate_missing, Toast.LENGTH_SHORT).show();
            return;
        }

        int[] span = findEnclosingObjectSpan(text, index);
        int start = span == null ? index : span[0];

        binding.codeEdit.requestFocus();
        binding.codeEdit.setSelection(start);
        binding.codeEdit.post(() -> scrollToOffset(binding, start));
    }

    // 找出形如 "id": "<actionId>" 的键位置（连线的值也含动作 id，不能直接 indexOf）
    private static int findIdFieldIndex(String text, String actionId) {
        int from = 0;
        while (true) {
            int key = text.indexOf("\"id\"", from);
            if (key < 0) return -1;
            int i = key + 4;
            while (i < text.length() && Character.isWhitespace(text.charAt(i))) i++;
            if (i < text.length() && text.charAt(i) == ':') {
                i++;
                while (i < text.length() && Character.isWhitespace(text.charAt(i))) i++;
                if (i < text.length() && text.charAt(i) == '"') {
                    int end = text.indexOf('"', i + 1);
                    if (end > i && text.substring(i + 1, end).equals(actionId)) return key;
                }
            }
            from = key + 4;
        }
    }

    // 从位置往外找最小的完整 JSON 对象范围，用于选中整张卡片的代码
    private static int[] findEnclosingObjectSpan(String text, int index) {        int bestStart = -1;
        int bestEnd = -1;
        List<Integer> stack = new ArrayList<>();
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') inString = false;
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                stack.add(i);
            } else if (c == '}' && !stack.isEmpty()) {
                int start = stack.remove(stack.size() - 1);
                if (start <= index && index <= i && start > bestStart) {
                    bestStart = start;
                    bestEnd = i + 1;
                }
            }
        }
        return bestStart < 0 ? null : new int[]{bestStart, bestEnd};
    }

    // 搜索：从光标后找下一处，找不到就从头再找
    private static void searchText(Context context, DialogTaskCodeBinding binding) {
        String text = currentText(binding);
        int selectionStart = binding.codeEdit.getSelectionStart();
        int selectionEnd = binding.codeEdit.getSelectionEnd();
        String selected = selectionStart >= 0 && selectionEnd > selectionStart
                ? text.substring(Math.min(selectionStart, text.length()), Math.min(selectionEnd, text.length())) : "";
        String query = lastSearch.isEmpty() ? selected : lastSearch;

        AppUtil.showEditDialog(context, R.string.task_code_search, query, result -> {
            if (result == null || result.isEmpty()) return;
            lastSearch = result;
            int from = Math.max(binding.codeEdit.getSelectionEnd(), 0);
            int index = text.indexOf(result, Math.min(from, text.length()));
            if (index < 0) index = text.indexOf(result);
            if (index < 0) {
                Toast.makeText(context, R.string.task_code_search_not_found, Toast.LENGTH_SHORT).show();
                return;
            }
            binding.codeEdit.requestFocus();
            binding.codeEdit.setSelection(index, index + result.length());
            final int found = index;
            binding.codeEdit.post(() -> scrollToOffset(binding, found));
        });
    }

    // 复制：优先复制选中内容，否则复制光标所在的整张卡片，再否则复制全文
    private static void copyText(Context context, DialogTaskCodeBinding binding) {
        String text = currentText(binding);
        int start = binding.codeEdit.getSelectionStart();
        int end = binding.codeEdit.getSelectionEnd();

        String copied;
        if (start >= 0 && end > start) {
            copied = text.substring(Math.min(start, text.length()), Math.min(end, text.length()));
        } else {
            int index = Math.max(0, Math.min(start, text.length() - 1));
            int[] span = findActionSpanAt(text, index);
            copied = span == null ? text : text.substring(span[0], span[1]);
        }
        if (copied.isEmpty()) {
            Toast.makeText(context, R.string.task_code_copy_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager != null) manager.setPrimaryClip(ClipData.newPlainText("task_code", copied));
        Toast.makeText(context, context.getString(R.string.task_code_copied, copied.length()), Toast.LENGTH_SHORT).show();
    }

    // 粘贴：在光标处插入剪贴板文本，光标停在新内容之后
    private static void pasteText(Context context, DialogTaskCodeBinding binding) {
        String clip = readClipboard(context);
        if (clip.isEmpty()) {
            Toast.makeText(context, R.string.task_code_paste_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        String text = currentText(binding);
        int start = Math.max(0, Math.min(binding.codeEdit.getSelectionStart(), text.length()));
        int end = Math.max(start, Math.min(binding.codeEdit.getSelectionEnd(), text.length()));
        String newText = text.substring(0, start) + clip + text.substring(end);
        int cursor = Math.min(start + clip.length(), newText.length());

        binding.codeEdit.setText(newText);
        binding.codeEdit.setSelection(cursor);
        binding.codeEdit.post(() -> scrollToOffset(binding, cursor));
        hideError(binding);
    }

    // 删除：有选中就删选中，否则删光标所在的整张卡片（含前后逗号）
    private static void deleteText(Context context, DialogTaskCodeBinding binding) {
        String text = currentText(binding);
        int start = binding.codeEdit.getSelectionStart();
        int end = binding.codeEdit.getSelectionEnd();
        if (start < 0) return;

        if (start == end) {
            int index = Math.max(0, Math.min(start, text.length() - 1));
            int[] span = findActionSpanAt(text, index);
            if (span == null) {
                Toast.makeText(context, R.string.task_code_delete_nothing, Toast.LENGTH_SHORT).show();
                return;
            }
            int[] range = expandForRemoval(text, span[0], span[1]);
            start = range[0];
            end = range[1];
        }
        if (end <= start) return;

        String newText = text.substring(0, Math.min(start, text.length())) + text.substring(Math.min(end, text.length()));
        int cursor = Math.min(start, newText.length());
        binding.codeEdit.setText(newText);
        binding.codeEdit.setSelection(cursor);
        binding.codeEdit.post(() -> scrollToOffset(binding, cursor));
        hideError(binding);
    }

    private static String currentText(DialogTaskCodeBinding binding) {
        CharSequence text = binding.codeEdit.getText();
        return text == null ? "" : text.toString();
    }

    private static String readClipboard(Context context) {
        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null || !manager.hasPrimaryClip()) return "";
        ClipData clip = manager.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return "";
        CharSequence text = clip.getItemAt(0).coerceToText(context);
        return text == null ? "" : text.toString();
    }

    private static void scrollToOffset(DialogTaskCodeBinding binding, int offset) {
        int clamped = Math.max(0, Math.min(offset, binding.codeEdit.length()));
        Layout layout = binding.codeEdit.getLayout();
        if (layout == null) {
            binding.codeEdit.bringPointIntoView(clamped);
            return;
        }
        int top = layout.getLineTop(layout.getLineForOffset(clamped));
        int padding = binding.codeEdit.getPaddingTop() + binding.codeEdit.getPaddingBottom();
        int height = Math.max(1, binding.codeEdit.getHeight() - padding);
        binding.codeEdit.scrollTo(0, Math.max(0, top - height / 3));
    }

    // 光标所在动作（含子任务里的动作）的代码范围
    private static int[] findActionSpanAt(String text, int index) {
        JsonObject document;
        try {
            document = TaskCode.parseDocument(text);
        } catch (TaskCode.TaskCodeException e) {
            return null;
        }

        Set<String> actionIds = new HashSet<>();
        collectActionIds(document, actionIds);
        int[] best = null;
        for (String actionId : actionIds) {
            int key = findIdFieldIndex(text, actionId);
            if (key < 0) continue;
            int[] span = findEnclosingObjectSpan(text, key);
            if (span == null || span[0] > index || index > span[1]) continue;
            if (best == null || span[0] > best[0]) best = span;
        }
        return best;
    }

    private static void collectActionIds(JsonObject task, Set<String> ids) {
        JsonElement actionsElement = task.get("actions");
        if (actionsElement != null && actionsElement.isJsonArray()) {
            for (JsonElement item : actionsElement.getAsJsonArray()) {
                if (!item.isJsonObject()) continue;
                String id = getString(item.getAsJsonObject(), "id");
                if (id != null) ids.add(id);
            }
        }

        JsonElement tasksElement = task.get("tasks");
        if (tasksElement != null && tasksElement.isJsonArray()) {
            for (JsonElement item : tasksElement.getAsJsonArray()) {
                if (item.isJsonObject()) collectActionIds(item.getAsJsonObject(), ids);
            }
        }
    }

    // 删除一个对象时顺带处理前后的逗号，保证剩下的 JSON 依然合法
    private static int[] expandForRemoval(String text, int start, int end) {
        int i = end;
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) i++;
        if (i < text.length() && text.charAt(i) == ',') {
            i++;
            while (i < text.length() && Character.isWhitespace(text.charAt(i))) i++;
            return new int[]{start, i};
        }

        int j = start - 1;
        while (j >= 0 && Character.isWhitespace(text.charAt(j))) j--;
        if (j >= 0 && text.charAt(j) == ',') return new int[]{j, end};
        return new int[]{start, end};
    }

    private static List<Integer> sortedActionIndexes(JsonArray actions) {
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < actions.size(); i++) indexes.add(i);
        indexes.sort((left, right) -> {
            Point leftPos = actionPos(actions.get(left));
            Point rightPos = actionPos(actions.get(right));
            if (leftPos.y != rightPos.y) return Integer.compare(leftPos.y, rightPos.y);
            return Integer.compare(leftPos.x, rightPos.x);
        });
        return indexes;
    }

    private static Point actionPos(JsonElement element) {
        if (element != null && element.isJsonObject()) {
            JsonElement pos = element.getAsJsonObject().get("pos");
            if (pos != null && pos.isJsonObject()) {
                return new Point(getInt(pos.getAsJsonObject(), "x", 0), getInt(pos.getAsJsonObject(), "y", 0));
            }
        }
        return new Point(0, 0);
    }

    private static JsonObject getOrCreatePos(JsonObject action) {
        JsonElement pos = action.get("pos");
        if (pos != null && pos.isJsonObject()) return pos.getAsJsonObject();
        JsonObject created = new JsonObject();
        created.addProperty("x", 0);
        created.addProperty("y", 0);
        action.add("pos", created);
        return created;
    }

    private static String actionLabel(JsonElement element) {
        if (element == null || !element.isJsonObject()) return "?";
        JsonObject object = element.getAsJsonObject();
        String typeName = getString(object, "type");
        ActionType type = parseType(typeName);
        ActionInfo info = type == null ? null : ActionInfo.getActionInfo(type);
        String typeTitle = info == null ? (typeName == null ? "?" : typeName) : displayTitle(info, typeName);
        String custom = getString(object, "title");
        if (custom == null || custom.isEmpty()) return typeTitle;
        return custom + "（" + typeTitle + "）";
    }

    private static String displayTitle(ActionInfo info, String fallback) {
        String title = info.getTitle();
        if (title == null) return fallback;
        title = title.replace("%s", "").replace("%d", "").replaceAll("\\s+", " ").trim();
        return title.isEmpty() ? fallback : title;
    }

    private static ActionType parseType(String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            return ActionType.valueOf(name);
        } catch (Exception e) {
            return null;
        }
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
                Point pos = actionPos(item);
                x = Math.max(x, pos.x);
                y = Math.max(y, pos.y);
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

    private static void saveText(Context context, DialogTaskCodeBinding binding, AlertDialog dialog, DocumentListener listener, Runnable onSaved) {
        CharSequence text = binding.codeEdit.getText();
        try {
            listener.onSave(text == null ? "" : text.toString());
        } catch (TaskCode.TaskCodeException e) {
            showError(context, binding, e);
            return;
        } catch (Exception e) {
            binding.codeError.setText(context.getString(R.string.task_code_failed, String.valueOf(e.getMessage())));
            binding.codeError.setVisibility(View.VISIBLE);
            return;
        }

        dialog.dismiss();
        if (onSaved != null) onSaved.run();
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
