package top.bogey.touch_tool.bean.action.map;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.service.TaskRunnable;

public class MapGetKeysAction extends MapCalculateAction {
    private final transient Pin mapPin = new Pin(new PinMap());
    private final transient Pin keysPin = new Pin(new PinList(), R.string.map_action_key, true);

    public MapGetKeysAction() {
        super(ActionType.MAP_KEYS);
        addPins(mapPin, keysPin);
    }

    public MapGetKeysAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(mapPin, keysPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinMap map = getPinValue(runnable, mapPin);
        PinList list = keysPin.getValue();
        list.addAll(map.keySet());
    }

    @Override
    public void resetReturnValue(TaskRunnable runnable, Pin pin) {
        keysPin.setValue(new PinList());
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(mapPin, keysPin);
    }


    @NonNull
    @Override
    public List<Pin> getDynamicKeyTypePins() {
        return Collections.singletonList(keysPin);
    }
}
