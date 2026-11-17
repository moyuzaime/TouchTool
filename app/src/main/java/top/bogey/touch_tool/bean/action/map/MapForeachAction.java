package top.bogey.touch_tool.bean.action.map;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.service.TaskRunnable;

public class MapForeachAction extends MapExecuteAction {
    private final transient Pin breakPin = new Pin(new PinExecute(), R.string.map_foreach_action_break);
    private final transient Pin mapPin = new Pin(new PinMap());
    private final transient Pin keyPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.map_action_key, true);
    private final transient Pin valuePin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.map_action_value, true);
    private final transient Pin completePin = new Pin(new PinExecute(), R.string.for_loop_action_complete, true);

    private transient boolean isBreak = false;

    public MapForeachAction() {
        super(ActionType.MAP_FOREACH);
        addPins(breakPin, mapPin, keyPin, valuePin, completePin);
    }

    public MapForeachAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(breakPin, mapPin);
        reAddPin(keyPin, true);
        reAddPin(valuePin, true);
        reAddPin(completePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        if (pin == inPin) {
            isBreak = false;
            Action startAction = runnable.getAction();
            PinMap map = getPinValue(runnable, mapPin);
            for (Map.Entry<PinObject, PinObject> entry : map.entrySet()) {
                if (runnable.isCurrentInterrupt()) return;
                if (!startAction.equals(runnable.getAction())) return;
                if (isBreak) break;
                keyPin.setValue(returnValue(entry.getKey()));
                valuePin.setValue(returnValue(entry.getValue()));
                executeNext(runnable, outPin);
            }
            executeNext(runnable, completePin);
        } else {
            isBreak = true;
            super.beforeExecuteNext(runnable, null);
        }
    }

    @Override
    public void beforeExecuteNext(TaskRunnable runnable, Pin pin) {
        if (pin == completePin) {
            super.beforeExecuteNext(runnable, pin);
        }
    }

    @Override
    public void resetReturnValue(TaskRunnable runnable, Pin pin) {
        if (pin == inPin) {
            super.resetReturnValue(runnable, pin);
        }
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(mapPin, keyPin, valuePin);
    }

    @NonNull
    @Override
    public List<Pin> getDynamicKeyTypePins() {
        return Collections.singletonList(keyPin);
    }

    @NonNull
    @Override
    public List<Pin> getDynamicValueTypePins() {
        return Collections.singletonList(valuePin);
    }
}
