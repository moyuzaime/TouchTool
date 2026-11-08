package top.bogey.touch_tool.bean.action.file;

import com.google.gson.JsonObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

// 写入指定行：行号存在则覆盖该行，超出末尾则追加到末尾
public class WriteFileLineAction extends ExecuteAction {
    private final transient Pin pathPin = new Pin(new PinString(), R.string.file_action_path);
    private final transient Pin linePin = new Pin(new top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger(1), R.string.file_action_line_no);
    private final transient Pin dataPin = new Pin(new PinString(), R.string.file_action_content);

    public WriteFileLineAction() {
        super(ActionType.WRITE_FILE_LINE);
        addPins(pathPin, linePin, dataPin);
    }

    public WriteFileLineAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(pathPin, linePin, dataPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinObject path = getPinValue(runnable, pathPin);
        int lineNo = ((PinNumber<?>) getPinValue(runnable, linePin)).intValue();
        String data = getPinValue(runnable, dataPin).toString();
        try {
            File file = new File(path.toString());
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            List<String> lines = file.isFile()
                    ? new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8))
                    : new ArrayList<>();
            if (lineNo >= 1 && lineNo <= lines.size()) {
                lines.set(lineNo - 1, data);
            } else {
                lines.add(data);
            }
            Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
        }
        executeNext(runnable, outPin);
    }
}