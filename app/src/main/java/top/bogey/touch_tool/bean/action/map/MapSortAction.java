package top.bogey.touch_tool.bean.action.map;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.service.TaskRunnable;

/**
 * 字典按键排序：按键的升序或降序生成新字典
 */
public class MapSortAction extends MapCalculateAction {
    private static final int ORDER_DESC = 1;

    private final transient Pin mapPin = new Pin(new PinMap());
    private final transient Pin orderPin = new NotLinkAblePin(new PinSingleSelect(R.array.map_sort_order), R.string.map_sort_action_order);
    private final transient Pin resultPin = new Pin(new PinMap(), R.string.map_sort_action_result, true);

    public MapSortAction() {
        super(ActionType.MAP_SORT);
        addPins(mapPin, orderPin, resultPin);
    }

    public MapSortAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(mapPin, orderPin, resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinMap map = getPinValue(runnable, mapPin);
        PinMap result = new PinMap((PinObject) map.getKeyType().copy(), (PinObject) map.getValueType().copy());
        List<PinObject> keys = new ArrayList<>(map.keySet());
        boolean desc = orderPin.getValue(PinSingleSelect.class).getIndex() == ORDER_DESC;
        keys.sort((first, second) -> desc ? compareKey(second, first) : compareKey(first, second));
        for (PinObject key : keys) {
            result.put(key, map.get(key));
        }
        resultPin.setValue(result);
    }

    // 键比较按「类型序号 + 值」给出全序，混合类型也不会违反排序契约
    private int compareKey(PinObject first, PinObject second) {
        if (first instanceof PinNumber<?> number1 && second instanceof PinNumber<?> number2) {
            return Double.compare(number1.doubleValue(), number2.doubleValue());
        }
        if (first instanceof PinString string1 && second instanceof PinString string2) {
            return string1.toString().compareTo(string2.toString());
        }
        int typeCompare = Integer.compare(first.getType().ordinal(), second.getType().ordinal());
        if (typeCompare != 0) return typeCompare;
        return first.toString().compareTo(second.toString());
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(mapPin, resultPin);
    }
}
