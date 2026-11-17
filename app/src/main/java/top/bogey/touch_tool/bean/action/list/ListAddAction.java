package top.bogey.touch_tool.bean.action.list;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.PinTypeUtil;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskRunnable;

public class ListAddAction extends ListExecuteAction {
    private final transient Pin listPin = new Pin(new PinList());
    private final transient Pin elementPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object);
    private final transient Pin resultPin = new Pin(new PinBoolean(), R.string.pin_boolean_result, true);
    private final transient Pin indexPin = Pin.withIndexTip(new Pin(new PinInteger(-1), R.string.list_add_action_index));

    public ListAddAction() {
        super(ActionType.LIST_ADD);
        addPins(listPin, elementPin, resultPin, indexPin);
    }

    public ListAddAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPin(listPin);
        reAddPin(elementPin, true);
        reAddPins(resultPin, indexPin);
        // 老任务里位置针脚存的是默认隐藏，这里统一取消隐藏，让按位置插入可见
        indexPin.setHide(false);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinList list = getPinValue(runnable, listPin);
        PinObject element = getPinValue(runnable, elementPin);
        // 元素类型与容器声明不一致时只记日志，不改变插入结果
        PinTypeUtil.warnRuntime(list.getValueType(), element);
        PinNumber<?> index = getPinValue(runnable, indexPin);
        int indexValue = index.intValue();
        if (indexValue == 0) indexValue = 1;
        int size = list.size();
        boolean added = false;
        if (indexValue >= 1 && indexValue <= size + 1) {
            list.add(indexValue - 1, element);
            added = true;
        } else if (indexValue <= -1 && indexValue >= -(size + 1)) {
            list.add(size + indexValue + 1, element);
            added = true;
        }
        resultPin.getValue(PinBoolean.class).setValue(added);
        executeNext(runnable, outPin);
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(listPin, elementPin);
    }

    @Override
    public void check(ActionCheckResult result, Task task) {
        super.check(result, task);
        PinTypeUtil.checkListPin(result, listPin, elementPin, task);
    }

}
