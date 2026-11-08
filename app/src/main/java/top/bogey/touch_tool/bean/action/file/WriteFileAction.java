package top.bogey.touch_tool.bean.action.file;

import com.google.gson.JsonObject;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.service.TaskRunnable;

// 写入文本到文件：覆盖或追加
public class WriteFileAction extends ExecuteAction {
    private final transient Pin pathPin = new Pin(new PinString(), R.string.file_action_path);
    private final transient Pin dataPin = new Pin(new PinString(), R.string.file_action_content);
    private final transient Pin modePin = new NotLinkAblePin(new PinSingleSelect(R.array.file_write_mode), R.string.file_action_write_mode);

    public WriteFileAction() {
        super(ActionType.WRITE_FILE);
        addPins(pathPin, dataPin, modePin);
    }

    public WriteFileAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(pathPin, dataPin, modePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinObject path = getPinValue(runnable, pathPin);
        PinObject data = getPinValue(runnable, dataPin);
        boolean append = modePin.getValue(PinSingleSelect.class).getIndex() == 1;
        try {
            File file = new File(path.toString());
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            if (append) {
                Files.write(file.toPath(), (data.toString() + "\n").getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                Files.write(file.toPath(), data.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Throwable ignored) {
        }
        executeNext(runnable, outPin);
    }
}
