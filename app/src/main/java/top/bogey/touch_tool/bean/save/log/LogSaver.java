package top.bogey.touch_tool.bean.save.log;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.bean.other.log.LogInfo;
import top.bogey.touch_tool.utils.AppLog;
import top.bogey.touch_tool.utils.AppUtil;

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

    private final static String LOG_DIR = MainApplication.getInstance().getCacheDir().getAbsolutePath() + "/" + AppUtil.LOG_DIR_NAME;
    private final Map<String, LogSave> saves = new ConcurrentHashMap<>();
    // 监听者会被任务线程遍历、UI 线程增删，用写时复制集合避免并发修改异常打断任务
    private final Set<LogSaveListener> listeners = new java.util.concurrent.CopyOnWriteArraySet<>();

    public LogSave getLogSave(String key) {
        LogSave logSave = saves.computeIfAbsent(key, k -> new LogSave(key, LOG_DIR));
        // 旧 MMKV 存档迁移到 SQLite：每个任务只做一次，内部有幂等标记
        try {
            LogStore.getInstance().migrateIfNeeded(key, logSave);
        } catch (Throwable e) {
            AppLog.e("日志库", "迁移检查失败", e);
        }
        return logSave;
    }

    public void addLog(String key, LogInfo log, boolean autoUid) {
        addLog(key, log, autoUid, null);
    }

    // parentUid 只用于界面增量追加：空=根日志，非空=该父节点下新增的子日志
    public void addLog(String key, LogInfo log, boolean autoUid, String parentUid) {
        LogSave logSave = getLogSave(key);   // 仍要取一次：顺带完成旧存档的一次性迁移
        // 到这里写入只落 SQLite；旧 MMKV 存档仅用于迁移读取，不再写入
        LogStore.getInstance().append(key, log, parentUid);
        notifyNewLog(logSave, log, parentUid);
    }

    // 动作节点结束时值补齐：只更新库里这一行的 payload，不再整条重写到 MMKV
    public void updateLog(String key, LogInfo log) {
        LogSave logSave = getLogSave(key);
        LogStore.getInstance().updatePayload(key, log);
        notifyNewLog(logSave, log);
    }

    private void notifyNewLog(LogSave logSave, LogInfo log) {
        notifyNewLog(logSave, log, null);
    }

    private void notifyNewLog(LogSave logSave, LogInfo log, String parentUid) {
        listeners.stream().filter(Objects::nonNull).forEach(v -> v.onNewLog(logSave, log, parentUid));
    }

    public void clearLog(String key) {
        LogStore.getInstance().clear(key);
        LogSave logSave = saves.get(key);
        if (logSave == null) return;
        logSave.clearLog();
    }

    // 任务结束或界面退出前把待写队列落库
    public void flushLog(String key) {
        LogStore.getInstance().flushNow();
    }

    public void addListener(LogSaveListener listener) {
        listeners.add(listener);
    }

    public void removeListener(LogSaveListener listener) {
        listeners.remove(listener);
    }
}
