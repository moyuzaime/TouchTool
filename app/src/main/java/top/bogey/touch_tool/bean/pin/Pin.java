package top.bogey.touch_tool.bean.pin;

import androidx.annotation.StringRes;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.base.Identity;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.utils.GsonUtil;

public class Pin extends Identity {
    private PinBase value;
    // 类型声明（Step 5.0），null 表示老任务：连线与编辑器走运行值回退
    private PinTypeRef typeRef;
    private Map<String, String> links = new HashMap<>();

    private boolean out;
    // 是否为动态针脚，动态针脚可以被删除
    private boolean dynamic;
    private boolean hide;

    private transient String ownerId;
    private transient @StringRes int titleId;
    private final transient Set<PinListener> listeners = new HashSet<>();

    public Pin(PinBase value) {
        this(value, 0);
    }

    public Pin(PinBase value, @StringRes int titleId) {
        this(value, titleId, false);
    }

    public Pin(PinBase value, boolean out) {
        this(value, 0, out);
    }

    public Pin(PinBase value, @StringRes int titleId, boolean out) {
        this(value, titleId, out, false);
    }

    public Pin(PinBase value, @StringRes int titleId, boolean out, boolean dynamic) {
        this(value, titleId, out, dynamic, false);
    }

    public Pin(PinBase value, @StringRes int titleId, boolean out, boolean dynamic, boolean hide) {
        super();
        this.value = value;
        this.titleId = titleId;
        this.out = out;
        this.dynamic = dynamic;
        this.hide = hide;
    }

    public Pin(JsonObject jsonObject) {
        super(jsonObject);
        value = GsonUtil.getAsObject(jsonObject, "value", PinBase.class, null);
        typeRef = GsonUtil.getAsObject(jsonObject, "typeRef", PinTypeRef.class, null);
        links = GsonUtil.getAsObject(jsonObject, "links", TypeToken.getParameterized(HashMap.class, String.class, String.class).getType(), new HashMap<>());
        out = GsonUtil.getAsBoolean(jsonObject, "out", false);
        dynamic = GsonUtil.getAsBoolean(jsonObject, "dynamic", false);
        hide = GsonUtil.getAsBoolean(jsonObject, "hide", false);
    }

    public void sync(Pin pin) {
        setId(pin.getId());
        setUid(pin.getUid());
        setTitle(pin.title);
        setDescription(pin.description);

        // 值需要让他自己同步
        getValue().sync(pin.getValue());
        setLinks(pin.getLinks());
        if (pin.typeRef != null) typeRef = pin.typeRef.copy();

        setOut(pin.isOut());
        setDynamic(pin.isDynamic());
        setHide(pin.isHide());
    }

    public Pin getLinkedPin(Task task) {
        return getLinkedPin(task.getActions());
    }

    public Pin getLinkedPin(List<Action> actions) {
        for (Map.Entry<String, String> entry : links.entrySet()) {
            String pinId = entry.getKey();
            String actionId = entry.getValue();
            for (Action action : actions) {
                if (action.getId().equals(actionId)) {
                    Pin pin = action.getPinById(pinId);
                    if (pin != null) return pin;
                }
            }
        }
        return null;
    }

    protected void addLink(Task task, Pin pin) {
        if (isSingleLink()) clearLinks(task);
        links.put(pin.getId(), pin.getOwnerId());
        listeners.stream().filter(Objects::nonNull).forEach(listener -> listener.onLinkedTo(task, this, pin));
    }

    public void mutualAddLink(Task task, Pin pin) {
        if (isSingleLink()) {
            addLink(task, pin);
            pin.addLink(task, this);
        } else {
            pin.addLink(task, this);
            addLink(task, pin);
        }
    }

    public void directAddLink(Pin pin) {
        if (isSingleLink()) links.clear();
        links.put(pin.getId(), pin.getOwnerId());
    }

