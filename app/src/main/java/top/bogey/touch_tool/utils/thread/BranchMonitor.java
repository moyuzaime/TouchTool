package top.bogey.touch_tool.utils.thread;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import top.bogey.touch_tool.utils.AppLog;

// 「运行中 N」角标的数据源：按动作 id 统计正在跑的分支数
// 运行中的任务是存档任务的副本，但动作 id 不变，蓝图卡片据此匹配到运行态
public class BranchMonitor {
    public interface Listener {
        void onBranchCountChanged(String actionId, int count);
    }

    private final static Map<String, AtomicInteger> COUNTS = new ConcurrentHashMap<>();
    private final static Set<Listener> LISTENERS = ConcurrentHashMap.newKeySet();

    public static void addRunning(String actionId) {
        if (actionId == null) return;
        AtomicInteger count = COUNTS.computeIfAbsent(actionId, key -> new AtomicInteger());
        notifyListeners(actionId, count.incrementAndGet());
    }

    public static void removeRunning(String actionId) {
        if (actionId == null) return;
        AtomicInteger count = COUNTS.get(actionId);
        if (count == null) return;
        int value = count.decrementAndGet();
        if (value <= 0) {
            COUNTS.remove(actionId, count);
            value = 0;
        }
        notifyListeners(actionId, value);
    }

    public static int getRunning(String actionId) {
        AtomicInteger count = actionId == null ? null : COUNTS.get(actionId);
        return count == null ? 0 : count.get();
    }

    public static void addListener(Listener listener) {
        if (listener == null) return;
        LISTENERS.add(listener);
    }

    public static void removeListener(Listener listener) {
        LISTENERS.remove(listener);
    }

    // 回调可能来自分支线程，监听方自己负责切到 UI 线程
    private static void notifyListeners(String actionId, int count) {
        for (Listener listener : LISTENERS) {
            try {
                listener.onBranchCountChanged(actionId, count);
            } catch (Throwable e) {
                AppLog.e("线程", "分支角标回调异常", e);
            }
        }
    }
}
