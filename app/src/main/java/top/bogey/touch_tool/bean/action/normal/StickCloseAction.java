package top.bogey.touch_tool.bean.action.normal;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

public class StickCloseAction extends ExecuteAction {
    private final transient Pin idPin = new Pin(new PinString(), R.string.stick_close_action_id);
    // 打开后关闭全部贴图，取代单独的「关闭所有贴图」卡片
    private final transient Pin allPin = new Pin(new PinBoolean(false), R.string.stick_close_action_all);

    public StickCloseAction() {
        super(ActionType.CLOSE_STICK);
        addPins(idPin, allPin);
    }

    public StickCloseAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(idPin, allPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinBoolean all = getPinValue(runnable, allPin);
        if (all.getValue()) {
            // 全部关闭：贴图窗口按类型整体回收
            FloatWindow.dismiss(top.bogey.touch_tool.ui.custom.float_view.StickScreenFloatView.class);
        } else {
            PinString id = getPinValue(runnable, idPin);
            FloatWindow.dismiss(id.getValue());
        }

        executeNext(runnable, outPin);
    }
}
