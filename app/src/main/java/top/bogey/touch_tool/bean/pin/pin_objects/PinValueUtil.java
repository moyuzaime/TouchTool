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

    // 按声明的类型新建一个值，动态类型按文本处理（空列表 / 空字典能直接填常量）
    public static PinObject newValue(PinObject type) {
        if (type == null || type.isDynamic()) return new PinString();
        PinInfo info = PinInfo.getPinInfo(type);
        if (info != null) {
            PinBase value = info.newInstance();
            if (value instanceof PinObject pinObject) return pinObject;
        }
        return (PinObject) type.copy();
    }

    // 能否用文本直接编辑：动态类型按文本处理，单选项需要选择器这里不支持
    public static boolean isTextEditable(PinObject type) {
        if (type == null || type.isDynamic()) return true;
        if (type instanceof PinSingleSelect) return false;
        return type instanceof PinString || type instanceof PinBoolean
                || type instanceof PinInteger || type instanceof PinFloat
                || type instanceof PinDouble || type instanceof PinLong;
    }

    // 新建值时的默认文本
    public static String getDefaultText(PinObject type) {
        if (type == null || type.isDynamic()) return "";
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

    // 把任意值转成指定类型的新值，转换失败返回 null
    public static PinObject convertTo(PinBase value, PinObject type) {
        if (value == null || type == null) return null;
        PinObject target = newValue(type);
        if (!setText(target, value.toString())) return null;
        return target;
    }

    // 用文本更新值，返回是否成功
    public static boolean setText(PinObject value, String text) {        if (value == null || value instanceof PinSingleSelect) return false;
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
