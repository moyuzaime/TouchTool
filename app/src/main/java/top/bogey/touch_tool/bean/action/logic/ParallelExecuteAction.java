package top.bogey.touch_tool.bean.action.logic;

import com.google.gson.JsonObject;

import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
import top.bogey.touch_tool.utils.thread.BranchMonitor;
import top.bogey.touch_tool.utils.tree.ITreeNodeData;

public class ParallelExecuteAction extends ExecuteAction implements DynamicPinsAction {
    private final static Pin morePin = new Pin(new PinExecute(), R.string.pin_execute, true);

    private final transient Pin countPin = new Pin(new PinInteger(1), R.string.parallel_action_count);
    private final transient Pin timeoutPin = new Pin(new PinInteger(5000), R.string.parallel_action_timeout);

    private final transient Pin addPin = new AlwaysShowPin(new PinAdd(morePin), R.string.pin_add_execute, true);
    private final transient Pin resultPin = new Pin(new PinBoolean(), R.string.pin_boolean_result, true);
    private final transient Pin completePin = new Pin(new PinExecute(), R.string.random_action_complete, true);


    public ParallelExecuteAction() {
        super(ActionType.PARALLEL_LOGIC);
        Pin secondPin = new Pin(new PinExecute(), R.string.pin_execute, true, true);
        addPins(countPin, timeoutPin, secondPin, addPin, resultPin, completePin);
    }

