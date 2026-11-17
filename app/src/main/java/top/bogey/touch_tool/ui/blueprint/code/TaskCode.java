package top.bogey.touch_tool.ui.blueprint.code;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionInfo;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinType;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.bean.task.Variable;
import top.bogey.touch_tool.utils.GsonUtil;

// 任务与 JSON 文本互转：生成整任务文本（标题、标签、变量、子任务、动作），校验后回写
public class TaskCode {
    private TaskCode() {
    }

    // 生成当前任务的文本：根任务 id 只读，其余字段可改
    public static String dump(Task task) {
        return GsonUtil.prettyGson.toJson(dumpTask(task));
    }

    // 只做缩进格式化，不校验内容
    public static String format(String text) throws TaskCodeException {
        JsonElement element = parse(text);
        return GsonUtil.prettyGson.toJson(element);
    }

    // 校验文本并把整个任务写回；任何一步校验失败都不改动任务
    public static void apply(Task task, String text) throws TaskCodeException {
        JsonElement root = parse(text);
        if (!root.isJsonObject()) throw new TaskCodeException("最外层必须是任务对象 { … }");

        Set<String> taskIds = new HashSet<>();
        Set<String> variableIds = new HashSet<>();
        ParsedTask parsed = parseTask(root.getAsJsonObject(), task.getId(), true, taskIds, variableIds);
        applyTask(task, parsed);
    }

    // 逐层解析并校验，不触碰运行中的任务对象
    private static ParsedTask parseTask(JsonObject object, String rootId, boolean root, Set<String> taskIds, Set<String> variableIds) throws TaskCodeException {
        String id = getString(object, "id");
        if (id == null || id.isEmpty()) throw new TaskCodeException(root ? "顶层任务缺少 id" : "子任务缺少 id");
        if (!taskIds.add(id)) throw new TaskCodeException("任务 id 重复：" + id);
        if (root && !id.equals(rootId)) {
            throw new TaskCodeException("顶层任务的 id 必须保持 " + rootId + "（改 id 会断掉任务引用）");
        }

        ParsedTask parsed = new ParsedTask();
        parsed.id = id;
        parsed.title = getStringOrDefault(object, "title", "");
        parsed.description = getStringOrDefault(object, "description", "");
        parsed.flag = getIntOrDefault(object, "flag", 0);
        parsed.tags = getStrings(object, "tags");
        parsed.variables = parseVariables(object, variableIds);

        JsonElement actionsElement = object.get("actions");
        if (actionsElement == null || !actionsElement.isJsonArray()) throw new TaskCodeException("缺少动作数组 actions");
        parsed.actions = parseActions(actionsElement.getAsJsonArray());

        JsonElement tasksElement = object.get("tasks");
        if (tasksElement != null && !tasksElement.isJsonNull()) {
            if (!tasksElement.isJsonArray()) throw new TaskCodeException("tasks 必须是数组");
            for (JsonElement childElement : tasksElement.getAsJsonArray()) {
                if (!childElement.isJsonObject()) throw new TaskCodeException("子任务不是 JSON 对象");
                String childId = getString(childElement.getAsJsonObject(), "id");
                try {
                    parsed.tasks.add(parseTask(childElement.getAsJsonObject(), null, false, taskIds, variableIds));
                } catch (TaskCodeException e) {
                    String prefix = childId == null || childId.isEmpty() ? "子任务" : "子任务 " + childId;
                    throw new TaskCodeException(e.getIndex(), prefix + " 里：" + e.getMessage());
                }
            }
        }
        return parsed;
    }

    private static List<Variable> parseVariables(JsonObject object, Set<String> variableIds) throws TaskCodeException {
        List<Variable> variables = new ArrayList<>();
        JsonElement element = object.get("variables");
        if (element == null || element.isJsonNull()) return variables;
        if (!element.isJsonArray()) throw new TaskCodeException("variables 必须是数组");
        for (JsonElement item : element.getAsJsonArray()) {
            if (!item.isJsonObject()) throw new TaskCodeException("变量不是 JSON 对象");
            String varId = getString(item.getAsJsonObject(), "id");
            if (varId == null || varId.isEmpty()) throw new TaskCodeException("变量缺少 id");
            if (!variableIds.add(varId)) throw new TaskCodeException("变量 id 重复：" + varId);

            Variable variable;
            try {
                variable = GsonUtil.prettyGson.fromJson(item, Variable.class);
            } catch (Exception e) {
                throw new TaskCodeException("变量 " + varId + " 反序列化失败：" + shortError(e));
            }
            if (variable == null || !varId.equals(variable.getId())) {
                throw new TaskCodeException("变量 " + varId + " 反序列化失败，检查字段名与取值");
            }
            variables.add(variable);
        }
        return variables;
    }

