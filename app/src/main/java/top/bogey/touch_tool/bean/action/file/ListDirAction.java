package top.bogey.touch_tool.bean.action.file;

import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

// 列出目录下的文件名
public class ListDirAction extends CalculateAction {
    private final transient Pin pathPin = new Pin(new PinString(), R.string.file_action_path);
    private final transient Pin filesPin = new Pin(new PinList(new PinString()), R.string.file_action_files, true);

    public ListDirAction() {
        super(ActionType.LIST_DIR);
        addPins(pathPin, filesPin);
    }

    public ListDirAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(pathPin, filesPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinObject path = getPinValue(runnable, pathPin);
        List<String> names = new ArrayList<>();
        File dir = new File(path.toString());
        if (dir.isDirectory()) {
            String[] list = dir.list();
            if (list != null) for (String name : list) names.add(name);
        }
        PinList files = filesPin.getValue(PinList.class);
        files.clear();
        for (String name : names) files.add(new PinString(name));
    }
}