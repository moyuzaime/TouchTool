package top.bogey.touch_tool.bean.other.log;

import android.content.Context;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.GsonUtil;
import top.bogey.touch_tool.utils.tree.ILazyTreeNodeData;
import top.bogey.touch_tool.utils.tree.ITreeNodeData;

public class LogInfo implements ILazyTreeNodeData {
    private String uid;
    private final long time;
    private final Log log;
    private final List<String> children = new ArrayList<>();
    private final transient List<LogInfo> childrenLog = new ArrayList<>();

    public LogInfo(Log log) {
        uid = UUID.randomUUID().toString();
        time = System.currentTimeMillis();
        this.log = log;
    }

    public LogInfo(JsonObject jsonObject) {
        uid = GsonUtil.getAsString(jsonObject, "uid", "");
        time = GsonUtil.getAsLong(jsonObject, "time", 0);
        log = GsonUtil.getAsObject(jsonObject, "log", Log.class, new NormalLog(""));
        children.addAll(GsonUtil.getAsObject(jsonObject, "children", TypeToken.getParameterized(ArrayList.class, String.class).getType(), new ArrayList<>()));
    }

    public void syncLog(Log log) {
        if (this.log instanceof ActionLog actionLog && log instanceof ActionLog logLog) {
            actionLog.syncLog(logLog);
        }
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public long getTime() {
        return time;
    }

    public String getTime(Context context) {
        return AppUtil.formatDateTime(context, time, true, false);
    }

    public String getLog() {
        return log.getLog();
    }

    public Log getLogObject() {
        return log;
    }

    public void setChildren(List<String> children) {
        this.children.clear();
        this.children.addAll(children);
    }

    public void addChild(String child) {
        children.add(child);
    }

    @Override
    public List<Object> getChildrenFlags() {
        return new ArrayList<>(children);
    }

    public void addChild(LogInfo child) {
        childrenLog.add(child);
        addChild(child.getUid());
    }

    @Override
    public List<ITreeNodeData> getChildrenData() {
        return new ArrayList<>(childrenLog);
    }

    public static class LogInfoDeserialize implements JsonDeserializer<LogInfo> {
        @Override
        public LogInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new LogInfo(json.getAsJsonObject());
        }
    }
}
