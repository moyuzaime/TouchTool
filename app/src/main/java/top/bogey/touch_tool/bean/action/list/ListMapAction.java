package top.bogey.touch_tool.bean.action.list;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.service.TaskRunnable;

/**
 * 列表映射：每个元素跑一次映射流程，用流程给出的值组成新列表
 */
public class ListMapAction extends ListExecuteAction {
    private final transient Pin listPin = new Pin(new PinList());
    private final transient Pin elementPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object, true);
    private final transient Pin flowPin = new Pin(new PinExecute(), R.string.list_map_action_flow, true);
    private final transient Pin mappedPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.list_map_action_value);
    private final transient Pin resultPin = new Pin(new PinList(), R.string.list_map_action_result, true);

    public ListMapAction() {
        super(ActionType.LIST_MAP);
        addPins(listPin, elementPin, flowPin, mappedPin, resultPin);
    }

    public ListMapAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(listPin, elementPin, flowPin, mappedPin, resultPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinList list = getPinValue(runnable, listPin);
        PinList result = new PinList();
        Action startAction = runnable.getAction();
        // 按进入时的快照遍历，映射流程里改列表不会中断遍历
        for (PinObject value : new ArrayList<>(list)) {
            if (runnable.isCurrentInterrupt()) return;
            if (!startAction.equals(runnable.getAction())) return;
            elementPin.setValue(returnValue(value));
            executeNext(runnable, flowPin);
            PinObject mapped = getPinValue(runnable, mappedPin);
            if (mapped == null) continue;
            // 结果类型跟随第一个映射出来的值
            if (result.isDynamic()) {
                PinObject type = (PinObject) mapped.copy();
                type.reset();
                result.setValueType(type);
            }
            result.add(mapped);
        }
        resultPin.setValue(result);
        executeNext(runnable, outPin);
    }

    @Override
    public void resetReturnValue(TaskRunnable runnable, Pin pin) {
        if (!pin.isOut() && pin.isSameClass(PinExecute.class)) {
            resultPin.setValue(new PinList());
        }
    }

    // 每跑一次映射流程都写执行日志会淹没日志树，只记外层
    @Override
    public void beforeExecuteNext(TaskRunnable runnable, Pin pin) {
        if (pin == outPin) {
            super.beforeExecuteNext(runnable, pin);
        }
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(listPin, elementPin, mappedPin, resultPin);
    }
}
