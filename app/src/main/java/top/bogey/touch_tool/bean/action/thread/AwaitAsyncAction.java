package top.bogey.touch_tool.bean.action.thread;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.thread.AsyncUtil;

// 等待完成：等某个句柄（或全部异步分支）结束，可设置超时
public class AwaitAsyncAction extends ExecuteAction {
    private final transient Pin handlePin = new Pin(new PinString(), R.string.async_action_handle);
    private final transient Pin timeoutPin = new Pin(new PinInteger(0), R.string.await_async_timeout);
    private final transient Pin timeoutResultPin = new Pin(new PinBoolean(), R.string.await_async_timeout_result, true);

    public AwaitAsyncAction() {
        super(ActionType.AWAIT_ASYNC);
        addPins(handlePin, timeoutPin, timeoutResultPin);
    }

    public AwaitAsyncAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(handlePin, timeoutPin, timeoutResultPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinString handleValue = getPinValue(runnable, handlePin);
        PinInteger timeoutValue = getPinValue(runnable, timeoutPin);
        String handle = handleValue == null ? "" : handleValue.getValue();
        int timeout = timeoutValue == null ? 0 : timeoutValue.intValue();
        long endTime = timeout > 0 ? System.currentTimeMillis() + timeout : 0;

        boolean timedOut = false;
        TaskRunnable child = AsyncUtil.get(handle);
        if (child != null) {
            // 没有直接的 join，只能轮询运行状态
            while (!child.isInterrupt() && AsyncUtil.get(handle) != null) {
                if (timeout > 0 && System.currentTimeMillis() > endTime) {
                    timedOut = true;
                    break;
                }
                runnable.sleep(50);
            }
        } else {
            // 句柄为空或已结束时等待本运行登记的全部异步分支
            while (AsyncUtil.size(runnable) > 0) {
                if (timeout > 0 && System.currentTimeMillis() > endTime) {
                    timedOut = true;
                    break;
                }
                runnable.sleep(50);
            }
        }

        timeoutResultPin.getValue(PinBoolean.class).setValue(timedOut);
        executeNext(runnable, outPin);
    }

    public Pin getHandlePin() {
        return handlePin;
    }

    public Pin getTimeoutPin() {
        return timeoutPin;
    }
}
