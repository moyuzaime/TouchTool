package top.bogey.touch_tool.bean.pin.pin_objects;

import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.utils.AppLog;

/**
 * 容器元素的软类型校验：只提示，不拦截，不改变已有任务的运行结果
 */
public class PinTypeUtil {

    // 元素能否放进声明了 containerType 的容器，动态类型一律放行
    public static boolean isCompatible(PinObject containerType, PinBase element) {
        if (containerType == null || element == null) return true;
        if (containerType.isDynamic() || element.isDynamic()) return true;
        return containerType.linkFromAble(element);
    }

    // 运行期只记一条日志，元素照常放入
    public static void warnRuntime(PinObject containerType, PinBase element) {
        if (isCompatible(containerType, element)) return;
        AppLog.d("容器", "元素类型与容器声明类型不一致：" + describe(containerType) + " <- " + describe(element));
    }

    // 单个元素针脚的列表容器校验
    public static void checkListPin(ActionCheckResult result, Pin containerPin, Pin elementPin, Task task) {
        if (containerPin == null || !(containerPin.getValue() instanceof PinList list)) return;
        checkLinkedPin(result, list.getValueType(), elementPin, task);
    }

    // 一组动态元素针脚的列表容器校验
    public static void checkListPins(ActionCheckResult result, Pin containerPin, List<Pin> elementPins, Task task) {
        if (containerPin == null || !(containerPin.getValue() instanceof PinList list)) return;
        for (Pin elementPin : elementPins) {
            checkLinkedPin(result, list.getValueType(), elementPin, task);
        }
    }

    // 单组键值针脚的字典容器校验
    public static void checkMapPins(ActionCheckResult result, Pin containerPin, Pin keyPin, Pin valuePin, Task task) {
        if (containerPin == null || !(containerPin.getValue() instanceof PinMap map)) return;
        checkLinkedPin(result, map.getKeyType(), keyPin, task);
        checkLinkedPin(result, map.getValueType(), valuePin, task);
    }

    // 成对出现的键值针脚的字典容器校验
    public static void checkMapPairs(ActionCheckResult result, Pin containerPin, List<Pin> dynamicPins, Task task) {
        if (containerPin == null || !(containerPin.getValue() instanceof PinMap map)) return;
        for (int i = 0; i + 1 < dynamicPins.size(); i += 2) {
            checkLinkedPin(result, map.getKeyType(), dynamicPins.get(i), task);
            checkLinkedPin(result, map.getValueType(), dynamicPins.get(i + 1), task);
        }
    }

    // 已连线的元素针脚和容器声明类型不一致时给一条警告，不自动断开连线
    private static void checkLinkedPin(ActionCheckResult result, PinObject containerType, Pin elementPin, Task task) {
        if (elementPin == null || !elementPin.isLinked()) return;
        Pin linkedPin = elementPin.getLinkedPin(task);
        if (linkedPin == null) return;
        if (isCompatible(containerType, linkedPin.getValue())) return;
        result.addResult(ActionCheckResult.ResultType.WARNING, R.string.check_container_element_type_warning);
    }

    private static String describe(PinBase value) {
        if (value == null) return "null";
        return value.getType().name() + "/" + value.getSubType().name();
    }
}
