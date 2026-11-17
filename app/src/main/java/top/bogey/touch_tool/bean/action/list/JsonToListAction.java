package top.bogey.touch_tool.bean.action.list;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.util.Arrays;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinFileContentString;
import top.bogey.touch_tool.service.TaskRunnable;

/**
 * JSON 文本转列表：与「列表转JSON」配对，结果针脚是列表，避免借用「JSON解析」的字典针脚
 */
public class JsonToListAction extends ListCalculateAction {
    private final transient Pin jsonPin = new Pin(new PinFileContentString(), R.string.pin_string);
    private final transient Pin resultPin = new Pin(new PinList(), R.string.json_to_list_action_result, true);

    public JsonToListAction() {
        super(ActionType.JSON_TO_LIST);
        addPins(jsonPin, resultPin);
    }

    public JsonToListAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(jsonPin, resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinObject json = getPinValue(runnable, jsonPin);
        PinList list = new PinList();
        try {
            String text = json.toString();
            if (text.startsWith("[")) {
                List<Object> values = new Gson().fromJson(text, new TypeToken<List<Object>>() {
                }.getType());
                if (values != null) {
                    PinBase parsed = PinBase.parseValue(values);
                    if (parsed instanceof PinList pinList) list = pinList;
                }
            }
        } catch (Exception ignored) {
        }
        resultPin.setValue(list);
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(jsonPin, resultPin);
    }
}
