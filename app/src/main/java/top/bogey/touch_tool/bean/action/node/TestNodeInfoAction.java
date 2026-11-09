package top.bogey.touch_tool.bean.action.node;

import android.os.Build;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.other.NodeInfo;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinAdd;
import top.bogey.touch_tool.bean.pin.pin_objects.PinNode;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

// 测试卡：可添加「输出项」针脚，每个针脚下拉选择内容，结果汇总到「结果」针脚
public class TestNodeInfoAction extends CalculateAction {
    private final transient Pin nodePin = new Pin(new PinNode(), R.string.pin_node);
    private final transient Pin addPin = new Pin(new PinAdd(new Pin(new PinSingleSelect(R.array.node_info_type))));
    private final transient Pin resultPin = new Pin(new PinList(new PinString()), R.string.node_info_test_result, true);

    public TestNodeInfoAction() {
        super(ActionType.TEST_NODE_INFO);
        addPins(nodePin, addPin, resultPin);
    }

    public TestNodeInfoAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(nodePin, addPin, resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinList result = resultPin.getValue(PinList.class);
        result.clear();

        PinNode node = getPinValue(runnable, nodePin);
        if (node == null) return;
        NodeInfo nodeInfo = node.getNodeInfo();
        if (nodeInfo == null) return;

        for (Pin dynamicPin : getDynamicPins()) {
            PinSingleSelect select = dynamicPin.getValue(PinSingleSelect.class);
            if (select == null) continue;
            result.add(new PinString(readNodeInfo(nodeInfo, select.getIndex())));
        }
    }

    // 添加针脚之后的所有输出项
    public List<Pin> getDynamicPins() {
        PinAdd pinAdd = addPin.getValue(PinAdd.class);
        return pinAdd == null ? Collections.emptyList() : pinAdd.getPins();
    }

    private String readNodeInfo(NodeInfo nodeInfo, int index) {
        AccessibilityNodeInfo info = nodeInfo.node;
        switch (index) {
            case 0:
                return safe(nodeInfo.clazz);
            case 1:
                return safe(nodeInfo.id);
            case 2:
                return String.valueOf(nodeInfo.index);
            case 3:
                return safe(nodeInfo.text);
            case 4:
                return safe(nodeInfo.desc);
            case 5:
                return String.valueOf(nodeInfo.index);
            case 6:
                return String.valueOf(nodeInfo.usable);
            case 7:
                return String.valueOf(nodeInfo.visible);
            case 8:
                return nodeInfo.area == null ? "" : nodeInfo.area.toShortString();
            case 9:
                return nodeInfo.area == null ? "" : nodeInfo.area.width() + "x" + nodeInfo.area.height();
            case 10:
                return nodeInfo.getParent() == null ? "" : safe(nodeInfo.getParent().clazz);
            case 11:
                return String.valueOf(nodeInfo.getChildCount() > 0);
            case 12:
                return info == null || info.getPackageName() == null ? "" : info.getPackageName().toString();
            case 13:
                return String.valueOf(info != null && info.isLongClickable());
            case 14:
                return String.valueOf(info != null && info.isScrollable());
            case 15:
                return String.valueOf(info != null && info.isFocusable());
            case 16:
                return String.valueOf(info != null && info.isEditable());
            case 17:
                return String.valueOf(info != null && info.isSelected());
            case 18:
                return String.valueOf(info != null && info.isChecked());
            case 19:
                return String.valueOf(info != null && info.isFocused());
            case 20:
                return String.valueOf(info != null && info.isEnabled());
            case 21:
                return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && info != null && info.getHintText() != null
                        ? info.getHintText().toString() : "";
            case 22:
                return String.valueOf(getNodeDepth(nodeInfo));
            case 23:
                if (info == null) return "0";
                List<AccessibilityNodeInfo.AccessibilityAction> actions = info.getActionList();
                return String.valueOf(actions == null ? 0 : actions.size());
            case 24:
                return info == null ? "0" : String.valueOf(info.getWindowId());
            case 25:
                return info == null ? "0" : String.valueOf(info.getInputType());
            default:
                return "";
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private int getNodeDepth(NodeInfo nodeInfo) {
        int depth = 0;
        NodeInfo parent = nodeInfo.getParent();
        while (parent != null) {
            depth++;
            parent = parent.getParent();
        }
        return depth;
    }
}
