package top.bogey.touch_tool.bean.save.task;

import android.net.Uri;

import com.tencent.mmkv.MMKV;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.other.Usage;
import top.bogey.touch_tool.bean.save.TagSaver;
import top.bogey.touch_tool.bean.save.log.LogSave;
import top.bogey.touch_tool.bean.save.log.LogSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.ui.InstantActivity;
import top.bogey.touch_tool.utils.AppUtil;

public class TaskSaver {
    private static TaskSaver instance;

    public static TaskSaver getInstance() {
        synchronized (TaskSaver.class) {
            if (instance == null) {
                instance = new TaskSaver();
            }
        }
        return instance;
    }

    private static final String EMPTY_TAG = MainApplication.getInstance().getString(R.string.tag_empty);

    public static boolean matchTag(String tag, List<String> tags) {
        boolean emptyTags = tags == null || tags.isEmpty();
        if (Objects.equals(tag, EMPTY_TAG) && emptyTags) {
            return true;
        }
        if (emptyTags) return false;
        return tags.contains(tag);
    }

    // 定时回收线程会遍历，任务线程会增删，必须用并发容器，否则会 ConcurrentModificationException
    private final Map<String, TaskSave> saves = new java.util.concurrent.ConcurrentHashMap<>();
    private final MMKV mmkv = MMKV.mmkvWithID("TASK_DB", MMKV.SINGLE_PROCESS_MODE);
    private final Set<TaskSaveListener> listeners = new HashSet<>();

    private TaskSaver() {
        recycle();
        load();
    }

    private void load() {
        String[] keys = mmkv.allKeys();
        if (keys == null) return;

        for (String key : keys) {
            TaskSave taskSave = new TaskSave(mmkv, key);
            saves.put(key, taskSave);
        }
    }

