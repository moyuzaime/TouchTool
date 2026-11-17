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
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinDouble;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.service.TaskRunnable;

public class ListSortAction extends ListExecuteAction {
    private final transient Pin listPin = new Pin(new PinList());
    private final transient Pin resultPin = new Pin(new PinDouble(), R.string.list_sort_action_result);
    private final transient Pin elementPin1 = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object, true);
    private final transient Pin elementPin2 = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object, true);
    private final transient Pin comparePin = new Pin(new PinExecute(), R.string.list_sort_action_compare, true);

    public ListSortAction() {
        super(ActionType.LIST_SORT);
        addPins(listPin, resultPin, elementPin1, elementPin2, comparePin);
    }

    public ListSortAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(listPin, resultPin);
        reAddPin(elementPin1, true);
        reAddPin(elementPin2, true);
        reAddPin(comparePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinList list = getPinValue(runnable, listPin);
        list.sort((element1, element2) -> {
            elementPin1.setValue(element1.copy());
            elementPin2.setValue(element2.copy());
            executeNext(runnable, comparePin);
            PinNumber<?> value = getPinValue(runnable, resultPin);
            return Double.compare(value.doubleValue(), 0);
        });
        executeNext(runnable, outPin);
    }

    @Override
    public void beforeExecuteNext(TaskRunnable runnable, Pin pin) {
        if (pin == outPin) {
            super.beforeExecuteNext(runnable, pin);
        }
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(listPin, elementPin1, elementPin2);
    }
}