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
        // 已经收尾的运行不会再有 onFinish 回调，记了也没人释放，直接还回去
        if (runnable.isFinished()) {
            if (lock.isHeldByCurrentThread()) lock.unlock();
            return;
        }
        Set<ReentrantLock> created = ConcurrentHashMap.newKeySet();
        Set<ReentrantLock> set = HELD.putIfAbsent(runnable, created);
        if (set == null) {
            set = created;
            runnable.addListener(new ITaskListener() {
                @Override
                public void onFinish(TaskRunnable run) {
                    releaseAll(run);
                }
            });
        }
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

    // 调试用：锁与计数器的登记情况，设置页长按版本号可查看
    public static String debugSnapshot() {
        StringBuilder builder = new StringBuilder();
        builder.append("锁登记 ").append(LOCKS.size())
                .append("，持有中的运行 ").append(HELD.size()).append('\n');
        for (Map.Entry<String, ReentrantLock> entry : LOCKS.entrySet()) {
            ReentrantLock lock = entry.getValue();
            if (!lock.isLocked()) continue;
            builder.append("  锁 [").append(entry.getKey()).append("] 已占用");
            if (lock.isHeldByCurrentThread()) builder.append("，当前线程持有");
            builder.append('\n');
        }
        builder.append("计数器 ").append(COUNTERS.size()).append('\n');
        for (Map.Entry<String, AtomicLong> entry : COUNTERS.entrySet()) {
            builder.append("  [").append(entry.getKey()).append("] = ").append(entry.getValue().get()).append('\n');
        }
        return builder.toString();
    }
}
