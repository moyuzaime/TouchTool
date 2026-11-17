package top.bogey.touch_tool.bean.pin;

import androidx.annotation.Nullable;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.Objects;

import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.utils.GsonUtil;

/**
 * 针脚的类型声明（Step 5.0）：容器针脚声明「元素 / 键 / 值」的类型契约
 * <p>
 * CONCRETE 时 template 是具体类型模板；ANY 是显式任意类型；UNKNOWN 是尚未定型，
 * 等首次连线推断。针脚上 typeRef 为 null 表示老任务没有声明，连线与编辑器走运行值回退。
 */
public class PinTypeRef {
    public enum Kind {
        // 尚未定型，等首次连线推断
        UNKNOWN,
        // 具体类型，看 template
        CONCRETE,
        // 显式任意类型，允许混合内容
        ANY
    }

    private Kind kind;
    // 仅 CONCRETE 有效：元素 / 键 / 值的类型模板
    private PinObject template;

    public PinTypeRef(Kind kind) {
        this(kind, null);
    }

    public PinTypeRef(Kind kind, PinObject template) {
        this.kind = kind;
        this.template = template;
    }

    public static PinTypeRef unknown() {
        return new PinTypeRef(Kind.UNKNOWN);
    }

    public static PinTypeRef any() {
        return new PinTypeRef(Kind.ANY);
    }

    public static PinTypeRef concrete(PinObject template) {
        return new PinTypeRef(Kind.CONCRETE, template);
    }

    public Kind getKind() {
        return kind;
    }

    @Nullable
    public PinObject getTemplate() {
        return template;
    }

    public boolean isUnknown() {
        return kind == Kind.UNKNOWN;
    }

    public boolean isAny() {
        return kind == Kind.ANY;
    }

    public boolean isConcrete() {
        return kind == Kind.CONCRETE;
    }

    public PinTypeRef copy() {
        return new PinTypeRef(kind, template == null ? null : (PinObject) template.copy());
    }

    public void sync(PinTypeRef typeRef) {
        if (typeRef == null) return;
        kind = typeRef.kind;
        template = typeRef.template == null ? null : (PinObject) typeRef.template.copy();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof PinTypeRef that)) return false;
        return kind == that.kind && Objects.equals(template, that.template);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, template);
    }

    public static class PinTypeRefDeserializer implements JsonDeserializer<PinTypeRef> {
        @Override
        public PinTypeRef deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            Kind kind = GsonUtil.getAsObject(jsonObject, "kind", Kind.class, Kind.UNKNOWN);
            PinBase template = GsonUtil.getAsObject(jsonObject, "template", PinBase.class, null);
            if (template instanceof PinObject pinObject) return new PinTypeRef(kind, pinObject);
            return new PinTypeRef(kind);
        }
    }
}
