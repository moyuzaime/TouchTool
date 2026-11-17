package top.bogey.touch_tool.bean.action.list;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinDouble;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.AppLog;

public class ListSortAction extends ListExecuteAction {
    // 排序方式：按元素自身升/降序、按字典键、自定义比较流程
    public static final int MODE_ASC = 0;
    public static final int MODE_DESC = 1;
    public static final int MODE_KEY = 2;
    public static final int MODE_CUSTOM = 3;
    // 自定义比较的次数上限，超出后中止排序，避免逐对跑子流程把任务拖死
    private static final int MAX_COMPARE_COUNT = 20000;

    private final transient Pin listPin = new Pin(new PinList());
    private final transient Pin resultPin = new Pin(new PinDouble(), R.string.list_sort_action_result);
    private final transient Pin elementPin1 = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object, true);
    private final transient Pin elementPin2 = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object, true);
    private final transient Pin comparePin = new Pin(new PinExecute(), R.string.list_sort_action_compare, true);
    // 方式与比较键追加在针脚列表末尾，老任务前五个针脚按原顺序匹配
    private final transient Pin modePin = new NotLinkAblePin(new PinSingleSelect(R.array.list_sort_mode), R.string.list_sort_action_mode);
    private final transient Pin keyPin = new Pin(new PinString(), R.string.list_sort_action_key);

    public ListSortAction() {
        super(ActionType.LIST_SORT);
        addPins(listPin, resultPin, elementPin1, elementPin2, comparePin, modePin, keyPin);
    }

    public ListSortAction(JsonObject jsonObject) {
        super(jsonObject);
        // 老任务没有方式针脚，固定为自定义比较，行为与旧版一致
        boolean hasModePin = tmpPins.stream().anyMatch(pin -> pin.isSameClass(modePin));
        reAddPins(listPin, resultPin);
        reAddPin(elementPin1, true);
        reAddPin(elementPin2, true);
        reAddPin(comparePin);
        reAddPin(modePin);
        reAddPin(keyPin, true);
        if (!hasModePin) modePin.getValue(PinSingleSelect.class).setIndex(MODE_CUSTOM);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinList list = getPinValue(runnable, listPin);
        int mode = modePin.getValue(PinSingleSelect.class).getIndex();
        switch (mode) {
            case MODE_ASC -> sortBySelf(runnable, list, 1);
            case MODE_DESC -> sortBySelf(runnable, list, -1);
            case MODE_KEY -> sortByKey(runnable, list);
            default -> sortByCompare(runnable, list);
        }
        executeNext(runnable, outPin);
    }

    @Override
    public void beforeExecuteNext(TaskRunnable runnable, Pin pin) {
        if (pin == outPin) {
            super.beforeExecuteNext(runnable, pin);
        }
    }

    // 按元素自身比较，排序不执行子流程
    private void sortBySelf(TaskRunnable runnable, PinList list, int direction) {
        boolean sorted = trySort(list, (first, second) -> compareValue(first, second) * direction);
        if (!sorted) runnable.addLog(getString(R.string.list_sort_action_failed));
    }

    // 按指定键比较字典列表里的值
    private void sortByKey(TaskRunnable runnable, PinList list) {
        String key = keyPin.getValue(PinString.class).getValue();
        if (key == null || key.isEmpty()) {
            runnable.addLog(getString(R.string.list_sort_action_key_empty));
            return;
        }
        boolean sorted = trySort(list, (first, second) -> compareValue(getMapValue(first, key), getMapValue(second, key)));
        if (!sorted) runnable.addLog(getString(R.string.list_sort_action_failed));
    }

    // 自定义比较：每对元素执行一次比较流程，结果未写时按相等处理
    private void sortByCompare(TaskRunnable runnable, PinList list) {
        int[] count = new int[1];
        boolean sorted = trySort(list, (first, second) -> {
            if (++count[0] > MAX_COMPARE_COUNT) {
                throw new IllegalStateException(getString(R.string.list_sort_action_too_many));
            }
            elementPin1.setValue(first.copy());
            elementPin2.setValue(second.copy());
            executeNext(runnable, comparePin);
            PinNumber<?> value = getPinValue(runnable, resultPin);
            return value == null ? 0 : Double.compare(value.doubleValue(), 0);
        });
        if (!sorted) runnable.addLog(getString(R.string.list_sort_action_failed));
    }

    // 稳定归并排序，比较过程抛异常时中止并保留原顺序
    private boolean trySort(PinList list, SortCompare compare) {
        int size = list.size();
        if (size < 2) return true;

        PinObject[] source = list.toArray(new PinObject[0]);
        PinObject[] target = new PinObject[size];
        try {
            for (int width = 1; width < size; width *= 2) {
                for (int start = 0; start < size; start += width * 2) {
                    int mid = Math.min(start + width, size);
                    int end = Math.min(start + width * 2, size);
                    merge(source, target, start, mid, end, compare);
                }
                PinObject[] temp = source;
                source = target;
                target = temp;
            }
        } catch (RuntimeException e) {
            AppLog.e(e);
            return false;
        }

        list.clear();
        for (PinObject value : source) list.add(value);
        return true;
    }

    private void merge(PinObject[] source, PinObject[] target, int start, int mid, int end, SortCompare compare) {
        int left = start;
        int right = mid;
        for (int i = start; i < end; i++) {
            if (left >= mid) {
                target[i] = source[right++];
            } else if (right >= end) {
                target[i] = source[left++];
            } else if (compare.compare(source[left], source[right]) <= 0) {
                // 相等时先取左侧，保证排序稳定
                target[i] = source[left++];
            } else {
                target[i] = source[right++];
            }
        }
    }

    // 元素自身比较：数字按数值、文本按字符串、布尔 false 在前，其余类型视为相等
    private int compareValue(PinObject first, PinObject second) {
        if (first == null || second == null) return 0;
        if (first instanceof PinNumber<?> number1 && second instanceof PinNumber<?> number2) {
            return Integer.signum(Double.compare(number1.doubleValue(), number2.doubleValue()));
        }
        if (first instanceof PinBoolean boolean1 && second instanceof PinBoolean boolean2) {
            return Integer.signum(Boolean.compare(boolean1.getValue(), boolean2.getValue()));
        }
        if (first instanceof PinString string1 && second instanceof PinString string2) {
            return Integer.signum(string1.toString().compareTo(string2.toString()));
        }
        return 0;
    }

    // 取字典元素里指定键的值，元素不是字典时返回 null
    private PinObject getMapValue(PinObject element, String key) {
        if (element instanceof PinMap map) return map.get(new PinString(key));
        return null;
    }

    private String getString(int resId) {
        return MainApplication.getInstance().getString(resId);
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(listPin, elementPin1, elementPin2);
    }

    // 比较两个元素，返回负数表示前者排在前面
    private interface SortCompare {
        int compare(PinObject first, PinObject second);
    }
}
