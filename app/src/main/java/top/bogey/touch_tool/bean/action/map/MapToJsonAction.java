package top.bogey.touch_tool.bean.action.map;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.GsonUtil;

/**
 * 字典转 JSON 文本，输出可以被「解析 JSON」还原
 */
public class MapToJsonAction extends MapCalculateAction {
    private final transient Pin mapPin = new Pin(new PinMap());
    private final transient Pin resultPin = new Pin(new PinString(), R.string.map_to_json_action_result, true);

    public MapToJsonAction() {
        super(ActionType.MAP_TO_JSON);
        addPins(mapPin, resultPin);
    }

    public MapToJsonAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(mapPin, resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinMap map = getPinValue(runnable, mapPin);
        resultPin.getValue(PinString.class).setValue(GsonUtil.toJsonElement(map).toString());
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(mapPin, resultPin);
    }
}
