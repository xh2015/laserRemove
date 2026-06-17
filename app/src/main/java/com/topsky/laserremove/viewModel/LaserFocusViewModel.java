package com.topsky.laserremove.viewModel;

import android.app.Application;

import com.topsky.laserremove.base.BaseViewModel;
import com.topsky.laserremove.manager.DataJsonManager;
import com.topsky.laserremove.net.netty.NettyManager;
import com.topsky.laserremove.net.netty.NettyMessage;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class LaserFocusViewModel extends BaseViewModel {
    private static final String TAG = "LaserControlViewModel";
    private boolean fromDistance;
    private Integer distance;

    //距离
    private final MutableLiveData<Integer> laserDistance = new MutableLiveData<>(10);

    public LiveData<Integer> getLaserDistance() {
        return laserDistance;
    }

    public LaserFocusViewModel(@NonNull Application application) {
        super(application);
    }

    public void onControlSendSuccess() {
        if (fromDistance && distance != null) {
            laserDistance.postValue(distance);
        }
    }

    public void handleNettyMessage(NettyMessage message) {
        if (message == null) {
            return;
        }
    }

    //获取距离
    public void getLaserDistanceByServer() {
        NettyManager.getInstance().sendMessage(
                NettyMessage.MODULE_LASER_DISTANCE,
                (byte) 0x00,
                null
        );
    }

    //根据距离发送脉冲设置指令
    public void sendLaserPulseByDistance(int distance) {
        Integer value = DataJsonManager.getInstance().getValue(String.valueOf(distance));
        if (value == null) {
            return;
        }
        sendLaserPulse(value, distance);
    }

    public void sendLaserPulse(int pulse) {
        sendLaserPulse(pulse, -1);
    }

    public void sendLaserPulse(int pulse, int distance) {
        byte[] data = new byte[]{
                (byte) ((pulse >> 24) & 0xFF),
                (byte) ((pulse >> 16) & 0xFF),
                (byte) ((pulse >> 8) & 0xFF),
                (byte) (pulse & 0xFF),
                (byte) 0x00
        };
        if (distance <= 0) {
            fromDistance = false;
            this.distance = null;
        } else {
            fromDistance = true;
            this.distance = distance;
        }
        NettyManager.getInstance().sendMessage(
                NettyMessage.MODULE_LASER_FOCUS,
                (byte) 0x01,
                data
        );
    }
}
