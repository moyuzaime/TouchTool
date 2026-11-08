package top.bogey.touch_tool.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import top.bogey.touch_tool.bean.save.setting.SettingSaver;
import top.bogey.touch_tool.ui.MainActivity;
import top.bogey.touch_tool.utils.AppLog;
import top.bogey.touch_tool.utils.AppUtil;

// 开机自启：开了开关才拉起界面，并顺手重绑一次无障碍服务
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!SettingSaver.APP_AUTO_START.get()) return;
        try {
            Intent launch = new Intent(context, MainActivity.class);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launch);
        } catch (Throwable e) {
            AppLog.e("开机自启", "拉起界面失败", e);
        }
        AppUtil.restartAccessibilityService(context);
    }
}