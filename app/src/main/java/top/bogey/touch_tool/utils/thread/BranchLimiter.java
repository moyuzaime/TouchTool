package top.bogey.touch_tool.utils.thread;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import top.bogey.touch_tool.bean.save.setting.SettingSaver;
import top.bogey.touch_tool.service.TaskRunnable;

// 单次任务运行的并行分支上限：拿不到名额的分支按「未启动」处理，
// 行为不再取决于线程池里还剩几条，嵌套并行也不会一瞬间把池子打满
public class BranchLimiter {
    private final static Map<TaskRunnable, AtomicInteger> USED = new ConcurrentHashMap<>();

    // 尝试占一个分支名额；上限为 0 表示不拦截（仍然计数，供统计与停止宽限使用）
    public static boolean tryAcquire(TaskRunnable root) {
        if (root == null) return true;
        int limit = SettingSaver.TASK_BRANCH_LIMIT.get();
        AtomicInteger current = new AtomicInteger();
        USED.compute(root, (key, used) -> {
            if (used == null) used = new AtomicInteger();
            current.set(used.incrementAndGet());
            return used;
        });
        int now = current.get();
        if (limit <= 0 || now <= limit) return true;
        // 不阻塞等名额：父运行可能正被自己的子分支占满，等下去就是互相等锁
        release(root);
        return false;
    }

    public static void release(TaskRunnable root) {
        if (root == null) return;
        USED.computeIfPresent(root, (key, used) -> used.decrementAndGet() <= 0 ? null : used);
    }

    // 某个任务运行当前在跑的分支数
    public static int used(TaskRunnable root) {
        if (root == null) return 0;
        AtomicInteger used = USED.get(root);
        return used == null ? 0 : used.get();
    }

    // 有分支在跑的任务运行数
    public static int runningTaskCount() {
        return USED.size();
    }
}
