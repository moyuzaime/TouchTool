package top.bogey.touch_tool.bean.action.node;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.other.NodeInfo;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinNode;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinNodePathString;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinNodePathTextString;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.bean.pin.special_pin.ShowAblePin;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.custom.float_view.MarkTargetFloatView;

public class IsNodeExistAction extends CalculateAction {
    private final transient Pin typePin = new NotLinkAblePin(new PinSingleSelect(R.array.find_node_type), R.string.is_node_exist_action_type);
    private final transient Pin pathPin = new PathShowablePin(new PinNodePathString(), R.string.is_node_exist_action_path);
    private final transient Pin fullPathPin = new PathShowablePin(new PinBoolean(true), R.string.is_node_exist_action_full_path);
    private final transient Pin textPin = new TextShowablePin(new PinString(), R.string.pin_string);
    private final transient Pin idPin = new IdShowablePin(new PinString(), R.string.is_node_exist_action_id);
    private final transient Pin classPin = new ClassShowablePin(new PinString(), R.string.is_node_exist_action_class);
    private final transient Pin descPin = new DescShowablePin(new PinString(), R.string.is_node_exist_action_node_desc);
    private final transient Pin areaPin = new NotAllPathShowablePin(new PinArea(), R.string.pin_area);
    private final transient Pin pathTextPin = new PathTextShowablePin(new PinNodePathTextString(), R.string.find_node_action_regex_path);
    private final transient Pin resultPin = new Pin(new PinBoolean(), R.string.pin_boolean_result, true);
    // 命中的控件，供下游动作直接使用
    private final transient Pin nodePin = new Pin(new PinNode(), R.string.pin_node, true);
    // 命中的全部控件（路径模式只有单个，列表针脚在该模式下隐藏）
    private final transient Pin nodesPin = new NotPathShowablePin(new PinList(new PinNode()), true);

    public IsNodeExistAction() {
        super(ActionType.IS_NODE_EXIST);
        addPins(typePin, pathPin, fullPathPin, textPin, idPin, classPin, descPin, areaPin, pathTextPin, resultPin, nodePin, nodesPin);
    }

    public IsNodeExistAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(typePin, pathPin, fullPathPin, textPin, idPin, classPin, descPin, areaPin, pathTextPin, resultPin, nodePin, nodesPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinSingleSelect type = typePin.getValue();
        boolean result = false;
        NodeInfo foundNode = null;
        List<NodeInfo> foundNodes = new ArrayList<>();

        switch (type.getIndex()) {
            case 0 -> {
                PinObject pathString = getPinValue(runnable, pathPin);
                PinNodePathString path = new PinNodePathString(pathString.toString());
                PinBoolean fullPath = getPinValue(runnable, fullPathPin);
                NodeInfo nodeInfo = path.findNode(NodeInfo.getWindows(), fullPath.getValue());
                if (nodeInfo == null) break;
                result = true;
                foundNode = nodeInfo;
                foundNodes.add(nodeInfo);
                MarkTargetFloatView.showTargetArea(nodeInfo.area);
            }
            case 1, 2 -> {
                PinArea area = getPinValue(runnable, areaPin);
                NodeInfo nodeInfo = NodeInfo.getActiveWindow();
                if (nodeInfo == null) break;
                List<NodeInfo> children;
                if (type.getIndex() == 1) {
                    PinObject text = getPinValue(runnable, textPin);
                    String value = text.toString();
                    if (value.isEmpty()) break;
                    children = nodeInfo.findChildrenByText(value, area.getValue());
                } else {
                    PinObject id = getPinValue(runnable, idPin);
                    String value = id.toString();
                    if (value.isEmpty()) break;
                    children = nodeInfo.findChildrenById(value, area.getValue());
                }
                if (children == null || children.isEmpty()) break;
                foundNode = children.get(0);
                foundNodes.addAll(children);
                for (NodeInfo child : children) {
                    MarkTargetFloatView.showTargetArea(child.area);
                }
                result = true;
            }
            case 3 -> {
                PinObject pathString = getPinValue(runnable, pathTextPin);
                PinNodePathTextString path = new PinNodePathTextString(pathString.toString());
                List<NodeInfo> findNodes = path.findNodes(NodeInfo.getWindows());
                if (findNodes == null || findNodes.isEmpty()) break;
                foundNode = findNodes.get(0);
                foundNodes.addAll(findNodes);
                for (NodeInfo findNode : findNodes) {
                    MarkTargetFloatView.showTargetArea(findNode.area);
                }
                result = true;
            }
            case 4 -> {
                PinArea area = getPinValue(runnable, areaPin);
                PinObject className = getPinValue(runnable, classPin);
                NodeInfo nodeInfo = NodeInfo.getActiveWindow();
                if (nodeInfo == null) break;
                List<NodeInfo> children = nodeInfo.findChildrenByClass(className.toString(), area.getValue());
                if (children == null || children.isEmpty()) break;
                foundNode = children.get(0);
                foundNodes.addAll(children);
                for (NodeInfo child : children) {
                    MarkTargetFloatView.showTargetArea(child.area);
                }
                result = true;
            }
            case 5 -> {
                PinArea area = getPinValue(runnable, areaPin);
                PinObject desc = getPinValue(runnable, descPin);
                NodeInfo nodeInfo = NodeInfo.getActiveWindow();
                if (nodeInfo == null) break;
                List<NodeInfo> children = nodeInfo.findChildrenByDesc(desc.toString(), area.getValue());
                if (children == null || children.isEmpty()) break;
                foundNode = children.get(0);
                foundNodes.addAll(children);
                for (NodeInfo child : children) {
                    MarkTargetFloatView.showTargetArea(child.area);
                }
                result = true;
            }
        }

        resultPin.getValue(PinBoolean.class).setValue(result);
        nodePin.getValue(PinNode.class).setNodeInfo(result ? foundNode : null);
        PinList nodeList = nodesPin.getValue(PinList.class);
        nodeList.clear();
        for (NodeInfo node : foundNodes) nodeList.add(new PinNode(node));
    }

