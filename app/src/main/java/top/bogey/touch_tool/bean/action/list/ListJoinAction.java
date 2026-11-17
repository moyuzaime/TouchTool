package top.bogey.touch_tool.bean.action.list;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

/**
 * 列表连接文本：元素逐个 toString 后用分隔符连起来
 */
public class ListJoinAction extends ListCalculateAction {
    private final transient Pin listPin = new Pin(new PinList());
    private final transient Pin separatorPin = new Pin(new PinString(), R.string.list_join_action_separator);
    private final transient Pin resultPin = new Pin(new PinString(), R.string.list_join_action_result, true);

    public ListJoinAction() {
        super(ActionType.LIST_JOIN);
        addPins(listPin, separatorPin, resultPin);
    }

    public ListJoinAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(listPin, separatorPin, resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinList list = getPinValue(runnable, listPin);
        PinString separator = getPinValue(runnable, separatorPin);
        String join = separator == null ? "" : separator.toString();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) builder.append(join);
            builder.append(list.get(i));
        }
        resultPin.getValue(PinString.class).setValue(builder.toString());
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(listPin, resultPin);
    }
}
