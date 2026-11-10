package top.bogey.touch_tool.ui.blueprint.pin_widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.UUID;

import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinAdd;
import top.bogey.touch_tool.databinding.PinWidgetAddBinding;
import top.bogey.touch_tool.ui.blueprint.card.ActionCard;
import top.bogey.touch_tool.ui.blueprint.pin.PinView;

@SuppressLint("ViewConstructor")
public class PinWidgetAdd extends PinWidget<PinAdd> {
    private final PinWidgetAddBinding binding;

    public PinWidgetAdd(@NonNull Context context, ActionCard card, PinView pinView, PinAdd pinBase, boolean custom) {
        super(context, card, pinView, pinBase, custom);
        binding = PinWidgetAddBinding.inflate(LayoutInflater.from(context), this, true);
        init();
    }

    @Override
    protected void initBase() {
        binding.addPinButton.setOnClickListener(v -> {
            List<Pin> pins = pinBase.getPins();
            for (Pin pin : pins) {
                Pin copy = pin.newCopy();
                copy.setUid(UUID.randomUUID().toString());
                copy.setTitleId(pin.getTitleId());
                copy.setDynamic(true);
                card.addPin(pinView.getPin(), copy);
            }
        });
    }

    @Override
    protected void initCustom() {

    }
}
