package top.bogey.touch_tool.bean.action.map;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.service.TaskRunnable;

/**
 * 字典键值互换：值需要能作为键（文本、数字等）
 */
public class MapInvertAction extends MapCalculateAction {
    private final transient Pin mapPin = new Pin(new PinMap());
    private final transient Pin resultPin = new Pin(new PinMap(), R.string.map_invert_action_result, true);

    public MapInvertAction() {
        super(ActionType.MAP_INVERT);
        addPins(mapPin, resultPin);
    }

    public MapInvertAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(mapPin, resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinMap map = getPinValue(runnable, mapPin);
        // 互换后键类型来自原来的值类型
        PinMap result = new PinMap((PinObject) map.getValueType().copy(), (PinObject) map.getKeyType().copy());
        for (Map.Entry<PinObject, PinObject> entry : map.entrySet()) {
            if (entry.getValue() == null) continue;
            result.put(entry.getValue(), entry.getKey());
        }
        resultPin.setValue(result);
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(mapPin, resultPin);
    }
}
