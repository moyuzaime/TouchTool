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
    // 分支名按顺序保存：动态针脚重建时值会丢，靠它恢复
    private java.util.List<String> branchNames = new ArrayList<>();


    public ParallelExecuteAction() {
        super(ActionType.PARALLEL_LOGIC);
        addPins(countPin, timeoutPin, addPin, resultPin, completePin);
        // 继承来的出口针脚不参与执行：直接移除，流程收尾统一走「完成」
        removePin(outPin);
    }

    public ParallelExecuteAction(JsonObject jsonObject) {
        super(jsonObject);
        // 旧数据里 outPin 曾被存成字符串执行针脚，和基类字段类型对不上，会残留一个未消费的针脚，
        // 复制/加载时就表现为凭空多出一个执行针脚；这里把它丢掉
        if (!tmpPins.isEmpty() && tmpPins.get(0).getValue() instanceof top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinStringExecute) {
            tmpPins.remove(0);
        }
        reAddPins(countPin, timeoutPin);
        reAddPins(morePin);
        reAddPins(addPin, resultPin, completePin);
        // 移除继承来的出口针脚，避免它在卡面上多出一个「执行」
        removePin(outPin);
        hydrateBranchNames();
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

        AppLog.e("DIAG", "并行执行 分支数=" + validPins.size() + " 超时=" + timeout.intValue() + " count=" + countValue + " debug=" + runnable.isDebug());
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
                    AppLog.e("DIAG", "分支结束 cacheLogs=" + run.getCacheLogList().size());
                    // 分支日志已实时写入日志库，这里只放行计数
                    latch.countDown();

                    // 完成数 <= 0 视为「全部完成才结束」，不做提前停止
                    if (countValue > 0 && countValue <= latch.getCount()) {
                        runnableList.forEach(r -> {
                            if (r != null) r.stop();
                        });
                        resultPin.getValue(PinBoolean.class).setValue(true);
                    }
                }
            });
            if (taskRunnable != null) {
                AppLog.e("DIAG", "分支启动 name=" + branchName + " runnable=" + taskRunnable);
            } else {
                AppLog.e("DIAG", "分支未启动 name=" + branchName);
            }
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

    // 给某个执行分支命名：同时写针脚值和持久化列表，重建针脚时也能恢复
    public void setBranchName(Pin pin, String name) {
        if (pin == null) return;
        String value = name == null ? "" : name.trim();
        int index = getDynamicPins().indexOf(pin);
        if (index >= 0) {
            while (branchNames.size() <= index) branchNames.add("");
            branchNames.set(index, value);
        }
        if (pin.getValue() instanceof top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinStringExecute stringExecute) {
            stringExecute.setValue(value);
        } else {
            pin.setTitle(value);
        }
    }

    // 分支当前名字：优先取针脚值，其次按序号取持久化列表，最后回落到标题
    public String getBranchName(Pin pin) {
        if (pin == null) return "";
        if (pin.getValue() instanceof top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinStringExecute stringExecute) {
            String value = stringExecute.getValue();
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        int index = getDynamicPins().indexOf(pin);
        if (index >= 0 && index < branchNames.size()) {
            String name = branchNames.get(index);
            if (name != null && !name.trim().isEmpty()) return name.trim();
        }
        String title = pin.getTitle();
        String def = MainApplication.getInstance().getString(R.string.pin_execute);
        String branch = MainApplication.getInstance().getString(R.string.parallel_branch_pin);
        if (title == null || def.equals(title) || branch.equals(title)) return "";
        return title;
    }

    // 针脚重建后把名字写回针脚值（界面才能显示）
    private void hydrateBranchNames() {
        List<Pin> dynamicPins = getDynamicPins();
        for (int i = 0; i < dynamicPins.size(); i++) {
            Pin pin = dynamicPins.get(i);
            String name = i < branchNames.size() ? branchNames.get(i) : null;
            if (name == null || name.trim().isEmpty()) continue;
            if (pin.getValue() instanceof top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinStringExecute stringExecute) {
                stringExecute.setValue(name);
            }
        }
    }

    @Override
    public List<Pin> getDynamicPins() {
        List<Pin> pins = new ArrayList<>();
        boolean start = false;
        for (Pin pin : getPins()) {
            if (pin == addPin) start = false;
            if (start) pins.add(pin);
            if (pin == timeoutPin) start = true;
        }
        return pins;
    }
}
