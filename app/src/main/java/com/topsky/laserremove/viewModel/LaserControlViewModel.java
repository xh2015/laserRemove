package com.topsky.laserremove.viewModel;

import android.app.Application;

import com.topsky.laserremove.base.BaseViewModel;
import com.topsky.laserremove.net.netty.NettyManager;
import com.topsky.laserremove.net.netty.NettyMessage;

import androidx.annotation.NonNull;

public class LaserControlViewModel extends BaseViewModel {
    private static final String TAG = "LaserControlViewModel";

    public LaserControlViewModel(@NonNull Application application) {
        super(application);
    }

    public void onControlSendSuccess() {
        //注意：当前在子线程中执行，请勿在子线程中更新UI
    }

    public void handleNettyMessage(NettyMessage message) {
        //注意：当前在子线程中执行，请勿在子线程中更新UI
    }

    public void sendLaserSwitch(boolean needOpen, int powerPercent) {
        int clampedPower = Math.max(0, Math.min(100, powerPercent));
        byte[] data = new byte[]{ (byte) (needOpen ? 0x00 : 0x01), needOpen ? (byte) clampedPower : (byte) 0x00 };
        NettyManager.getInstance().sendMessage(
                NettyMessage.MODULE_LASER_CONTROL,
                (byte) 0x00,
                data
        );
    }
}
