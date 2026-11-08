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
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.service.TaskRunnable;

// 行操作：写入指定行 / 替换指定行 / 删除指定行
public class WriteFileLineAction extends ExecuteAction {
    private final transient Pin pathPin = new Pin(new PinString(), R.string.file_action_path);
    private final transient Pin linePin = new Pin(new PinInteger(1), R.string.file_action_line_no);
    private final transient Pin dataPin = new Pin(new PinString(), R.string.file_action_content);
    private final transient Pin modePin = new NotLinkAblePin(new PinSingleSelect(R.array.file_line_mode), R.string.file_action_line_mode);

    public WriteFileLineAction() {
        super(ActionType.WRITE_FILE_LINE);
        addPins(pathPin, linePin, dataPin, modePin);
    }

    public WriteFileLineAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(pathPin, linePin, dataPin, modePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinObject path = getPinValue(runnable, pathPin);
        int lineNo = ((PinNumber<?>) getPinValue(runnable, linePin)).intValue();
        String data = getPinValue(runnable, dataPin).toString();
        int mode = modePin.getValue(PinSingleSelect.class).getIndex();
        try {
            File file = new File(path.toString());
            boolean exists = file.isFile();
            List<String> lines = exists
                    ? new ArrayList<>(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8))
                    : new ArrayList<>();
            boolean changed = false;
            if (mode == 2) {
                // 删除行
                if (lineNo >= 1 && lineNo <= lines.size()) {
                    lines.remove(lineNo - 1);
                    changed = true;
                }
            } else if (mode == 1) {
                // 替换行：只处理已存在的行
                if (lineNo >= 1 && lineNo <= lines.size()) {
                    lines.set(lineNo - 1, data);
                    changed = true;
                }
            } else {
                // 写入行：存在则覆盖，否则追加
                if (lineNo >= 1 && lineNo <= lines.size()) {
                    lines.set(lineNo - 1, data);
                } else {
                    lines.add(data);
                }
                changed = true;
            }
            if (changed) {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
            }
        } catch (Throwable ignored) {
        }
        executeNext(runnable, outPin);
    }
}
