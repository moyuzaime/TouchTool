package top.bogey.touch_tool.bean.action.thread;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.action.start.InnerStartAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.ITaskListener;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.AppLog;
import top.bogey.touch_tool.utils.thread.AsyncUtil;

// 异步执行：把「异步」针脚后面的动作放到另一条线程跑，主流程立刻继续
public class AsyncAction extends ExecuteAction {
    private final transient Pin asyncPin = new Pin(new PinExecute(), R.string.async_action_branch, true);
    private final transient Pin handlePin = new Pin(new PinString(), R.string.async_action_handle, true);

    public AsyncAction() {
        super(ActionType.ASYNC);
        addPins(asyncPin, handlePin);
    }

    public AsyncAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(asyncPin, handlePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        MainAccessibilityService service = MainApplication.getInstance().getService();
        TaskRunnable child = null;
        if (service != null) {
            child = service.runTask(runnable.getTask(), new InnerStartAction(asyncPin), new ITaskListener() {
                @Override
                public void onExecute(TaskRunnable run, Action action, int progress) {
                    if (runnable.isCurrentInterrupt()) run.stop();
                }
            });
        }

        String handle = "";
        if (child == null) {
            AppLog.e("异步执行", "分支未能启动（线程池已满或服务不可用）");
        } else {
            handle = AsyncUtil.register(runnable, child);
        }
        handlePin.getValue(PinString.class).setValue(handle);

        executeNext(runnable, outPin);
    }

    public Pin getAsyncPin() {
        return asyncPin;
    }
}
