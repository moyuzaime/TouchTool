package top.bogey.touch_tool.utils.thread;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import top.bogey.touch_tool.service.ITaskListener;
import top.bogey.touch_tool.service.TaskRunnable;

// 异步分支登记表：按句柄查找、按父运行批量取消
public class AsyncUtil {
    private final static Map<String, TaskRunnable> HANDLES = new ConcurrentHashMap<>();
    private final static Map<TaskRunnable, Set<String>> CHILDREN = new ConcurrentHashMap<>();
    // 已挂过「运行结束清理」的父运行，避免重复注册监听
    private final static Set<TaskRunnable> HOOKED = ConcurrentHashMap.newKeySet();

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
        CHILDREN.computeIfAbsent(parent, k -> ConcurrentHashMap.newKeySet()).add(handle);
        child.addListener(new ITaskListener() {
            @Override
            public void onFinish(TaskRunnable run) {
                HANDLES.remove(handle);
                Set<String> set = CHILDREN.get(parent);
                if (set != null) {
                    set.remove(handle);
                    if (set.isEmpty()) CHILDREN.remove(parent);
                }
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
        runnable.stop();
        HANDLES.remove(handle);
        return true;
    }

    // 结束某个父运行登记的全部异步分支，返回结束的数量
    public static int cancelAll(TaskRunnable parent) {
        Set<String> set = CHILDREN.remove(parent);
        if (set == null) return 0;
        int count = 0;
        for (String handle : set) {
            TaskRunnable runnable = HANDLES.remove(handle);
            if (runnable == null) continue;
            runnable.stop();
            count++;
        }
        return count;
    }

    public static int size(TaskRunnable parent) {
        Set<String> set = CHILDREN.get(parent);
        return set == null ? 0 : set.size();
    }
}