    public boolean addLinks(Task task, Map<String, String> links) {
        boolean linked = false;
        for (Map.Entry<String, String> entry : links.entrySet()) {
            String pinId = entry.getKey();
            String actionId = entry.getValue();
            Action action = task.getAction(actionId);
            if (action == null) continue;
            Pin pin = action.getPinById(pinId);
            if (pin == null) continue;
            if (!linkAble(pin)) continue;
            mutualAddLink(task, pin);
            linked = true;
        }
        return linked;
    }

    private void removeLink(Task task, Pin pin) {
        links.remove(pin.getId());
        listeners.stream().filter(Objects::nonNull).forEach(listener -> listener.onUnLinkedFrom(task, this, pin));
    }

    public void mutualRemoveLink(Task task, Pin pin) {
        removeLink(task, pin);
        pin.removeLink(task, this);
    }

    public void clearLinks(Task task) {
        Map<String, String> links = new HashMap<>(this.links);
        for (Map.Entry<String, String> entry : links.entrySet()) {
            String pinId = entry.getKey();
            String actionId = entry.getValue();
            Action action = task.getAction(actionId);
            if (action == null) continue;
            Pin pin = action.getPinById(pinId);
            if (pin == null) continue;
            mutualRemoveLink(task, pin);
        }
        links.clear();
    }

    public Class<? extends PinBase> getPinClass() {
        if (value == null) return null;
        return value.getClass();
    }

    public boolean isVertical() {
        if (value == null) return false;
        return value.getType() == PinType.EXECUTE;
    }

    public boolean isSingleLink() {
        if (isVertical()) return out;
        return !out;
    }

    public boolean isLinked() {
        return !links.isEmpty();
    }

    public boolean isSameClass(Pin pin) {
        if (pin == null) return false;
        return isSameClass(pin.getValue());
    }

    public boolean isSameClass(PinBase value) {
        if (value == null) return false;
        return isSameClass(value.getClass());
    }

    public boolean isSameClass(Class<? extends PinBase> clazz) {
        return Objects.equals(getPinClass(), clazz);
    }

    public boolean linkAble(Pin pin) {
        if (pin == null) return false;
        if (out == pin.isOut()) return false;
        if (Objects.equals(ownerId, pin.getOwnerId())) return false;
        // 有类型声明时按声明判断（Step 5.0），否则回退到按运行值判断
        Boolean declared = linkAbleByTypeRef(pin);
        if (declared != null) return declared;
        return linkAble(pin.getValue());
    }

    /**
     * 按 PinTypeRef 声明判断连线；任一侧没有声明时返回 null，走老任务的按值判断
     * <p>
     * UNKNOWN → 具体：允许（首连定型）；ANY → 具体：拒绝，必须先类型判断/转换；
     * 具体 → ANY / UNKNOWN：允许；两侧都具体时比较模板类型。
     */
    private Boolean linkAbleByTypeRef(Pin pin) {
        PinTypeRef from = out ? typeRef : pin.typeRef;
        PinTypeRef to = out ? pin.typeRef : typeRef;
        if (from == null || to == null) return null;

        if (from.isUnknown()) return true;
        if (from.isAny()) return !to.isConcrete();
        if (to.isAny() || to.isUnknown()) return true;

        PinObject source = from.getTemplate();
        PinObject target = to.getTemplate();
        if (source == null || target == null) return true;
        return target.linkFromAble(source) || source.linkToAble(target);
    }

    public PinTypeRef getTypeRef() {
        return typeRef;
    }

    public void setTypeRef(PinTypeRef typeRef) {
        this.typeRef = typeRef;
    }

    public boolean isTypeAny() {
        return typeRef != null && typeRef.isAny();
    }

    public boolean isTypeConcrete() {
        return typeRef != null && typeRef.isConcrete();
    }

    public boolean isTypeUnknown() {
        return typeRef != null && typeRef.isUnknown();
    }

    // 声明的元素 / 键 / 值模板，没有声明时返回 null
    public PinObject getTypeTemplate() {
        return typeRef == null ? null : typeRef.getTemplate();
    }

