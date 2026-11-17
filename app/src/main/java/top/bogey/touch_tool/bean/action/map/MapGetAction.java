package top.bogey.touch_tool.bean.action.map;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.service.TaskRunnable;

public class MapGetAction extends MapCalculateAction {
    private final transient Pin mapPin = new Pin(new PinMap());
    private final transient Pin keyPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.map_action_key);
    private final transient Pin resultPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.map_action_value, true);
    private final transient Pin existPin = new Pin(new PinBoolean(), R.string.pin_boolean_result, true);
    // 默认值针脚追加在末尾，保证老任务前四个针脚按原顺序匹配
    private final transient Pin defaultPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.map_get_action_default);

    public MapGetAction() {
        super(ActionType.MAP_GET);
        addPins(mapPin, keyPin, resultPin, existPin, defaultPin);
    }

    public MapGetAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPin(mapPin);
        reAddPin(keyPin, true);
        reAddPin(resultPin, true);
        reAddPin(existPin);
        reAddPin(defaultPin, true);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinMap map = getPinValue(runnable, mapPin);
        PinObject key = getPinValue(runnable, keyPin);
        PinObject object = map.get(key);
        if (object != null) {
            existPin.getValue(PinBoolean.class).setValue(true);
            resultPin.setValue(returnValue(object));
        } else {
            existPin.getValue(PinBoolean.class).setValue(false);
            // 未命中时输出默认值，默认值为空对象，与旧版未命中表现一致
            PinObject defaultValue = getPinValue(runnable, defaultPin);
            resultPin.setValue(defaultValue == null ? new PinObject(PinSubType.DYNAMIC) : defaultValue);
        }
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(mapPin, keyPin, resultPin, defaultPin);
    }

    @NonNull
    @Override
    public List<Pin> getDynamicKeyTypePins() {
        return Collections.singletonList(keyPin);
    }

    @NonNull
    @Override
    public List<Pin> getDynamicValueTypePins() {
        return Arrays.asList(resultPin, defaultPin);
    }
}
