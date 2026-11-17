package top.bogey.touch_tool.bean.pin.pin_objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import top.bogey.touch_tool.utils.GsonUtil;

public class PinMap extends PinObject implements Map<PinObject, PinObject> {
    protected PinObject keyType = new PinObject(PinSubType.DYNAMIC);
    protected PinObject valueType = new PinObject(PinSubType.DYNAMIC);
    protected Map<PinObject, PinObject> valueMap = new LinkedHashMap<>();

    public PinMap() {
        super(PinType.MAP);
    }

    public PinMap(PinObject keyType, PinObject valueType) {
        this();
        this.keyType = keyType;
        this.valueType = valueType;
    }

    public PinMap(JsonObject jsonObject) {
        super(jsonObject);
        keyType = (PinObject) GsonUtil.getAsObject(jsonObject, "keyType", PinBase.class, new PinObject(PinSubType.DYNAMIC));
        valueType = (PinObject) GsonUtil.getAsObject(jsonObject, "valueType", PinBase.class, new PinObject(PinSubType.DYNAMIC));
        List<PinObject> values = GsonUtil.getAsObject(jsonObject, "values", TypeToken.getParameterized(List.class, PinBase.class).getType(), new ArrayList<>());
        valueMap = getValueMap(values);
    }

    @Override
    public void reset() {
        super.reset();
        valueMap.clear();
    }

    @Override
    public void sync(PinBase value) {
        if (value instanceof PinMap pinMap) {
            keyType = pinMap.keyType;
            valueType = pinMap.valueType;
            valueMap = pinMap.valueMap;
        }
    }

    @Override
    public PinMap copy() {
        PinMap pinMap = new PinMap(keyType, valueType);
        valueMap.forEach((key, value) -> pinMap.valueMap.put((PinObject) key.copy(), (PinObject) value.copy()));
        return pinMap;
    }

    @Override
    public boolean linkFromAble(PinBase pin) {
        if (getType().getGroup() == pin.getType().getGroup()) {
            if (isDynamic() || pin.isDynamic()) return true;
            if (pin instanceof PinMap pinMap) {
                if (pinMap.isDynamicKey() || isDynamicKey()) {
                    if (pinMap.isDynamicValue() || isDynamicValue()) {
                        return true;
                    }
                    return getValueType().linkFromAble(pinMap.getValueType());
                }

                if (pinMap.isDynamicValue() || isDynamicValue()) {
                    if (pinMap.isDynamicKey() || isDynamicKey()) {
                        return true;
                    }
                    return getKeyType().linkFromAble(pinMap.getKeyType());
                }

                return getKeyType().linkFromAble(pinMap.getKeyType()) && getValueType().linkFromAble(pinMap.getValueType());
            }
        }
        return false;
    }

    @Override
    public boolean linkToAble(PinBase pin) {
        if (getType().getGroup() == pin.getType().getGroup()) {
            if (isDynamic() || pin.isDynamic()) return true;
            if (pin instanceof PinMap pinMap) {
                if (pinMap.isDynamicKey() || isDynamicKey()) {
                    if (pinMap.isDynamicValue() || isDynamicValue()) {
                        return true;
                    }
                    return getValueType().linkFromAble(pinMap.getValueType());
                }

                if (pinMap.isDynamicValue() || isDynamicValue()) {
                    if (pinMap.isDynamicKey() || isDynamicKey()) {
                        return true;
                    }
                    return getKeyType().linkFromAble(pinMap.getKeyType());
                }

                return getKeyType().linkFromAble(pinMap.getKeyType()) && getValueType().linkFromAble(pinMap.getValueType());
            }
        }
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        return valueMap.toString();
    }

    public PinObject getKeyType() {
        return keyType;
    }

    public void setKeyType(PinObject keyType) {
        this.keyType = keyType;
        reset();
    }

    public PinObject getValueType() {
        return valueType;
    }

    public void setValueType(PinObject valueType) {
        this.valueType = valueType;
        reset();
    }

    @Override
    public boolean isDynamic() {
        return keyType.isDynamic() && valueType.isDynamic();
    }

    public boolean isHalfDynamic() {
        return keyType.isDynamic() || valueType.isDynamic();
    }

    public boolean isDynamicKey() {
        return keyType.isDynamic();
    }

    public boolean isDynamicValue() {
        return valueType.isDynamic();
    }


    @Override
    public final boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof PinMap pinMap)) return false;
        if (!super.equals(object)) return false;

        return getKeyType() == pinMap.getKeyType() && getValueType() == pinMap.getValueType() && valueMap.equals(pinMap.valueMap);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getKeyType().hashCode();
        result = 31 * result + getValueType().hashCode();
        result = 31 * result + valueMap.hashCode();
        return result;
    }

    @Override
    public int size() {
        return valueMap.size();
    }

    @Override
    public boolean isEmpty() {
        return valueMap.isEmpty();
    }

    @Override
    public boolean containsKey(@Nullable Object key) {
        return valueMap.containsKey(key);
    }

    @Override
    public boolean containsValue(@Nullable Object value) {
        return valueMap.containsValue(value);
    }

    @Nullable
    @Override
    public PinObject get(@Nullable Object key) {
        return valueMap.get(key);
    }

    @Nullable
    @Override
    public PinObject put(PinObject key, PinObject value) {
        return valueMap.put(key, value);
    }

    @Nullable
    @Override
    public PinObject remove(@Nullable Object key) {
        return valueMap.remove(key);
    }

    @Override
    public void putAll(@NonNull Map<? extends PinObject, ? extends PinObject> m) {
        valueMap.putAll(m);
    }

    @Override
    public void clear() {
        valueMap.clear();
    }

    @NonNull
    @Override
    public Set<PinObject> keySet() {
        return valueMap.keySet();
    }

    @NonNull
    @Override
    public Collection<PinObject> values() {
        return valueMap.values();
    }

    @NonNull
    @Override
    public Set<Entry<PinObject, PinObject>> entrySet() {
        return valueMap.entrySet();
    }

    public static class PinMapSerializer implements JsonSerializer<PinMap> {
        public JsonElement serialize(PinMap src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("type", src.getType().name());
            jsonObject.addProperty("subType", src.getSubType().name());
            jsonObject.add("keyType", context.serialize(src.keyType));
            jsonObject.add("valueType", context.serialize(src.valueType));
            jsonObject.add("values", context.serialize(getValueList(src.valueMap)));
            return jsonObject;
        }
    }

    private static List<PinObject> getValueList(Map<PinObject, PinObject> valueMap) {
        List<PinObject> list = new ArrayList<>();
        for (Entry<PinObject, PinObject> entry : valueMap.entrySet()) {
            list.add(entry.getKey());
            list.add(entry.getValue());
        }
        return list;
    }

    private static Map<PinObject, PinObject> getValueMap(List<PinObject> list) {
        Map<PinObject, PinObject> map = new LinkedHashMap<>();
        for (int i = 0; i < list.size(); i += 2) {
            map.put(list.get(i), list.get(i + 1));
        }
        return map;
    }
}
