package top.bogey.touch_tool.ui.blueprint.pin_widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.databinding.PinWidgetContainerValueBinding;
import top.bogey.touch_tool.ui.blueprint.card.ActionCard;
import top.bogey.touch_tool.ui.blueprint.pin.PinView;
import top.bogey.touch_tool.ui.blueprint.selecter.pin_container.PinContainerValueDialog;

/**
 * 列表常量的编辑入口：输出针脚和已连线的输入针脚不显示
 */
@SuppressLint("ViewConstructor")
public class PinWidgetListValue extends PinWidget<PinList> {
    private final PinWidgetContainerValueBinding binding;

    public PinWidgetListValue(@NonNull Context context, ActionCard card, PinView pinView, PinList pinBase, boolean custom) {
        super(context, card, pinView, pinBase, custom);
        binding = PinWidgetContainerValueBinding.inflate(LayoutInflater.from(context), this, true);
        init();
    }

    @Override
    protected void initBase() {
        // 已连线的输入针脚运行期会被来源值覆盖，不显示编辑器
        Pin pin = pinView.getPin();
        if (pin.isOut() || pin.isLinked()) {
            binding.text.setVisibility(GONE);
            binding.pickButton.setVisibility(GONE);
            return;
        }
        refreshText();
        binding.pickButton.setOnClickListener(v -> showEditor());
    }

    private void refreshText() {
        binding.text.setVisibility(VISIBLE);
        binding.pickButton.setVisibility(VISIBLE);
        binding.text.setText(getContext().getString(R.string.pin_container_count, pinBase.size()));
    }

    private void showEditor() {
        PinContainerValueDialog dialog = new PinContainerValueDialog(getContext(), pinBase);
        new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.pin_container_edit_list)
                .setView(dialog)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.enter, (d, which) -> {
                    dialog.applyTo(pinBase);
                    refreshText();
                    pinView.getPin().notifyValueUpdated(card.getTask());
                })
                .show();
    }

    @Override
    protected void initCustom() {

    }
}
