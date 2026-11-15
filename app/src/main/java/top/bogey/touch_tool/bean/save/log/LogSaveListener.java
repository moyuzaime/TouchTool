package top.bogey.touch_tool.bean.save.log;

import top.bogey.touch_tool.bean.other.log.LogInfo;

public interface LogSaveListener {
    void onNewLog(LogSave logSave, LogInfo log);
}
