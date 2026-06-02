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
 * 云台控制ViewModel
 * 处理模组代码 0xF0-0xF7 的消息
 */
public class CloudPlatformViewModel extends BaseViewModel {
    private static final String TAG = "CloudPlatformViewModel";
    
    // 云台连接状态
    private final MutableLiveData<Boolean> cpConnectStatus = new MutableLiveData<>(false);

    public LiveData<Boolean> getCpConnectStatus() {
        return cpConnectStatus;
    }

    public CloudPlatformViewModel(@NonNull Application application) {
        super(application);
    }

    public void setCpConnectStatus(boolean isConnected) {
        cpConnectStatus.postValue(isConnected);
    }

    /**
     * 处理Netty消息
     */
    public void handleNettyMessage(NettyMessage message) {
        if (message == null) {
            return;
        }
        
        LogUtils.d(TAG, "Handling message: " + message);
        
        byte moduleCode = message.getModuleCode();
        byte commandCode = message.getCommandCode();
        byte[] data = message.getData();
        
        // 根据模组代码和指令代码处理消息
        switch (moduleCode) {
            case NettyMessage.MODULE_PTZ:
                handlePtzMessage(commandCode, data);
                break;
            case NettyMessage.MODULE_LASER_DISTANCE:
                handleLaserDistanceMessage(commandCode, data);
                break;
            case NettyMessage.MODULE_LASER_INDICATOR:
                handleLaserIndicatorMessage(commandCode, data);
                break;
            case NettyMessage.MODULE_HUMAN_SENSOR:
                handleHumanSensorMessage(commandCode, data);
                break;
            case NettyMessage.MODULE_ACCELEROMETER:
                handleAccelerometerMessage(commandCode, data);
                break;
            case NettyMessage.MODULE_LASER_FOCUS:
                handleLaserFocusMessage(commandCode, data);
                break;
            case NettyMessage.MODULE_LASER_COMMAND:
                handleLaserCommandMessage(commandCode, data);
                break;
            default:
                LogUtils.w(TAG, "Unknown module code: " + String.format("0x%02X", moduleCode));
        }
    }

    //处理云台消息
    private void handlePtzMessage(byte commandCode, byte[] data) {
        LogUtils.d(TAG, "PTZ message, command: " + String.format("0x%02X", commandCode));
        // 根据指令代码处理云台控制逻辑
        switch (commandCode) {
            case 0x00:
                // 停止转动
                break;
            case 0x01:
                // 向上
                break;
            case 0x02:
                // 向下
                break;
            case 0x03:
                // 向左
                break;
            case 0x04:
                // 向右
                break;
            default:
                LogUtils.w(TAG, "Unknown PTZ command: " + String.format("0x%02X", commandCode));
        }
    }

    //处理激光测距消息
    private void handleLaserDistanceMessage(byte commandCode, byte[] data) {
        LogUtils.d(TAG, "Laser distance message, command: " + String.format("0x%02X", commandCode));
        // 解析测距数据等
    }

    //处理激光指示消息
    private void handleLaserIndicatorMessage(byte commandCode, byte[] data) {
        LogUtils.d(TAG, "Laser indicator message, command: " + String.format("0x%02X", commandCode));
    }

    //处理人体感应消息
    private void handleHumanSensorMessage(byte commandCode, byte[] data) {
        LogUtils.d(TAG, "Human sensor message, command: " + String.format("0x%02X", commandCode));
    }

    //处理加速度计消息
    private void handleAccelerometerMessage(byte commandCode, byte[] data) {
        LogUtils.d(TAG, "Accelerometer message, command: " + String.format("0x%02X", commandCode));
    }

    //处理激光调焦消息
    private void handleLaserFocusMessage(byte commandCode, byte[] data) {
        LogUtils.d(TAG, "Laser focus message, command: " + String.format("0x%02X", commandCode));
    }

    //处理激光指令消息
    private void handleLaserCommandMessage(byte commandCode, byte[] data) {
    }

    //region 云台方向控制 发送转动 0下 1左 2上 3右
    public void controlCpDirection(int direction, boolean isPress) {
        // 构建云台控制指令
        byte commandCode;
        switch (direction) {
            case 0: // 下
                commandCode = isPress ? (byte) 0x02 : (byte) 0x00;
                break;
            case 1: // 左
                commandCode = isPress ? (byte) 0x03 : (byte) 0x00;
                break;
            case 2: // 上
                commandCode = isPress ? (byte) 0x01 : (byte) 0x00;
                break;
            case 3: // 右
                commandCode = isPress ? (byte) 0x04 : (byte) 0x00;
                break;
            default:
                commandCode = (byte) 0x00;
        }
        
        // 发送指令
        NettyManager.getInstance().sendMessage(
                NettyMessage.MODULE_PTZ,
                commandCode,
                null
        );
    }
    //endregion
}
