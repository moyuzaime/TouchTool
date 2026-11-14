package top.bogey.touch_tool.bean.save.log;

import top.bogey.touch_tool.bean.other.log.LogInfo;

public interface LogSaveListener {
    // taskId 为所属任务；parentUid 为空表示根日志，非空表示该父节点下新增的子日志
    void onNewLog(String taskId, LogInfo log, String parentUid);

    default void onNewLog(String taskId, LogInfo log) {
        onNewLog(taskId, log, null);
    }
}
