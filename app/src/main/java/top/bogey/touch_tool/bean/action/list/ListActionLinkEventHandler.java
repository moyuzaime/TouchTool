package top.bogey.touch_tool.bean.action.list;

import java.util.List;

import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinAdd;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.task.Task;

public class ListActionLinkEventHandler {


    public static void onLinkedTo(List<Pin> valuePins, Task task, Pin origin, Pin to) {
        onLinkedTo(valuePins, task, origin, to, false);
    }

    public static void onLinkedTo(List<Pin> valuePins, Task task, Pin origin, Pin to, boolean direct) {
        if (!direct) {
            // 判断当前连接的针脚是否可以确定动态针脚类型
            int count = 0;
            // 连接的针脚本身不为动态的，不执行
            if (!origin.getValue().isDynamic()) return;

            for (Pin pin : valuePins) {
                if (!pin.isLinked()) continue;
                Pin linkedPin = pin.getLinkedPin(task);
                if (linkedPin == null) continue;
                // 两边的值都是动态的，无法确定类型，跳过
                if (pin.getValue().isDynamic() && linkedPin.getValue().isDynamic()) continue;
                count++;
            }
            if (count != 1) return;
        }

        // 第一个有效连接针脚时，设置动态针脚类型
        PinBase template;
        PinBase originValue = origin.getValue();
        PinBase toValue = to.getValue();
        // 列表操作只有列表针脚和普通针脚能连接
        if (originValue instanceof PinList && toValue instanceof PinList toPinList) {
            template = toPinList.getValueType().copy();
        } else {
            template = toValue.copy();
        }
        template.reset();

        for (Pin pin : valuePins) {
            PinBase value = pin.getValue();

            if (value instanceof PinAdd pinAdd) {
                Pin addPin = pinAdd.getPin();
                PinBase addValue = addPin.getValue();
                if (addValue instanceof PinList pinList) {
                    pinList.setValueType((PinObject) template.copy());
                    pinList.reset();
                    addPin.setValue(task, pinList);
                } else {
                    addPin.setValue(task, template.copy());
                }
            } else if (value instanceof PinList pinList) {
                pinList.setValueType((PinObject) template.copy());
                pinList.reset();
                pin.setValue(task, pinList);
            } else {
                pin.setValue(task, template.copy());
            }

            // 已连接的动态针脚需要继续更新连接的动作
            if (pin.isLinked() && (pin != origin || direct)) {
                Pin linkedPin = pin.getLinkedPin(task);
                if (linkedPin != null && linkedPin.getValue().isDynamic()) {
                    Action action = task.getAction(linkedPin.getOwnerId());
                    if (action != null) action.onLinkedTo(task, linkedPin, pin);
                }
            }
        }
    }

    public static void onUnLinkedFrom(List<Pin> valuePins, Task task, Pin origin) {
        // 只有动态类型针脚中的连接断开才处理
        if (!valuePins.contains(origin)) {
            return;
        }
        boolean flag = true;
        for (Pin pin : valuePins) {
            if (pin.isLinked()) {
                flag = false;
                break;
            }
        }
        // 还有连接，不恢复动态类型
        if (!flag) {
            return;
        }
        for (Pin pin : valuePins) {
            PinBase value = pin.getValue();
            if (value instanceof PinAdd pinAdd) {
                Pin addPin = pinAdd.getPin();
                PinBase addValue = addPin.getValue();
                if (addValue instanceof PinList pinList) {
                    pinList.setValueType(new PinObject(PinSubType.DYNAMIC));
                    pinList.reset();
                    addPin.setValue(task, pinList);
                } else {
                    addPin.setValue(task, new PinObject(PinSubType.DYNAMIC));
                }
            } else if (value instanceof PinList pinList) {
                pinList.setValueType(new PinObject(PinSubType.DYNAMIC));
                pinList.reset();
                pin.setValue(task, pinList);
            } else {
                pin.setValue(task, new PinObject(PinSubType.DYNAMIC));
            }
        }
    }
}
