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
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.service.TaskRunnable;

/**
 * 列表筛选：每个元素跑一次条件流程，条件为真的元素保留
 */
public class ListFilterAction extends ListExecuteAction {
    private final transient Pin listPin = new Pin(new PinList());
    private final transient Pin elementPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object, true);
    private final transient Pin flowPin = new Pin(new PinExecute(), R.string.list_filter_action_flow, true);
    private final transient Pin conditionPin = new Pin(new PinBoolean(), R.string.list_filter_action_condition);
    private final transient Pin resultPin = new Pin(new PinList(), R.string.list_filter_action_result, true);

    public ListFilterAction() {
        super(ActionType.LIST_FILTER);
        addPins(listPin, elementPin, flowPin, conditionPin, resultPin);
    }

    public ListFilterAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(listPin, elementPin, flowPin, conditionPin, resultPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinList list = getPinValue(runnable, listPin);
        PinList result = new PinList((PinObject) list.getValueType().copy());
        Action startAction = runnable.getAction();
        // 按进入时的快照遍历，条件流程里改列表不会中断遍历
        for (PinObject value : new ArrayList<>(list)) {
            if (runnable.isCurrentInterrupt()) return;
            if (!startAction.equals(runnable.getAction())) return;
            elementPin.setValue(returnValue(value));
            executeNext(runnable, flowPin);
            PinBoolean condition = getPinValue(runnable, conditionPin);
            if (condition != null && condition.getValue()) result.add(value);
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

    // 每跑一次条件流程都写执行日志会淹没日志树，只记外层
    @Override
    public void beforeExecuteNext(TaskRunnable runnable, Pin pin) {
        if (pin == outPin) {
            super.beforeExecuteNext(runnable, pin);
        }
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(listPin, elementPin, resultPin);
    }
}
