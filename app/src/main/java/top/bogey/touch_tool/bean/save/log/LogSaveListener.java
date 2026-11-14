package top.bogey.touch_tool.bean.save.log;

import top.bogey.touch_tool.bean.other.log.LogInfo;

public interface LogSaveListener {
    void onNewLog(LogSave logSave, LogInfo log);

    // 增量追加用：parentUid 为空表示这是根日志，否则是某父节点下新增的子日志
    default void onNewLog(LogSave logSave, LogInfo log, String parentUid) {
        onNewLog(logSave, log);
    }
}
