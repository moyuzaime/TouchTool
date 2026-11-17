package top.bogey.touch_tool.bean.action.list;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

/**
 * 列表去重：保持首次出现的顺序，可忽略文本大小写
 */
public class ListDeduplicateAction extends ListCalculateAction {
    private final transient Pin listPin = new Pin(new PinList());
    private final transient Pin ignoreCasePin = new Pin(new PinBoolean(), R.string.list_deduplicate_action_ignore_case);
    private final transient Pin resultPin = new Pin(new PinList(), R.string.list_deduplicate_action_result, true);
    private final transient Pin countPin = new Pin(new PinInteger(), R.string.list_deduplicate_action_count, true);

    public ListDeduplicateAction() {
        super(ActionType.LIST_DEDUPLICATE);
        addPins(listPin, ignoreCasePin, resultPin, countPin);
    }

    public ListDeduplicateAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(listPin, ignoreCasePin, resultPin, countPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinList list = getPinValue(runnable, listPin);
        PinBoolean ignoreCase = getPinValue(runnable, ignoreCasePin);
        PinList result = new PinList((PinObject) list.getValueType().copy());
        Set<Object> seen = new LinkedHashSet<>();
        for (PinObject value : list) {
            if (value == null) continue;
            Object key = value;
            if (ignoreCase.getValue() && value instanceof PinString string) {
                key = string.toString().toLowerCase(Locale.ROOT);
            }
            if (seen.add(key)) result.add(value);
        }
        resultPin.setValue(result);
        countPin.getValue(PinInteger.class).setValue(list.size() - result.size());
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(listPin, resultPin);
    }
}
