package top.bogey.touch_tool.bean.action.list;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

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
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.service.TaskRunnable;

/**
 * 列表查找首个：找到第一个满足条件的元素就停止，输出元素、索引与是否存在
 */
public class ListFindFirstAction extends ListExecuteAction {
    private final transient Pin listPin = new Pin(new PinList());
    private final transient Pin elementPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object, true);
    private final transient Pin flowPin = new Pin(new PinExecute(), R.string.list_find_first_action_flow, true);
    private final transient Pin conditionPin = new Pin(new PinBoolean(), R.string.list_find_first_action_condition);
    private final transient Pin resultPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.list_find_first_action_result, true);
    private final transient Pin indexPin = new Pin(new PinInteger(), R.string.list_find_first_action_index, true);
    private final transient Pin existPin = new Pin(new PinBoolean(), R.string.list_find_first_action_exist, true);

    public ListFindFirstAction() {
        super(ActionType.LIST_FIND_FIRST);
        addPins(listPin, elementPin, flowPin, conditionPin, resultPin, indexPin, existPin);
    }

    public ListFindFirstAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(listPin, elementPin, flowPin, conditionPin, resultPin, indexPin, existPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinList list = getPinValue(runnable, listPin);
        Action startAction = runnable.getAction();
        boolean found = false;
        PinObject foundValue = null;
        int foundIndex = 0;
        for (int i = 0; i < list.size(); i++) {
            if (runnable.isCurrentInterrupt()) return;
            if (!startAction.equals(runnable.getAction())) return;
            PinObject value = list.get(i);
            elementPin.setValue(returnValue(value));
            executeNext(runnable, flowPin);
            PinBoolean condition = getPinValue(runnable, conditionPin);
            if (condition != null && condition.getValue()) {
                found = true;
                foundValue = value;
                foundIndex = i + 1;
                break;
            }
        }
        existPin.getValue(PinBoolean.class).setValue(found);
        indexPin.getValue(PinInteger.class).setValue(foundIndex);
        resultPin.setValue(foundValue == null ? new PinObject(PinSubType.DYNAMIC) : returnValue(foundValue));
        executeNext(runnable, outPin);
    }

    @Override
    public void resetReturnValue(TaskRunnable runnable, Pin pin) {
        if (!pin.isOut() && pin.isSameClass(PinExecute.class)) {
            resultPin.setValue(new PinObject(PinSubType.DYNAMIC));
            indexPin.setValue(new PinInteger());
            existPin.setValue(new PinBoolean());
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
