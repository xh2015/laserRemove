package com.topsky.laserremove;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Process;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 全局异常捕获器（单例）
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static CrashHandler instance;
    private Thread.UncaughtExceptionHandler defaultHandler;
    private Context context;
    // 用来存储设备信息和异常信息
    private Map<String, String> deviceInfo = new HashMap<>();

    public static CrashHandler getInstance() {
        if (instance == null) {
            synchronized (CrashHandler.class) {
                if (instance == null) {
                    instance = new CrashHandler();
                }
            }
        }
        return instance;
    }

    /**
     * 初始化
     */
    public void init(Context ctx) {
        context = ctx;
        // 获取系统默认的 UncaughtException 处理器
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        // 设置当前实例为系统默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 当发生未捕获的异常时，会自动回调这个方法
     */
    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        collectDeviceInfo(context);

        final Throwable finalThrowable = throwable;
        Thread saveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                saveCrashInfo2File(finalThrowable);
            }
        });
        saveThread.start();
        try {
            saveThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable);
        } else {
            Process.killProcess(Process.myPid());
            System.exit(1);
        }
    }

    /**
     * 收集设备参数信息
     */
    private void collectDeviceInfo(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                deviceInfo.put("versionName", pi.versionName);
                deviceInfo.put("versionCode", String.valueOf(pi.versionCode));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        deviceInfo.put("model", Build.MODEL);
        deviceInfo.put("manufacturer", Build.MANUFACTURER);
        deviceInfo.put("sdkVersion", String.valueOf(Build.VERSION.SDK_INT));
        deviceInfo.put("osVersion", Build.VERSION.RELEASE);
        deviceInfo.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
    }

    /**
     * 保存异常信息到文件
     * @return 返回文件名，便于后续查找
     */
    private String saveCrashInfo2File(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        // 拼接设备信息
        for (Map.Entry<String, String> entry : deviceInfo.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }
        sb.append("\n");

        // 获取异常堆栈信息
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        sb.append(writer.toString());

        // 写入文件
        String time = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "crash_" + time + ".log";

        // 保存到应用的外部存储专属目录（无需动态申请权限，API 19+）
        File dir = new File(context.getExternalFilesDir(null), "crash_logs");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dir, fileName);
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file));
            bw.write(sb.toString());
            return fileName;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}