package top.bogey.touch_tool.bean.save.log;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import top.bogey.touch_tool.bean.other.log.LogInfo;
import top.bogey.touch_tool.utils.AppLog;

// 日志门面：只负责「写 SQLite + 通知界面」，旧 MMKV 存档由 LegacyLogMigration 一次性迁移后删除
public class LogSaver {
    private static LogSaver instance;

    public static LogSaver getInstance() {
        synchronized (LogSaver.class) {
            if (instance == null) {
                instance = new LogSaver();
            }
        }
        return instance;
    }

    // 监听者会被任务线程遍历、UI 线程增删，用写时复制集合避免并发修改异常打断任务
    private final Set<LogSaveListener> listeners = new CopyOnWriteArraySet<>();

    private LogSaver() {
        // 首次使用前把旧 MMKV 存档迁进 SQLite（幂等，失败会保留旧文件）
        try {
            LegacyLogMigration.runOnce();
        } catch (Throwable e) {
            AppLog.e("日志库", "旧日志迁移失败", e);
        }
    }

    public void addLog(String key, LogInfo log, boolean autoUid) {
        addLog(key, log, autoUid, null);
    }

    // parentUid 只用于界面增量追加：空=根日志，非空=该父节点下新增的子日志
    public void addLog(String key, LogInfo log, boolean autoUid, String parentUid) {
        LogStore.getInstance().append(key, log, parentUid);
        notifyNewLog(key, log, parentUid);
    }

    // 动作节点结束时值补齐：只更新库里这一行
    public void updateLog(String key, LogInfo log) {
        LogStore.getInstance().updatePayload(key, log);
        notifyNewLog(key, log, null);
    }

    public void clearLog(String key) {
        LogStore.getInstance().clear(key);
    }

    // 待写队列落库
    public void flushLog(String key) {
        LogStore.getInstance().flushNow();
    }

    private void notifyNewLog(String taskId, LogInfo log, String parentUid) {
        listeners.stream().filter(Objects::nonNull).forEach(v -> v.onNewLog(taskId, log, parentUid));
    }

    public void addListener(LogSaveListener listener) {
        listeners.add(listener);
    }

    public void removeListener(LogSaveListener listener) {
        listeners.remove(listener);
    }
}
