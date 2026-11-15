package top.bogey.touch_tool.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import top.bogey.touch_tool.utils.thread.AsyncUtil;
import top.bogey.touch_tool.utils.thread.BranchLimiter;
import top.bogey.touch_tool.utils.thread.MutexUtil;
import top.bogey.touch_tool.utils.thread.TaskQueue;
import top.bogey.touch_tool.utils.thread.TaskThreadPoolExecutor;

public class ThreadUtil {
    private static final TaskThreadPoolExecutor executorService = new TaskThreadPoolExecutor("辅助线程池", 2, 10, 60, TimeUnit.SECONDS, new TaskQueue<>(10), "tt-aux-");
    // 任务池上限 48 / 队列 32：池子只是兜底容量，分支行为由「每任务分支上限」控制（默认不限制）
    private static final TaskThreadPoolExecutor taskService = new TaskThreadPoolExecutor("任务线程池", 5, 48, 60, TimeUnit.SECONDS, new TaskQueue<>(32), "tt-pool-");

    // 发出即忘的辅助线程，调用方（删标签、自动备份、导入模型、整理布局等）不关心结果，
    // 池满时这里兜住拒绝异常，只记日志，不让它把 UI 线程炸掉
    public static void execute(Runnable runnable) {
        try {
            executorService.execute(runnable);
        } catch (RejectedExecutionException e) {
            AppLog.e("线程", "execute: 线程池已满，任务未能提交", e);
        }
    }

    public static ExecutorService getExecutorService() {
        return executorService;
    }

    public static Future<?> submitTask(Runnable runnable) {
        return taskService.submit(runnable);
    }

    // 线程池与三张登记表的运行态快照：设置页长按版本号查看，排「任务卡住/分支没跑」用
    public static String debugSnapshot() {
        StringBuilder builder = new StringBuilder();
        builder.append(taskService.snapshot()).append('\n');
        builder.append(executorService.snapshot()).append('\n');
        builder.append("异步分支：").append(AsyncUtil.debugSnapshot()).append('\n');
        builder.append("任务分支：有分支在跑的任务运行 ").append(BranchLimiter.runningTaskCount()).append(" 个").append('\n');
        builder.append("锁与计数器：\n").append(MutexUtil.debugSnapshot());
        return builder.toString();
    }
}
