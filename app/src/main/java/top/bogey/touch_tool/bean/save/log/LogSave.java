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

    private static final String ORDER = "LOG_ORDER";

    // 顺序表：存储 key 改成 GUID 后，顺序靠这张表维护
    public java.util.List<String> getOrder() {
        java.util.List<String> order = new java.util.ArrayList<>();
        String raw = mmkv.decodeString(ORDER, "");
        if (raw == null || raw.isEmpty()) {
            // 兼容旧数据：旧日志 key 就是 1..N
            int count = mmkv.decodeInt(COUNT, 0);
            for (int i = 1; i <= count; i++) order.add(String.valueOf(i));
            return order;
        }
        for (String s : raw.split(",")) {
            if (s.isEmpty()) continue;
            // 丢弃已被删除/覆盖的旧 key，避免出现「日志加载失败」空行
            if (!mmkv.containsKey(s)) continue;
            order.add(s);
        }
        return order;
    }

    private void saveOrder(java.util.List<String> order) {
        mmkv.encode(ORDER, String.join(",", order));
    }

    public String getUidAt(int index) {
        java.util.List<String> order = getOrder();
        if (index < 0 || index >= order.size()) return null;
        return order.get(index);
    }

    public int getPosition(String uid) {
        return getOrder().indexOf(uid);
    }
    public LogInfo getLog(String key) {
        try {
            return GsonUtil.getAsObject(mmkv.decodeString(key), LogInfo.class, null);
        } catch (Exception e) {
            return null;
        }
    }

    public int getLogCount() {
        return getOrder().size();
    }

    public synchronized void addLog(LogInfo log, boolean autoUid) {
        // 统一用 GUID 做 key：不再用序号覆盖 uid，避免 children 与 key 两套 id
        if (log.getUid() == null || log.getUid().isEmpty()) {
            log.setUid(java.util.UUID.randomUUID().toString());
        }
        mmkv.encode(log.getUid(), GsonUtil.toJson(log));
        java.util.List<String> order = getOrder();
        if (!order.contains(log.getUid())) {
            order.add(log.getUid());
            saveOrder(order);
        }
        // 兼容旧读取方：COUNT 继续维护
        mmkv.encode(COUNT, order.size());
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

    // 最后一次写入时间，供日志窗口判断是否需要刷新
    public long getLastTime() {
        return time;
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
        // flag 可能是旧数据的序号，也可能是新数据的 GUID
        if (flag instanceof Number) return getLog(String.valueOf(flag));
        return getLog(String.valueOf(flag));
    }
}
