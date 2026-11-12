package top.bogey.touch_tool.bean.action.thread;

import com.google.gson.JsonObject;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.thread.MutexUtil;

// 加锁：多个并行分支按名字互斥，拿不到锁走失败分支
public class MutexLockAction extends ExecuteAction {
    private final transient Pin namePin = new Pin(new PinString("lock"), R.string.mutex_lock_name);
    private final transient Pin timeoutPin = new Pin(new PinInteger(3000), R.string.mutex_lock_timeout);
    private final transient Pin resultPin = new Pin(new PinBoolean(), R.string.mutex_lock_result, true);
    private final transient Pin failPin = new Pin(new PinExecute(), R.string.mutex_lock_fail, true);

    public MutexLockAction() {
        super(ActionType.MUTEX_LOCK);
        addPins(namePin, timeoutPin, resultPin, failPin);
    }

    public MutexLockAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(namePin, timeoutPin, resultPin, failPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinString nameValue = getPinValue(runnable, namePin);
        PinInteger timeoutValue = getPinValue(runnable, timeoutPin);
        String name = nameValue == null ? "" : nameValue.getValue();
        if (name == null || name.isEmpty()) name = "lock";

        ReentrantLock lock = MutexUtil.getLock(name);
        boolean locked = false;
        try {
            int timeout = timeoutValue == null ? 0 : timeoutValue.intValue();
            if (timeout <= 0) {
                lock.lockInterruptibly();
                locked = true;
            } else {
                locked = lock.tryLock(timeout, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        resultPin.getValue(PinBoolean.class).setValue(locked);
        if (locked) {
            MutexUtil.remember(runnable, lock);
            executeNext(runnable, outPin);
        } else {
            executeNext(runnable, failPin);
        }
    }

    public Pin getNamePin() {
        return namePin;
    }

    public Pin getTimeoutPin() {
        return timeoutPin;
    }
}
