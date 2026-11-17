package top.bogey.touch_tool.bean.action.map;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinAdd;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.task.Task;

public class MapActionLinkEventHandler {

    public static void onLinkedTo(List<Pin> pins, List<Pin> keyPins, List<Pin> valuePins, Task task, Pin origin, Pin to) {
        onLinkedTo(pins, keyPins, valuePins, task, origin, to, false);
    }

    public static void onLinkedTo(List<Pin> pins, List<Pin> keyPins, List<Pin> valuePins, Task task, Pin origin, Pin to, boolean direct) {
        boolean keyFlag, valueFlag;
        if (direct && to.getValue() instanceof PinMap pinMap) {
            keyFlag = !pinMap.isDynamicKey();
            valueFlag = !pinMap.isDynamicValue();
        } else {
            // 连接的针脚本身不为动态的，不执行
            if (origin.getValue() instanceof PinMap pinMap) {
                if (!pinMap.isHalfDynamic()) return;
            } else {
                if (!origin.getValue().isDynamic()) return;
            }

            // 判断当前连接的针脚是否可以确定动态针脚类型
            List<Pin> keys = new ArrayList<>();
            List<Pin> values = new ArrayList<>();
            for (Pin pin : pins) {
                if (pin.isLinked()) {
                    PinBase value = pin.getValue();

                    Pin linkedPin = pin.getLinkedPin(task);
                    if (linkedPin == null) continue;
                    PinBase linkedValue = linkedPin.getValue();

                    if (keyPins.contains(pin)) {
                        // 两边的值都是动态的，无法确定类型，跳过
                        if (value.isDynamic() && linkedValue.isDynamic()) continue;
                        keys.add(pin);
                    } else if (valuePins.contains(pin)) {
                        // 两边的值都是动态的，无法确定类型，跳过
                        if (value.isDynamic() && linkedValue.isDynamic()) continue;
                        values.add(pin);
                    } else if (value instanceof PinMap pinMap) {
                        if (linkedValue instanceof PinMap linkedPinMap) {
                            if (!(pinMap.isDynamicKey() && linkedPinMap.isDynamicKey())) keys.add(pin);
                            if (!(pinMap.isDynamicValue() && linkedPinMap.isDynamicValue())) values.add(pin);
                        }
                    }
                }
            }

            // 第一个有效连接针脚时，设置动态针脚类型
            keyFlag = keys.size() == 1 && keys.contains(origin);
            valueFlag = values.size() == 1 && values.contains(origin);
        }

        if (!keyFlag && !valueFlag) {
            return;
        }

        // 当针脚为首个连接的Key或首个连接的Value时，设置动态针脚类型
        PinBase keyTemplate = null, valueTemplate = null;
        PinBase originValue = origin.getValue();
        PinBase toValue = to.getValue();
        // 字典操作有字典，列表和普通针脚能连接
        if (originValue instanceof PinMap && toValue instanceof PinMap toPinMap) {
            if (keyFlag) keyTemplate = toPinMap.getKeyType().copy();
            if (valueFlag) valueTemplate = toPinMap.getValueType().copy();
        } else if (originValue instanceof PinList && toValue instanceof PinList toPinList) {
            if (keyFlag) keyTemplate = toPinList.getValueType().copy();
            if (valueFlag) valueTemplate = toPinList.getValueType().copy();
        } else {
            if (keyFlag) keyTemplate = toValue.copy();
            if (valueFlag) valueTemplate = toValue.copy();
        }
        if (keyFlag) keyTemplate.reset();
        if (valueFlag) valueTemplate.reset();

        for (Pin pin : pins) {
            PinBase value = pin.getValue();
            boolean isDynamic = value.isDynamic();

            if (value instanceof PinAdd pinAdd) {
                if (keyFlag) pinAdd.getPin(0).setValue(keyTemplate.copy());
                if (valueFlag) pinAdd.getPin(1).setValue(valueTemplate.copy());
            } else if (value instanceof PinMap pinMap) {
                isDynamic = pinMap.isHalfDynamic();
                if (keyFlag) pinMap.setKeyType((PinObject) keyTemplate.copy());
                if (valueFlag) pinMap.setValueType((PinObject) valueTemplate.copy());
                pinMap.reset();
                pin.setValue(task, pinMap); // 通知针脚刷新
            } else if (value instanceof PinList pinList) {
                if (keyFlag && keyPins.contains(pin)) pinList.setValueType((PinObject) keyTemplate.copy());
                if (valueFlag && valuePins.contains(pin)) pinList.setValueType((PinObject) valueTemplate.copy());
                pinList.reset();
                pin.setValue(task, pinList); // 通知针脚刷新
            } else if (keyFlag && keyPins.contains(pin)) {
                pin.setValue(task, keyTemplate.copy());
            } else if (valueFlag && valuePins.contains(pin)) {
                pin.setValue(task, valueTemplate.copy());
            }

            // 已连接的动态针脚需要继续更新连接的动作
            if (isDynamic && pin.isLinked() && (pin != origin || direct)) {
                Pin linkedPin = pin.getLinkedPin(task);
                if (linkedPin != null) {
                    PinBase linkedValue = linkedPin.getValue();
                    boolean flag;
                    if (linkedValue instanceof PinMap pinMap) flag = pinMap.isHalfDynamic();
                    else flag = linkedValue.isDynamic();
                    if (flag) {
                        Action action = task.getAction(linkedPin.getOwnerId());
                        if (action != null) action.onLinkedTo(task, linkedPin, pin);
                    }
                }
            }
        }
    }

    public static void onUnLinkedFrom(List<Pin> pins, List<Pin> keyPins, List<Pin> valuePins, Task task, Pin origin) {
        int keyCount = 0;
        int valueCount = 0;

        for (Pin pin : pins) {
            if (pin.isLinked()) {
                if (keyPins.contains(pin)) {
                    keyCount++;
                } else if (valuePins.contains(pin)) {
                    valueCount++;
                } else if (pin.getValue() instanceof PinMap pinMap) {
                    if (!pinMap.isDynamicKey()) keyCount++;
                    if (!pinMap.isDynamicValue()) valueCount++;
                }
            }
        }

        boolean keyFlag = keyCount == 0 && (origin.isSameClass(PinMap.class) || keyPins.contains(origin));
        boolean valueFlag = valueCount == 0 && (origin.isSameClass(PinMap.class) || valuePins.contains(origin));
        if (!keyFlag && !valueFlag) return;

        for (Pin pin : pins) {
            PinBase value = pin.getValue();
            if (value instanceof PinAdd pinAdd) {
                if (keyFlag) pinAdd.getPin(0).setValue(new PinObject(PinSubType.DYNAMIC));
                if (valueFlag) pinAdd.getPin(1).setValue(new PinObject(PinSubType.DYNAMIC));
            } else if (value instanceof PinMap pinMap) {
                if (keyFlag) pinMap.setKeyType(new PinObject(PinSubType.DYNAMIC));
                if (valueFlag) pinMap.setValueType(new PinObject(PinSubType.DYNAMIC));
                pinMap.reset();
                pin.setValue(task, pinMap); // 通知针脚刷新
            } else if (value instanceof PinList pinList) {
                pinList.setValueType(new PinObject(PinSubType.DYNAMIC));
                pinList.reset();
                pin.setValue(task, pinList); // 通知针脚刷新
            } else if (keyFlag && keyPins.contains(pin)) {
                pin.setValue(task, new PinObject(PinSubType.DYNAMIC));
            } else if (valueFlag && valuePins.contains(pin)) {
                pin.setValue(task, new PinObject(PinSubType.DYNAMIC));
            }
        }
    }
}
