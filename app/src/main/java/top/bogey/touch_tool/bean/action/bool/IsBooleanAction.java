package top.bogey.touch_tool.bean.action.bool;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;
/**
 * 类型判断：IS_BOOLEAN
 */
public class IsBooleanAction extends CalculateAction {
    private final transient Pin objectPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object);
    private final transient Pin resultPin = new Pin(new PinBoolean(), R.string.pin_boolean_result, true);

    public IsBooleanAction() {
        super(ActionType.IS_BOOLEAN);
        addPins(objectPin, resultPin);
    }

    public IsBooleanAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(objectPin, resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinObject object = getPinValue(runnable, objectPin);
        resultPin.getValue(PinBoolean.class).setValue(object instanceof PinBoolean);
    }
}