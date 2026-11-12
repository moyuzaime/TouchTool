package top.bogey.touch_tool.bean.action.thread;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.thread.AsyncUtil;

// 取消子任务：按句柄结束异步分支，句柄留空则结束本运行的全部异步分支
public class CancelAsyncAction extends ExecuteAction {
    private final transient Pin handlePin = new Pin(new PinString(), R.string.async_action_handle);
    private final transient Pin resultPin = new Pin(new PinBoolean(), R.string.cancel_async_result, true);

    public CancelAsyncAction() {
        super(ActionType.CANCEL_ASYNC);
        addPins(handlePin, resultPin);
    }

    public CancelAsyncAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(handlePin, resultPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinString handleValue = getPinValue(runnable, handlePin);
        String handle = handleValue == null ? "" : handleValue.getValue();

        boolean success;
        if (handle == null || handle.isEmpty()) {
            success = AsyncUtil.cancelAll(runnable) > 0;
        } else {
            success = AsyncUtil.cancel(handle);
        }
        resultPin.getValue(PinBoolean.class).setValue(success);

        executeNext(runnable, outPin);
    }

    public Pin getHandlePin() {
        return handlePin;
    }
}
