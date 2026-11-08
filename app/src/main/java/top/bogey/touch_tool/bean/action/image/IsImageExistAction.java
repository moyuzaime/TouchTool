package top.bogey.touch_tool.bean.action.image;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;

import com.google.gson.JsonObject;

import java.util.List;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.action.system.SwitchCaptureAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.MatchResult;

public class IsImageExistAction extends CalculateAction {
    private final transient Pin sourcePin = new Pin(new PinImage(), R.string.pin_image_full, false, false, true);
    private final transient Pin templatePin = new Pin(new PinImage(), R.string.is_image_exist_action_template);
    private final transient Pin similarityPin = new Pin(new PinInteger(80), R.string.is_image_exist_action_similarity);
    private final transient Pin scalePin = new Pin(new PinSingleSelect(R.array.match_image_scale, 1), R.string.image_action_scale, false, false, true);
    private final transient Pin resultPin = new Pin(new PinBoolean(), R.string.pin_boolean_result, true);
    private final transient Pin cannyPin = new Pin(new PinBoolean(false), R.string.image_action_canny);
    // 命中的位置与实际相似度，供下游使用
    private final transient Pin areaPin = new Pin(new PinArea(), R.string.pin_area, true);
    private final transient Pin matchValuePin = new Pin(new PinInteger(), R.string.find_image_action_result_similarity, true);

    public IsImageExistAction() {
        super(ActionType.IS_IMAGE_EXIST);
        addPins(sourcePin, templatePin, similarityPin, scalePin, resultPin, cannyPin, areaPin, matchValuePin);
    }

    public IsImageExistAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(sourcePin, templatePin, similarityPin, scalePin, resultPin, cannyPin, areaPin, matchValuePin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        Bitmap bitmap;
        if (sourcePin.isLinked()) {
            PinImage source = getPinValue(runnable, sourcePin);
            bitmap = source.getImage();
        } else {
            MainAccessibilityService service = MainApplication.getInstance().getService();
            bitmap = service.tryGetScreenShot();
        }
        PinImage template = getPinValue(runnable, templatePin);
        PinNumber<?> similarity = getPinValue(runnable, similarityPin);
        PinSingleSelect scale = getPinValue(runnable, scalePin);
        PinBoolean canny = getPinValue(runnable, cannyPin);

        MatchResult matchResult = DisplayUtil.matchTemplateResult(bitmap, template.getImage(), null, similarity.intValue(), scale.getIndex(), canny.getValue());
        boolean found = matchResult != null && matchResult.area != null && !matchResult.area.isEmpty();

        resultPin.getValue(PinBoolean.class).setValue(found);
        if (found) {
            // 命中位置按原图坐标输出（区域裁剪后已由 matchTemplateResult 偏移回去）
            areaPin.getValue(PinArea.class).setValue(matchResult.area);
            matchValuePin.getValue(PinInteger.class).setValue((int) (matchResult.value * 100));
        } else {
            areaPin.getValue(PinArea.class).setValue(new Rect());
            matchValuePin.getValue(PinInteger.class).setValue(0);
        }
    }

    @Override
    public void check(ActionCheckResult result, Task task) {
        super.check(result, task);
        if (!sourcePin.isLinked()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                List<Action> actions = task.getActions(SwitchCaptureAction.class);
                if (actions.isEmpty()) {
                    result.addResult(ActionCheckResult.ResultType.WARNING, R.string.check_need_capture_warning);
                }
            }
        }
    }
}