    private static void applyTask(Task task, ParsedTask parsed) {
        task.setTitle(parsed.title);
        task.setDescription(parsed.description);
        task.setFlag(parsed.flag);
        task.setTags(new ArrayList<>(parsed.tags));

        // 动作整体替换
        for (Action action : new ArrayList<>(task.getActions())) {
            task.removeAction(action.getId());
        }
        for (Action action : parsed.actions) {
            task.addAction(action);
        }

        // 变量按 id 复用原对象，只同步可改字段，避免外部还拿着旧对象
        Map<String, Variable> existingVariables = new HashMap<>();
        for (Variable variable : task.getVariables()) existingVariables.put(variable.getId(), variable);
        for (Variable variable : new ArrayList<>(task.getVariables())) {
            task.removeVariable(variable.getId());
        }
        for (Variable variable : parsed.variables) {
            Variable live = existingVariables.get(variable.getId());
            if (live == null) {
                task.addVariable(variable);
                continue;
            }
            live.setTitle(variable.getTitle());
            live.setDescription(variable.getDescription());
            live.setValue(variable.getValue());
            live.setSaveValue(variable.getSaveValue());
            live.setTags(new ArrayList<>(variable.getTags()));
            task.addVariable(live);
        }

        // 子任务按 id 复用原对象，文本顺序即新的排列顺序；文本里没有的移除
        Map<String, Task> existingTasks = new HashMap<>();
        for (Task child : task.getTasks()) existingTasks.put(child.getId(), child);
        for (Task child : new ArrayList<>(task.getTasks())) {
            task.removeTask(child.getId());
        }
        for (ParsedTask child : parsed.tasks) {
            Task live = existingTasks.get(child.id);
            if (live == null) {
                live = new Task();
                live.setId(child.id);
            }
            task.addTask(live);
            applyTask(live, child);
        }
    }

