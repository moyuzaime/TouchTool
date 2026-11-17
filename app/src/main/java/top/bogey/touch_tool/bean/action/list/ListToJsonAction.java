package top.bogey.touch_tool.bean.action.list;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.GsonUtil;

/**
 * 列表转 JSON 文本，输出可以被「解析 JSON」还原
 */
public class ListToJsonAction extends ListCalculateAction {
    private final transient Pin listPin = new Pin(new PinList());
    private final transient Pin resultPin = new Pin(new PinString(), R.string.list_to_json_action_result, true);

    public ListToJsonAction() {
        super(ActionType.LIST_TO_JSON);
        addPins(listPin, resultPin);
    }

    public ListToJsonAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(listPin, resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinList list = getPinValue(runnable, listPin);
        resultPin.getValue(PinString.class).setValue(GsonUtil.toJsonElement(list).toString());
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(listPin, resultPin);
    }
}
