package top.bogey.touch_tool.bean.action.string;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Map;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

/**
 * 列表 / 字典转 JSON 文本，输出可以被「解析 JSON」还原
 */
public class ToJsonAction extends CalculateAction {
    private final transient Pin objectPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object);
    private final transient Pin resultPin = new Pin(new PinString(), R.string.to_json_action_result, true);

    public ToJsonAction() {
        super(ActionType.TO_JSON);
        addPins(objectPin, resultPin);
    }

    public ToJsonAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(objectPin, resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinObject object = getPinValue(runnable, objectPin);
        resultPin.getValue(PinString.class).setValue(toJsonElement(object).toString());
    }

    // 列表转数组、字典转对象，其余类型按 JSON 基本类型输出
    private JsonElement toJsonElement(PinBase value) {
        if (value == null) return JsonNull.INSTANCE;
        if (value instanceof PinList list) {
            JsonArray array = new JsonArray();
            for (PinObject element : list) {
                array.add(toJsonElement(element));
            }
            return array;
        }
        if (value instanceof PinMap map) {
            JsonObject object = new JsonObject();
            for (Map.Entry<PinObject, PinObject> entry : map.entrySet()) {
                object.add(entry.getKey().toString(), toJsonElement(entry.getValue()));
            }
            return object;
        }
        if (value instanceof PinNumber<?> number && number.getValue() != null) {
            return new JsonPrimitive(number.getValue());
        }
        if (value instanceof PinBoolean bool) {
            return new JsonPrimitive(bool.getValue());
        }
        return new JsonPrimitive(value.toString());
    }
}
