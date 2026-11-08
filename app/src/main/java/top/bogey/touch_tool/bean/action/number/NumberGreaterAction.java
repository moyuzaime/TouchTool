package top.bogey.touch_tool.bean.action.number;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.service.TaskRunnable;

public class NumberGreaterAction extends NumberResultAction {

    public NumberGreaterAction() {
        super(ActionType.NUMBER_GREATER);
    }

    public NumberGreaterAction(JsonObject jsonObject) {
        super(jsonObject);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinNumber<?> first = getPinValue(runnable, firstPin);
        PinNumber<?> second = getPinValue(runnable, secondPin);
        int compare = Double.compare(first.doubleValue(), second.doubleValue());
        boolean result = switch (modePin.getValue(PinSingleSelect.class).getIndex()) {
            case 0 -> compare > 0;
            case 1 -> compare < 0;
            case 2 -> compare == 0;
            case 3 -> compare >= 0;
            case 4 -> compare <= 0;
            default -> compare != 0;
        };
        resultPin.getValue(PinBoolean.class).setValue(result);
    }
}
