package top.bogey.touch_tool.utils.thread;

import android.os.SystemClock;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TaskThreadPoolExecutor extends ThreadPoolExecutor {
    private final AtomicInteger submittedTaskCount = new AtomicInteger(0);
    private final String name;
    // 运行态指标：提交被拒的次数、任务在队列里等待的时长
    private final AtomicInteger rejectedTaskCount = new AtomicInteger(0);
    private final AtomicLong queueWaitCount = new AtomicLong(0);
    private final AtomicLong totalQueueWaitMs = new AtomicLong(0);
    private final AtomicLong maxQueueWaitMs = new AtomicLong(0);

    public TaskThreadPoolExecutor(String name, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, TaskQueue<Runnable> workQueue, String namePrefix) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, new NamedThreadFactory(namePrefix));
        this.name = name;
        workQueue.setExecutor(this);
        // 空闲的核心线程也回收：后台不需要常驻几条线程，任务来了会重新创建
        allowCoreThreadTimeOut(true);
    }

    public int getSubmittedTaskCount() {
        return submittedTaskCount.get();
    }

    public int getRejectedTaskCount() {
        return rejectedTaskCount.get();
    }

    public long getMaxQueueWaitMs() {
        return maxQueueWaitMs.get();
    }

    public long getAverageQueueWaitMs() {
        long count = queueWaitCount.get();
        return count == 0 ? 0 : totalQueueWaitMs.get() / count;
    }

    // 一行运行态摘要，供调试页展示
    public String snapshot() {
        int queueSize = getQueue().size();
        return name + "：活跃 " + getActiveCount() + "/" + getPoolSize()
                + "，排队 " + queueSize + "/" + (queueSize + getQueue().remainingCapacity())
                + "，提交中 " + getSubmittedTaskCount()
                + "，拒绝 " + getRejectedTaskCount()
                + "，排队等待 最长 " + getMaxQueueWaitMs() + "ms / 平均 " + getAverageQueueWaitMs() + "ms";
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        submittedTaskCount.decrementAndGet();
    }

    @Override
    public void execute(Runnable command) {
        if (command == null) throw new NullPointerException();
        submittedTaskCount.incrementAndGet();
        try {
            super.execute(new TimedRunnable(command, SystemClock.uptimeMillis()));
        } catch (Throwable throwable) {
            // 提交失败（容量已满被拒绝）必须如实抛出：吞掉的话调用方会以为任务已提交，
            // 而它从未执行，也不会触发任何回调
            if (throwable instanceof RejectedExecutionException) rejectedTaskCount.incrementAndGet();
            submittedTaskCount.decrementAndGet();
            throw throwable;
        }
    }

    // 包一层记录任务在队列里等了多久，用来发现「超时里一大截被排队吃掉」
    private class TimedRunnable implements Runnable {
        private final Runnable command;
        private final long enqueueTime;

        public TimedRunnable(Runnable command, long enqueueTime) {
            this.command = command;
            this.enqueueTime = enqueueTime;
        }

        @Override
        public void run() {
            long wait = SystemClock.uptimeMillis() - enqueueTime;
            queueWaitCount.incrementAndGet();
            totalQueueWaitMs.addAndGet(wait);
            maxQueueWaitMs.accumulateAndGet(wait, Math::max);
            command.run();
        }
    }

    // 池线程按前缀编号命名，logcat / ANR trace / dumpsys 里能直接看出是哪个池
    private static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger index = new AtomicInteger(0);

        public NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, prefix + index.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
