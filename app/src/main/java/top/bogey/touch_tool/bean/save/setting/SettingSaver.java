package top.bogey.touch_tool.bean.save.setting;

import android.app.Activity;
import android.app.Application;
import android.graphics.Color;
import android.graphics.Point;

import top.bogey.touch_tool.bean.save.setting.special.AppHideActivityBackground;
import top.bogey.touch_tool.bean.save.setting.special.AppKeepAliveService;
import top.bogey.touch_tool.bean.save.setting.special.AppRunTimes;
import top.bogey.touch_tool.bean.save.setting.special.DynamicColor;
import top.bogey.touch_tool.bean.save.setting.special.DynamicColorOption;
import top.bogey.touch_tool.bean.save.setting.special.NightMode;
import top.bogey.touch_tool.bean.save.setting.special.PermissionExactAlarm;

public class SettingSaver {
    private static SettingSaver instance;

    public static SettingSaver getInstance() {
        synchronized (SettingSaver.class) {
            if (instance == null) {
                instance = new SettingSaver();
            }
        }
        return instance;
    }

    /// 自动记录型

    /// 运行次数
    public final static AppRunTimes APP_RUN_TIMES = new AppRunTimes("APP_RUN_TIMES", 0);
    /// 运行错误
    public final static SettingSave<String> APP_RUNNING_ERROR = new SettingSave<>("APP_RUNNING_ERROR", "");

    /// 悬浮窗位置
    public final static SettingSave<Point> FLOAT_VIEW_POS = new SettingSave<>("FLOAT_VIEW_POS", new Point(0, 0));
    /// 控件选择器选择方式
    public final static SettingSave<Integer> NODE_PICKER_PICK_TYPE = new SettingSave<>("NODE_PICKER_PICK_TYPE", 0);


    /// 应用设置
    /// 服务是否已启用
    public final static SettingSave<Boolean> APP_SERVICE = new SettingSave<>("APP_SERVICE", false);
    /// 是否已显示服务开启提示
    public final static SettingSave<Boolean> SHOW_APP_SERVICE_ENABLE_TIPS = new SettingSave<>("SHOW_APP_SERVICE_ENABLE_TIPS", false);
    /// 隐藏应用后台
    public final static AppHideActivityBackground APP_HIDE_ACTIVITY_BACKGROUND = new AppHideActivityBackground("APP_HIDE_ACTIVITY_BACKGROUND", false);
    /// 前台保活服务
    public final static AppKeepAliveService APP_KEEP_ALIVE_SERVICE = new AppKeepAliveService("APP_KEEP_ALIVE_SERVICE", false);

    /// 权限
    /// 超级用户权限
    public final static SettingSave<Integer> PERMISSION_SUPER_USER = new SettingSave<>("PERMISSION_SUPER_USER", 0);
    /// 通知权限
    public final static SettingSave<Integer> PERMISSION_NOTIFICATION = new SettingSave<>("PERMISSION_NOTIFICATION", 0);
    /// 精确时间权限
    public final static PermissionExactAlarm PERMISSION_EXACT_ALARM = new PermissionExactAlarm("PERMISSION_EXACT_ALARM", false);
    /// 蓝牙权限
    public final static SettingSave<Boolean> PERMISSION_BLUETOOTH = new SettingSave<>("PERMISSION_BLUETOOTH", false);
    /// 定位权限
    public final static SettingSave<Boolean> PERMISSION_LOCATION = new SettingSave<>("PERMISSION_LOCATION", false);

    /// 任务设置
    /// 自动备份
    public final static SettingSave<Integer> TASK_AUTO_BACKUP = new SettingSave<>("AUTO_BACKUP", 0);
    public final static SettingSave<Integer> TASK_AUTO_BACKUP_TIMES = new SettingSave<>("AUTO_BACKUP_TIMES", 0);
    /// 是否手势轨迹
    public final static SettingSave<Boolean> TASK_GESTURE_TRACE = new SettingSave<>("TASK_GESTURE_TRACE", false);
    /// 是否显示目标标记
    public final static SettingSave<Boolean> TASK_TARGET_MARK = new SettingSave<>("TASK_TARGET_MARK", false);
    /// 是否显示任务运行提示
    public final static SettingSave<Boolean> TASK_RUNNING_TIPS = new SettingSave<>("TASK_RUNNING_TIPS", true);
    /// 是否记录任务详细日志
    public final static SettingSave<Boolean> TASK_DETAIL_LOG = new SettingSave<>("TASK_DETAIL_LOG", true);
    /// 是否每次重置任务运行日志
    public final static SettingSave<Boolean> TASK_RESET_DETAIL_LOG = new SettingSave<>("TASK_RESET_DETAIL_LOG", false);
    /// 是否启用音量键关闭任务
    public final static SettingSave<Boolean> TASK_VOLUME_KEY_STOP = new SettingSave<>("TASK_VOLUME_KEY_STOP", false);