    private static List<Action> parseActions(JsonArray array) throws TaskCodeException {
        // 第一遍：结构、类型、id、针脚值、连线目标
        Map<String, JsonObject> actionObjects = new HashMap<>();
        Map<String, Integer> actionIndexes = new HashMap<>();
        Map<String, String> pinOwners = new HashMap<>();
        Map<String, JsonObject> pinObjects = new HashMap<>();
        for (int i = 0; i < array.size(); i++) {
            int index = i + 1;
            JsonElement element = array.get(i);
            if (!element.isJsonObject()) throw new TaskCodeException(index, "不是 JSON 对象");
            JsonObject object = element.getAsJsonObject();

            String typeName = getString(object, "type");
            ActionType type = parseType(typeName);
            if (type == null) throw new TaskCodeException(index, "缺少 type 或类型无法识别：" + typeName);
            if (ActionInfo.getActionInfo(type) == null) throw new TaskCodeException(index, "类型未注册：" + typeName);

            String actionId = getString(object, "id");
            if (actionId == null || actionId.isEmpty()) throw new TaskCodeException(index, "缺少 id");
            if (actionObjects.containsKey(actionId)) throw new TaskCodeException(index, "id 与前面的动作重复：" + actionId);
            actionObjects.put(actionId, object);
            actionIndexes.put(actionId, index);

            JsonElement pinsElement = object.get("pins");
            if (pinsElement == null || !pinsElement.isJsonArray() || pinsElement.getAsJsonArray().isEmpty()) {
                throw new TaskCodeException(index, "缺少针脚数组 pins");
            }
            Set<String> actionPinIds = new HashSet<>();
            for (JsonElement pinElement : pinsElement.getAsJsonArray()) {
                if (!pinElement.isJsonObject()) throw new TaskCodeException(index, "针脚不是 JSON 对象");
                JsonObject pinObject = pinElement.getAsJsonObject();
                String pinId = getString(pinObject, "id");
                if (pinId == null || pinId.isEmpty()) throw new TaskCodeException(index, "针脚缺少 id");
                if (!actionPinIds.add(pinId)) throw new TaskCodeException(index, "针脚 id 重复：" + pinId);
                if (pinObjects.containsKey(pinId)) throw new TaskCodeException(index, "针脚 id 与其它动作的针脚重复：" + pinId);
                pinObjects.put(pinId, pinObject);
                pinOwners.put(pinId, actionId);

                JsonElement valueElement = pinObject.get("value");
                if (valueElement == null || !valueElement.isJsonObject()) {
                    throw new TaskCodeException(index, "针脚 " + pinId + " 缺少 value");
                }
                String valueType = getString(valueElement.getAsJsonObject(), "type");
                if (parsePinType(valueType) == null) {
                    throw new TaskCodeException(index, "针脚 " + pinId + " 的值类型无法识别：" + valueType);
                }
            }
        }

        // 第二遍：连线必须指向存在的针脚，且两端都要写、方向相反
        for (Map.Entry<String, JsonObject> entry : actionObjects.entrySet()) {
            String actionId = entry.getKey();
            int index = actionIndexes.get(actionId);
            for (JsonElement pinElement : entry.getValue().getAsJsonArray("pins")) {
                JsonObject pinObject = pinElement.getAsJsonObject();
                String pinId = getString(pinObject, "id");
                JsonElement linksElement = pinObject.get("links");
                if (linksElement == null || linksElement.isJsonNull()) continue;
                if (!linksElement.isJsonObject()) throw new TaskCodeException(index, "针脚 " + pinId + " 的 links 不是对象");
                for (Map.Entry<String, JsonElement> link : linksElement.getAsJsonObject().entrySet()) {
                    String targetPinId = link.getKey();
                    String targetActionId = link.getValue().isJsonPrimitive() ? link.getValue().getAsString() : null;
                    if (targetActionId == null || !actionObjects.containsKey(targetActionId)) {
                        throw new TaskCodeException(index, "针脚 " + pinId + " 的连线指向不存在的动作：" + targetActionId);
                    }
                    JsonObject targetPinObject = pinObjects.get(targetPinId);
                    if (targetPinObject == null || !targetActionId.equals(pinOwners.get(targetPinId))) {
                        throw new TaskCodeException(index, "针脚 " + pinId + " 的连线指向不存在的针脚：" + targetPinId);
                    }
                    if (targetActionId.equals(actionId)) {
                        throw new TaskCodeException(index, "针脚 " + pinId + " 连到了同一张卡的动作上");
                    }
                    if (isOut(pinObject) == isOut(targetPinObject)) {
                        throw new TaskCodeException(index, "针脚 " + pinId + " 与 " + targetPinId + " 方向相同，不能连线");
                    }
                    // 连线是成对的，对端必须写回同一条，否则运行期取不到值
                    JsonObject backLinks = targetPinObject.has("links") && targetPinObject.get("links").isJsonObject()
                            ? targetPinObject.getAsJsonObject("links") : null;
                    String backActionId = backLinks != null && backLinks.has(pinId) ? getString(backLinks, pinId) : null;
                    if (!actionId.equals(backActionId)) {
                        throw new TaskCodeException(index, "针脚 " + pinId + " 与 " + targetPinId + " 的连线不成对，对端也要写回 links");
                    }
                }
            }
        }

        // 第三遍：真正反序列化，类型或字段不对会在这里落空
        List<Action> actions;
        try {
            actions = GsonUtil.prettyGson.fromJson(array, new TypeToken<List<Action>>() {
            }.getType());
        } catch (Exception e) {
            throw new TaskCodeException("动作反序列化失败：" + shortError(e));
        }
        if (actions == null || actions.size() != array.size()) {
            throw new TaskCodeException("动作反序列化失败，共 " + array.size() + " 个，成功 " + (actions == null ? 0 : actions.size()) + " 个");
        }

        // 第四遍：动作的针脚必须与文本一一对应，避免被静默丢弃或补成默认针脚
        for (int i = 0; i < actions.size(); i++) {
            int index = i + 1;
            Action action = actions.get(i);
            if (action == null) throw new TaskCodeException(index, "反序列化为空，检查字段名与取值");
            JsonObject object = array.get(i).getAsJsonObject();
            String actionId = getString(object, "id");
            if (!actionId.equals(action.getId())) throw new TaskCodeException(index, "动作 id 未按文本生效：" + actionId);

            Set<String> expectPinIds = new HashSet<>();
            for (JsonElement pinElement : object.getAsJsonArray("pins")) {
                expectPinIds.add(getString(pinElement.getAsJsonObject(), "id"));
            }
            Set<String> actualPinIds = new HashSet<>();
            for (Pin pin : action.getPins()) {
                if (!actualPinIds.add(pin.getId())) throw new TaskCodeException(index, "针脚 id 重复：" + pin.getId());
            }
            if (!actualPinIds.equals(expectPinIds)) {
                Set<String> missing = new HashSet<>(expectPinIds);
                missing.removeAll(actualPinIds);
                Set<String> extra = new HashSet<>(actualPinIds);
                extra.removeAll(expectPinIds);
                StringBuilder detail = new StringBuilder("针脚与文本不一致");
                if (!missing.isEmpty()) detail.append("，未被接受：").append(join(missing));
                if (!extra.isEmpty()) detail.append("，被补成默认针脚：").append(join(extra));
                throw new TaskCodeException(index, detail.toString());
            }

            for (Pin pin : action.getPins()) {
                JsonObject pinObject = pinObjects.get(pin.getId());
                Map<String, String> expectLinks = new HashMap<>();
                JsonElement linksElement = pinObject.get("links");
                if (linksElement != null && linksElement.isJsonObject()) {
                    for (Map.Entry<String, JsonElement> link : linksElement.getAsJsonObject().entrySet()) {
                        expectLinks.put(link.getKey(), link.getValue().getAsString());
                    }
                }
                if (!expectLinks.equals(pin.getLinks())) {
                    throw new TaskCodeException(index, "针脚 " + pin.getId() + " 的连线未被接受，检查两端的类型与方向");
                }
            }
        }
        return actions;
    }

