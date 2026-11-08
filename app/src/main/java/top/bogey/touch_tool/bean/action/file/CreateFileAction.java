package top.bogey.touch_tool.bean.action.file;

import com.google.gson.JsonObject;

import java.io.File;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

// 创建空文件（已存在则忽略）
public class CreateFileAction extends ExecuteAction {
    private final transient Pin pathPin = new Pin(new PinString(), R.string.file_action_path);

    public CreateFileAction() {
        super(ActionType.CREATE_FILE);
        addPins(pathPin);
    }

    public CreateFileAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(pathPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinObject path = getPinValue(runnable, pathPin);
        try {
            File file = new File(path.toString());
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (!file.exists()) file.createNewFile();
        } catch (Throwable ignored) {
        }
        executeNext(runnable, outPin);
    }
}