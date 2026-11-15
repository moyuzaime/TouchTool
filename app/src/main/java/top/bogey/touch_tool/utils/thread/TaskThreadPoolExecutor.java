package top.bogey.touch_tool.utils.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskThreadPoolExecutor extends ThreadPoolExecutor {
    private final AtomicInteger submittedTaskCount = new AtomicInteger(0);

    public TaskThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, TaskQueue<Runnable> workQueue, String namePrefix) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, new NamedThreadFactory(namePrefix));
        workQueue.setExecutor(this);
        // 空闲的核心线程也回收：后台不需要常驻几条线程，任务来了会重新创建
        allowCoreThreadTimeOut(true);
    }

    public int getSubmittedTaskCount() {
        return submittedTaskCount.get();
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
            super.execute(command);
        } catch (Throwable throwable) {
            // 提交失败（容量已满被拒绝）必须如实抛出：吞掉的话调用方会以为任务已提交，
            // 而它从未执行，也不会触发任何回调
            submittedTaskCount.decrementAndGet();
            throw throwable;
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
