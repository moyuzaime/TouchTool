package top.bogey.touch_tool.bean.save.log;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.bean.other.log.LogInfo;
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
        return saves.computeIfAbsent(key, k -> new LogSave(key, LOG_DIR));
    }

    public void addLog(String key, LogInfo log, boolean autoUid) {
        LogSave logSave = getLogSave(key);
        logSave.addLog(log, autoUid);
        notifyNewLog(logSave, log);
    }

    // 子日志写入后父节点内容变了，用同一个 uid 重写一次并通知，日志窗口才能实时看到嵌套项
    public void updateLog(String key, LogInfo log) {
        LogSave logSave = getLogSave(key);
        LogInfo merged = log;
        // 读最新副本再合并 children，避免用旧副本把刚挂上的子节点覆盖掉
        synchronized (logSave) {
            LogInfo stored = logSave.getLog(log.getUid());
            if (stored != null) {
                for (Object flag : stored.getChildrenFlags()) {
                    String childUid = String.valueOf(flag);
                    boolean has = false;
                    for (Object own : log.getChildrenFlags()) {
                        if (String.valueOf(own).equals(childUid)) {
                            has = true;
                            break;
                        }
                    }
                    if (!has) log.addChild(childUid);
                }
            }
            logSave.addLog(log, false);
        }
        notifyNewLog(logSave, merged);
    }

    private void notifyNewLog(LogSave logSave, LogInfo log) {
        listeners.stream().filter(Objects::nonNull).forEach(v -> v.onNewLog(logSave, log));
    }

    public void clearLog(String key) {
        LogSave logSave = saves.get(key);
        if (logSave == null) return;
        logSave.clearLog();
    }

    // 顺序表延迟刷盘：任务结束或有窗口在用时补写一次
    public void flushOrder(String key) {
        if (key == null) return;
        LogSave logSave = saves.get(key);
        if (logSave != null) logSave.flushOrder();
    }

    public void addListener(LogSaveListener listener) {
        listeners.add(listener);
    }

    public void removeListener(LogSaveListener listener) {
        listeners.remove(listener);
    }
}