    /// 蓝图设置
    /// 蓝图是否可编辑
    public final static SettingSave<Boolean> BLUEPRINT_EDITABLE = new SettingSave<>("BLUEPRINT_EDITABLE", true);
    /// 蓝图最后打开的分组
    public final static SettingSave<String> BLUEPRINT_LAST_GROUP = new SettingSave<>("BLUEPRINT_LAST_GROUP", "");
    // 是否记录无障碍界面事件（逐条格式化开销大，默认关闭）
    // 服务掉线时自动重绑
    public final static SettingSave<Boolean> APP_AUTO_RESTART_SERVICE = new SettingSave<>("APP_AUTO_RESTART_SERVICE", false);
    // 开机自启
    public final static SettingSave<Boolean> APP_AUTO_START = new SettingSave<>("APP_AUTO_START", false);
    public final static SettingSave<Boolean> APP_LOG_EVENT = new SettingSave<>("APP_LOG_EVENT", false);
    // 悬浮窗位置：tag|x|y;tag2|x|y
    public final static SettingSave<String> FLOAT_WINDOW_LOCATION = new SettingSave<>("FLOAT_WINDOW_LOCATION", "");
    /// 蓝图最后打开的子分组
    public final static SettingSave<String> BLUEPRINT_LAST_SUB_GROUP = new SettingSave<>("BLUEPRINT_LAST_SUB_GROUP", "");
    /// 蓝图卡片搜索展开状态
    public final static SettingSave<Boolean> BLUEPRINT_CARD_SEARCH_EXPAND = new SettingSave<>("BLUEPRINT_CARD_SEARCH_EXPAND", false);
    /// 蓝图卡片默认展开状态
    public final static SettingSave<Integer> BLUEPRINT_CARD_EXPAND_STATE = new SettingSave<>("BLUEPRINT_CARD_EXPAND_STATE", 2);
    /// 蓝图卡片整理时默认间距
    public final static SettingSave<Integer> BLUEPRINT_CARD_ARRANGE_PADDING = new SettingSave<>("BLUEPRINT_CARD_ARRANGE_PADDING", 2);
    /// 蓝图搜索带拼音
    public final static SettingSave<Boolean> BLUEPRINT_SEARCH_WITH_PINYIN = new SettingSave<>("BLUEPRINT_SEARCH_WITH_PINYIN", false);
    /// 蓝图搜索带位置
    public final static SettingSave<Boolean> BLUEPRINT_SEARCH_WITH_POSITION = new SettingSave<>("BLUEPRINT_SEARCH_WITH_POSITION", false);
    /// 蓝图搜索带大小写
    public final static SettingSave<Boolean> BLUEPRINT_SEARCH_WITH_CASE = new SettingSave<>("BLUEPRINT_SEARCH_WITH_CASE", false);
    /// 蓝图搜索带正则
    public final static SettingSave<Boolean> BLUEPRINT_SEARCH_WITH_REGEX = new SettingSave<>("BLUEPRINT_SEARCH_WITH_REGEX", false);

    /// 偏好设置
    /// 小窗优化
    public final static SettingSave<Boolean> FREE_FORM_OPTIMIZE = new SettingSave<>("FREE_FORM_OPTIMIZE", false);
    /// 夜间模式
    public final static NightMode NIGHT_MODE = new NightMode("NIGHT_MODE", 0);
    /// 动态颜色
    public final static DynamicColor DYNAMIC_COLOR = new DynamicColor("DYNAMIC_COLOR", true);
    public final static DynamicColorOption DYNAMIC_COLOR_OPTIONS = new DynamicColorOption("DYNAMIC_COLOR_OPTIONS", Color.BLACK);

