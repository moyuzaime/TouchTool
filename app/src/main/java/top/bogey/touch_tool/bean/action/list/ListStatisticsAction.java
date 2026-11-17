package top.bogey.touch_tool.bean.action.list;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinDouble;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.service.TaskRunnable;

/**
 * 列表统计：对数字元素求和 / 最大 / 最小 / 平均，非数字元素跳过
 */
public class ListStatisticsAction extends ListCalculateAction {
    private static final int MODE_SUM = 0;
    private static final int MODE_MAX = 1;
    private static final int MODE_MIN = 2;
    private static final int MODE_AVG = 3;

    private final transient Pin listPin = new Pin(new PinList());
    private final transient Pin modePin = new NotLinkAblePin(new PinSingleSelect(R.array.list_statistic_mode), R.string.list_statistics_action_mode);
    private final transient Pin resultPin = new Pin(new PinDouble(), R.string.list_statistics_action_result, true);
    private final transient Pin countPin = new Pin(new PinInteger(), R.string.list_statistics_action_count, true);

    public ListStatisticsAction() {
        super(ActionType.LIST_STATISTICS);
        addPins(listPin, modePin, resultPin, countPin);
    }

    public ListStatisticsAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(listPin, modePin, resultPin, countPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinList list = getPinValue(runnable, listPin);
        double sum = 0;
        double max = Double.NEGATIVE_INFINITY;
        double min = Double.POSITIVE_INFINITY;
        int count = 0;
        for (PinObject value : list) {
            if (!(value instanceof PinNumber<?> number) || number.getValue() == null) continue;
            double numberValue = number.doubleValue();
            sum += numberValue;
            max = Math.max(max, numberValue);
            min = Math.min(min, numberValue);
            count++;
        }
        double result = switch (modePin.getValue(PinSingleSelect.class).getIndex()) {
            case MODE_MAX -> count == 0 ? 0 : max;
            case MODE_MIN -> count == 0 ? 0 : min;
            case MODE_AVG -> count == 0 ? 0 : sum / count;
            default -> sum;
        };
        resultPin.getValue(PinDouble.class).setValue(result);
        countPin.getValue(PinInteger.class).setValue(count);
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(listPin, resultPin);
    }
}
