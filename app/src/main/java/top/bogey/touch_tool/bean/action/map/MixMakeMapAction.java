package top.bogey.touch_tool.bean.action.map;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.DynamicPinsAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinAdd;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskRunnable;

// 混合新建字典：与「新建字典」的差别是不统一键值类型，每对键值各自取不同类型
// 依据 261114_1909 版 APK 反编译重建（MixMakeMapAction）
public class MixMakeMapAction extends MapCalculateAction implements DynamicPinsAction {
    private final transient Pin keyMorePin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.map_action_key);
    private final transient Pin valueMorePin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.map_action_value);
    private final transient Pin addPin = new Pin(new PinAdd(Arrays.asList(keyMorePin, valueMorePin)), R.string.pin_add_pin);
    private final transient Pin mapPin = new Pin(new PinMap(), true);

    public MixMakeMapAction() {
        super(ActionType.MIX_MAP_MAKE);
        Pin keyPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.map_action_key, false, true);
        Pin valuePin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.map_action_value, false, true);
        addPins(keyPin, valuePin, addPin, mapPin);
    }

    public MixMakeMapAction(JsonObject jsonObject) {
        super(jsonObject);
        Pin pin = tmpPins.get(0);
        while (!pin.isSameClass(PinAdd.class)) {
            reAddPin(keyMorePin.newCopy(), true);
            reAddPin(valueMorePin.newCopy(), true);
            pin = tmpPins.get(0);
        }
        reAddPins(addPin, mapPin);
    }

    @Override
    protected void handleUnLinkFrom(Task task, Pin origin) {
    }

    @Override
    public void resetReturnValue(TaskRunnable runnable, Pin pin) {
        mapPin.getValue().reset();
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinMap map = mapPin.getValue();
        List<Pin> dynamicPins = getDynamicPins();
        for (int i = 0; i + 1 < dynamicPins.size(); i += 2) {
            Pin keyPin = dynamicPins.get(i);
            Pin valuePin = dynamicPins.get(i + 1);
            PinObject keyObject = getPinValue(runnable, keyPin);
            PinObject valueObject = getPinValue(runnable, valuePin);
            map.put(keyObject, valueObject);
        }
    }

    // 返回空列表即不做类型统一：每对键值可以各自取不同类型
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

    @Override
    public void removePin(Pin pin) {
        List<Pin> pins = getPins();
        int index = pins.indexOf(pin);
        if (index < 0 || index >= pins.size()) return;
        int i = index % 2;
        if (i == 0 && index + 1 < pins.size()) {
            super.removePin(pins.get(index + 1));
            super.removePin(pin);
        }
        if (i == 1 && index - 1 >= 0) {
            super.removePin(pin);
            super.removePin(pins.get(index - 1));
        }
    }

    @Override
    public void removePin(Task context, Pin pin) {
        List<Pin> pins = getPins();
        int index = pins.indexOf(pin);
        if (index < 0) return;
        int i = index % 2;
        if (i == 0 && index + 1 < pins.size()) {
            pin.clearLinks(context);
            pins.get(index + 1).clearLinks(context);
        }
        if (i == 1 && index - 1 >= 0) {
            pin.clearLinks(context);
            pins.get(index - 1).clearLinks(context);
        }
        removePin(pin);
    }

    public Pin getMapPin() {
        return mapPin;
    }
}
