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
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.service.TaskRunnable;

public class ListAppendAction extends ListExecuteAction implements DynamicPinsAction {
    private final static Pin morePin = new Pin(new PinList());

    private final transient Pin addPin = new Pin(new PinAdd(morePin), R.string.pin_add_pin);
    private final transient Pin resultPin = new Pin(new PinList(), R.string.pin_boolean_result, true);

    public ListAppendAction() {
        super(ActionType.LIST_APPEND);
        Pin firstPin = new Pin(new PinList(), 0, false, true);
        Pin secondPin = new Pin(new PinList(), 0, false, true);
        addPins(firstPin, secondPin, addPin, resultPin);
    }

    public ListAppendAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(morePin);
        reAddPins(addPin, resultPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinList result = resultPin.getValue(PinList.class);
        for (Pin p : getDynamicPins()) {
            PinList list = getPinValue(runnable, p);
            if (list != null) {
                result.addAll(list);
            }
        }
        executeNext(runnable, outPin);
    }

    @Override
    public void resetReturnValue(TaskRunnable runnable, Pin pin) {
        if (!pin.isOut() && pin.isSameClass(PinExecute.class)) {
            resultPin.setValue(new PinList());
        }
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        List<Pin> pins = new ArrayList<>(getDynamicPins());
        pins.add(resultPin);
        return pins;
    }

    @Override
    public List<Pin> getDynamicPins() {
        List<Pin> pins = new ArrayList<>();
        boolean start = false;
        for (Pin pin : getPins()) {
            if (pin == addPin) start = false;
            if (start) pins.add(pin);
            if (pin == outPin) start = true;
        }
        return pins;
    }
}
