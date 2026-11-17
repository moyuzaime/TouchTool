package top.bogey.touch_tool.bean.pin.pin_objects.pin_list;

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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.PinType;
import top.bogey.touch_tool.utils.GsonUtil;

public class PinList extends PinObject implements List<PinObject> {
    protected PinObject valueType = new PinObject(PinSubType.DYNAMIC);
    protected List<PinObject> values = new ArrayList<>();

    public PinList() {
        super(PinType.LIST);
    }

    public PinList(PinObject valueType) {
        this();
        this.valueType = valueType;
    }

    public PinList(PinSubType subType, PinObject valueType) {
        super(PinType.LIST, subType);
        this.valueType = valueType;
    }

    public PinList(PinType type, PinObject valueType) {
        super(type);
        this.valueType = valueType;
    }

    public PinList(PinType type, PinSubType subType, PinObject valueType) {
        super(type, subType);
        this.valueType = valueType;
    }

    public PinList(JsonObject jsonObject) {
        super(jsonObject);
        valueType = (PinObject) GsonUtil.getAsObject(jsonObject, "valueType", PinBase.class, new PinObject(PinSubType.DYNAMIC));
        values = GsonUtil.getAsObject(jsonObject, "values", TypeToken.getParameterized(ArrayList.class, PinBase.class).getType(), new ArrayList<>());
    }

    @Override
    public void reset() {
        super.reset();
        clear();
    }

    @Override
    public void sync(PinBase value) {
        if (value instanceof PinList pinList) {
            valueType = pinList.valueType;
            values = pinList.values;
        }
    }

    @Override
    public PinList copy() {
        PinList pinList = new PinList(type, subType, valueType);
        for (PinObject value : values) {
            pinList.values.add((PinObject) value.copy());
        }
        return pinList;
    }

    @Override
    public boolean linkFromAble(PinBase pin) {
        if (getType().getGroup() == pin.getType().getGroup()) {
            if (isDynamic() || pin.isDynamic()) return true;
            if (pin instanceof PinList pinList) {
                return getValueType().linkFromAble(pinList.getValueType());
            }
        }
        return false;
    }

    @Override
    public boolean linkToAble(PinBase pin) {
        if (getType().getGroup() == pin.getType().getGroup()) {
            if (isDynamic() || pin.isDynamic()) return true;
            if (pin instanceof PinList pinList) {
                return getValueType().linkFromAble(pinList.getValueType());
            }
        }
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        return values.toString();
    }

    public PinObject getValueType() {
        return valueType;
    }

    public void setValueType(PinObject valueType) {
        this.valueType = valueType;
    }

    @Override
    public boolean isDynamic() {
        return valueType.isDynamic();
    }

    @Override
    public final boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof PinList that)) return false;
        if (!super.equals(object)) return false;

        return isDynamic() == that.isDynamic() && getValueType() == that.getValueType() && values.equals(that.values);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getValueType().hashCode();
        result = 31 * result + values.hashCode();
        result = 31 * result + Boolean.hashCode(isDynamic());
        return result;
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public boolean contains(@Nullable Object o) {
        return values.contains(o);
    }

    @NonNull
    @Override
    public Iterator<PinObject> iterator() {
        return values.iterator();
    }

    @NonNull
    @Override
    public Object[] toArray() {
        return values.toArray();
    }

    @NonNull
    @Override
    public <T> T[] toArray(@NonNull T[] a) {
        return values.toArray(a);
    }

    @Override
    public boolean add(PinObject pinObject) {
        return values.add(pinObject);
    }

    @Override
    public boolean remove(@Nullable Object o) {
        return values.remove(o);
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        return new HashSet<>(values).containsAll(c);
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends PinObject> c) {
        return values.addAll(c);
    }

    @Override
    public boolean addAll(int index, @NonNull Collection<? extends PinObject> c) {
        return values.addAll(index, c);
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        return values.removeAll(c);
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        return values.retainAll(c);
    }

    @Override
    public void clear() {
        values.clear();
    }

    @Override
    public PinObject get(int index) {
        return values.get(index);
    }

    @Override
    public PinObject set(int index, PinObject element) {
        return values.set(index, element);
    }

    @Override
    public void add(int index, PinObject element) {
        values.add(index, element);
    }

    @Override
    public PinObject remove(int index) {
        return values.remove(index);
    }

    @Override
    public int indexOf(@Nullable Object o) {
        return values.indexOf(o);
    }

    @Override
    public int lastIndexOf(@Nullable Object o) {
        return values.lastIndexOf(o);
    }

    @NonNull
    @Override
    public ListIterator<PinObject> listIterator() {
        return values.listIterator();
    }

    @NonNull
    @Override
    public ListIterator<PinObject> listIterator(int index) {
        return values.listIterator(index);
    }

    @NonNull
    @Override
    public List<PinObject> subList(int fromIndex, int toIndex) {
        return values.subList(fromIndex, toIndex);
    }

    public static class PinListSerializer implements JsonSerializer<PinList> {
        @Override
        public JsonElement serialize(PinList src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("type", src.getType().name());
            jsonObject.addProperty("subType", src.getSubType().name());
            jsonObject.add("valueType", context.serialize(src.getValueType()));
            jsonObject.add("values", context.serialize(src.values));
            return jsonObject;
        }
    }
}
