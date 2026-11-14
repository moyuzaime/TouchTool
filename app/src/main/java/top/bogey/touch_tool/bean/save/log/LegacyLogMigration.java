package top.bogey.touch_tool.bean.save.log;

import com.tencent.mmkv.MMKV;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.bean.other.log.LogInfo;
import top.bogey.touch_tool.utils.AppLog;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.GsonUtil;

// 旧版把每次任务的日志存在 MMKV（cache/log/<任务id>，key 为日志 uid，另有 order 顺序表）。
// 这里负责把残留存档一次性导入 SQLite，全部成功后再删掉旧文件，避免升级后历史日志丢失。
public class LegacyLogMigration {
    private final static String LEGACY_MARK = "__legacy_purged__";
    private final static String ORDER_KEY = "order";

    public static String legacyDir() {
        return MainApplication.getInstance().getCacheDir().getAbsolutePath() + "/" + AppUtil.LOG_DIR_NAME;
    }

    // 迁移 + 清理；返回是否已完成（已完成时后续调用直接返回）
    public static synchronized boolean runOnce() {
        try {
            if (LogStore.getInstance().isMigrated(LEGACY_MARK)) return true;
            File dir = new File(legacyDir());
            if (!dir.exists()) {
                LogStore.getInstance().markMigrated(LEGACY_MARK);
                return true;
            }
            File[] files = dir.listFiles();
            if (files == null) return false;

            List<String> taskIds = new ArrayList<>();
            for (File file : files) {
                String name = file.getName();
                // 任务存档就是一个以任务 id 命名的 MMKV 文件（另有 .crc 伴生文件）
                if (name.endsWith(".crc") || name.endsWith(".lock")) continue;
                if (name.length() != 36) continue;
                taskIds.add(name);
            }

            boolean allOk = true;
            for (String taskId : taskIds) {
                if (LogStore.getInstance().isMigrated(taskId)) continue;
                if (!migrateTask(taskId, dir)) allOk = false;
            }
            if (!allOk) {
                AppLog.e("日志库", "有任务迁移失败，保留旧存档不删除");
                return false;
            }
            // 全部迁移完成：删掉旧文件（目录保留，便于后续排查）
            for (File file : files) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
            LogStore.getInstance().markMigrated(LEGACY_MARK);
            return true;
        } catch (Throwable e) {
            AppLog.e("日志库", "旧日志迁移失败", e);
            return false;
        }
    }

    private static boolean migrateTask(String taskId, File dir) {
        MMKV mmkv = null;
        try {
            mmkv = MMKV.mmkvWithID(taskId, MMKV.MULTI_PROCESS_MODE, null, dir.getAbsolutePath());
            String raw = mmkv.decodeString(ORDER_KEY, "");
            if (raw == null || raw.isEmpty()) {
                LogStore.getInstance().markMigrated(taskId);
                return true;
            }
            Map<String, String> parentOf = new HashMap<>();
            Map<String, LogInfo> infos = new LinkedHashMap<>();
            for (String uid : raw.split(",")) {
                if (uid.isEmpty()) continue;
                String json = mmkv.decodeString(uid);
                if (json == null) continue;
                LogInfo info = GsonUtil.getAsObject(json, LogInfo.class, null);
                if (info == null) continue;
                infos.put(uid, info);
                for (Object flag : info.getChildrenFlags()) {
                    parentOf.put(String.valueOf(flag), uid);
                }
            }
            List<String> ordered = new ArrayList<>(infos.keySet());
            ordered.sort((a, b) -> Long.compare(infos.get(a).getTime(), infos.get(b).getTime()));
            List<Object[]> rows = new ArrayList<>();
            for (String uid : ordered) {
                LogInfo info = infos.get(uid);
                Object[] row = new Object[]{
                        uid,
                        parentOf.get(uid),
                        info.getTime(),
                        info.getLogObject() == null ? "NORMAL" : info.getLogObject().getType().name(),
                        info.getLogObject() instanceof top.bogey.touch_tool.bean.other.log.ActionLog actionLog
                                ? actionLog.getActionId() : null,
                        GsonUtil.toJson(info)
                };
                rows.add(row);
            }
            LogStore.getInstance().migrateRows(taskId, rows);
            return LogStore.getInstance().isMigrated(taskId);
        } catch (Throwable e) {
            AppLog.e("日志库", "迁移任务 " + taskId + " 失败", e);
            return false;
        } finally {
            if (mmkv != null) {
                try {
                    mmkv.close();
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
