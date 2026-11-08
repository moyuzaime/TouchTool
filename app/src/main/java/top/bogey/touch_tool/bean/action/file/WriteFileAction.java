package top.bogey.touch_tool.bean.action.file;

import com.google.gson.JsonObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

// 写入文本到文件（覆盖）
public class WriteFileAction extends ExecuteAction {
    private final transient Pin pathPin = new Pin(new PinString(), R.string.file_action_path);
    private final transient Pin dataPin = new Pin(new PinString(), R.string.file_action_content);

    public WriteFileAction() {
        super(ActionType.WRITE_FILE);
        addPins(pathPin, dataPin);
    }

    public WriteFileAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(pathPin, dataPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinObject path = getPinValue(runnable, pathPin);
        PinObject data = getPinValue(runnable, dataPin);
        try {
            File file = new File(path.toString());
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            Files.write(file.toPath(), data.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
        }
        executeNext(runnable, outPin);
    }
}