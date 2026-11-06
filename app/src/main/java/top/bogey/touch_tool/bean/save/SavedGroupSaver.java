package top.bogey.touch_tool.bean.save;

import com.google.gson.reflect.TypeToken;
import com.tencent.mmkv.MMKV;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.utils.GsonUtil;

/**
 * 卡片组按任务 id 存在独立的 MMKV 里：属于视图书签，不写进任务数据。
 */
public class SavedGroupSaver {
    private static final String DB_NAME = "SAVED_GROUP_DB";

    private static SavedGroupSaver instance;

    public static SavedGroupSaver getInstance() {
        synchronized (SavedGroupSaver.class) {
            if (instance == null) instance = new SavedGroupSaver();
        }
        return instance;
    }

    private final MMKV mmkv = MMKV.mmkvWithID(DB_NAME, MMKV.SINGLE_PROCESS_MODE);

    public List<SavedCardGroup> getGroups(String taskId) {
        if (taskId == null) return new ArrayList<>();
        String json = mmkv.decodeString(taskId);
        if (json == null || json.isEmpty()) return new ArrayList<>();
        List<SavedCardGroup> groups = GsonUtil.getAsObject(json, new TypeToken<List<SavedCardGroup>>() {
        }.getType(), null);
        return groups == null ? new ArrayList<>() : groups;
    }

    public void saveGroups(String taskId, List<SavedCardGroup> groups) {
        if (taskId == null) return;
        if (groups == null || groups.isEmpty()) mmkv.remove(taskId);
        else mmkv.encode(taskId, GsonUtil.toJson(groups));
    }
}
