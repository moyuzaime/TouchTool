package top.bogey.touch_tool.bean.action.list;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.DynamicPinsAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinAdd;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.PinTypeUtil;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskRunnable;

public class MakeListAction extends ListCalculateAction implements DynamicPinsAction {
    private final transient Pin morePin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object);
    private final transient Pin addPin = new Pin(new PinAdd(morePin), R.string.pin_add_pin);
    private final transient Pin listPin = new Pin(new PinList(), true);
    // 类型模式：自动（首连定型）/ 任意类型，追加在针脚列表末尾
    private final transient Pin modePin = new NotLinkAblePin(new PinSingleSelect(R.array.container_type_mode), R.string.container_type_mode_pin);

    public MakeListAction() {
        super(ActionType.LIST_MAKE);
        Pin firstPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object, false, true);
        addPins(firstPin, addPin, listPin, modePin);
        applyTypeMode();
    }

    public MakeListAction(JsonObject jsonObject) {
        super(jsonObject);
        // 老任务没有类型模式针脚，保持 typeRef 为 null 走兼容回退
        boolean hasModePin = tmpPins.stream().anyMatch(pin -> pin.isSameClass(modePin));
        reAddPins(morePin, false);
        reAddPins(addPin, listPin);
        reAddPin(modePin);
        if (hasModePin) applyTypeMode();

        // 添加针脚的默认对象类型要符合列表
        PinObject valueType = listPin.getValue(PinList.class).getValueType();
        ArrayList<Pin> list = new ArrayList<>();
        list.add(new Pin(valueType.copy(), R.string.pin_object));
        addPin.getValue(PinAdd.class).setPins(list);
    }

    // 按类型模式写容器与元素针脚的声明：任意类型 → ANY，自动 → UNKNOWN 等首连定型
    private void applyTypeMode() {
        boolean any = modePin.getValue(PinSingleSelect.class).getIndex() == 1;
        for (Pin pin : getDynamicPins()) {
            if (any) pin.markTypeAny();
            else pin.markTypeUnknown();
        }
        if (any) listPin.markTypeAny();
        else listPin.markTypeUnknown();
    }

    @Override
    public void onValueUpdated(Task task, Pin origin, PinBase value) {
        super.onValueUpdated(task, origin, value);
        if (origin == modePin) applyTypeMode();
    }

    @Override
    public void handleUnLinkFrom(Task task, Pin origin) {
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinList list = listPin.getValue(PinList.class);
        for (Pin dynamicPin : getDynamicPins()) {
            PinObject value = getPinValue(runnable, dynamicPin);
            // 元素类型与容器声明不一致时只记日志，不丢弃元素
            PinTypeUtil.warnRuntime(list.getValueType(), value);
            list.add(value);
        }
    }

    @Override
    public void resetReturnValue(TaskRunnable runnable, Pin pin) {
        // 重建列表时保留元素类型，否则每次运行都会退回动态类型
        listPin.setValue(new PinList((PinObject) listPin.getValue(PinList.class).getValueType().copy()));
    }

    @Override
    public void check(ActionCheckResult result, Task task) {
        super.check(result, task);
        PinTypeUtil.checkListPins(result, listPin, getDynamicPins(), task);
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
