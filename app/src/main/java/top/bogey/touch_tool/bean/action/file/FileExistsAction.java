package top.bogey.touch_tool.bean.action.file;

import com.google.gson.JsonObject;

import java.io.File;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

// 文件或文件夹是否存在
public class FileExistsAction extends CalculateAction {
    private final transient Pin pathPin = new Pin(new PinString(), R.string.file_action_path);
    private final transient Pin resultPin = new Pin(new PinBoolean(), R.string.pin_boolean_result, true);

    public FileExistsAction() {
        super(ActionType.FILE_EXISTS);
        addPins(pathPin, resultPin);
    }

    public FileExistsAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(pathPin, resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinObject path = getPinValue(runnable, pathPin);
        resultPin.getValue(PinBoolean.class).setValue(new File(path.toString()).exists());
    }
}