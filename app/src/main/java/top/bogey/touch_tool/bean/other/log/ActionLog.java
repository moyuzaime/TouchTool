package top.bogey.touch_tool.bean.other.log;

import android.graphics.Bitmap;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.normal.LoggerAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.GsonUtil;

public class ActionLog extends Log {
    private final int index;

    private final String topTaskId;
    private final String taskId;
    private final String actionId;
    private final boolean execute;
    private final Map<String, PinObject> values = new LinkedHashMap<>();

    public ActionLog(int index, Task task, Action action, boolean execute) {
        super(LogType.ACTION);
        this.index = index;
        this.execute = execute;
        topTaskId = task.getTopParent().getId();
        taskId = task.getId();
        actionId = action.getId();

        for (Pin pin : action.getPins()) {
            PinBase value = pin.getValue();
            if (value instanceof PinObject pinObject) {
                // 图片不再原样进日志，缩小一下
                if (value instanceof PinImage pinImage) {
                    Bitmap bitmap = DisplayUtil.safeScaleBitmap(pinImage.getImage(), 100, 100);
                    PinImage copy = (PinImage) pinImage.copy();
                    copy.setImage(bitmap);
                    values.put(pin.getId(), copy);
                } else values.put(pin.getId(), pinObject);
            }
        }

        StringBuilder builder = new StringBuilder();
        if (index == -1 && action instanceof LoggerAction loggerAction) {
            builder.append(loggerAction.getLogPin().getValue().toString());
        } else {
            builder.append("[").append(index).append("] ");
            builder.append(action.getFullDescription());
        }
        log = builder.toString();
    }

    public ActionLog(int index, Task task, Action action, boolean execute, String tag) {
        this(index, task, action, execute);
        if (tag != null && !tag.isEmpty()) log = tag + " " + log;
    }

    public ActionLog(JsonObject jsonObject) {
        super(jsonObject);
        index = GsonUtil.getAsInt(jsonObject, "index", -1);

        topTaskId = GsonUtil.getAsString(jsonObject, "topTaskId", "");
        taskId = GsonUtil.getAsString(jsonObject, "taskId", "");
        actionId = GsonUtil.getAsString(jsonObject, "actionId", "");
        execute = GsonUtil.getAsBoolean(jsonObject, "execute", true);
        values.putAll(GsonUtil.getAsObject(jsonObject, "values", TypeToken.getParameterized(LinkedHashMap.class, String.class, PinBase.class).getType(), new HashMap<>()));
    }

    // values 会被任务线程改写（syncLog），同时 UI 线程在刷新日志行时遍历，
    // 直接返回原表会抛 ConcurrentModificationException，所以读写都加锁、读方拿快照
    private final transient Object valuesLock = new Object();

    public void syncLog(ActionLog log) {
        if (taskId.equals(log.taskId) && actionId.equals(log.actionId)) {
            synchronized (valuesLock) {
                this.values.clear();
                this.values.putAll(log.values);
            }
        }
    }

    public int getIndex() {
        return index;
    }

    public String getTopTaskId() {
        return topTaskId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getActionId() {
        return actionId;
    }

    public boolean isExecute() {
        return execute;
    }

    public Map<String, PinObject> getValues() {
        synchronized (valuesLock) {
            return new LinkedHashMap<>(values);
        }
    }

    // payload 过大时（动作值里带图片）落库前清掉值，避免单条日志把库撑爆
    public void clearValues() {
        synchronized (valuesLock) {
            values.clear();
        }
    }
}
