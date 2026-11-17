package top.bogey.touch_tool.bean.action.map;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.PinValueUtil;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinDouble;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskRunnable;

public class MapGetAction extends MapCalculateAction {
    // 输出类型：自动按值自身，指定类型时运行期转换
    private static final int TYPE_AUTO = 0;
    private static final int TYPE_OBJECT = 4;

    private final transient Pin mapPin = new Pin(new PinMap());
    private final transient Pin keyPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.map_action_key);
    private final transient Pin resultPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.map_action_value, true);
    private final transient Pin existPin = new Pin(new PinBoolean(), R.string.pin_boolean_result, true);
    // 默认值针脚追加在末尾，保证老任务前四个针脚按原顺序匹配
    private final transient Pin defaultPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.map_get_action_default);
    // 输出类型追加在最后
    private final transient Pin typePin = new NotLinkAblePin(new PinSingleSelect(R.array.pin_output_type), R.string.pin_output_type);

    public MapGetAction() {
        super(ActionType.MAP_GET);
        addPins(mapPin, keyPin, resultPin, existPin, defaultPin, typePin);
    }

    public MapGetAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPin(mapPin);
        reAddPin(keyPin, true);
        reAddPin(resultPin, true);
        reAddPin(existPin);
        reAddPin(defaultPin, true);
        reAddPin(typePin);
        applyTypeRef();
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinMap map = getPinValue(runnable, mapPin);
        PinObject key = getPinValue(runnable, keyPin);
        PinObject object = map.get(key);

        int typeIndex = typePin.getValue(PinSingleSelect.class).getIndex();
        if (typeIndex == TYPE_AUTO) {
            if (object != null) {
                existPin.getValue(PinBoolean.class).setValue(true);
                resultPin.setValue(returnValue(object));
            } else {
                existPin.getValue(PinBoolean.class).setValue(false);
                // 未命中时输出默认值，默认值为空对象，与旧版未命中表现一致
                PinObject defaultValue = getPinValue(runnable, defaultPin);
                resultPin.setValue(defaultValue == null ? new PinObject(PinSubType.DYNAMIC) : defaultValue);
            }
            return;
        }

        if (object == null) {
            // 未命中先看默认值，不参与类型转换
            existPin.getValue(PinBoolean.class).setValue(false);
            PinObject defaultValue = getPinValue(runnable, defaultPin);
            resultPin.setValue(defaultValue == null ? new PinObject(PinSubType.DYNAMIC) : defaultValue);
            return;
        }

        PinObject template = getTypedTemplate(typeIndex);
        PinObject target;
        boolean ok;
        if (template == null) {
            target = (PinObject) object.copy();
            ok = true;
        } else {
            target = PinValueUtil.convertTo(object, template);
            ok = target != null;
            // 转换失败输出默认值，并把结果标志置 false
            if (target == null) target = PinValueUtil.newValue(template);
        }
        resultPin.setValue(target);
        existPin.getValue(PinBoolean.class).setValue(ok);
    }

    // 指定输出类型时把声明写成该类型，任意对象对应 ANY
    private void applyTypeRef() {
        int typeIndex = typePin.getValue(PinSingleSelect.class).getIndex();
        if (typeIndex == TYPE_AUTO) return;
        PinObject template = getTypedTemplate(typeIndex);
        if (template == null) resultPin.markTypeAny();
        else resultPin.markTypeConcrete(template);
    }

    private PinObject getTypedTemplate(int typeIndex) {
        return switch (typeIndex) {
            case 1 -> new PinString();
            case 2 -> new PinDouble();
            case 3 -> new PinBoolean();
            default -> null;
        };
    }

    @Override
    public void onValueUpdated(Task task, Pin origin, PinBase value) {
        super.onValueUpdated(task, origin, value);
        if (origin == typePin) applyTypeRef();
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(mapPin, keyPin, resultPin, defaultPin);
    }

    @NonNull
    @Override
    public List<Pin> getDynamicKeyTypePins() {
        return Collections.singletonList(keyPin);
    }

    @NonNull
    @Override
    public List<Pin> getDynamicValueTypePins() {
        return Arrays.asList(resultPin, defaultPin);
    }
}
