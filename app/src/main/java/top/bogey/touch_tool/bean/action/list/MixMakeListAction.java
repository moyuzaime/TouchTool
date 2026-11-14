package top.bogey.touch_tool.bean.action.list;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.DynamicPinsAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinAdd;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskRunnable;

// 混合新建列表：与「新建列表」的差别是不做类型统一，每个动态针脚可以各自取不同类型
// 依据 261114_1909 版 APK 反编译重建（MixMakeListAction）
public class MixMakeListAction extends ListCalculateAction implements DynamicPinsAction {
    private final transient Pin morePin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object);
    private final transient Pin addPin = new Pin(new PinAdd(morePin), R.string.pin_add_pin);
    private final transient Pin listPin = new Pin(new PinList(), true);

    public MixMakeListAction() {
        super(ActionType.MIX_LIST_MAKE);
        addPins(new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object, false, true), addPin, listPin);
    }

    public MixMakeListAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(morePin, false);
        reAddPins(addPin, listPin);
    }

    @Override
    public void handleUnLinkFrom(Task task, Pin origin) {
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        for (Pin dynamicPin : getDynamicPins()) {
            listPin.getValue(PinList.class).add(getPinValue(runnable, dynamicPin));
        }
    }

    @Override
    public void resetReturnValue(TaskRunnable runnable, Pin pin) {
        listPin.setValue(new PinList());
    }

    // 返回空列表即不做类型统一：每行可以各自取不同类型，这是「混合」与「新建列表」的唯一差别
    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return java.util.Collections.emptyList();
    }

    @Override
    public List<Pin> getDynamicPins() {
        List<Pin> pins = new ArrayList<>();
        boolean start = true;
        for (Pin pin : getPins()) {
            if (pin == addPin) start = false;
            if (start) pins.add(pin);
        }
        return pins;
    }

    public Pin getListPin() {
        return listPin;
    }
}
