package top.bogey.touch_tool.bean.action.string;

import com.google.gson.JsonObject;

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
 * 取类型名：输出稳定的英文枚举名，便于日志与分支
 */
public class TypeNameAction extends CalculateAction {
    private final transient Pin objectPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object);
    private final transient Pin resultPin = new Pin(new PinString(), R.string.type_name_action_result, true);

    public TypeNameAction() {
        super(ActionType.TYPE_NAME);
        addPins(objectPin, resultPin);
    }

    public TypeNameAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(objectPin, resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinObject object = getPinValue(runnable, objectPin);
        resultPin.getValue(PinString.class).setValue(getTypeName(object));
    }

    private String getTypeName(PinBase value) {
        if (value == null) return "OBJECT";
        if (value instanceof PinList) return "LIST";
        if (value instanceof PinMap) return "MAP";
        if (value instanceof PinBoolean) return "BOOLEAN";
        if (value instanceof PinNumber<?>) return "NUMBER";
        if (value instanceof PinString) return "TEXT";
        return "OBJECT";
    }
}