    public ParallelExecuteAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(countPin, timeoutPin);
        reAddPins(morePin);
        reAddPins(addPin, resultPin, completePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinNumber<?> count = getPinValue(runnable, countPin);
        PinNumber<?> timeout = getPinValue(runnable, timeoutPin);

        MainAccessibilityService service = MainApplication.getInstance().getService();

        List<Pin> validPins = new ArrayList<>();
        for (Pin dynamicPin : getDynamicPins()) {
            if (!dynamicPin.isLinked()) continue;
            validPins.add(dynamicPin);
        }

        int total = validPins.size();
        // 完成数按 1 起步：0 或负数按 1 处理
        int need = Math.max(1, count.intValue());
        CountDownLatch latch = new CountDownLatch(total);
        // 分支结束时会在自己的线程里遍历这个列表，而主线程可能还在往里加，必须用并发容器
        List<TaskRunnable> runnableList = new CopyOnWriteArrayList<>();
        AtomicInteger doneCount = new AtomicInteger();
        AtomicInteger rejectedCount = new AtomicInteger();
        AtomicBoolean completed = new AtomicBoolean(false);

        Task currTask = runnable.getTask();
        if (runnable.isDebug()) currTask.addFlag(Task.FLAG_DEBUG);
        // 并行节点自己的日志：分支日志统一挂到它下面，分支晚于主流程结束也不会脱层
        LogInfo container = runnable.getCurrentLogInfo();

        // 超时从提交起算，提交本身花的时间（线程池满时会排队）单独记下来，便于判断「超时被排队吃掉」
        long startTime = SystemClock.uptimeMillis();

        for (Pin dynamicPin : validPins) {
            // 先记运行态再 runTask：分支可能瞬间就跑完，晚记会把「已结束」又加回来
            BranchMonitor.addRunning(getId());
            TaskRunnable taskRunnable = service == null ? null : service.runTask(currTask, new InnerStartAction(dynamicPin), new ITaskListener() {
                @Override
                public void onExecute(TaskRunnable run, Action action, int progress) {
                    if (runnable.isCurrentInterrupt()) run.stop();
                }

                @Override
                public void onFinish(TaskRunnable run) {
                    BranchMonitor.removeRunning(getId());
                    try {
                        List<LogInfo> logList = run.getCacheLogList();
                        List<String> logs = saveLogs(runnable, logList);
                        LogInfo logInfo = new LogInfo(new ActionLog(runnable.getProgress() + 1, currTask, ParallelExecuteAction.this, true));
                        logInfo.setChildren(logs);
                        // 分支结束时主流程可能已经走完、日志栈已清空，这时再走 runnable.addLog()
                        // 会把分支写成新的根节点；直接挂到并行的日志节点下，父子关系才稳定
                        if (container == null) {
                            runnable.addLog(logInfo, 0);
                        } else {
                            synchronized (container) {
                                container.addChild(logInfo);
                                String taskId = runnable.getStartTask().getId();
                                LogSaver.getInstance().addLog(taskId, logInfo, false);
                                // 并行节点可能已落盘，补写一次把新挂的分支带上
                                LogSaver.getInstance().addLog(taskId, container, false);
                            }
                        }
                    } catch (Throwable e) {
                        // 日志挂载失败不能让计数停住，否则并行卡要等满超时
                        AppLog.e("并行执行", "分支日志挂载失败", e);
                    } finally {
                        doneCount.incrementAndGet();
                        latch.countDown();
                        // 达成完成数：停掉还在跑的分支，主流程可以继续了
                        if (doneCount.get() >= need && completed.compareAndSet(false, true)) {
                            stopBranches(runnableList);
                        }
                    }
                }
            }, runnable);
            if (taskRunnable == null) {
                // 未启动也要推进计数，否则每次运行都要等满超时；也不能把 null 塞进列表
                BranchMonitor.removeRunning(getId());
                rejectedCount.incrementAndGet();
                latch.countDown();
            } else {
                runnableList.add(taskRunnable);
            }
        }
        if (rejectedCount.get() > 0) {
            AppLog.e("并行执行", "分支未启动 " + rejectedCount.get() + " 个（线程池已满或服务不可用）");
        }
        long submitCost = SystemClock.uptimeMillis() - startTime;
        // 提交期间就达成完成数时，早停只覆盖了当时已经在列表里的分支；循环结束后补一次，
        // 否则后提交的分支会继续跑，主流程要等满超时（超时设为 0 时一直等）
        boolean earlyStopped = doneCount.get() >= need;
        if (earlyStopped) stopBranches(runnableList);

        long timeoutMs = timeout.intValue();
        boolean timedOut = false;
        boolean interrupted = false;
        try {
            if (timeoutMs <= 0) {
                // 超时 <= 0 表示一直等；分支停止是协作式的，卡在不响应中断的动作里只能靠停止任务收场
                latch.await();
            } else {
                // 超时从提交起算，提交本身花掉的时间要从等待窗口里扣掉
                long remain = timeoutMs - (SystemClock.uptimeMillis() - startTime);
                timedOut = !latch.await(Math.max(0, remain), TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            interrupted = true;
            AppLog.e(e);
            Thread.currentThread().interrupt();
        }

        // 超时、提前完成或任务被停止时，仍在跑的分支必须收掉，
        // 否则任务结束后它们还在点屏、写日志，日志树里还会迟到冒出新条目
        int stoppedCount = stopBranches(runnableList);
        if (stoppedCount > 0) {
            // 分支里的 sleep 是 100ms 切片，给一点宽限让它自己收尾，日志节点才能挂到并行节点下
            try {
                latch.await(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            AppLog.e("并行执行", "停止仍在运行的分支 " + stoppedCount + " 个");
        }

        // 只有完成数达成才算成功；超时和任务停止都算未达成
        resultPin.getValue(PinBoolean.class).setValue(doneCount.get() >= need);

        String outcome;
        if (interrupted) outcome = "任务停止";
        else if (timedOut) outcome = "超时";
        else if (doneCount.get() >= need) outcome = "达成完成数";
        else outcome = "分支全部结束";
        AppLog.d("并行执行", "分支 启动 " + runnableList.size() + "/" + total
                + "，完成 " + doneCount.get()
                + "，未启动 " + rejectedCount.get()
                + "，停止 " + stoppedCount
                + "，结果 " + outcome
                + "，提交 " + submitCost + "ms，总用时 " + (SystemClock.uptimeMillis() - startTime) + "ms");
        executeNext(runnable, completePin);
    }

    // 停掉还没结束的分支，返回实际停止的数量
    private int stopBranches(List<TaskRunnable> runnableList) {
        int count = 0;
        for (TaskRunnable taskRunnable : runnableList) {
            if (taskRunnable == null || taskRunnable.isInterrupt()) continue;
            taskRunnable.stop();
            count++;
        }
        return count;
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
