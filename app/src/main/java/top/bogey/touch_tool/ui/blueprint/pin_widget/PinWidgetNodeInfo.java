package top.bogey.touch_tool.ui.blueprint.pin_widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.ListPopupWindow;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinNodeInfoValue;
import top.bogey.touch_tool.ui.blueprint.card.ActionCard;
import top.bogey.touch_tool.ui.blueprint.pin.PinView;
import top.bogey.touch_tool.utils.DisplayUtil;

// 控件信息针脚控件：下拉选择要输出的内容，下面显示计算出的值
@SuppressLint("ViewConstructor")
public class PinWidgetNodeInfo extends PinWidget<PinNodeInfoValue> {
    private final TextView spinner;
    private final TextView valueText;
    private final String[] options;
    private ArrayAdapter<String> adapter;

    public PinWidgetNodeInfo(@NonNull Context context, ActionCard card, PinView pinView, PinNodeInfoValue pinBase, boolean custom) {
        super(context, card, pinView, pinBase, custom);
        options = context.getResources().getStringArray(R.array.node_info_type);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, (int) DisplayUtil.dp2px(context, 2), 0, (int) DisplayUtil.dp2px(context, 2));

        spinner = new TextView(context);
        spinner.setTextSize(14);
        spinner.setPadding(0, (int) DisplayUtil.dp2px(context, 2), 0, (int) DisplayUtil.dp2px(context, 2));

        valueText = new TextView(context);
        valueText.setTextSize(12);
        valueText.setPadding(0, 0, 0, 0);

        layout.addView(spinner);
        layout.addView(valueText);
        addView(layout);
        init();
    }

    @Override
    protected void initBase() {
        adapter = new ArrayAdapter<>(getContext(), R.layout.widget_textview_item, options);
        int index = Math.max(0, Math.min(pinBase.getIndex(), options.length - 1));
        spinner.setText(options[index]);
        String value = pinBase.getValue();
        valueText.setText(value == null ? "" : value);

        spinner.setOnClickListener(v -> {
            ListPopupWindow popup = new ListPopupWindow(getContext());
            popup.setAdapter(adapter);
            popup.setAnchorView(spinner);
            popup.setModal(true);
            popup.setWidth(DisplayUtil.measureArrayAdapterContentWidth(getContext(), adapter));
            popup.setOnItemClickListener((parent, view, position, id) -> {
                pinBase.setIndex(position);
                spinner.setText(options[position]);
                pinView.getPin().notifyValueUpdated(card.getTask());
                popup.dismiss();
            });
            popup.show();
        });
    }

    @Override
    protected void initCustom() {
    }
}