    /// 手动执行悬浮窗
    /// 手动执行悬浮窗展开状态
    public final static SettingSave<Boolean> MANUAL_PLAY_VIEW_EXPAND_STATE = new SettingSave<>("MANUAL_PLAY_VIEW_EXPAND_STATE", false);
    /// 手动执行悬浮窗位置
    public final static SettingSave<Point> MANUAL_PLAY_VIEW_POS = new SettingSave<>("MANUAL_PLAY_VIEW_POS", new Point(0, 0));
    /// 手动执行悬浮窗显示时机
    public final static SettingSave<Integer> MANUAL_PLAY_VIEW_SHOW_TYPE = new SettingSave<>("MANUAL_PLAY_VIEW_SHOW_TYPE", 2);
    /// 手动执行悬浮窗按钮暂停/停止
    public final static SettingSave<Integer> MANUAL_PLAY_VIEW_BUTTON_PAUSE_TYPE = new SettingSave<>("MANUAL_PLAY_VIEW_BUTTON_PAUSE_TYPE", 0);
    /// 手动执行悬浮窗按钮长按跳转
    public final static SettingSave<Boolean> MANUAL_PLAY_VIEW_BUTTON_LONG_PRESS_JUMP = new SettingSave<>("MANUAL_PLAY_VIEW_BUTTON_LONG_PRESS_JUMP", false);
    /// 手动执行悬浮窗隐藏策略
    public final static SettingSave<Integer> MANUAL_PLAY_VIEW_HIDE_TYPE = new SettingSave<>("MANUAL_PLAY_VIEW_HIDE_TYPE", 0);
    /// 手动执行悬浮窗是否截屏时隐藏
    public final static SettingSave<Boolean> MANUAL_PLAY_VIEW_HIDE_WHEN_SCREENSHOT = new SettingSave<>("MANUAL_PLAY_VIEW_HIDE_WHEN_SCREENSHOT", false);
    /// 手动执行悬浮窗按钮是否执行时隐藏
    public final static SettingSave<Boolean> MANUAL_PLAY_VIEW_BUTTON_HIDE_WHEN_EXECUTE = new SettingSave<>("MANUAL_PLAY_VIEW_HIDE_WHEN_EXECUTE", false);
    /// 手动执行悬浮窗是否未使用时淡化
    public final static SettingSave<Boolean> MANUAL_PLAY_VIEW_NOT_USED_FADE = new SettingSave<>("MANUAL_PLAY_VIEW_NOT_USED_FADE", true);
    /// 手动执行悬浮窗淡化程度
    public final static SettingSave<Integer> MANUAL_PLAY_VIEW_NOT_USED_FADE_LEVEL = new SettingSave<>("MANUAL_PLAY_VIEW_NOT_USED_FADE_LEVEL", 50);
    /// 手动执行悬浮窗贴边距离
    public final static SettingSave<Integer> MANUAL_PLAY_VIEW_PADDING = new SettingSave<>("MANUAL_PLAY_VIEW_PADDING", 0);
    /// 手动执行悬浮窗按钮宽度
    public final static SettingSave<Integer> MANUAL_PLAY_VIEW_BUTTON_WIDTH = new SettingSave<>("MANUAL_PLAY_VIEW_BUTTON_WIDTH", 1);
    /// 手动执行悬浮窗按钮高度
    public final static SettingSave<Integer> MANUAL_PLAY_VIEW_BUTTON_HEIGHT = new SettingSave<>("MANUAL_PLAY_VIEW_BUTTON_HEIGHT", 1);
    /// 手动执行悬浮窗独立按钮大小
    public final static SettingSave<Integer> MANUAL_PLAY_VIEW_SINGLE_BUTTON_SIZE = new SettingSave<>("MANUAL_PLAY_VIEW_SINGLE_BUTTON_SIZE", 1);
    /// 手动执行悬浮窗收起时宽度
    public final static SettingSave<Integer> MANUAL_PLAY_VIEW_ZOOM_SIZE = new SettingSave<>("MANUAL_PLAY_VIEW_ZOOM_SIZE", 1);

    public void init(Activity activity) {
        APP_HIDE_ACTIVITY_BACKGROUND.handle(activity);
        NIGHT_MODE.handle();
        APP_KEEP_ALIVE_SERVICE.handle(activity);
    }

    public void init(Application application) {
        DYNAMIC_COLOR.handle(application);
    }
}
