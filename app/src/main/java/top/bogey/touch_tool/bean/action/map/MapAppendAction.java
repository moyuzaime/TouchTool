package top.bogey.touch_tool.bean.action.map;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.DynamicPinsAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinAdd;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.service.TaskRunnable;

public class MapAppendAction extends MapExecuteAction implements DynamicPinsAction {
    private final static Pin morePin = new Pin(new PinMap());

    private final transient Pin addPin = new Pin(new PinAdd(morePin), R.string.pin_add_pin);
    private final transient Pin resultPin = new Pin(new PinMap(), R.string.pin_boolean_result, true);

    public MapAppendAction() {
        super(ActionType.MAP_APPEND);
        Pin firstPin = new Pin(new PinMap(), 0, false, true);
        Pin secondPin = new Pin(new PinMap(), 0, false, true);
        addPins(firstPin, secondPin, addPin, resultPin);
    }

    public MapAppendAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(morePin);
        reAddPins(addPin, resultPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinMap result = resultPin.getValue(PinMap.class);
        for (Pin p : getDynamicPins()) {
            PinMap map = getPinValue(runnable, p);
            if (map != null) {
                result.putAll(map);
            }
        }
        executeNext(runnable, outPin);
    }

    @Override
    public void resetReturnValue(TaskRunnable runnable, Pin pin) {
        if (!pin.isOut() && pin.isSameClass(PinExecute.class)) {
            resultPin.setValue(new PinMap());
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