    private void recycle() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                saves.forEach((key, save) -> save.recycle());
            }
        }, 0, 5 * 60 * 1000);
    }

    public List<Task> getTasks() {
        List<Task> tasks = new ArrayList<>();
        saves.forEach((k, v) -> tasks.add(v.getTask()));
        return tasks;
    }

    public List<Task> getTasks(String tag) {
        List<Task> tasks = new ArrayList<>();
        saves.forEach((k, v) -> {
            Task task = v.getTask();
            if (matchTag(tag, task.getTags())) {
                tasks.add(task);
            }
        });
        return tasks;
    }

    public List<Task> getTasks(Class<? extends Action> actionClass) {
        List<Task> tasks = new ArrayList<>();
        saves.forEach((k, v) -> {
            Task task = v.getTask();
            List<Action> actions = task.getActions(actionClass);
            if (actions != null && !actions.isEmpty()) {
                tasks.add(task);
            }
        });
        return tasks;
    }

    public List<Task> getOrderTasks() {
        LinkedHashSet<Task> tasks = new LinkedHashSet<>();
        TagSaver tagSaver = TagSaver.getInstance();
        List<String> tags = new ArrayList<>(tagSaver.getTags());
        tags.add(EMPTY_TAG);
        for (String tag : tags) {
            List<Task> taskList = getTasks(tag);
            AppUtil.chineseSort(taskList, Task::getTitle);
            Map<String, Task> tagTasks = new LinkedHashMap<>();
            for (Task task : taskList) {
                tagTasks.put(task.getId(), task);
            }

            List<String> taskOrder = tagSaver.getTaskOrder(tag);
            if (taskOrder == null || taskOrder.isEmpty()) {
                tasks.addAll(tagTasks.values());
            } else {
                for (String taskId : taskOrder) {
                    Task task = tagTasks.remove(taskId);
                    if (task != null) {
                        tasks.add(task);
                    }
                }
                tasks.addAll(tagTasks.values());
            }
        }
        return new ArrayList<>(tasks);
    }

    public List<Task> getOrderTasks(Class<? extends Action> actionClass) {
        LinkedHashSet<Task> tasks = new LinkedHashSet<>();
        TagSaver tagSaver = TagSaver.getInstance();
        List<String> tags = new ArrayList<>(tagSaver.getTags());
        tags.add(EMPTY_TAG);
        for (String tag : tags) {
            List<Task> taskList = getTasks(tag);
            AppUtil.chineseSort(taskList, Task::getTitle);
            Map<String, Task> tagTasks = new LinkedHashMap<>();
            for (Task task : taskList) {
                tagTasks.put(task.getId(), task);
            }

            List<String> taskOrder = tagSaver.getTaskOrder(tag);
            if (taskOrder == null || taskOrder.isEmpty()) {
                for (Task task : tagTasks.values()) {
                    List<Action> actions = task.getActions(actionClass);
                    if (actions != null && !actions.isEmpty()) {
                        tasks.add(task);
                    }
                }
            } else {
                for (String taskId : taskOrder) {
                    Task task = tagTasks.remove(taskId);
                    if (task == null) continue;
                    List<Action> actions = task.getActions(actionClass);
                    if (actions != null && !actions.isEmpty()) {
                        tasks.add(task);
                    }
                }
                tagTasks.values().forEach(task -> {
                    List<Action> actions = task.getActions(actionClass);
                    if (actions != null && !actions.isEmpty()) {
                        tasks.add(task);
                    }
                });
            }
        }
        return new ArrayList<>(tasks);
    }

    public List<Task> searchTasks(String searchText) {
        List<Task> tasks = new ArrayList<>();

        Uri uri = Uri.parse(searchText);
        if ("tt".equals(uri.getScheme()) && "do_action".equals(uri.getHost())) {
            String taskId = uri.getQueryParameter(InstantActivity.TASK_ID);
            if (taskId != null) {
                tasks.add(getTask(taskId));
                return tasks;
            }
        }

        try {
            UUID.fromString(searchText);
            tasks.add(getTask(searchText));
            return tasks;
        } catch (Exception ignored) {
        }

        for (Map.Entry<String, TaskSave> entry : saves.entrySet()) {
            TaskSave v = entry.getValue();
            Task task = v.getTask();
            if (AppUtil.isStringContainsWithPinyin(task.getFullDescription(), searchText)) {
                tasks.add(task);
            }
        }
        return tasks;
    }

    public List<String> getTaskTags() {
        Set<String> tags = new HashSet<>();
        boolean emptyTag = false;
        for (Map.Entry<String, TaskSave> entry : saves.entrySet()) {
            TaskSave v = entry.getValue();
            Task task = v.getTask();
            if (task.getTags() != null && !task.getTags().isEmpty()) {
                tags.addAll(task.getTags());
            } else {
                emptyTag = true;
            }
        }
        List<String> list = new ArrayList<>();
        for (String tag : TagSaver.getInstance().getTags()) {
            if (tags.contains(tag)) {
                list.add(tag);
                tags.remove(tag);
            }
        }
        list.addAll(tags);
        if (emptyTag || saves.isEmpty()) list.add(EMPTY_TAG);
        return list;
    }

    public Task getTask(String taskId) {
        if (taskId == null || taskId.isEmpty()) return null;
        TaskSave taskSave = saves.get(taskId);
        if (taskSave == null) return null;
        return taskSave.getTask();
    }

    public Task downFindTask(Task context, String taskId) {
        if (context == null) return getTask(taskId);
        Task task = context.downFindTask(taskId);
        if (task == null) task = getTask(taskId);
        return task;
    }

    public Task upFindTask(Task context, String taskId) {
        if (context == null) return getTask(taskId);
        Task task = context.upFindTask(taskId);
        if (task == null) task = getTask(taskId);
        return task;
    }

    public Task getOriginTask(String taskId) {
        TaskSave taskSave = saves.get(taskId);
        if (taskSave == null) return null;
        return taskSave.getOriginTask();
    }

    public void saveTask(Task task) {
        TaskSave taskSave = saves.get(task.getId());
        if (taskSave == null) {
            saves.put(task.getId(), new TaskSave(mmkv, task));
            listeners.stream().filter(Objects::nonNull).forEach(v -> v.onCreate(task));
        } else {
            taskSave.setTask(task);
            listeners.stream().filter(Objects::nonNull).forEach(v -> v.onUpdate(task));
        }

        MainAccessibilityService service = MainApplication.getInstance().getService();
        if (service != null) {
            service.replaceAlarm(task);
            service.replaceBroadcast(task);
        }
    }

    public void removeTask(String id) {
        TaskSave taskSave = saves.remove(id);
        if (taskSave == null) return;
        Task task = taskSave.getTask();
        task.setEnable(false);
        listeners.stream().filter(Objects::nonNull).forEach(v -> v.onRemove(task));
        taskSave.remove();

        MainAccessibilityService service = MainApplication.getInstance().getService();
        if (service != null) {
            service.replaceAlarm(task);
            service.replaceBroadcast(task);
        }

        LogSave logSave = LogSaver.getInstance().getLogSave(id);
        if (logSave == null) return;
        logSave.destroy();
    }

    public List<Usage> getTaskUses(String id) {
        List<Usage> usages = new ArrayList<>();
        saves.forEach((k, v) -> {
            Task task = v.getTask();
            usages.addAll(task.getTaskUses(id));
        });
        return usages;
    }

    public void removeTasksTag(String tag) {
        saves.forEach((k, v) -> {
            Task task = v.getTask();
            task.removeInnerTag(tag);
            task.save();
        });
    }

    public void addListener(TaskSaveListener listener) {
        listeners.add(listener);
    }

    public void removeListener(TaskSaveListener listener) {
        listeners.remove(listener);
    }
}
