package top.bogey.touch_tool.bean.action.map;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.service.TaskRunnable;

public class MapGetValuesAction extends MapCalculateAction {
    private final transient Pin mapPin = new Pin(new PinMap());
    private final transient Pin values = new Pin(new PinList(), R.string.map_action_value, true);

    public MapGetValuesAction() {
        super(ActionType.MAP_VALUES);
        addPins(mapPin, values);
    }

    public MapGetValuesAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(mapPin, values);
    }

    @Override
    public void resetReturnValue(TaskRunnable runnable, Pin pin) {
        values.setValue(new PinList());
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinMap map = getPinValue(runnable, mapPin);
        // 结果列表继承字典的值类型，避免派生结果一直是动态类型
        PinList list = new PinList((PinObject) map.getValueType().copy());
        list.addAll(map.values());
        values.setValue(list);
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(mapPin, values);
    }


    @NonNull
    @Override
    public List<Pin> getDynamicValueTypePins() {
        return Collections.singletonList(values);
    }
}
