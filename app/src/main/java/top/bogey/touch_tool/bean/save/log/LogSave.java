package top.bogey.touch_tool.bean.save.log;

import com.tencent.mmkv.MMKV;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import top.bogey.touch_tool.bean.other.log.LogInfo;
import top.bogey.touch_tool.utils.GsonUtil;
import top.bogey.touch_tool.utils.tree.ILazyTreeNodeData;
import top.bogey.touch_tool.utils.tree.ITreeNodeDataLoader;

// 单个任务的日志存档：以日志自身 uid 作为 MMKV key，顺序由 order 表维护
public class LogSave implements ITreeNodeDataLoader {
    private final static String COUNT = "count";   // 仅老数据使用，迁移后删除
    private final static String ORDER = "order";   // 逗号分隔的 uid 顺序表

    private final MMKV mmkv;
    private final String key;

    private long time = System.currentTimeMillis();

    public LogSave(String key, String path) {
        this.key = key;
        mmkv = MMKV.mmkvWithID(key, MMKV.MULTI_PROCESS_MODE, null, path);
        migrateIfNeeded();
    }

    // 老数据迁移：1..N 的 key 改写成日志自身的 uid（children 引用的就是它），并建立 order 表
    private synchronized void migrateIfNeeded() {
        String raw = mmkv.decodeString(ORDER, "");
        if (raw != null && !raw.isEmpty()) return;

        int count = mmkv.decodeInt(COUNT, 0);
        if (count <= 0) return;

        List<String> order = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String oldKey = String.valueOf(i);
            LogInfo info = getLog(oldKey);
            if (info == null) continue;
            String uid = info.getUid();
            if (uid == null || uid.isEmpty()) {
                uid = UUID.randomUUID().toString();
                info.setUid(uid);
            }
            // children 里存的就是 uid，这里保持不改写
            mmkv.encode(uid, GsonUtil.toJson(info));
            order.add(uid);
            if (!oldKey.equals(uid)) mmkv.removeValueForKey(oldKey);
        }
        mmkv.encode(ORDER, String.join(",", order));
        mmkv.removeValueForKey(COUNT);
    }

    public LogInfo getLog(String key) {
        try {
            return GsonUtil.getAsObject(mmkv.decodeString(key), LogInfo.class, null);
        } catch (Exception e) {
            return null;
        }
    }

    // 仅供排查：原始 JSON
    public String debugRaw(String key) {
        try {
            return mmkv.decodeString(key);
        } catch (Throwable e) {
            return null;
        }
    }

    public List<String> getOrder() {
        List<String> order = new ArrayList<>();
        String raw = mmkv.decodeString(ORDER, "");
        if (raw == null || raw.isEmpty()) return order;
        for (String s : raw.split(",")) {
            if (s.isEmpty()) continue;
            if (mmkv.decodeString(s) == null) continue;
            order.add(s);
        }
        return order;
    }

    private void saveOrder(List<String> order) {
        mmkv.encode(ORDER, String.join(",", order));
    }

    public String getUidAt(int index) {
        List<String> order = getOrder();
        if (index < 0 || index >= order.size()) return null;
        return order.get(index);
    }

    public int getPosition(String uid) {
        return getOrder().indexOf(uid);
    }

    public int getLogCount() {
        return getOrder().size();
    }

    public synchronized void addLog(LogInfo log, boolean autoUid) {
        // 始终用日志自身 uid（GUID）做 key，不再用序号覆盖，避免 children 引用失效
        if (log.getUid() == null || log.getUid().isEmpty()) {
            log.setUid(UUID.randomUUID().toString());
        }
        mmkv.encode(log.getUid(), GsonUtil.toJson(log));
        List<String> order = getOrder();
        if (!order.contains(log.getUid())) {
            order.add(log.getUid());
            saveOrder(order);
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
        return getLog(String.valueOf(flag));
    }
}
