package top.bogey.touch_tool.bean.action.thread;

import com.google.gson.JsonObject;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.AppLog;
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

        boolean timedOut = false;
        TaskRunnable child = AsyncUtil.get(handle);
        try {
            if (child != null) {
                // 等完成信号；子被停止时用停止信号提前退出，避免卡在不可中断的动作上
                CompletableFuture<Object> waitFor = CompletableFuture.anyOf(child.getDone(), child.getStopSignal());
                if (timeout > 0) {
                    waitFor.get(timeout, TimeUnit.MILLISECONDS);
                } else {
                    waitFor.get();
                }
            } else {
                // 句柄为空或已结束：等本运行登记的全部异步分支，同样等完成信号
                timedOut = !AsyncUtil.awaitAll(runnable, timeout);
            }
        } catch (TimeoutException e) {
            timedOut = true;
        } catch (ExecutionException e) {
            AppLog.e(e);
        } catch (InterruptedException e) {
            // 任务被停止时线程被中断，保持中断标记，后面的 executeNext 会直接返回
            Thread.currentThread().interrupt();
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
