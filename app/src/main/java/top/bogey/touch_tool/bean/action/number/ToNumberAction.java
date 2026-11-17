package top.bogey.touch_tool.bean.action.number;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.PinValueUtil;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinDouble;
import top.bogey.touch_tool.service.TaskRunnable;

/**
 * 转数字：文本 / 布尔 / 数字都能转，失败输出 0 并置「是否成功」为 false
 */
public class ToNumberAction extends CalculateAction {
    private final transient Pin objectPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object);
    private final transient Pin resultPin = new Pin(new PinDouble(), R.string.to_number_action_result, true);
    private final transient Pin successPin = new Pin(new PinBoolean(), R.string.to_number_action_success, true);

    public ToNumberAction() {
        super(ActionType.TO_NUMBER);
        addPins(objectPin, resultPin, successPin);
    }

    public ToNumberAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(objectPin, resultPin, successPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinObject object = getPinValue(runnable, objectPin);
        PinObject converted = PinValueUtil.convertTo(object, new PinDouble());
        boolean success = converted instanceof PinDouble;
        double value = 0;
        if (success) {
            Double number = ((PinDouble) converted).getValue();
            if (number != null) value = number;
        }
        resultPin.getValue(PinDouble.class).setValue(value);
        successPin.getValue(PinBoolean.class).setValue(success);
    }
}