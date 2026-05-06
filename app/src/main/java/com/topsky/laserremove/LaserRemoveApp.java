package com.topsky.laserremove;

import com.blankj.utilcode.util.LogUtils;

import androidx.multidex.MultiDexApplication;

public class LaserRemoveApp extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.getConfig()
                .setLogSwitch(BuildConfig.DEBUG);
    }
}
