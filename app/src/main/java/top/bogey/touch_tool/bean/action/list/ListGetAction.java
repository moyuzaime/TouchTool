package top.bogey.touch_tool.bean.action.list;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.PinValueUtil;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinDouble;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskRunnable;

public class ListGetAction extends ListCalculateAction {
    // 输出类型：自动按元素自身，指定类型时运行期转换
    private static final int TYPE_AUTO = 0;
    private static final int TYPE_OBJECT = 4;

    private final transient Pin listPin = new Pin(new PinList());
    private final transient Pin indexPin = Pin.withIndexTip(new Pin(new PinInteger(-1), R.string.list_action_index));
    private final transient Pin resultPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object, true);
    private final transient Pin existPin = new Pin(new PinBoolean(), R.string.pin_boolean_result, true);
    // 输出类型追加在末尾，老任务前四个针脚按原顺序匹配
    private final transient Pin typePin = new NotLinkAblePin(new PinSingleSelect(R.array.pin_output_type), R.string.pin_output_type);

    public ListGetAction() {
        super(ActionType.LIST_GET);
        addPins(listPin, indexPin, resultPin, existPin, typePin);
    }

    public ListGetAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(listPin, indexPin);
        reAddPin(resultPin, true);
        reAddPin(existPin);
        reAddPin(typePin);
        applyTypeRef();
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinList list = getPinValue(runnable, listPin);
        PinNumber<?> index = getPinValue(runnable, indexPin);
        int indexValue = index.intValue();
        if (indexValue == 0) indexValue = 1;
        int size = list.size();
        PinObject element = null;
        if (indexValue >= 1 && indexValue <= size) {
            element = list.get(indexValue - 1);
        } else if (indexValue < 0 && indexValue >= -size) {
            element = list.get(size + indexValue);
        }

        int typeIndex = typePin.getValue(PinSingleSelect.class).getIndex();
        if (typeIndex == TYPE_AUTO) {
            if (element != null) {
                existPin.getValue(PinBoolean.class).setValue(true);
                resultPin.setValue(returnValue(element));
            }
            return;
        }

        PinObject template = getTypedTemplate(typeIndex);
        PinObject target;
        boolean ok;
        if (template == null) {
            // 任意对象：原样输出
            target = element == null ? new PinObject(PinSubType.DYNAMIC) : (PinObject) element.copy();
            ok = element != null;
        } else {
            target = PinValueUtil.convertTo(element, template);
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
        return Arrays.asList(listPin, resultPin);
    }

}
