package top.bogey.touch_tool.bean.action.thread;

import com.google.gson.JsonObject;

import java.util.concurrent.atomic.AtomicLong;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinDouble;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.thread.MutexUtil;

// 原子累加：跨并行分支安全计数，输出累加后的值
public class AtomicAddAction extends ExecuteAction {
    private final transient Pin namePin = new Pin(new PinString("counter"), R.string.atomic_add_name);
    private final transient Pin deltaPin = new Pin(new PinDouble(1), R.string.atomic_add_delta);
    private final transient Pin resultPin = new Pin(new PinDouble(), R.string.atomic_add_result, true);

    public AtomicAddAction() {
        super(ActionType.ATOMIC_ADD);
        addPins(namePin, deltaPin, resultPin);
    }

    public AtomicAddAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(namePin, deltaPin, resultPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinString nameValue = getPinValue(runnable, namePin);
        PinDouble deltaValue = getPinValue(runnable, deltaPin);
        String name = nameValue == null ? "" : nameValue.getValue();
        if (name == null || name.isEmpty()) name = "counter";

        AtomicLong counter = MutexUtil.getCounter(name);
        long delta = deltaValue == null ? 1 : Math.round(deltaValue.doubleValue());
        long value = counter.addAndGet(delta);
        resultPin.getValue(PinDouble.class).setValue((double) value);

        executeNext(runnable, outPin);
    }

    public Pin getNamePin() {
        return namePin;
    }

    public Pin getDeltaPin() {
        return deltaPin;
    }
}
