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
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

// 读取文件所有行，输出文本列表
public class ReadAllFileLinesAction extends CalculateAction {
    private final transient Pin pathPin = new Pin(new PinString(), R.string.file_action_path);
    private final transient Pin linesPin = new Pin(new PinList(new PinString()), R.string.file_action_texts, true);

    public ReadAllFileLinesAction() {
        super(ActionType.READ_ALL_FILE_LINES);
        addPins(pathPin, linesPin);
    }

    public ReadAllFileLinesAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(pathPin, linesPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinObject path = getPinValue(runnable, pathPin);
        PinList lines = linesPin.getValue(PinList.class);
        lines.clear();
        try {
            File file = new File(path.toString());
            if (file.isFile()) {
                List<String> texts = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                for (String text : texts) lines.add(new PinString(text));
            }
        } catch (Throwable ignored) {
        }
    }
}