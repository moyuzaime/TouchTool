package top.bogey.touch_tool.ui.tool;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinColor;
import top.bogey.touch_tool.databinding.ViewToolBinding;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.ui.blueprint.picker.ColorPickerPreview;
import top.bogey.touch_tool.ui.blueprint.picker.NodePickerPreview;
import top.bogey.touch_tool.ui.tool.app_info.AppInfoFloatView;
import top.bogey.touch_tool.ui.tool.log.LogFloatView;

public class ToolView extends Fragment {
    public static final String TOOL_CAPTURE_SERVICE = "capture_service";
    public static final String TOOL_PACKAGE_ACTIVITY = "package_activity";
    public static final String TOOL_NODE_PICKER = "node_picker";
    public static final String TOOL_COLOR_PICKER = "color_picker";
    public static final String TOOL_RUNNING_LOG = "running_log";
    public static final String TOOL_MODEL_MANAGER = "model_manager";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewToolBinding binding = ViewToolBinding.inflate(inflater, container, false);

        List<ToolItem> items = new ArrayList<>();
        items.add(new ToolItem(TOOL_CAPTURE_SERVICE, R.drawable.icon_videocam, R.string.capture_service));
        items.add(new ToolItem(TOOL_MODEL_MANAGER, R.drawable.icon_detection_and_zone, R.string.model_manager));
        items.add(new ToolItem(TOOL_PACKAGE_ACTIVITY, R.drawable.icon_apps, R.string.package_activity));
        items.add(new ToolItem(TOOL_NODE_PICKER, R.drawable.icon_widgets, R.string.node_picker));
        items.add(new ToolItem(TOOL_COLOR_PICKER, R.drawable.icon_palette, R.string.color_picker));
        items.add(new ToolItem(TOOL_RUNNING_LOG, R.drawable.icon_draw, R.string.running_log));

        ToolViewAdapter adapter = new ToolViewAdapter(items);
        binding.toolList.setAdapter(adapter);

        return binding.getRoot();
    }

    public static void openTool(Context context, String toolName) {
        if (toolName == null || toolName.isEmpty()) return;

        switch (toolName) {
            case TOOL_CAPTURE_SERVICE -> {
                MainAccessibilityService service = MainApplication.getInstance().getService();
                if (service != null && service.isEnabled()) {
                    if (service.isCaptureEnabled()) {
                        service.stopCapture();
                    } else {
                        service.startCapture(null);
                    }
                }
            }
            case TOOL_MODEL_MANAGER -> {
                NavController controller = Navigation.findNavController(MainApplication.getInstance().getActivity(), R.id.conView);
                controller.navigate(ToolViewDirections.actionToolToModelManagerView());
            }
            case TOOL_PACKAGE_ACTIVITY -> {
                if (!checkService()) return;
                AppInfoFloatView.showTool(context);
            }
            case TOOL_NODE_PICKER -> {
                if (!checkService()) return;
                new NodePickerPreview(context, null, null).show();
            }
            case TOOL_COLOR_PICKER -> {
                if (!checkService()) return;
                new ColorPickerPreview(context, null, new PinColor.ColorInfo(0, 0, 0)).show();
            }
            case TOOL_RUNNING_LOG -> LogFloatView.showTool(context);
        }
    }

    // 无障碍服务没起来时悬浮窗加不进去，点了会没反应，这里先判断并提示
    private static boolean checkService() {
        MainAccessibilityService service = MainApplication.getInstance().getService();
        // 仅非空不够：服务被系统停用后单例还在，但窗口 token 已失效，加窗口会抛 BadToken
        if (service != null && service.isConnected()) return true;
        Toast.makeText(MainApplication.getInstance(), R.string.app_setting_enable_error, Toast.LENGTH_SHORT).show();
        return false;
    }
}
