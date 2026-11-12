package top.bogey.touch_tool.ui.blueprint.pin_widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.view.LayoutInflater;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinIconExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinStringExecute;
import top.bogey.touch_tool.databinding.PinWidgetExecuteBinding;
import top.bogey.touch_tool.ui.blueprint.card.ActionCard;
import top.bogey.touch_tool.ui.blueprint.pin.PinView;
import top.bogey.touch_tool.ui.blueprint.selecter.select_icon.SelectIconDialog;
import top.bogey.touch_tool.utils.listener.TextChangedListener;

@SuppressLint("ViewConstructor")
public class PinWidgetExecute extends PinWidget<PinExecute> {
    private final PinWidgetExecuteBinding binding;

    public PinWidgetExecute(@NonNull Context context, ActionCard card, PinView pinView, PinExecute pinBase, boolean custom) {
        super(context, card, pinView, pinBase, custom);
        binding = PinWidgetExecuteBinding.inflate(LayoutInflater.from(context), this, true);
        init();
    }

    @Keep
    public PinWidgetExecute(@NonNull Context context, ActionCard card, PinView pinView, PinStringExecute pinBase, boolean custom) {
        this(context, card, pinView, (PinExecute) pinBase, custom);
    }

    @Keep
    public PinWidgetExecute(@NonNull Context context, ActionCard card, PinView pinView, PinIconExecute pinBase, boolean custom) {
        this(context, card, pinView, (PinExecute) pinBase, custom);
    }

    @Override
    protected void initBase() {
        binding.title.setSaveEnabled(false);
        binding.title.setSaveFromParentEnabled(false);

        switch (pinBase.getSubType()) {
            case WITH_STRING -> {
                pinView.getTitleView().setVisibility(GONE);
                binding.titleBox.setVisibility(VISIBLE);
                binding.pickButton.setVisibility(GONE);
                PinStringExecute stringExecute = (PinStringExecute) pinBase;
                binding.title.setText(stringExecute.getValue());
                binding.title.addTextChangedListener(new TextChangedListener() {
                    @Override
                    public void afterTextChanged(Editable s) {
                        stringExecute.setValue(s.toString());
                        pinView.getPin().notifyValueUpdated(card.getTask());
                        saveNow();
                    }
                });
            }
            case WITH_ICON -> {
                pinView.getTitleView().setVisibility(GONE);
                binding.titleBox.setVisibility(VISIBLE);
                binding.pickButton.setVisibility(VISIBLE);
                PinIconExecute iconExecute = (PinIconExecute) pinBase;
                binding.pickButton.setImageBitmap(iconExecute.getImage());
                binding.pickButton.setOnClickListener(v -> new SelectIconDialog(getContext(), result -> {
                    binding.pickButton.setImageBitmap(result);
                    iconExecute.setImage(result);
                    pinView.getPin().notifyValueUpdated(card.getTask());
                }).show());
                binding.title.setText(iconExecute.getValue());
                binding.title.addTextChangedListener(new TextChangedListener() {
                    @Override
                    public void afterTextChanged(Editable s) {
                        iconExecute.setValue(s.toString());
                        pinView.getPin().notifyValueUpdated(card.getTask());
                        saveNow();
                    }
                });
            }
        }
    }

    // 名字类针脚编辑后防抖保存，避免只改了内存、运行时读存档拿不到
    private void saveNow() {
        try {
            if (card != null && card.getTask() != null) {
                card.getTask().save();
                top.bogey.touch_tool.utils.AppLog.e("分支命名", "saved");
            }
        } catch (Throwable e) {
            top.bogey.touch_tool.utils.AppLog.e("分支命名", "save failed");
        }
    }

    @Override
    protected void initCustom() {

    }
}
