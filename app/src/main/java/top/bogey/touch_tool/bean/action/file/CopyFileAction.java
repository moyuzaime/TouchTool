package top.bogey.touch_tool.bean.action.file;

import com.google.gson.JsonObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

// 复制文件
public class CopyFileAction extends ExecuteAction {
    private final transient Pin sourcePin = new Pin(new PinString(), R.string.file_action_source);
    private final transient Pin targetPin = new Pin(new PinString(), R.string.file_action_target);

    public CopyFileAction() {
        super(ActionType.COPY_FILE);
        addPins(sourcePin, targetPin);
    }

    public CopyFileAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(sourcePin, targetPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinObject source = getPinValue(runnable, sourcePin);
        PinObject target = getPinValue(runnable, targetPin);
        try {
            File sourceFile = new File(source.toString());
            File targetFile = new File(target.toString());
            File parent = targetFile.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (Throwable ignored) {
        }
        executeNext(runnable, outPin);
    }
}