    // 生成一个任务的文本结构：id、标题、描述、标记、标签、变量、子任务、动作
    private static JsonObject dumpTask(Task task) {
        JsonObject object = new JsonObject();
        object.addProperty("id", task.getId());
        object.addProperty("title", task.getTitle() == null ? "" : task.getTitle());
        object.addProperty("description", task.getDescription() == null ? "" : task.getDescription());
        object.addProperty("flag", task.getFlag());

        JsonArray tags = new JsonArray();
        for (String tag : task.getTags()) tags.add(tag);
        object.add("tags", tags);

        JsonArray variables = new JsonArray();
        for (Variable variable : task.getVariables()) {
            JsonObject variableObject = GsonUtil.prettyGson.toJsonTree(variable).getAsJsonObject();
            // uid 不在文本里：变量按 id 复用原对象，改 uid 也不会生效
            variableObject.remove("uid");
            variables.add(variableObject);
        }
        object.add("variables", variables);

        JsonArray tasks = new JsonArray();
        for (Task child : task.getTasks()) tasks.add(dumpTask(child));
        object.add("tasks", tasks);

        object.add("actions", GsonUtil.prettyGson.toJsonTree(new ArrayList<>(task.getActions())));
        return object;
    }

    private static JsonElement parse(String text) throws TaskCodeException {
        if (text == null || text.trim().isEmpty()) throw new TaskCodeException("内容为空");
        try {
            return JsonParser.parseString(text);
        } catch (Exception e) {
            throw new TaskCodeException("JSON 语法错误：" + shortError(e));
        }
    }

    // Gson 的报错带异常类名和文档链接，只保留位置信息
    private static String shortError(Throwable e) {
        String message = e.getMessage();
        if (message == null || message.isEmpty()) return e.getClass().getSimpleName();

        int see = message.indexOf("See http");
        if (see > 0) message = message.substring(0, see);
        message = message.replace("\r", " ").replace("\n", " ").trim();

        int colon = message.indexOf(": ");
        if (colon > 0 && colon < 80 && message.substring(0, colon).contains("Exception")) {
            message = message.substring(colon + 2).trim();
        }
        return message;
    }

    private static ActionType parseType(String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            return ActionType.valueOf(name);
        } catch (Exception e) {
            return null;
        }
    }

    private static PinType parsePinType(String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            return PinType.valueOf(name);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isOut(JsonObject pinObject) {
        JsonElement out = pinObject.get("out");
        return out != null && out.isJsonPrimitive() && out.getAsBoolean();
    }

    private static String getString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) return null;
        try {
            return element.getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String getStringOrDefault(JsonObject object, String key, String defaultValue) throws TaskCodeException {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return defaultValue;
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new TaskCodeException(key + " 必须是字符串");
        }
        return element.getAsString();
    }

    private static int getIntOrDefault(JsonObject object, String key, int defaultValue) throws TaskCodeException {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return defaultValue;
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new TaskCodeException(key + " 必须是数字");
        }
        return element.getAsInt();
    }

    private static List<String> getStrings(JsonObject object, String key) throws TaskCodeException {
        List<String> strings = new ArrayList<>();
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) return strings;
        if (!element.isJsonArray()) throw new TaskCodeException(key + " 必须是字符串数组");
        for (JsonElement item : element.getAsJsonArray()) {
            if (!item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) {
                throw new TaskCodeException(key + " 里只能是字符串");
            }
            strings.add(item.getAsString());
        }
        return strings;
    }

    private static String join(Set<String> ids) {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (String id : ids) {
            if (count > 0) builder.append("、");
            builder.append(id);
            if (++count >= 5 && ids.size() > 5) {
                builder.append(" 等 ").append(ids.size()).append(" 个");
                break;
            }
        }
        return builder.toString();
    }

    // 解析出来的任务结构，校验全部通过后才用于回写
    private static class ParsedTask {
        private String id;
        private String title;
        private String description;
        private int flag;
        private List<String> tags = new ArrayList<>();
        private List<Variable> variables = new ArrayList<>();
        private List<Action> actions = new ArrayList<>();
        private final List<ParsedTask> tasks = new ArrayList<>();
    }

    // 带有动作序号的校验错误，序号从 1 开始，0 表示整体错误
    public static class TaskCodeException extends Exception {
        private final int index;

        public TaskCodeException(String message) {
            this(0, message);
        }

        public TaskCodeException(int index, String message) {
            super(message);
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }
}
