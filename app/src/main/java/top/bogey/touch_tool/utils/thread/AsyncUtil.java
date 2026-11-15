package top.bogey.touch_tool.utils.thread;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import top.bogey.touch_tool.service.ITaskListener;
import top.bogey.touch_tool.service.TaskRunnable;

// 异步分支登记表：按句柄查找、按父运行批量取消
public class AsyncUtil {
    private final static Map<String, TaskRunnable> HANDLES = new ConcurrentHashMap<>();
    private final static Map<TaskRunnable, Set<String>> CHILDREN = new ConcurrentHashMap<>();
    // 已挂过「运行结束清理」的父运行，避免重复注册监听
    private final static Set<TaskRunnable> HOOKED = ConcurrentHashMap.newKeySet();
    // 父运行名下的子分支全部结束的完成信号：数量归零时 complete，重新登记时重建
    private final static Map<TaskRunnable, CompletableFuture<Void>> EMPTY_SIGNALS = new ConcurrentHashMap<>();

    // 登记一个异步分支，结束时自动移除
    public static String register(TaskRunnable parent, TaskRunnable child) {
        String handle = UUID.randomUUID().toString().substring(0, 8);
        HANDLES.put(handle, child);
        // 父运行结束时统一收掉它的异步分支，避免任务结束后分支还在后台跑
        if (HOOKED.add(parent)) {
            parent.addListener(new ITaskListener() {
                @Override
                public void onFinish(TaskRunnable run) {
                    HOOKED.remove(run);
                    cancelAll(run);
                }
            });
        }
        // 计数与完成信号同一把锁：等待方拿到的一定是「还有子分支」时的那一个信号
        synchronized (AsyncUtil.class) {
            CHILDREN.computeIfAbsent(parent, k -> ConcurrentHashMap.newKeySet()).add(handle);
            EMPTY_SIGNALS.computeIfAbsent(parent, k -> new CompletableFuture<>());
        }
        child.addListener(new ITaskListener() {
            @Override
            public void onFinish(TaskRunnable run) {
                remove(parent, handle);
            }
        });
        return handle;
    }

    public static TaskRunnable get(String handle) {
        if (handle == null || handle.isEmpty()) return null;
        return HANDLES.get(handle);
    }

    // 结束某个异步分支，返回是否找到
    public static boolean cancel(String handle) {
        TaskRunnable runnable = get(handle);
        if (runnable == null) return false;
        // 从所有父运行的子集合里摘掉，否则 size(parent) 永远大于 0，空句柄等待只能等超时
        for (TaskRunnable parent : new ArrayList<>(CHILDREN.keySet())) {
            remove(parent, handle);
        }
        remove(null, handle);
        runnable.stop();
        return true;
    }

    // 结束某个父运行登记的全部异步分支，返回结束的数量
    public static int cancelAll(TaskRunnable parent) {
        Set<String> set;
        CompletableFuture<Void> signal;
        synchronized (AsyncUtil.class) {
            set = CHILDREN.remove(parent);
            signal = EMPTY_SIGNALS.remove(parent);
        }
        if (signal != null) signal.complete(null);
        if (set == null) return 0;

        int count = 0;
        for (String handle : new ArrayList<>(set)) {
            TaskRunnable runnable = HANDLES.remove(handle);
            if (runnable == null) continue;
            runnable.stop();
            count++;
        }
        return count;
    }

    // 等待某个运行登记的全部异步分支结束；返回 false 表示超时
    public static boolean awaitAll(TaskRunnable parent, long timeoutMs) {
        long endTime = timeoutMs > 0 ? System.currentTimeMillis() + timeoutMs : 0;
        while (true) {
            CompletableFuture<Void> signal;
            synchronized (AsyncUtil.class) {
                if (size(parent) == 0) return true;
                signal = EMPTY_SIGNALS.computeIfAbsent(parent, k -> new CompletableFuture<>());
            }
            try {
                if (endTime > 0) {
                    long remain = endTime - System.currentTimeMillis();
                    if (remain <= 0) return size(parent) == 0;
                    signal.get(remain, TimeUnit.MILLISECONDS);
                } else {
                    signal.get();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return size(parent) == 0;
            } catch (TimeoutException | ExecutionException e) {
                return size(parent) == 0;
            }
        }
    }

    public static int size(TaskRunnable parent) {
        Set<String> set = CHILDREN.get(parent);
        return set == null ? 0 : set.size();
    }

    public static int handleCount() {
        return HANDLES.size();
    }

    public static int parentCount() {
        return CHILDREN.size();
    }

    // 摘掉一个句柄：句柄表与父运行的子集合都要清，归零时唤醒等待方
    private static void remove(TaskRunnable parent, String handle) {
        CompletableFuture<Void> signal = null;
        synchronized (AsyncUtil.class) {
            HANDLES.remove(handle);
            if (parent != null) {
                Set<String> set = CHILDREN.get(parent);
                if (set == null) {
                    // 父运行名下的子集合已被整体清掉，兜底把完成信号收掉
                    signal = EMPTY_SIGNALS.remove(parent);
                } else {
                    set.remove(handle);
                    if (set.isEmpty() && CHILDREN.remove(parent, set)) {
                        signal = EMPTY_SIGNALS.remove(parent);
                    }
                }
            }
        }
        if (signal != null) signal.complete(null);
    }
}
