package top.bogey.touch_tool.bean.action.map;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.service.TaskRunnable;

/**
 * 字典是否包含：判断键或判断值
 */
public class MapContainByAction extends MapCalculateAction {
    private static final int MODE_KEY = 0;

    private final transient Pin mapPin = new Pin(new PinMap());
    private final transient Pin modePin = new NotLinkAblePin(new PinSingleSelect(R.array.map_contain_mode), R.string.map_contain_by_action_mode);
    private final transient Pin elementPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object);
    private final transient Pin resultPin = new Pin(new PinBoolean(), R.string.pin_boolean_result, true);

    public MapContainByAction() {
        super(ActionType.MAP_CONTAIN_BY);
        addPins(mapPin, modePin, elementPin, resultPin);
    }

    public MapContainByAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPin(mapPin);
        reAddPin(modePin);
        reAddPin(elementPin, true);
        reAddPin(resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinMap map = getPinValue(runnable, mapPin);
        PinObject element = getPinValue(runnable, elementPin);
        boolean contains = modePin.getValue(PinSingleSelect.class).getIndex() == MODE_KEY
                ? map.containsKey(element) : map.containsValue(element);
        resultPin.getValue(PinBoolean.class).setValue(contains);
    }

    // 元素针脚按方式和键/值角色对应，保持动态，避免被单侧类型拦住连线
    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(mapPin, elementPin);
    }
}
