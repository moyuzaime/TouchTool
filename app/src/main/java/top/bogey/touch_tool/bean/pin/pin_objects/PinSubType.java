package top.bogey.touch_tool.bean.pin.pin_objects;

public enum PinSubType {
    NORMAL, DYNAMIC,

    // 文本：链接，快捷方式，铃声路径，自动填充，单行文本，控件路径，任务ID
    URL, SHORTCUT, RINGTONE, AUTO_PIN, FILE_CONTENT,
    SINGLE_LINE, NODE_PATH, NODE_PATH_TEXT, TASK_ID, ALL_TASK_ID, SINGLE_SELECT,

    // 数字：整数，浮点数，长整型，大浮点数
    INTEGER, FLOAT, LONG, DOUBLE,

    // 时间：日期，时间，周期
    DATE, TIME, PERIODIC,

    // 界面：单个应用，单个应用+活动，单个活动，单个应用+导出活动，单个发送活动，多个应用，多个应用+活动，多个应用+导出活动
    SINGLE_APP, SINGLE_APP_WITH_ACTIVITY, SINGLE_ACTIVITY, SINGLE_APP_WITH_EXPORT_ACTIVITY, SINGLE_SEND_ACTIVITY, SINGLE_SHORTCUT_ACTIVITY,
    MULTI_APP, MULTI_APP_WITH_ACTIVITY, MULTI_APP_WITH_EXPORT_ACTIVITY,

    // 多选
    MULTI_SELECT,

    // 其他：带图标，带文字
    WITH_ICON, WITH_STRING
}
