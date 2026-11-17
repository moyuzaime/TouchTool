package top.bogey.touch_tool.bean.pin.pin_objects;

import top.bogey.touch_tool.bean.pin.PinInfo;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinDouble;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinFloat;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinLong;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;

/**
 * 列表 / 字典常量编辑器用到的值读写
 */
public class PinValueUtil {

    // 按声明的类型新建一个值，类型未知时复制类型对象兜底
    public static PinObject newValue(PinObject type) {
        if (type == null) return new PinObject(PinSubType.DYNAMIC);
        PinInfo info = PinInfo.getPinInfo(type);
        if (info != null) {
            PinBase value = info.newInstance();
            if (value instanceof PinObject pinObject) return pinObject;
        }
        return (PinObject) type.copy();
    }

    // 能否用文本直接编辑，单选项需要选择器，这里不支持
    public static boolean isTextEditable(PinObject type) {
        if (type == null || type instanceof PinSingleSelect) return false;
        return type instanceof PinString || type instanceof PinBoolean
                || type instanceof PinInteger || type instanceof PinFloat
                || type instanceof PinDouble || type instanceof PinLong;
    }

    // 新建值时的默认文本
    public static String getDefaultText(PinObject type) {
        if (type instanceof PinBoolean) return "false";
        if (type instanceof PinNumber<?>) return "0";
        return "";
    }

    // 值的显示文本，数字未赋值时显示空
    public static String getText(PinBase value) {
        if (value == null) return "";
        if (value instanceof PinNumber<?> number && number.getValue() == null) return "";
        return value.toString();
    }

    // 用文本更新值，返回是否成功
    public static boolean setText(PinObject value, String text) {
        if (value == null || value instanceof PinSingleSelect) return false;
        if (value instanceof PinString string) {
            string.setValue(text);
            return true;
        }
        if (value instanceof PinBoolean bool) {
            bool.setValue("true".equalsIgnoreCase(text) || "1".equals(text));
            return true;
        }
        if (value instanceof PinNumber<?>) return value.cast(text);
        return false;
    }
}
