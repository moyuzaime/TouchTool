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

public class MakeListAction extends ListCalculateAction implements DynamicPinsAction {
    private final transient Pin morePin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object);
    private final transient Pin addPin = new Pin(new PinAdd(morePin), R.string.pin_add_pin);
    private final transient Pin listPin = new Pin(new PinList(), true);

    public MakeListAction() {
        super(ActionType.LIST_MAKE);
        Pin firstPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object, false, true);
        addPins(firstPin, addPin, listPin);
    }

    public MakeListAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(morePin, false);
        reAddPins(addPin, listPin);

        // 添加针脚的默认对象类型要符合列表
        PinObject valueType = listPin.getValue(PinList.class).getValueType();
        ArrayList<Pin> list = new ArrayList<>();
        list.add(new Pin(valueType.copy(), R.string.pin_object));
        addPin.getValue(PinAdd.class).setPins(list);
    }

    @Override
    public void handleUnLinkFrom(Task task, Pin origin) {
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        for (Pin dynamicPin : getDynamicPins()) {
            PinObject value = getPinValue(runnable, dynamicPin);
            listPin.getValue(PinList.class).add(value);
        }
    }

    @Override
    public void resetReturnValue(TaskRunnable runnable, Pin pin) {
        listPin.setValue(new PinList());
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        List<Pin> dynamicPins = getDynamicPins();
        dynamicPins.add(addPin);
        dynamicPins.add(listPin);
        return dynamicPins;
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
