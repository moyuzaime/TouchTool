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

// 删除文件或文件夹（文件夹递归删除）
public class DeleteFileAction extends ExecuteAction {
    private final transient Pin pathPin = new Pin(new PinString(), R.string.file_action_path);

    public DeleteFileAction() {
        super(ActionType.DELETE_FILE);
        addPins(pathPin);
    }

    public DeleteFileAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(pathPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinObject path = getPinValue(runnable, pathPin);
        delete(new File(path.toString()));
        executeNext(runnable, outPin);
    }

    private void delete(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) delete(child);
        }
        try {
            file.delete();
        } catch (Throwable ignored) {
        }
    }
}