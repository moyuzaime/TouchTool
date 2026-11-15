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
    private final Set<LogSaveListener> listeners = new HashSet<>();

    public LogSave getLogSave(String key) {
        return saves.computeIfAbsent(key, k -> new LogSave(key, LOG_DIR));
    }

    public void addLog(String key, LogInfo log, boolean autoUid) {
        LogSave logSave = getLogSave(key);
        logSave.addLog(log, autoUid);
        if (autoUid) listeners.stream().filter(Objects::nonNull).forEach(v -> v.onNewLog(logSave, log));
    }

    public void clearLog(String key) {
        LogSave logSave = saves.get(key);
        if (logSave == null) return;
        logSave.clearLog();
    }

    public void addListener(LogSaveListener listener) {
        listeners.add(listener);
    }

    public void removeListener(LogSaveListener listener) {
        listeners.remove(listener);
    }
}
