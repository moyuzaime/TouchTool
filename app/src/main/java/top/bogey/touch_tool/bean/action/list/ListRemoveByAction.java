package top.bogey.touch_tool.bean.action.list;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.service.TaskRunnable;

/**
 * 移除元素：按索引移除（可输出被移除的对象）或按对象移除
 */
public class ListRemoveByAction extends ListExecuteAction {
    private static final int MODE_INDEX = 0;
    private static final int MODE_OBJECT = 1;

    private final transient Pin listPin = new Pin(new PinList());
    private final transient Pin modePin = new NotLinkAblePin(new PinSingleSelect(R.array.list_remove_mode), R.string.list_remove_by_action_mode);
    private final transient Pin indexPin = new Pin(new PinInteger(-1), R.string.list_action_index);
    private final transient Pin objectPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object);
    private final transient Pin removedPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.list_remove_by_action_object, true);
    private final transient Pin resultPin = new Pin(new PinBoolean(), R.string.pin_boolean_result, true);

    public ListRemoveByAction() {
        super(ActionType.LIST_REMOVE_BY);
        addPins(listPin, modePin, indexPin, objectPin, removedPin, resultPin);
    }

    public ListRemoveByAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPin(listPin);
        reAddPin(modePin);
        reAddPin(indexPin);
        reAddPin(objectPin, true);
        reAddPin(removedPin, true);
        reAddPin(resultPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinList list = getPinValue(runnable, listPin);
        boolean removed = false;
        if (modePin.getValue(PinSingleSelect.class).getIndex() == MODE_OBJECT) {
            PinObject object = getPinValue(runnable, objectPin);
            removed = list.remove(object);
        } else {
            PinNumber<?> index = getPinValue(runnable, indexPin);
            int indexValue = index.intValue();
            if (indexValue == 0) indexValue = 1;
            int size = list.size();
            if (indexValue >= 1 && indexValue <= size) {
                removedPin.setValue(list.remove(indexValue - 1));
                removed = true;
            } else if (indexValue < 0 && indexValue >= -size) {
                removedPin.setValue(list.remove(size + indexValue));
                removed = true;
            }
        }
        resultPin.getValue(PinBoolean.class).setValue(removed);
        executeNext(runnable, outPin);
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(listPin, objectPin, removedPin);
    }
}
