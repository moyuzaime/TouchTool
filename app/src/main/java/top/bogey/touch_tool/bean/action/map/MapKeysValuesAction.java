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
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.service.TaskRunnable;

/**
 * 获取所有键或值：结果列表继承字典对应的类型
 */
public class MapKeysValuesAction extends MapCalculateAction {
    private static final int MODE_KEY = 0;

    private final transient Pin mapPin = new Pin(new PinMap());
    private final transient Pin modePin = new NotLinkAblePin(new PinSingleSelect(R.array.map_keys_values_mode), R.string.map_keys_values_action_mode);
    private final transient Pin resultPin = new Pin(new PinList(), R.string.map_keys_values_action_result, true);

    public MapKeysValuesAction() {
        super(ActionType.MAP_KEYS_VALUES);
        addPins(mapPin, modePin, resultPin);
    }

    public MapKeysValuesAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPin(mapPin);
        reAddPin(modePin);
        reAddPin(resultPin, true);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinMap map = getPinValue(runnable, mapPin);
        boolean keys = isKeys();
        // 结果列表继承字典的键类型或值类型
        PinList list = new PinList((PinObject) (keys ? map.getKeyType() : map.getValueType()).copy());
        list.addAll(keys ? map.keySet() : map.values());
        resultPin.setValue(list);
    }

    @Override
    public void resetReturnValue(TaskRunnable runnable, Pin pin) {
        resultPin.setValue(new PinList());
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(mapPin, resultPin);
    }

    @NonNull
    @Override
    public List<Pin> getDynamicKeyTypePins() {
        return isKeys() ? Collections.singletonList(resultPin) : Collections.emptyList();
    }

    @NonNull
    @Override
    public List<Pin> getDynamicValueTypePins() {
        return isKeys() ? Collections.emptyList() : Collections.singletonList(resultPin);
    }

    private boolean isKeys() {
        return modePin.getValue(PinSingleSelect.class).getIndex() == MODE_KEY;
    }
}
