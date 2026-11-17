package top.bogey.touch_tool.ui.blueprint.selecter.pin_container;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.pin.PinInfo;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinValueUtil;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.databinding.DialogPinContainerValueBinding;
import top.bogey.touch_tool.databinding.ItemPinValueRowBinding;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.listener.TextChangedListener;

/**
 * 列表 / 字典常量的编辑面板：改的是副本，点确定时才写回针脚
 */
@SuppressLint("ViewConstructor")
public class PinContainerValueDialog extends FrameLayout {
    private final DialogPinContainerValueBinding binding;
    private final List<Row> rows = new ArrayList<>();

    private final boolean mapMode;
    private final PinObject keyType;
    private final PinObject valueType;

    public PinContainerValueDialog(@NonNull Context context, PinList pinList) {
        super(context);
        binding = DialogPinContainerValueBinding.inflate(LayoutInflater.from(context), this, true);
        mapMode = false;
        keyType = null;
        valueType = pinList.getValueType();
        for (PinObject value : pinList) {
            rows.add(new Row(null, (PinObject) value.copy()));
        }
        init();
    }

    public PinContainerValueDialog(@NonNull Context context, PinMap pinMap) {
        super(context);
        binding = DialogPinContainerValueBinding.inflate(LayoutInflater.from(context), this, true);
        mapMode = true;
        keyType = pinMap.getKeyType();
        valueType = pinMap.getValueType();
        for (Map.Entry<PinObject, PinObject> entry : pinMap.entrySet()) {
            rows.add(new Row((PinObject) entry.getKey().copy(), (PinObject) entry.getValue().copy()));
        }
        init();
    }

    private void init() {
        binding.hint.setText(R.string.pin_container_hint);
        binding.addButton.setOnClickListener(v -> {
            rows.add(new Row(mapMode ? newRowValue(keyType) : null, newRowValue(valueType)));
            buildRows();
        });
        // 清空只清副本，取消仍然不改原值
        binding.clearButton.setOnClickListener(v -> {
            rows.clear();
            buildRows();
        });
        buildRows();
    }

    private void buildRows() {
        binding.rowBox.removeAllViews();
        if (rows.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText(R.string.pin_container_empty);
            int padding = (int) DisplayUtil.dp2px(getContext(), 8);
            empty.setPadding(0, padding, 0, padding);
            binding.rowBox.addView(empty);
        }
        for (int i = 0; i < rows.size(); i++) {
            buildRowView(i);
        }
        applyContentHeight();
    }

    private void buildRowView(int index) {
        Row row = rows.get(index);
        ItemPinValueRowBinding rowBinding = ItemPinValueRowBinding.inflate(LayoutInflater.from(getContext()), binding.rowBox, false);

        rowBinding.typeText.setText(getTypeTitle(row));
        rowBinding.upButton.setEnabled(index > 0);
        rowBinding.upButton.setOnClickListener(v -> swap(index, index - 1));
        rowBinding.downButton.setEnabled(index < rows.size() - 1);
        rowBinding.downButton.setOnClickListener(v -> swap(index, index + 1));
        rowBinding.removeButton.setOnClickListener(v -> {
            rows.remove(index);
            buildRows();
        });

        if (mapMode) {
            rowBinding.keyText.setText(PinValueUtil.getText(row.key()));
            bindText(rowBinding.keyText, row.key(), keyType);
        } else {
            rowBinding.keyBox.setVisibility(GONE);
        }
        rowBinding.valueText.setText(PinValueUtil.getText(row.value()));
        bindText(rowBinding.valueText, row.value(), valueType);

        binding.rowBox.addView(rowBinding.getRoot());
    }

    // 可编辑的类型挂文本监听，其它类型只读
    private void bindText(TextInputEditText editText, PinObject value, PinObject type) {
        if (!PinValueUtil.isTextEditable(type)) {
            editText.setEnabled(false);
            return;
        }
        editText.setEnabled(true);
        editText.addTextChangedListener(new TextChangedListener() {
            @Override
            public void afterTextChanged(Editable s) {
                PinValueUtil.setText(value, s.toString());
            }
        });
    }

    private void swap(int from, int to) {
        if (to < 0 || to >= rows.size()) return;
        rows.add(to, rows.remove(from));
        buildRows();
    }

    private String getTypeTitle(Row row) {
        if (mapMode) return getTypeTitle(keyType, row.key()) + " → " + getTypeTitle(valueType, row.value());
        return getTypeTitle(valueType, row.value());
    }

    // 容器类型是动态时按元素 / 键值的实际类型显示
    private String getTypeTitle(PinObject type, PinObject value) {
        PinObject titleType = (type == null || type.isDynamic()) && value != null ? value : type;
        String title = "";
        if (titleType != null) {
            PinInfo info = PinInfo.getPinInfo(titleType);
            if (info != null) title = info.getTitle();
        }
        if (PinValueUtil.isTextEditable(type)) return title;
        return getContext().getString(R.string.pin_container_read_only, title);
    }

    // 按行数估算高度并封顶，行多时在弹窗内滚动
    private void applyContentHeight() {
        int rowHeight = (int) DisplayUtil.dp2px(getContext(), 96);
        int header = (int) DisplayUtil.dp2px(getContext(), 150);
        int max = (int) (DisplayUtil.getScreenSize(getContext()).getHeight() * 0.6f);
        DisplayUtil.setViewHeight(this, Math.min(header + rows.size() * rowHeight, max));
    }

    private PinObject newRowValue(PinObject type) {
        PinObject value = PinValueUtil.newValue(type);
        PinValueUtil.setText(value, PinValueUtil.getDefaultText(type));
        return value;
    }

    public void applyTo(PinList pinList) {
        pinList.clear();
        for (Row row : rows) {
            if (row.value() != null) pinList.add(row.value());
        }
    }

    // overwrite 为 true 时后面的同键值覆盖前面的，false 时保留前面的
    public void applyTo(PinMap pinMap, boolean overwrite) {
        pinMap.clear();
        for (Row row : rows) {
            if (row.key() == null || row.value() == null) continue;
            if (!overwrite && pinMap.containsKey(row.key())) continue;
            pinMap.put(row.key(), row.value());
        }
    }

    // 重复键的个数
    public int getDuplicateCount() {
        Set<PinObject> keys = new HashSet<>();
        int count = 0;
        for (Row row : rows) {
            if (row.key() == null) continue;
            if (!keys.add(row.key())) count++;
        }
        return count;
    }

    public record Row(PinObject key, PinObject value) {
    }
}
