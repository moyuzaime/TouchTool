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
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

// 删除行：行号大于 0 时按行号删，否则按包含内容删
public class DeleteFileLineAction extends ExecuteAction {
    private final transient Pin pathPin = new Pin(new PinString(), R.string.file_action_path);
    private final transient Pin linePin = new Pin(new PinInteger(0), R.string.file_action_line_no);
    private final transient Pin containsPin = new Pin(new PinString(), R.string.file_action_contains);

    public DeleteFileLineAction() {
        super(ActionType.DELETE_FILE_LINE);
        addPins(pathPin, linePin, containsPin);
    }

    public DeleteFileLineAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(pathPin, linePin, containsPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinObject path = getPinValue(runnable, pathPin);
        int lineNo = ((top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber<?>) getPinValue(runnable, linePin)).intValue();
        String contains = getPinValue(runnable, containsPin).toString();
        try {
            File file = new File(path.toString());
            if (file.isFile()) {
                List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                List<String> result = new ArrayList<>();
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    boolean remove;
                    if (lineNo > 0) {
                        remove = (i + 1) == lineNo;
                    } else {
                        remove = !contains.isEmpty() && line.contains(contains);
                    }
                    if (!remove) result.add(line);
                }
                Files.write(file.toPath(), result, StandardCharsets.UTF_8);
            }
        } catch (Throwable ignored) {
        }
        executeNext(runnable, outPin);
    }
}