package top.bogey.touch_tool.bean.action.list;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.service.TaskRunnable;

public class ListSetAction extends ListExecuteAction {
    private final transient Pin listPin = new Pin(new PinList());
    private final transient Pin indexPin = new Pin(new PinInteger(-1), R.string.list_action_index);
    private final transient Pin elementPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object);
    private final transient Pin resultPin = new Pin(new PinObject(), R.string.list_set_action_value, true);

    public ListSetAction() {
        super(ActionType.LIST_SET);
        addPins(listPin, indexPin, elementPin, resultPin);
    }

    public ListSetAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(listPin, indexPin);
        reAddPin(elementPin, true);
        reAddPin(resultPin, true);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinList list = getPinValue(runnable, listPin);
        PinNumber<?> index = getPinValue(runnable, indexPin);
        PinObject element = getPinValue(runnable, elementPin);
        int indexValue = index.intValue();
        if (indexValue == 0) indexValue = 1;
        int size = list.size();
        if (indexValue >= 1 && indexValue <= size) {
            resultPin.setValue(list.set(indexValue - 1, element));
        } else if (indexValue < 0 && indexValue >= -size) {
            resultPin.setValue(list.set(size + indexValue, element));
        }
        executeNext(runnable, outPin);
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(listPin, elementPin, resultPin);
    }

}
