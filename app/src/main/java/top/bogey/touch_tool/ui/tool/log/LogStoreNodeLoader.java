package top.bogey.touch_tool.ui.tool.log;

import top.bogey.touch_tool.bean.other.log.LogInfo;
import top.bogey.touch_tool.bean.save.log.LogStore;
import top.bogey.touch_tool.utils.tree.ILazyTreeNodeData;
import top.bogey.touch_tool.utils.tree.ITreeNodeDataLoader;

// 日志树节点从 SQLite 现查：子节点 uid 由 parent_uid 反查得到，
// 展开某个节点时才真正加载它的子项，不再一次性把整棵树读进内存
public class LogStoreNodeLoader implements ITreeNodeDataLoader {
    private final String taskId;

    public LogStoreNodeLoader(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }

    @Override
    public ILazyTreeNodeData loadData(Object flag) {
        if (flag == null) return null;
        LogInfo info = LogStore.getInstance().load(taskId, String.valueOf(flag));
        return info;
    }
}
