package top.bogey.touch_tool.bean.action.logic;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.DynamicPinsAction;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.action.start.InnerStartAction;
import top.bogey.touch_tool.bean.other.log.ActionLog;
import top.bogey.touch_tool.bean.other.log.LogInfo;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinAdd;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.special_pin.AlwaysShowPin;
import top.bogey.touch_tool.bean.save.log.LogSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.ITaskListener;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.AppLog;
import top.bogey.touch_tool.utils.tree.ITreeNodeData;

public class ParallelExecuteAction extends ExecuteAction implements DynamicPinsAction {
    // 分支针脚用「图标执行/字符串执行」同款：自带一个可编辑的名字
    private final static Pin morePin = new Pin(new top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinStringExecute(), R.string.pin_execute, true);

    private final transient Pin countPin = new Pin(new PinInteger(1), R.string.parallel_action_count);
    private final transient Pin timeoutPin = new Pin(new PinInteger(5000), R.string.parallel_action_timeout);

    private final transient Pin addPin = new AlwaysShowPin(new PinAdd(morePin), R.string.pin_add_execute, true);
    private final transient Pin resultPin = new Pin(new PinBoolean(), R.string.pin_boolean_result, true);
    private final transient Pin completePin = new Pin(new PinExecute(), R.string.random_action_complete, true);


    public ParallelExecuteAction() {
        super(ActionType.PARALLEL_LOGIC);
        addPins(countPin, timeoutPin, addPin, resultPin, completePin);
        // 继承来的出口针脚在这里只当第一个并行分支用，流程收尾统一走「完成」
        outPin.setValue(new top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinStringExecute());
        outPin.setTitle(MainApplication.getInstance().getString(R.string.parallel_branch_pin));
        outPin.setHide(false);
    }

    public ParallelExecuteAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(countPin, timeoutPin);
        reAddPins(morePin);
        reAddPins(addPin, resultPin, completePin);

        // outPin 之前把名字存在标题里，切换成针脚值前先把旧标题搬过去，避免名字丢失
        String oldTitle = outPin.getTitle();
        String defTitle = MainApplication.getInstance().getString(R.string.pin_execute);
        String branchTitle = MainApplication.getInstance().getString(R.string.parallel_branch_pin);
        if (outPin.getValue() instanceof top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinStringExecute old) {
            String value = old.getValue();
            if (value == null || value.trim().isEmpty()) {
                if (oldTitle != null && !defTitle.equals(oldTitle) && !branchTitle.equals(oldTitle)) old.setValue(oldTitle);
            }
        } else {
            top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinStringExecute value =
                    new top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinStringExecute();
            if (oldTitle != null && !defTitle.equals(oldTitle) && !branchTitle.equals(oldTitle)) value.setValue(oldTitle);
            outPin.setValue(value);
        }
        outPin.setTitle(branchTitle);
        outPin.setHide(false);
        for (Pin dynamicPin : getDynamicPins()) {
            if (dynamicPin == outPin || dynamicPin == completePin) continue;
            if (dynamicPin.getValue() instanceof top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinStringExecute) continue;
            String title = dynamicPin.getTitle();
            String def = defTitle;
            top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinStringExecute value =
                    new top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinStringExecute();
            if (title != null && !def.equals(title) && !branchTitle.equals(title)) value.setValue(title);
            dynamicPin.setValue(value);
        }
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinNumber<?> count = getPinValue(runnable, countPin);
        PinNumber<?> timeout = getPinValue(runnable, timeoutPin);

        MainAccessibilityService service = MainApplication.getInstance().getService();
        if (service == null) {
            // 无障碍服务不可用时直接收尾，避免 NPE
            AppLog.e("并行执行", "无障碍服务不可用，跳过并行分支");
            resultPin.getValue(PinBoolean.class).setValue(false);
            executeNext(runnable, completePin);
            return;
        }

        List<Pin> validPins = new ArrayList<>();
        for (Pin dynamicPin : getDynamicPins()) {
            if (!dynamicPin.isLinked()) continue;
            validPins.add(dynamicPin);
        }

        CountDownLatch latch = new CountDownLatch(validPins.size());
        int countValue = count.intValue();
        List<TaskRunnable> runnableList = new ArrayList<>();
        // 已正常结束的分支，用于超时后只停还活着的
        java.util.Set<TaskRunnable> finished = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
        int rejectedCount = 0;

        Task currTask = runnable.getTask();
        if (runnable.isDebug()) currTask.addFlag(Task.FLAG_DEBUG);

