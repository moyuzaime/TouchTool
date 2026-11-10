package top.bogey.touch_tool.bean.action.node;

import android.os.Build;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.gson.JsonObject;

import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.other.NodeInfo;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinNode;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinNodePathString;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

public class GetNodeInfoAction extends CalculateAction {
    private final transient Pin nodePin = new Pin(new PinNode(), R.string.pin_node);
    private final transient Pin clazzPin = new Pin(new PinString(), R.string.get_node_info_action_clazz, true);
    private final transient Pin idPin = new Pin(new PinString(), R.string.get_node_info_action_id, true, false, true);
    private final transient Pin textPin = new Pin(new PinString(), R.string.pin_string, true);
    private final transient Pin descPin = new Pin(new PinString(), R.string.get_node_info_action_node_desc, true, false, true);
    private final transient Pin areaPin = new Pin(new PinArea(), R.string.pin_area, true);
    private final transient Pin usablePin = new Pin(new PinBoolean(), R.string.get_node_info_action_usable, true, false, true);
    private final transient Pin visiblePin = new Pin(new PinBoolean(), R.string.get_node_info_action_visible, true, false, true);
    private final transient Pin pathPin = new Pin(new PinNodePathString(), R.string.pin_string_node_path, true, false, true);
    // 更完整的控件属性，取自 AccessibilityNodeInfo
    private final transient Pin packagePin = new Pin(new PinString(), R.string.get_node_info_action_package, true);
    private final transient Pin longClickablePin = new Pin(new PinBoolean(), R.string.get_node_info_action_long_clickable, true);
    private final transient Pin scrollablePin = new Pin(new PinBoolean(), R.string.get_node_info_action_scrollable, true);
    private final transient Pin focusablePin = new Pin(new PinBoolean(), R.string.get_node_info_action_focusable, true);
    private final transient Pin editablePin = new Pin(new PinBoolean(), R.string.get_node_info_action_editable, true);
    private final transient Pin selectedPin = new Pin(new PinBoolean(), R.string.get_node_info_action_selected, true);
    private final transient Pin checkedPin = new Pin(new PinBoolean(), R.string.get_node_info_action_checked, true);
    private final transient Pin focusedPin = new Pin(new PinBoolean(), R.string.get_node_info_action_focused, true);
    private final transient Pin enabledPin = new Pin(new PinBoolean(), R.string.get_node_info_action_enabled, true);
    private final transient Pin hintPin = new Pin(new PinString(), R.string.get_node_info_action_hint, true);
    private final transient Pin depthPin = new Pin(new PinInteger(), R.string.get_node_info_action_depth, true);
    private final transient Pin actionCountPin = new Pin(new PinInteger(), R.string.get_node_info_action_action_count, true);
    private final transient Pin windowIdPin = new Pin(new PinInteger(), R.string.get_node_info_action_window_id, true);
    private final transient Pin inputTypePin = new Pin(new PinInteger(), R.string.get_node_info_action_input_type, true);
    private final transient Pin indexPin = new Pin(new PinInteger(), R.string.get_node_info_action_index, true);
    private final transient Pin childCountPin = new Pin(new PinInteger(), R.string.get_node_info_action_child_count, true);

    public GetNodeInfoAction() {
        super(ActionType.GET_NODE_INFO);
        addPins(nodePin, clazzPin, idPin, textPin, descPin, areaPin, usablePin, visiblePin, pathPin,
                packagePin, longClickablePin, scrollablePin, focusablePin, editablePin, selectedPin, checkedPin,
                focusedPin, enabledPin, hintPin, depthPin, actionCountPin, windowIdPin, inputTypePin, indexPin, childCountPin);
    }

    public GetNodeInfoAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(nodePin, clazzPin, idPin, textPin, descPin, areaPin, usablePin, visiblePin, pathPin,
                packagePin, longClickablePin, scrollablePin, focusablePin, editablePin, selectedPin, checkedPin,
                focusedPin, enabledPin, hintPin, depthPin, actionCountPin, windowIdPin, inputTypePin, indexPin, childCountPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinNode node = getPinValue(runnable, nodePin);
        if (node == null) return;
        NodeInfo nodeInfo = node.getNodeInfo();
        if (nodeInfo == null) return;
        clazzPin.getValue(PinString.class).setValue(nodeInfo.clazz);
        idPin.getValue(PinString.class).setValue(nodeInfo.id);
        textPin.getValue(PinString.class).setValue(nodeInfo.text);
        descPin.getValue(PinString.class).setValue(nodeInfo.desc);
        areaPin.getValue(PinArea.class).setValue(nodeInfo.area);
        usablePin.getValue(PinBoolean.class).setValue(nodeInfo.usable);
        visiblePin.getValue(PinBoolean.class).setValue(nodeInfo.visible);
        pathPin.getValue(PinNodePathString.class).setValue(nodeInfo);

        AccessibilityNodeInfo info = nodeInfo.node;
        packagePin.getValue(PinString.class).setValue(info == null || info.getPackageName() == null ? "" : info.getPackageName().toString());
        longClickablePin.getValue(PinBoolean.class).setValue(info != null && info.isLongClickable());
        scrollablePin.getValue(PinBoolean.class).setValue(info != null && info.isScrollable());
        focusablePin.getValue(PinBoolean.class).setValue(info != null && info.isFocusable());
        editablePin.getValue(PinBoolean.class).setValue(info != null && info.isEditable());
        selectedPin.getValue(PinBoolean.class).setValue(info != null && info.isSelected());
        checkedPin.getValue(PinBoolean.class).setValue(info != null && info.isChecked());
        focusedPin.getValue(PinBoolean.class).setValue(info != null && info.isFocused());
        enabledPin.getValue(PinBoolean.class).setValue(info != null && info.isEnabled());
        // 提示文本需要 API 26
        hintPin.getValue(PinString.class).setValue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && info != null && info.getHintText() != null
                ? info.getHintText().toString() : "");
        depthPin.getValue(PinInteger.class).setValue(getNodeDepth(nodeInfo));
        if (info == null) {
            actionCountPin.getValue(PinInteger.class).setValue(0);
            windowIdPin.getValue(PinInteger.class).setValue(0);
            inputTypePin.getValue(PinInteger.class).setValue(0);
        } else {
            List<AccessibilityNodeInfo.AccessibilityAction> actions = info.getActionList();
            actionCountPin.getValue(PinInteger.class).setValue(actions == null ? 0 : actions.size());
            windowIdPin.getValue(PinInteger.class).setValue(info.getWindowId());
            inputTypePin.getValue(PinInteger.class).setValue(info.getInputType());
        }
        indexPin.getValue(PinInteger.class).setValue(nodeInfo.index);
        childCountPin.getValue(PinInteger.class).setValue(nodeInfo.getChildCount());
    }

    // 层级深度：沿父节点链计数
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
