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
import top.bogey.touch_tool.service.TaskRunnable;

public class ListGetAction extends ListCalculateAction {
    private final transient Pin listPin = new Pin(new PinList());
    private final transient Pin indexPin = new Pin(new PinInteger(-1), R.string.list_action_index);
    private final transient Pin resultPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object, true);
    private final transient Pin existPin = new Pin(new PinBoolean(), R.string.pin_boolean_result, true);

    public ListGetAction() {
        super(ActionType.LIST_GET);
        addPins(listPin, indexPin, resultPin, existPin);
    }

    public ListGetAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(listPin, indexPin);
        reAddPin(resultPin, true);
        reAddPin(existPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinList list = getPinValue(runnable, listPin);
        PinNumber<?> index = getPinValue(runnable, indexPin);
        int indexValue = index.intValue();
        if (indexValue == 0) indexValue = 1;
        int size = list.size();
        if (indexValue >= 1 && indexValue <= size) {
            existPin.getValue(PinBoolean.class).setValue(true);
            resultPin.setValue(returnValue(list.get(indexValue - 1)));
        } else if (indexValue < 0 && indexValue >= -size) {
            existPin.getValue(PinBoolean.class).setValue(true);
            resultPin.setValue(returnValue(list.get(size + indexValue)));
        }
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(listPin, resultPin);
    }

}
