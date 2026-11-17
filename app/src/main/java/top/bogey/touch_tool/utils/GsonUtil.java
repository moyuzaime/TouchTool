package top.bogey.touch_tool.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.lang.reflect.Type;
import java.util.Map;

import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.other.log.Log;
import top.bogey.touch_tool.bean.other.log.LogInfo;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinApplications;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinMultiSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.save.model.LiteRTModel;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.bean.task.Variable;

public class GsonUtil {
    public static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Task.class, new Task.TaskDeserialize())
            .registerTypeAdapter(Variable.class, new Variable.VariableDeserialize())
            .registerTypeAdapter(Action.class, new Action.ActionDeserializer())
            .registerTypeAdapter(Pin.class, new Pin.PinDeserialize())

            .registerTypeAdapter(PinBase.class, new PinBase.PinBaseDeserializer())
            .registerTypeAdapter(PinList.class, new PinList.PinListSerializer())
            .registerTypeAdapter(PinApplications.class, new PinList.PinListSerializer())
            .registerTypeAdapter(PinMultiSelect.class, new PinList.PinListSerializer())
            .registerTypeAdapter(PinMap.class, new PinMap.PinMapSerializer())
            .registerTypeAdapter(PinImage.class, new PinImage.PinImageSerializer())

            .registerTypeAdapter(LogInfo.class, new LogInfo.LogInfoDeserialize())
            .registerTypeAdapter(Log.class, new Log.LogDeserialize())

            .registerTypeAdapter(LiteRTModel.class, new LiteRTModel.LiteRTModelDeserializer())

            .create();

    public static <T> T copy(T src, Class<T> clazz) {
        return gson.fromJson(gson.toJson(src), clazz);
    }

    public static <T> T copy(T src, Type typeOfSrc) {
        return gson.fromJson(gson.toJson(src), typeOfSrc);
    }

    public static String toJson(Object src) {
        return gson.toJson(src);
    }

    // 针脚值转 JSON 元素：列表转数组、字典转对象、数字与布尔按基本类型、其余按文本
    public static JsonElement toJsonElement(PinBase value) {
        if (value == null) return JsonNull.INSTANCE;
        if (value instanceof PinList list) {
            JsonArray array = new JsonArray();
            for (PinObject element : list) {
                array.add(toJsonElement(element));
            }
            return array;
        }
        if (value instanceof PinMap map) {
            JsonObject object = new JsonObject();
            for (Map.Entry<PinObject, PinObject> entry : map.entrySet()) {
                object.add(entry.getKey().toString(), toJsonElement(entry.getValue()));
            }
            return object;
        }
        if (value instanceof PinNumber<?> number && number.getValue() != null) {
            return new JsonPrimitive(number.getValue());
        }
        if (value instanceof PinBoolean bool) {
            return new JsonPrimitive(bool.getValue());
        }
        return new JsonPrimitive(value.toString());
    }

    public static <T> T getAsObject(JsonObject jsonObject, String key, Class<T> clazz, T defaultValue) {
        if (jsonObject.has(key)) {
            try {
                return gson.fromJson(jsonObject.get(key), clazz);
            } catch (Exception e) {
                AppLog.e(e);
            }
        }
        return defaultValue;
    }

    public static <T> T getAsObject(JsonObject jsonObject, String key, Type typeOfSrc, T defaultValue) {
        if (jsonObject.has(key)) {
            try {
                return gson.fromJson(jsonObject.get(key), typeOfSrc);
            } catch (Exception e) {
                AppLog.e(e);
            }
        }
        return defaultValue;
    }

    public static <T> T getAsObject(String json, Class<T> clazz, T defaultValue) {
        if (json != null) {
            try {
                return gson.fromJson(json, clazz);
            } catch (Exception e) {
                AppLog.e(e);
            }
        }
        return defaultValue;
    }

    public static <T> T getAsObject(String json, Type typeOfSrc, T defaultValue) {
        if (json != null) {
            try {
                return gson.fromJson(json, typeOfSrc);
            } catch (Exception e) {
                AppLog.e(e);
            }
        }
        return defaultValue;
    }

    public static int getAsInt(JsonObject jsonObject, String key, int defaultValue) {
        if (jsonObject.has(key)) {
            try {
                return jsonObject.get(key).getAsInt();
            } catch (Exception e) {
                AppLog.e(e);
            }
        }
        return defaultValue;
    }

    public static float getAsFloat(JsonObject jsonObject, String key, float defaultValue) {
        if (jsonObject.has(key)) {
            try {
                return jsonObject.get(key).getAsFloat();
            } catch (Exception e) {
                AppLog.e(e);
            }
        }
        return defaultValue;
    }

    public static long getAsLong(JsonObject jsonObject, String key, long defaultValue) {
        if (jsonObject.has(key)) {
            try {
                return jsonObject.get(key).getAsLong();
            } catch (Exception e) {
                AppLog.e(e);
            }
        }
        return defaultValue;
    }

    public static double getAsDouble(JsonObject jsonObject, String key, double defaultValue) {
        if (jsonObject.has(key)) {
            try {
                return jsonObject.get(key).getAsDouble();
            } catch (Exception e) {
                AppLog.e(e);
            }
        }
        return defaultValue;
    }

    public static boolean getAsBoolean(JsonObject jsonObject, String key, boolean defaultValue) {
        if (jsonObject.has(key)) {
            try {
                return jsonObject.get(key).getAsBoolean();
            } catch (Exception e) {
                AppLog.e(e);
            }
        }
        return defaultValue;
    }


    public static String getAsString(JsonObject jsonObject, String key, String defaultValue) {
        if (jsonObject.has(key)) {
            try {
                return jsonObject.get(key).getAsString();
            } catch (Exception e) {
                AppLog.e(e);
            }
        }
        return defaultValue;
    }


}
