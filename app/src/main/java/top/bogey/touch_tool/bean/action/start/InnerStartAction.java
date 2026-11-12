package top.bogey.touch_tool.bean.action.start;

import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.service.TaskRunnable;

public class InnerStartAction extends StartAction {
    private final transient Pin startPin;
    // 并行分支名，用于日志前缀
    private final transient String branchTag;

    public InnerStartAction(Pin startPin) {
        this(startPin, null);
    }

    public InnerStartAction(Pin startPin, String branchName) {
        super(ActionType.INNER_START);
        this.startPin = startPin;
        this.branchTag = (branchName == null || branchName.isEmpty()) ? null : "[" + branchName + "]";
    }

    public String getBranchTag() {
        return branchTag;
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        super.execute(runnable, pin);
        executeNext(runnable, startPin);
    }

    @Override
    public RestartType getRestartType() {
        return RestartType.NEW;
    }
}
