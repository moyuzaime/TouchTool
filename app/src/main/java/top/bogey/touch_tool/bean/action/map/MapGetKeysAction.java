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
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
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
        // 结果列表继承字典的键类型，避免派生结果一直是动态类型
        PinList list = new PinList((PinObject) map.getKeyType().copy());
        list.addAll(map.keySet());
        keysPin.setValue(list);
        // 派生针脚的声明跟随字典
        keysPin.inheritType(Collections.singletonList(mapPin));
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
