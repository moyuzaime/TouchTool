package top.bogey.touch_tool.bean.save.log;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.bean.other.log.LogInfo;
import top.bogey.touch_tool.utils.AppLog;
import top.bogey.touch_tool.utils.GsonUtil;

// 日志事件表：一条日志一行，父子关系用 parent_uid 直接落库。
// 相比原来「MMKV 单 key 存 JSON + children 数组 + order 顺序表」：
// - 追加是 O(1) 的 INSERT，不再每条日志重写整张顺序表
// - 父子关系在写入时确定，不需要 updateLog 事后追认
// - 可按 id 分页查询，不必一次性解码全部日志
public class LogStore extends SQLiteOpenHelper {
    private final static String DB_NAME = "touch_tool_log.db";
    private final static int DB_VERSION = 1;

    private final static String TABLE_LOG = "log";
    private final static String TABLE_META = "log_meta";

    private static volatile LogStore instance;

    public static LogStore getInstance() {
        if (instance == null) {
            synchronized (LogStore.class) {
                if (instance == null) {
                    instance = new LogStore();
                }
            }
        }
        return instance;
    }

    private LogStore() {
        super(MainApplication.getInstance(), DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_LOG + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "task_id TEXT NOT NULL," +
                "uid TEXT NOT NULL," +
                "parent_uid TEXT," +
                "ts INTEGER NOT NULL," +
                "kind TEXT NOT NULL," +
                "action_id TEXT," +
                "payload TEXT NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_log_uid ON " + TABLE_LOG + "(task_id, uid)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_log_parent ON " + TABLE_LOG + "(task_id, parent_uid, id)");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_META + "(task_id TEXT PRIMARY KEY, migrated INTEGER NOT NULL DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    // 批量事务：每条日志一次 INSERT 都会触发一次提交开销，任务高频写日志时很明显。
    // 攒够 32 条或距上次落库超过 100ms 提交一次事务；任何读取前先 flush，界面才能立刻看到。
    private final static int BATCH_SIZE = 32;
    private final static long FLUSH_INTERVAL_MS = 100;

    private final java.util.ArrayDeque<Object[]> pendingRows = new java.util.ArrayDeque<>();
    private final Object pendingLock = new Object();
    private long lastFlushTime = 0;

    // ===== 写入 =====

    // 追加一条日志；parentUid 为空表示根日志（先入队，攒批后一次性入库）
    public long append(String taskId, LogInfo info, String parentUid) {
        if (taskId == null || info == null || info.getUid() == null) return -1;
        String payload = toPayload(info);
        if (payload == null) return -1;
        Object[] row = new Object[]{
                taskId,
                info.getUid(),
                parentUid,
                info.getTime(),
                info.getLogObject() == null ? "NORMAL" : info.getLogObject().getType().name(),
                actionIdOf(info),
                payload
        };
        synchronized (pendingLock) {
            pendingRows.add(row);
            long now = System.currentTimeMillis();
            if (pendingRows.size() >= BATCH_SIZE || now - lastFlushTime >= FLUSH_INTERVAL_MS) {
                flushLocked();
            }
        }
        return -1;
    }

    // 立即落库：任务结束、或任何读取之前调用
    public void flushNow() {
        synchronized (pendingLock) {
            flushLocked();
        }
    }

    private void flushLocked() {
        if (pendingRows.isEmpty()) return;
        lastFlushTime = System.currentTimeMillis();
        java.util.List<Object[]> rows = new java.util.ArrayList<>(pendingRows);
        pendingRows.clear();
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try {
                for (Object[] row : rows) {
                    ContentValues values = new ContentValues();
                    values.put("task_id", (String) row[0]);
                    values.put("uid", (String) row[1]);
                    values.put("parent_uid", (String) row[2]);
                    values.put("ts", (Long) row[3]);
                    values.put("kind", (String) row[4]);
                    values.put("action_id", (String) row[5]);
                    values.put("payload", (String) row[6]);
                    db.insertWithOnConflict(TABLE_LOG, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Throwable e) {
            AppLog.e("日志库", "批量写入失败", e);
        }
    }

    // 动作节点结束时值会补齐：只更新这一行的 payload，不动父子关系
    public void updatePayload(String taskId, LogInfo info) {
        flushNow();   // 读取前先把待写队列落库
        if (taskId == null || info == null || info.getUid() == null) return;
        String payload = toPayload(info);
        if (payload == null) return;
        ContentValues values = new ContentValues();
        values.put("payload", payload);
        try {
            getWritableDatabase().update(TABLE_LOG, values, "task_id = ? AND uid = ?",
                    new String[]{taskId, info.getUid()});
        } catch (Throwable e) {
            AppLog.e("日志库", "更新日志失败", e);
        }
    }

    // ===== 查询 =====

    public LogInfo load(String taskId, String uid) {
        flushNow();   // 读取前先把待写队列落库
        if (taskId == null || uid == null) return null;
        try (Cursor cursor = getReadableDatabase().query(TABLE_LOG, new String[]{"payload"},
                "task_id = ? AND uid = ?", new String[]{taskId, uid}, null, null, null)) {
            if (!cursor.moveToFirst()) return null;
            LogInfo info = parse(cursor.getString(0));
            if (info != null) {
                // 子节点由数据库现查，payload 里不再冗余 children
                info.setChildren(childUids(taskId, uid));
            }
            return info;
        } catch (Throwable e) {
            AppLog.e("日志库", "读取日志失败", e);
            return null;
        }
    }

    public List<String> childUids(String taskId, String parentUid) {
        flushNow();   // 读取前先把待写队列落库
        List<String> uids = new ArrayList<>();
        if (taskId == null) return uids;
        String where = parentUid == null ? "task_id = ? AND parent_uid IS NULL" : "task_id = ? AND parent_uid = ?";
        String[] args = parentUid == null ? new String[]{taskId} : new String[]{taskId, parentUid};
        try (Cursor cursor = getReadableDatabase().query(TABLE_LOG, new String[]{"uid"}, where, args, null, null, "id ASC")) {
            while (cursor.moveToNext()) uids.add(cursor.getString(0));
        } catch (Throwable e) {
            AppLog.e("日志库", "读取子节点失败", e);
        }
        return uids;
    }

    // 根日志 uid，按 id 倒序取最后 limit 条（分页窗口用）
    public List<String> rootUids(String taskId, int limit) {
        flushNow();   // 读取前先把待写队列落库
        List<String> uids = new ArrayList<>();
        if (taskId == null) return uids;
        String sql = "SELECT uid FROM " + TABLE_LOG + " WHERE task_id = ? AND parent_uid IS NULL ORDER BY id DESC LIMIT ?";
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, new String[]{taskId, String.valueOf(limit)})) {
            while (cursor.moveToNext()) uids.add(cursor.getString(0));
        } catch (Throwable e) {
            AppLog.e("日志库", "读取根日志失败", e);
        }
        java.util.Collections.reverse(uids);
        return uids;
    }

    // 早于某个 id 的根日志（"加载更多"）
    public List<String> rootUidsBefore(String taskId, long beforeId, int limit) {
        flushNow();   // 读取前先把待写队列落库
        List<String> uids = new ArrayList<>();
        if (taskId == null) return uids;
        String sql = "SELECT uid FROM " + TABLE_LOG + " WHERE task_id = ? AND parent_uid IS NULL AND id < ? ORDER BY id DESC LIMIT ?";
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, new String[]{taskId, String.valueOf(beforeId), String.valueOf(limit)})) {
            while (cursor.moveToNext()) uids.add(cursor.getString(0));
        } catch (Throwable e) {
            AppLog.e("日志库", "读取历史根日志失败", e);
        }
        java.util.Collections.reverse(uids);
        return uids;
    }

    public int count(String taskId) {
        flushNow();   // 读取前先把待写队列落库
        if (taskId == null) return 0;
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + TABLE_LOG + " WHERE task_id = ?", new String[]{taskId})) {
            if (cursor.moveToFirst()) return cursor.getInt(0);
        } catch (Throwable e) {
            AppLog.e("日志库", "统计日志失败", e);
        }
        return 0;
    }

    public void clear(String taskId) {
        if (taskId == null) return;
        synchronized (pendingLock) { pendingRows.clear(); }   // 清空时丢掉未落库的行
        try {
            getWritableDatabase().delete(TABLE_LOG, "task_id = ?", new String[]{taskId});
        } catch (Throwable e) {
            AppLog.e("日志库", "清空日志失败", e);
        }
    }

    // ===== 旧 MMKV 存档迁移（每个任务只做一次）=====

    public void migrateIfNeeded(String taskId, LogSave legacy) {
        if (taskId == null || legacy == null) return;
        if (isMigrated(taskId)) return;
        try {
            List<String> order = legacy.getOrder();
            if (order.isEmpty()) {
                markMigrated(taskId);
                return;
            }
            // 先建 uid → parent 映射，再逐条插入（旧数据只有 children 数组，没有 parent 字段）
            java.util.Map<String, String> parentOf = new java.util.HashMap<>();
            java.util.Map<String, LogInfo> infos = new java.util.LinkedHashMap<>();
            for (String uid : order) {
                LogInfo info = legacy.getLog(uid);
                if (info == null) continue;
                infos.put(uid, info);
                for (Object flag : info.getChildrenFlags()) {
                    parentOf.put(String.valueOf(flag), uid);
                }
            }
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try {
                java.util.List<String> ordered = new java.util.ArrayList<>(infos.keySet());
                ordered.sort((a, b) -> Long.compare(infos.get(a).getTime(), infos.get(b).getTime()));
                for (String uid : ordered) {
                    LogInfo info = infos.get(uid);
                    ContentValues values = new ContentValues();
                    values.put("task_id", taskId);
                    values.put("uid", uid);
                    values.put("parent_uid", parentOf.get(uid));
                    values.put("ts", info.getTime());
                    values.put("kind", info.getLogObject() == null ? "NORMAL" : info.getLogObject().getType().name());
                    values.put("action_id", actionIdOf(info));
                    values.put("payload", toPayload(info));
                    db.insertWithOnConflict(TABLE_LOG, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            markMigrated(taskId);
        } catch (Throwable e) {
            AppLog.e("日志库", "迁移旧日志失败", e);
        }
    }

    private boolean isMigrated(String taskId) {
        try (Cursor cursor = getReadableDatabase().query(TABLE_META, new String[]{"migrated"},
                "task_id = ?", new String[]{taskId}, null, null, null)) {
            return cursor.moveToFirst() && cursor.getInt(0) == 1;
        } catch (Throwable e) {
            return false;
        }
    }

    private void markMigrated(String taskId) {
        ContentValues values = new ContentValues();
        values.put("task_id", taskId);
        values.put("migrated", 1);
        try {
            getWritableDatabase().insertWithOnConflict(TABLE_META, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Throwable e) {
            AppLog.e("日志库", "标记迁移失败", e);
        }
    }

    // ===== payload 编解码 =====

    // 只存自身内容，children 由 parent_uid 推导，避免两处重复；
    // 拷贝一份再清 children，避免和 UI 线程读同一个对象时互相影响
    private String toPayload(LogInfo info) {
        try {
            LogInfo copy = GsonUtil.copy(info, LogInfo.class);
            if (copy == null) return null;
            copy.setChildren(new ArrayList<>());
            return GsonUtil.toJson(copy);
        } catch (Throwable e) {
            AppLog.e("日志库", "序列化日志失败", e);
            return null;
        }
    }

    private LogInfo parse(String payload) {
        try {
            return GsonUtil.getAsObject(payload, LogInfo.class, null);
        } catch (Throwable e) {
            AppLog.e("日志库", "反序列化日志失败", e);
            return null;
        }
    }

    private String actionIdOf(LogInfo info) {
        if (info.getLogObject() instanceof top.bogey.touch_tool.bean.other.log.ActionLog actionLog) {
            return actionLog.getActionId();
        }
        return null;
    }
}
