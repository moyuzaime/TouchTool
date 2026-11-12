package top.bogey.touch_tool.bean.action.thread;

import com.google.gson.JsonObject;

import java.util.concurrent.locks.ReentrantLock;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.thread.MutexUtil;

// 解锁：与「加锁」配对，名字相同即可释放
public class MutexUnlockAction extends ExecuteAction {
    private final transient Pin namePin = new Pin(new PinString("lock"), R.string.mutex_lock_name);

    public MutexUnlockAction() {
        super(ActionType.MUTEX_UNLOCK);
        addPins(namePin);
    }

    public MutexUnlockAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(namePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinString nameValue = getPinValue(runnable, namePin);
        String name = nameValue == null ? "" : nameValue.getValue();
        if (name == null || name.isEmpty()) name = "lock";

        ReentrantLock lock = MutexUtil.getLock(name);
        try {
            // 只有持锁线程才能释放，否则会抛 IllegalMonitorStateException
            if (lock.isHeldByCurrentThread()) lock.unlock();
        } catch (Throwable ignored) {
        }
        MutexUtil.forget(runnable, lock);
        executeNext(runnable, outPin);
    }

    public Pin getNamePin() {
        return namePin;
    }
}
