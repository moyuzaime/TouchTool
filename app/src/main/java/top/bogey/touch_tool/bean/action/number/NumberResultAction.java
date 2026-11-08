package top.bogey.touch_tool.bean.action.number;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinDouble;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;

public abstract class NumberResultAction extends CalculateAction {
    protected final transient Pin firstPin = new Pin(new PinDouble(), R.string.pin_number_double);
    protected final transient Pin secondPin = new Pin(new PinDouble(), R.string.pin_number_double);
    // 合并卡片：比较方式（>、<、= 等）
    protected final transient Pin modePin = new NotLinkAblePin(new PinSingleSelect(R.array.number_compare_type), R.string.number_compare_mode);
    protected final transient Pin resultPin = new Pin(new PinBoolean(), R.string.pin_boolean_result, true);

    public NumberResultAction(ActionType type) {
        super(type);
        addPins(firstPin, secondPin, modePin, resultPin);
    }

    public NumberResultAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(firstPin, secondPin, modePin, resultPin);
    }
}
