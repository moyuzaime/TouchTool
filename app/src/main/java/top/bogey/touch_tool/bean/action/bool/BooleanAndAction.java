package top.bogey.touch_tool.bean.action.bool;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.action.parent.DynamicPinsAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinAdd;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.service.TaskRunnable;

public class BooleanAndAction extends CalculateAction implements DynamicPinsAction {
    private final static Pin morePin = new Pin(new PinBoolean(), R.string.pin_boolean_condition);

    private final transient Pin addPin = new Pin(new PinAdd(morePin), R.string.pin_add_pin);
    // 合并卡片：组合方式（与/或）与是否短路
    private final transient Pin modePin = new NotLinkAblePin(new PinSingleSelect(R.array.boolean_combine_type), R.string.boolean_combine_mode);
    private final transient Pin shortPin = new Pin(new PinBoolean(false), R.string.boolean_combine_short);
    private final transient Pin resultPin = new Pin(new PinBoolean(), R.string.pin_boolean_result, true);

    public BooleanAndAction() {
        super(ActionType.BOOLEAN_AND);
        // 默认条件针脚，和动态添加的一样可以移除
        Pin firstPin = new Pin(new PinBoolean(), R.string.pin_boolean_condition, false, true);
        Pin secondPin = new Pin(new PinBoolean(), R.string.pin_boolean_condition, false, true);
        addPins(firstPin, secondPin, addPin, modePin, shortPin, resultPin);
    }

    public BooleanAndAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(morePin);
        reAddPins(addPin, modePin, shortPin, resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        boolean isOr = modePin.getValue(PinSingleSelect.class).getIndex() == 1;
        boolean shortCircuit = shortPin.getValue(PinBoolean.class).getValue();
        // 与：遇到 false 即为 false；或：遇到 true 即为 true
        boolean result = !isOr;
        for (Pin dynamicPin : getDynamicPins()) {
            boolean value = ((PinBoolean) getPinValue(runnable, dynamicPin)).getValue();
            if (isOr) {
                result |= value;
                if (result && shortCircuit) break;
            } else {
                result &= value;
                if (!result && shortCircuit) break;
            }
        }
        resultPin.getValue(PinBoolean.class).setValue(result);
    }

    @Override
    public List<Pin> getDynamicPins() {
        // 添加针脚之前的输入针脚都参与计算，默认针脚被移除后自然不再计入
        List<Pin> pins = new ArrayList<>();
        boolean start = true;
        for (Pin pin : getPins()) {
            if (pin == addPin) start = false;
            if (start) pins.add(pin);
        }
        return pins;
    }
}