    private int getTypeValue() {
        PinSingleSelect type = typePin.getValue();
        return type.getIndex();
    }

    private static class PathShowablePin extends ShowAblePin {
        public PathShowablePin(PinBase value, int titleId) {
            super(value, titleId);
        }

        @Override
        public boolean showAble(Task context) {
            IsNodeExistAction action = (IsNodeExistAction) context.getAction(getOwnerId());
            return action.getTypeValue() == 0;
        }
    }

    private static class PathTextShowablePin extends ShowAblePin {
        public PathTextShowablePin(PinBase value, int titleId) {
            super(value, titleId);
        }

        @Override
        public boolean showAble(Task context) {
            IsNodeExistAction action = (IsNodeExistAction) context.getAction(getOwnerId());
            return action.getTypeValue() == 3;
        }
    }

    private static class NotAllPathShowablePin extends ShowAblePin {
        public NotAllPathShowablePin(PinBase value, int titleId) {
            super(value, titleId);
        }

        @Override
        public boolean showAble(Task context) {
            IsNodeExistAction action = (IsNodeExistAction) context.getAction(getOwnerId());
            return action.getTypeValue() != 0 && action.getTypeValue() != 3;
        }
    }

    private static class NotPathShowablePin extends ShowAblePin {
        public NotPathShowablePin(PinBase value, int titleId, boolean out) {
            super(value, titleId, out);
        }

        public NotPathShowablePin(PinBase value, boolean out) {
            super(value, out);
        }

        @Override
        public boolean showAble(Task context) {
            IsNodeExistAction action = (IsNodeExistAction) context.getAction(getOwnerId());
            return action.getTypeValue() != 0;
        }
    }

    private static class TextShowablePin extends ShowAblePin {
        public TextShowablePin(PinBase value, int titleId) {
            super(value, titleId);
        }

        @Override
        public boolean showAble(Task context) {
            IsNodeExistAction action = (IsNodeExistAction) context.getAction(getOwnerId());
            return action.getTypeValue() == 1;
        }
    }

    private static class IdShowablePin extends ShowAblePin {
        public IdShowablePin(PinBase value, int titleId) {
            super(value, titleId);
        }

        @Override
        public boolean showAble(Task context) {
            IsNodeExistAction action = (IsNodeExistAction) context.getAction(getOwnerId());
            return action.getTypeValue() == 2;
        }
    }

    private static class ClassShowablePin extends ShowAblePin {
        public ClassShowablePin(PinBase value, int titleId) {
            super(value, titleId);
        }

        @Override
        public boolean showAble(Task context) {
            IsNodeExistAction action = (IsNodeExistAction) context.getAction(getOwnerId());
            return action.getTypeValue() == 4;
        }
    }

    private static class DescShowablePin extends ShowAblePin {
        public DescShowablePin(PinBase value, int titleId) {
            super(value, titleId);
        }

        @Override
        public boolean showAble(Task context) {
            IsNodeExistAction action = (IsNodeExistAction) context.getAction(getOwnerId());
            return action.getTypeValue() == 5;
        }
    }
}
