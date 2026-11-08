package top.bogey.touch_tool.bean.action.file;

import com.google.gson.JsonObject;

import java.io.File;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.service.TaskRunnable;

// 创建文件或文件夹
public class CreateFileAction extends ExecuteAction {
    private final transient Pin pathPin = new Pin(new PinString(), R.string.file_action_path);
    private final transient Pin modePin = new NotLinkAblePin(new PinSingleSelect(R.array.file_create_mode), R.string.file_action_create_mode);

    public CreateFileAction() {
        super(ActionType.CREATE_FILE);
        addPins(pathPin, modePin);
    }

    public CreateFileAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(pathPin, modePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinObject path = getPinValue(runnable, pathPin);
        boolean dir = modePin.getValue(PinSingleSelect.class).getIndex() == 1;
        try {
            File file = new File(path.toString());
            if (dir) {
                if (!file.exists()) file.mkdirs();
            } else {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                if (!file.exists()) file.createNewFile();
            }
        } catch (Throwable ignored) {
        }
        executeNext(runnable, outPin);
    }
}
