package top.bogey.touch_tool.bean.action.file;

import com.google.gson.JsonObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

// 读取指定行号的文本（行号从 1 开始）
public class ReadFileLineAction extends CalculateAction {
    private final transient Pin pathPin = new Pin(new PinString(), R.string.file_action_path);
    private final transient Pin linePin = new Pin(new PinInteger(1), R.string.file_action_line_no);
    private final transient Pin textPin = new Pin(new PinString(), R.string.file_action_content, true);

    public ReadFileLineAction() {
        super(ActionType.READ_FILE_LINE);
        addPins(pathPin, linePin, textPin);
    }

    public ReadFileLineAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(pathPin, linePin, textPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinObject path = getPinValue(runnable, pathPin);
        int lineNo = ((top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber<?>) getPinValue(runnable, linePin)).intValue();
        String text = "";
        try {
            File file = new File(path.toString());
            if (file.isFile()) {
                List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                if (lineNo >= 1 && lineNo <= lines.size()) text = lines.get(lineNo - 1);
            }
        } catch (Throwable ignored) {
        }
        textPin.getValue(PinString.class).setValue(text);
    }
}