package top.bogey.touch_tool.bean.pin.pin_objects.pin_string;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.utils.GsonUtil;

// 控件信息输出针脚：index 表示要输出的内容，value 为计算出的文本
public class PinNodeInfoValue extends PinString {
    private int index = 0;

    public PinNodeInfoValue() {
        super(PinSubType.NODE_INFO);
    }

    public PinNodeInfoValue(int index) {
        super(PinSubType.NODE_INFO);
        this.index = index;
    }

    public PinNodeInfoValue(JsonObject jsonObject) {
        super(jsonObject);
        index = GsonUtil.getAsInt(jsonObject, "index", 0);
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public void reset() {
        super.reset();
        index = 0;
    }

    @Override
    public void sync(PinBase value) {
        super.sync(value);
        if (value instanceof PinNodeInfoValue nodeInfoValue) index = nodeInfoValue.index;
    }
}