        for (Pin dynamicPin : validPins) {
            int branchIndex = validPins.indexOf(dynamicPin) + 1;
            String branchName = getBranchName(dynamicPin);
            if (branchName.isEmpty()) branchName = "分支" + branchIndex;
            TaskRunnable taskRunnable = service.runTask(currTask, new InnerStartAction(dynamicPin, branchName), new ITaskListener() {
                @Override
                public void onExecute(TaskRunnable run, Action action, int progress) {
                    if (runnable.isCurrentInterrupt()) run.stop();
                }

                @Override
                public void onFinish(TaskRunnable run) {
                    finished.add(run);
                    try {
                        List<LogInfo> logList = run.getCacheLogList();
                        List<String> logs = saveLogs(runnable, logList);
                        LogInfo logInfo = new LogInfo(new ActionLog(runnable.getProgress() + 1, currTask, ParallelExecuteAction.this, true));
                        logInfo.setChildren(logs);
                        runnable.addLog(logInfo, 0);
                    } catch (Throwable e) {
                        AppLog.e("并行执行", "保存分支日志失败");
                    } finally {
                        // 日志出错也必须放行计数，否则只能干等到超时
                        latch.countDown();
                    }

                    // 完成数 <= 0 视为「全部完成才结束」，不做提前停止
                    if (countValue > 0 && countValue <= latch.getCount()) {
                        runnableList.forEach(r -> {
                            if (r != null) r.stop();
                        });
                        resultPin.getValue(PinBoolean.class).setValue(true);
                    }
                }
            });
            runnableList.add(taskRunnable);
            if (taskRunnable == null) {
                // 线程池已满，分支没能启动，必须让计数器前进，避免一直等到超时
                rejectedCount++;
                latch.countDown();
                AppLog.e("并行执行", "线程池已满，分支未启动");
            }
        }

        ITaskListener listener = new ITaskListener() {
            @Override
            public void onPauseChanged(TaskRunnable runnable, boolean paused) {
                for (TaskRunnable taskRunnable : runnableList) {
                    if (taskRunnable == null) continue;
                    if (paused) taskRunnable.forcePause();
                    else taskRunnable.forceResume();
                }
            }
        };
        runnable.addListener(listener);

        try {
            if (timeout.intValue() <= 0) {
                latch.await();
            } else {
                latch.await(timeout.intValue(), TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            AppLog.e(e);
            resultPin.getValue(PinBoolean.class).setValue(false);
        }
        runnable.removeListener(listener);

        // 超时或提前完成后，停掉仍在运行的分支，避免它们继续在后台跑
        int stoppedCount = 0;
        for (TaskRunnable taskRunnable : runnableList) {
            if (taskRunnable == null || finished.contains(taskRunnable)) continue;
            taskRunnable.stop();
            stoppedCount++;
        }
        if (stoppedCount > 0) AppLog.e("并行执行", "已停止 " + stoppedCount + " 个仍在运行的分支");
        if (rejectedCount > 0) AppLog.e("并行执行", "有 " + rejectedCount + " 个分支因线程池已满未能启动");

        executeNext(runnable, completePin);
    }

    private List<String> saveLogs(TaskRunnable runnable, List<LogInfo> logList) {
        List<String> logs = new ArrayList<>();
        for (LogInfo logInfo : logList) {
            logs.add(logInfo.getUid());
            LogSaver.getInstance().addLog(runnable.getStartTask().getId(), logInfo, false);
            List<LogInfo> children = new ArrayList<>();
            for (ITreeNodeData child : logInfo.getChildrenData()) {
                LogInfo childLogInfo = (LogInfo) child;
                children.add(childLogInfo);
            }
            saveLogs(runnable, children);
        }
        return logs;
    }

    // 给某个执行分支命名：写进针脚标题，随动作一起序列化
    public void setBranchName(Pin pin, String name) {
        if (pin == null) return;
        pin.setTitle(name == null ? "" : name.trim());
    }

    // 分支当前名字；未命名时返回空串，界面上回落到默认「执行」
    public String getBranchName(Pin pin) {
        if (pin == null) return "";
        String title = pin.getTitle();
        if (title == null) return "";
        String def = MainApplication.getInstance().getString(R.string.pin_execute);
        return def.equals(title) ? "" : title;
    }
    @Override
    public List<Pin> getDynamicPins() {
        List<Pin> pins = new ArrayList<>();
        pins.add(outPin);
        boolean start = false;
        for (Pin pin : getPins()) {
            if (pin == addPin) start = false;
            if (start) pins.add(pin);
            if (pin == timeoutPin) start = true;
        }
        return pins;
    }
}
