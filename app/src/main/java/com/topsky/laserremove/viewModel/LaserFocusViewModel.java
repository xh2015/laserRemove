package com.topsky.laserremove.viewModel;

import android.app.Application;

import com.blankj.utilcode.util.LogUtils;
import com.topsky.laserremove.base.BaseViewModel;
import com.topsky.laserremove.manager.DataJsonManager;
import com.topsky.laserremove.net.netty.NettyManager;
import com.topsky.laserremove.net.netty.NettyMessage;
import com.topsky.laserremove.net.netty.NettyMessageUtils;

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
        //注意：当前在子线程中执行，请勿在子线程中更新UI
        if (fromDistance && distance != null) {
            laserDistance.postValue(distance);
        }
        LogUtils.d("激光测距指令发送成功并响应");
    }

    public void handleNettyMessage(NettyMessage message) {
        //注意：当前在子线程中执行，请勿在子线程中更新UI
        if (message == null) {
            return;
        }
        byte moduleCode = message.getModuleCode();
        if (moduleCode == NettyMessage.MODULE_LASER_DISTANCE) {
            byte commandCode = message.getCommandCode();
            if (commandCode != 0x01) {
                return;
            }

            byte[] data = message.getData();
            if (data.length < 3) {
                return;
            }
            boolean effective = data[0] == 0x00;
            if (!effective) {
                return;
            }
            byte big = data[1];
            byte small = data[2];
            if ((big & 0xFF) == 0xFF && (small & 0xFF) == 0xFF) {
                return;
            }
            int value = ((big & 0xFF) << 8) | (small & 0xFF);
            float distance = value / 10f;
            laserDistance.postValue(Math.round(distance));
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
        byte[] bytes = NettyMessageUtils.intToBytes(pulse);

        byte[] data = new byte[5];
        System.arraycopy(bytes, 0, data, 0, 4);
        data[4] = (byte) 0x00;

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

    //近焦 远焦
    public void changeLaserFocus(boolean near) {
        NettyManager.getInstance().sendMessage(
                NettyMessage.MODULE_LASER_FOCUS,
                (byte) 0x02,
                new byte[]{ near ? (byte) 0x01 : (byte) 0x00, (byte) 0x00 }
        );
    }
}
