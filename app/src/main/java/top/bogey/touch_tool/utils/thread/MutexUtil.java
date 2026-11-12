package top.bogey.touch_tool.utils.thread;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import top.bogey.touch_tool.service.ITaskListener;
import top.bogey.touch_tool.service.TaskRunnable;

// 跨分支共享的锁与计数器：给并行执行的多个分支做互斥和原子累加
public class MutexUtil {
    private final static Map<String, ReentrantLock> LOCKS = new ConcurrentHashMap<>();
    private final static Map<String, AtomicLong> COUNTERS = new ConcurrentHashMap<>();
    // 某个任务运行持有中的锁，任务结束时统一释放，避免死锁
    private final static Map<TaskRunnable, Set<ReentrantLock>> HELD = new ConcurrentHashMap<>();

    public static ReentrantLock getLock(String name) {
        return LOCKS.computeIfAbsent(name, k -> new ReentrantLock());
    }

    public static AtomicLong getCounter(String name) {
        return COUNTERS.computeIfAbsent(name, k -> new AtomicLong());
    }

    // 记录持有关系，并在该次运行结束时释放，防止异常退出后锁一直不还
    public static void remember(TaskRunnable runnable, ReentrantLock lock) {
        Set<ReentrantLock> set = HELD.computeIfAbsent(runnable, k -> {
            k.addListener(new ITaskListener() {
                @Override
                public void onFinish(TaskRunnable run) {
                    releaseAll(run);
                }
            });
            return ConcurrentHashMap.newKeySet();
        });
        set.add(lock);
    }

    public static void forget(TaskRunnable runnable, ReentrantLock lock) {
        Set<ReentrantLock> set = HELD.get(runnable);
        if (set == null) return;
        set.remove(lock);
        if (set.isEmpty()) HELD.remove(runnable);
    }

    private static void releaseAll(TaskRunnable runnable) {
        Set<ReentrantLock> set = HELD.remove(runnable);
        if (set == null) return;
        for (ReentrantLock lock : set) {
            try {
                while (lock.isHeldByCurrentThread()) lock.unlock();
            } catch (Throwable ignored) {
                // 持锁线程不是当前线程时无法释放，交给下一次加锁超时兜底
            }
        }
    }
}
