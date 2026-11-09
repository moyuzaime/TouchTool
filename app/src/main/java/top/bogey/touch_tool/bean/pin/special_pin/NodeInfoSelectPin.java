package top.bogey.touch_tool.bean.pin.special_pin;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;

// 控件信息输出项针脚：下拉选择要读取的控件信息，不可被连线
public class NodeInfoSelectPin extends NotLinkAblePin {

    public NodeInfoSelectPin() {
        super(new PinSingleSelect(R.array.node_info_type), R.string.node_info_test_item);
    }

    public NodeInfoSelectPin(JsonObject jsonObject) {
        super(jsonObject);
    }
}