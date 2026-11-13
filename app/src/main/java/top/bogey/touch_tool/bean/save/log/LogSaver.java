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
        // 同一动作同一毫秒只允许一条：不同 uid 的重复写入直接丢弃
        try {
            if (log.getLogObject() instanceof top.bogey.touch_tool.bean.other.log.ActionLog newAction) {
                synchronized (logSave) {
                    for (String existUid : logSave.getOrder()) {
                        if (existUid.equals(log.getUid())) continue;
                        LogInfo exist = logSave.getLog(existUid);
                        if (exist == null || exist.getTime() != log.getTime()) continue;
                        if (exist.getLogObject() instanceof top.bogey.touch_tool.bean.other.log.ActionLog existAction
                                && newAction.getActionId().equals(existAction.getActionId())) {
                            // 复用已存在的 uid：既避免重复条目，又保证子日志的父引用不会悬空
                            log.setUid(existUid);
                            break;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        logSave.addLog(log, autoUid);
        // 带 parentLogUid 的日志（分支）写入后统一挂到父日志 children，和调用路径无关
        try {
            String logParentUid = log.getParentLogUid();
            if (logParentUid == null && log.getLogObject() instanceof top.bogey.touch_tool.bean.other.log.ActionLog a0) {
                logParentUid = a0.getParentLogUid();
            }
            if (logParentUid != null && !logParentUid.isEmpty()) {
                synchronized (logSave) {
                    LogInfo parent = logSave.getLog(logParentUid);
                    if (parent != null) {
                        parent.addChild(log.getUid());
                        logSave.addLog(parent, false);
                        notifyNewLog(logSave, parent);   // 让界面把父节点的 children 同步出来
                    }
                }
            }
        } catch (Throwable ignored) {
        }
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

    public void addListener(LogSaveListener listener) {
        listeners.add(listener);
    }

    public void removeListener(LogSaveListener listener) {
        listeners.remove(listener);
    }
}