    // 字典声明里模板是一个 PinMap，键 / 值类型从这里取
    public PinObject getTypeKeyTemplate() {
        PinObject template = getTypeTemplate();
        return template instanceof PinMap map ? map.getKeyType() : null;
    }

    public PinObject getTypeValueTemplate() {
        PinObject template = getTypeTemplate();
        return template instanceof PinMap map ? map.getValueType() : null;
    }

    public void markTypeAny() {
        typeRef = PinTypeRef.any();
    }

    public void markTypeUnknown() {
        typeRef = PinTypeRef.unknown();
    }

    public void markTypeConcrete(PinObject template) {
        typeRef = PinTypeRef.concrete(template);
    }

    public boolean linkAble(PinBase value) {
        if (value == null || this.value == null) return false;
        if (out) return this.value.linkToAble(value) || value.linkFromAble(this.value);
        return this.value.linkFromAble(value) || value.linkToAble(this.value);
    }

    public boolean linkAble(Task context) {
        return linkAble();
    }

    public boolean linkAble() {
        return true;
    }

    public boolean showAble(Task context) {
        return true;
    }

    @Override
    public Pin copy() {
        Pin copy = GsonUtil.copy(this, Pin.class);
        copy.setTitleId(getTitleId());
        return copy;
    }

    @Override
    public Pin newCopy() {
        Pin copy = copy();
        copy.setId(UUID.randomUUID().toString());
        copy.links.clear();
        return copy;
    }

    public <T extends PinBase> T getValue() {
        if (value == null) return null;
        return (T) value;
    }

    public <T extends PinBase> T getValue(Class<T> clazz) {
        return clazz.cast(value);
    }

    public void setValue(PinBase value) {
        this.value = value;
    }

    public void setValue(Task task, PinBase value) {
        this.value = value;
        listeners.stream().filter(Objects::nonNull).forEach(listener -> listener.onValueReplaced(task, this, value));
    }

    public void notifyValueUpdated(Task task) {
        listeners.stream().filter(Objects::nonNull).forEach(listener -> listener.onValueUpdated(task, this, value));
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public void setLinks(Map<String, String> links) {
        this.links = links;
    }

    public boolean isOut() {
        return out;
    }

    public void setOut(boolean out) {
        this.out = out;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    public boolean isHide() {
        return hide;
    }

    public void setHide(boolean hide) {
        this.hide = hide;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public int getTitleId() {
        return titleId;
    }

    public void setTitleId(int titleId) {
        this.titleId = titleId;
    }

    @Override
    public String getTitle() {
        if (title != null && !title.isEmpty()) return title;
        if (titleId == 0) {
            if (isSameClass(PinList.class)) {
                PinList list = getValue();
                PinInfo info = PinInfo.getPinInfo(list.getValueType());
                if (info != null) return info.getTitle() + MainApplication.getInstance().getString(R.string.pin_list);
            }
            if (isSameClass(PinMap.class)) {
                PinMap map = getValue();
                PinInfo keyInfo = PinInfo.getPinInfo(map.getKeyType());
                PinInfo valueInfo = PinInfo.getPinInfo(map.getValueType());
                if (keyInfo != null && valueInfo != null) return keyInfo.getTitle() + "-" + valueInfo.getTitle();
            }
            return "";
        }
        return MainApplication.getInstance().getString(titleId);
    }

    // 索引类针脚统一说明取值规则：从 1 开始，负数从末尾数，0 等同 1
    public static <T extends Pin> T withIndexTip(T pin) {
        pin.setDescription(MainApplication.getInstance().getString(R.string.pin_index_tip));
        return pin;
    }

    @Override
    public void setTitle(String title) {
        super.setTitle(title);
        listeners.stream().filter(Objects::nonNull).forEach(listener -> listener.onTitleChanged(this, title));
    }

    public void addListener(PinListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PinListener listener) {
        listeners.remove(listener);
    }

    public static class PinDeserialize implements JsonDeserializer<Pin> {
        @Override
        public Pin deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new Pin(json.getAsJsonObject());
        }
    }
}
