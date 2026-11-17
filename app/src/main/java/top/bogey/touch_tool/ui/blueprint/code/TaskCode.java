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
import top.bogey.touch_tool.utils.GsonUtil;

// 任务动作清单与 JSON 文本互转：生成文本，校验后整体替换任务的动作
public class TaskCode {
    private TaskCode() {
    }

    // 生成当前任务的动作清单文本
    public static String dump(Task task) {
        return GsonUtil.toPrettyJson(new ArrayList<>(task.getActions()));
    }

    // 只做缩进格式化，不校验动作内容
    public static String format(String text) throws TaskCodeException {
        JsonElement element = parse(text);
        return GsonUtil.prettyGson.toJson(element);
    }

    // 校验文本并把动作整体写回任务；校验不通过时不改动任务
    public static void apply(Task task, String text) throws TaskCodeException {
        JsonArray array = parseActions(text);

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
            actions = GsonUtil.prettyGson.fromJson(text, new TypeToken<List<Action>>() {
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

        // 校验全部通过才替换，失败时任务保持原样
        for (Action action : new ArrayList<>(task.getActions())) {
            task.removeAction(action.getId());
        }
        for (Action action : actions) {
            task.addAction(action);
        }
    }

    private static JsonArray parseActions(String text) throws TaskCodeException {
        JsonElement root = parse(text);
        if (!root.isJsonArray()) throw new TaskCodeException("最外层必须是动作数组，例如 [ {…}, {…} ]");
        JsonArray array = root.getAsJsonArray();
        if (array.isEmpty()) throw new TaskCodeException("动作数组不能为空，至少保留一个动作");
        return array;
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
