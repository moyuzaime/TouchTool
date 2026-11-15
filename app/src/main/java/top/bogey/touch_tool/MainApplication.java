package top.bogey.touch_tool;

import android.app.Application;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import com.tencent.mmkv.MMKV;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;

import top.bogey.touch_tool.bean.save.setting.SettingSaver;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.ui.MainActivity;
import top.bogey.touch_tool.utils.AppUtil;

public class MainApplication extends Application implements Thread.UncaughtExceptionHandler {
    private static MainApplication instance;

    private WeakReference<MainActivity> activity = new WeakReference<>(null);
    private WeakReference<MainAccessibilityService> service = new WeakReference<>(null);
    public static String appName;

    public static MainApplication getInstance() {
        return instance;
    }

    private Thread.UncaughtExceptionHandler handler;

    private String version;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        appName = getString(R.string.app_name);

        MMKV.initialize(this);
        SettingSaver.APP_RUN_TIMES.add();
        SettingSaver.getInstance().init(this);

        // 创建外部存储的应用专属目录，方便用户自行往里放文件
        AppUtil.initExternalDir(this);

        handler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);

        PackageManager packageManager = getPackageManager();
        try {
            version = packageManager.getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {
        }
    }

    public void setActivity(MainActivity activity) {
        this.activity = new WeakReference<>(activity);
    }

    public MainActivity getActivity() {
        return activity.get();
    }

    public void setService(MainAccessibilityService service) {
        this.service = new WeakReference<>(service);
    }

    public MainAccessibilityService getService() {
        return service.get();
    }

     @Override
     public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
         String errorInfo = e.toString();
         try {
             StringWriter stringWriter = new StringWriter();
             PrintWriter printWriter = new PrintWriter(stringWriter);
             e.printStackTrace(printWriter);
             errorInfo = stringWriter.toString();
         } catch (Exception ignored) {
         }

         String fullError = version + "\n" + errorInfo;
         String[] lines = fullError.split("\n");
         if (lines.length > 100) {
             StringBuilder limitedError = new StringBuilder();
             for (int i = 0; i < 50; i++) {
                 limitedError.append(lines[i]).append("\n");
             }
             limitedError.append("... 省略中间 ").append(lines.length - 100).append(" 行 ...\n");
             for (int i = lines.length - 50; i < lines.length; i++) {
                 limitedError.append(lines[i]).append("\n");
             }
             fullError = limitedError.toString();
         }

         SettingSaver.APP_RUNNING_ERROR.set(fullError);
         handler.uncaughtException(t, e);
     }
}
