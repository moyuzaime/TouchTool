package top.bogey.touch_tool.bean.save.log;

import com.tencent.mmkv.MMKV;

import top.bogey.touch_tool.bean.other.log.LogInfo;
import top.bogey.touch_tool.utils.GsonUtil;
import top.bogey.touch_tool.utils.tree.ILazyTreeNodeData;
import top.bogey.touch_tool.utils.tree.ITreeNodeDataLoader;

public class LogSave implements ITreeNodeDataLoader {
    private final static String COUNT = "count";

    private final MMKV mmkv;
    private final String key;

    private long time = System.currentTimeMillis();

    public LogSave(String key, String path) {
        this.key = key;
        mmkv = MMKV.mmkvWithID(key, MMKV.MULTI_PROCESS_MODE, null, path);
    }

    public LogInfo getLog(String key) {
        try {
            return GsonUtil.getAsObject(mmkv.decodeString(key), LogInfo.class, null);
        } catch (Exception e) {
            return null;
        }
    }

    public int getLogCount() {
        return mmkv.decodeInt(COUNT, 0);
    }

    public synchronized void addLog(LogInfo log, boolean autoUid) {
        if (autoUid) {
            int index = getLogCount();
            index++;
            log.setUid(String.valueOf(index));
            mmkv.encode(log.getUid(), GsonUtil.toJson(log));
            mmkv.encode(COUNT, index);
        } else {
            mmkv.encode(log.getUid(), GsonUtil.toJson(log));
        }
        time = System.currentTimeMillis();
    }

    public void clearLog() {
        mmkv.clearAll();
        time = 0;
    }

    public void destroy() {
        MMKV.removeStorage(mmkv.mmapID());
    }

    public boolean recycle() {
        long current = System.currentTimeMillis();
        if (current - time > 5 * 60 * 1000) {
            mmkv.close();
            return true;
        }
        return false;
    }

    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        LogSave logSave = (LogSave) o;
        return getKey().equals(logSave.getKey());
    }

    @Override
    public int hashCode() {
        return getKey().hashCode();
    }

    @Override
    public ILazyTreeNodeData loadData(Object flag) {
        return getLog(String.valueOf(flag));
    }
}
