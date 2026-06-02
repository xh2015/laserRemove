package com.topsky.laserremove.viewModel;

import android.app.Application;

import com.blankj.utilcode.util.LogUtils;
import com.topsky.laserremove.base.BaseViewModel;
import com.topsky.laserremove.net.netty.NettyManager;
import com.topsky.laserremove.net.netty.NettyMessage;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * 激光控制ViewModel
 * 处理模组代码 0xF8 的消息
 */
public class LaserControlViewModel extends BaseViewModel {
    private static final String TAG = "LaserControlViewModel";

    // 激光连接状态
    private final MutableLiveData<Boolean> laserConnectStatus = new MutableLiveData<>(false);

    public LiveData<Boolean> getLaserConnectStatus() {
        return laserConnectStatus;
    }

    //功率
    private final MutableLiveData<Integer> laserPower = new MutableLiveData<>(10);

    public LiveData<Integer> getLaserPower() {
        return laserPower;
    }

    //距离
    private final MutableLiveData<Integer> laserDistance = new MutableLiveData<>(10);

    public LiveData<Integer> getLaserDistance() {
        return laserDistance;
    }

    //脉冲
    private final MutableLiveData<Integer> laserPulse = new MutableLiveData<>(10);

    public LiveData<Integer> getLaserPulse() {
        return laserPulse;
    }

    // 激光开关状态
    private final MutableLiveData<Boolean> laserSwitchStatus = new MutableLiveData<>(false);

    public LiveData<Boolean> getLaserSwitchStatus() {
        return laserSwitchStatus;
    }

    public void setLaserConnectStatus(boolean isConnected) {
        laserConnectStatus.postValue(isConnected);
    }

    public LaserControlViewModel(@NonNull Application application) {
        super(application);
    }

    public void setLaserPower(int power) {
        laserPower.postValue(power);
    }

    public void setLaserDistance(int distance) {
        laserDistance.postValue(distance);
    }

    public void setLaserPulse(int pulse) {
        laserPulse.postValue(pulse);
    }

    /**
     * 处理Netty消息
     */
    public void handleNettyMessage(NettyMessage message) {
        if (message == null) {
            return;
        }

        LogUtils.d(TAG, "Handling laser control message: " + message);

        byte commandCode = message.getCommandCode();
        byte[] data = message.getData();

        // 根据指令代码处理激光控制消息
        switch (commandCode) {
            case 0x00:
                // 激光开关状态
                if (data != null && data.length > 0) {
                    laserSwitchStatus.postValue(data[0] == 0x01);
                }
                break;
            case 0x01:
                // 功率设置响应
                if (data != null && data.length > 0) {
                    int power = data[0] & 0xFF;
                    laserPower.postValue(power);
                }
                break;
            case 0x02:
                // 距离数据上报
                if (data != null && data.length >= 2) {
                    int distance = (data[0] & 0xFF) << 8 | (data[1] & 0xFF);
                    laserDistance.postValue(distance);
                }
                break;
            case 0x03:
                // 脉冲设置响应
                if (data != null && data.length > 0) {
                    int pulse = data[0] & 0xFF;
                    laserPulse.postValue(pulse);
                }
                break;
            default:
                LogUtils.w(TAG, "Unknown laser control command: " + String.format("0x%02X", commandCode));
        }
    }

    /**
     * 发送激光开关指令
     */
    public void sendLaserSwitch(boolean isOn) {
        byte[] data = new byte[]{ (byte) (isOn ? 0x01 : 0x00) };

        NettyManager.getInstance().sendMessage(
                NettyMessage.MODULE_LASER_CONTROL,
                (byte) 0x00,
                data
        );

        laserSwitchStatus.postValue(isOn);
    }

    /**
     * 发送功率设置指令
     */
    public void sendLaserPower(int power) {
        if (power < 0 || power > 100) {
            LogUtils.e(TAG, "Invalid power value: " + power);
            return;
        }

        byte[] data = new byte[]{ (byte) power };

        NettyManager.getInstance().sendMessage(
                NettyMessage.MODULE_LASER_CONTROL,
                (byte) 0x01,
                data
        );

        laserPower.postValue(power);
    }

    //发送脉冲设置指令
    public void sendLaserPulse(int pulse) {
        byte[] data = new byte[]{ (byte) pulse };
        NettyManager.getInstance().sendMessage(
                NettyMessage.MODULE_LASER_CONTROL,
                (byte) 0x03,
                data
        );

        laserPulse.postValue(pulse);
    }
}
