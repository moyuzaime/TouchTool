package top.bogey.touch_tool.bean.action.file;

import com.google.gson.JsonObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

// 读取文件内容为文本
public class ReadFileAction extends CalculateAction {
    private final transient Pin pathPin = new Pin(new PinString(), R.string.file_action_path);
    private final transient Pin textPin = new Pin(new PinString(), R.string.file_action_content, true);

    public ReadFileAction() {
        super(ActionType.READ_FILE);
        addPins(pathPin, textPin);
    }

    public ReadFileAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(pathPin, textPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinObject path = getPinValue(runnable, pathPin);
        String content = "";
        try {
            File file = new File(path.toString());
            if (file.exists() && file.isFile()) {
                content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            }
        } catch (Throwable ignored) {
        }
        textPin.getValue(PinString.class).setValue(content);
    }
}