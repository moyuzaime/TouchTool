package top.bogey.touch_tool.ui.blueprint.code;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.pin.PinInfo;
import top.bogey.touch_tool.bean.pin.pin_objects.PinType;
import top.bogey.touch_tool.bean.task.Variable;
import top.bogey.touch_tool.databinding.DialogTaskCodeVariableBinding;
import top.bogey.touch_tool.utils.GsonUtil;

// 变量类型编辑：类型（单独/列表/字典）与列表元素、字典键/值类型；改类型会按变量自身逻辑重置值
public class TaskCodeVariableEditor {
    public interface Listener {
        void onEdited(JsonObject variable);
    }

    public static void show(Context context, JsonElement element, Listener listener) {
        if (element == null || !element.isJsonObject()) return;
        Variable variable;
        try {
            variable = GsonUtil.prettyGson.fromJson(element, Variable.class);
        } catch (Exception e) {
            Toast.makeText(context, R.string.task_code_variable_type_error, Toast.LENGTH_SHORT).show();
            return;
        }
        if (variable == null) return;
        new TaskCodeVariableEditor(context, variable, listener).showDialog();
    }

    private final Context context;
    private final DialogTaskCodeVariableBinding binding;
    private final Variable variable;
    private final Listener listener;

    private Variable.VariableType kind;
    private PinInfo valueInfo;
    private PinInfo keyInfo;

    private TaskCodeVariableEditor(Context context, Variable variable, Listener listener) {
        this.context = context;
        this.variable = variable;
        this.listener = listener;
        this.kind = variable.getType();
        this.valueInfo = kind == Variable.VariableType.MAP ? variable.getValuePinInfo() : variable.getPinInfo();
        this.keyInfo = variable.getKeyPinInfo();

        binding = DialogTaskCodeVariableBinding.inflate(LayoutInflater.from(context));
        binding.nameEdit.setText(variable.getTitle());
        binding.kindButton.setOnClickListener(v -> selectKind());
        binding.valueTypeButton.setOnClickListener(v -> selectPinInfo(true));
        binding.keyTypeButton.setOnClickListener(v -> selectPinInfo(false));
        refresh();
    }

    private void showDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.task_code_variable_edit)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.enter, null)
                .setNegativeButton(R.string.cancel, null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if (confirm()) dialog.dismiss();
        }));
        dialog.show();
    }

    private boolean confirm() {
        Variable edited = variable.copy();
        CharSequence name = binding.nameEdit.getText();
        edited.setTitle(name == null ? "" : name.toString().trim());

        if (edited.getType() != kind) {
            if (!edited.setType(kind)) {
                Toast.makeText(context, R.string.task_code_variable_type_error, Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        if (kind == Variable.VariableType.MAP) {
            if (keyInfo != null) edited.setKeyPinInfo(keyInfo);
            if (valueInfo != null) edited.setValuePinInfo(valueInfo);
        } else if (valueInfo != null) {
            edited.setPinInfo(valueInfo);
        }

        JsonObject object = GsonUtil.prettyGson.toJsonTree(edited).getAsJsonObject();
        object.remove("uid");
        if (listener != null) listener.onEdited(object);
        return true;
    }

    private void selectKind() {
        String[] kinds = context.getResources().getStringArray(R.array.pin_simple_type);
        Variable.VariableType[] values = Variable.VariableType.values();
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.task_code_variable_kind)
                .setItems(kinds, (dialog, which) -> {
                    if (which >= values.length) return;
                    kind = values[which];
                    refresh();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void selectPinInfo(boolean value) {
        List<PinInfo> infos = new ArrayList<>();
        List<CharSequence> labels = new ArrayList<>();
        for (Map.Entry<PinType, List<PinInfo>> entry : PinInfo.getCustomPinInfoMap().entrySet()) {
            for (PinInfo info : entry.getValue()) {
                infos.add(info);
                labels.add(info.getTitle());
            }
        }
        if (infos.isEmpty()) return;

        PinInfo current = value ? valueInfo : keyInfo;
        int checked = indexOf(infos, current);
        new MaterialAlertDialogBuilder(context)
                .setTitle(value ? R.string.task_code_variable_value_type : R.string.task_code_variable_key_type)
                .setSingleChoiceItems(labels.toArray(new CharSequence[0]), checked, (dialog, which) -> {
                    if (value) valueInfo = infos.get(which);
                    else keyInfo = infos.get(which);
                    dialog.dismiss();
                    refresh();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void refresh() {
        String[] kinds = context.getResources().getStringArray(R.array.pin_simple_type);
        int index = kind.ordinal();
        binding.kindButton.setText(kinds[Math.min(index, kinds.length - 1)]);

        boolean map = kind == Variable.VariableType.MAP;
        binding.keyTypeRow.setVisibility(map ? android.view.View.VISIBLE : android.view.View.GONE);
        if (map) binding.keyTypeButton.setText(keyInfo == null ? "" : keyInfo.getTitle());

        boolean list = kind == Variable.VariableType.LIST;
        binding.valueTypeLabel.setText(list ? R.string.task_code_variable_element_type : R.string.task_code_variable_value_type);
        binding.valueTypeButton.setText(valueInfo == null ? "" : valueInfo.getTitle());
    }

    private static int indexOf(List<PinInfo> infos, PinInfo info) {
        if (info == null) return -1;
        for (int i = 0; i < infos.size(); i++) {
            PinInfo item = infos.get(i);
            if (item.getType() == info.getType() && item.getSubType() == info.getSubType()) return i;
        }
        return -1;
    }
}
