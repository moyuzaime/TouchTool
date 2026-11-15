package top.bogey.touch_tool.bean.save.log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// 分支日志收集器：并行/异步分支会再起新的 TaskRunnable（含子任务），
// 它们各自缓存日志、互不知情，父级拿不到子级的内容。
// 这里按任务 id 维护一个栈，父级 begin 后，期间创建的所有 runnable 都把缓存日志落盘并登记到同一个收集器，
// 父级结束时用收集到的 uid 作为自己的 children。
public class LogCollector {
    private final static Map<String, Deque<LogCollector>> ACTIVE = new ConcurrentHashMap<>();

    private final List<String> uids = new CopyOnWriteArrayList<>();

    public static LogCollector begin(String taskId) {
        LogCollector collector = new LogCollector();
        ACTIVE.computeIfAbsent(taskId, k -> new ArrayDeque<>()).push(collector);
        return collector;
    }

    public static void end(String taskId, LogCollector collector) {
        Deque<LogCollector> deque = ACTIVE.get(taskId);
        if (deque == null) return;
        synchronized (deque) {
            deque.remove(collector);
            if (deque.isEmpty()) ACTIVE.remove(taskId);
        }
    }

    // 当前任务正在收集的收集器（最内层）；没有则返回 null
    public static LogCollector current(String taskId) {
        Deque<LogCollector> deque = ACTIVE.get(taskId);
        if (deque == null) return null;
        synchronized (deque) {
            return deque.peek();
        }
    }

    public void addUid(String uid) {
        if (uid != null) uids.add(uid);
    }

    public List<String> getUids() {
        return new ArrayList<>(uids);
    }
}
