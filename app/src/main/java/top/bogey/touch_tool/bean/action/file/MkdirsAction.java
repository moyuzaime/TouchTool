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

// 创建文件夹（含多级父目录）
public class MkdirsAction extends ExecuteAction {
    private final transient Pin pathPin = new Pin(new PinString(), R.string.file_action_path);

    public MkdirsAction() {
        super(ActionType.MKDIRS);
        addPins(pathPin);
    }

    public MkdirsAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(pathPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinObject path = getPinValue(runnable, pathPin);
        try {
            File dir = new File(path.toString());
            if (!dir.exists()) dir.mkdirs();
        } catch (Throwable ignored) {
        }
        executeNext(runnable, outPin);
    }
}