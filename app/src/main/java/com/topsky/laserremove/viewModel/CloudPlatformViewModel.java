package com.topsky.laserremove.viewModel;

import android.app.Application;
import android.widget.Toast;

import com.topsky.laserremove.LaserRemoveApp;
import com.topsky.laserremove.R;
import com.topsky.laserremove.base.BaseViewModel;
import com.topsky.laserremove.enums.SpeedType;
import com.topsky.laserremove.net.netty.NettyManager;
import com.topsky.laserremove.net.netty.NettyMessage;
import com.topsky.laserremove.net.netty.NettyMessageUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class CloudPlatformViewModel extends BaseViewModel {
    private static final String TAG = "CloudPlatformViewModel";
    // 连接状态
    private final MutableLiveData<Boolean> connected = new MutableLiveData<>(false);

    public LiveData<Boolean> getConnected() {
        return connected;
    }

    // 电压
    private final MutableLiveData<Float> voltage = new MutableLiveData<>(0f);

    public LiveData<Float> getVoltage() {
        return voltage;
    }

    public CloudPlatformViewModel(@NonNull Application application) {
        super(application);
    }

    private boolean connectedTip() {
        Boolean value = connected.getValue();
        if (value == null || !value) {
            Toast.makeText(LaserRemoveApp.getInstance(), R.string.ty_cloud_disconnect, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public void onControlSendSuccess() {

    }

    public void onConnectedChange(boolean connected) {
        this.connected.setValue(connected);
    }

    public void onVoltageChange(float voltage) {
        this.voltage.setValue(voltage);
    }

    public void handleNettyMessage(NettyMessage message) {
        if (message == null) {
            return;
        }

        byte moduleCode = message.getModuleCode();
        byte commandCode = message.getCommandCode();
        if (moduleCode == NettyMessage.MODULE_PTZ && commandCode == 0x08) {
            //获取角度
            byte[] data = message.getData();
            if (data.length >= 5) {
                boolean axisY = data[0] == 0x01;
                byte[] bytes = NettyMessageUtils.subArray(data, 1, 4);
                int value = NettyMessageUtils.bytesToInt(bytes);
                if (axisY) {
                    if (isMarkA) {
                        axisYA = value;
                    } else {
                        axisYB = value;
                    }
                } else {
                    if (isMarkA) {
                        axisXA = value;
                    } else {
                        axisXB = value;
                    }
                }
            }
        }
    }

    //region 云台方向控制 发送转动 0下 1左 2上 3右
    public void controlCpDirection(int direction, boolean isPress, @SpeedType int speedType) {
        /*if (!connectedTip()) {
            return;
        }*/
        // 构建云台控制指令
        byte[] data = new byte[3];
        byte[] dataStop = new byte[1];
        data[2] = speedType == SpeedType.SLOW ? (byte) 0x00 : (speedType == SpeedType.MIDDLE ? (byte) 0x01 : (byte) 0x02);
        switch (direction) {
            case 0: // 下
                data[0] = (byte) 0x01;
                dataStop[0] = (byte) 0x01;
                data[1] = (byte) 0x01;
                break;
            case 1: // 左
                data[0] = (byte) 0x00;
                dataStop[0] = (byte) 0x00;
                data[1] = (byte) 0x01;
                break;
            case 2: // 上
                data[0] = (byte) 0x01;
                dataStop[0] = (byte) 0x01;
                data[1] = (byte) 0x00;
                break;
            case 3: // 右
                data[0] = (byte) 0x00;
                dataStop[0] = (byte) 0x00;
                data[1] = (byte) 0x00;
                break;
        }
        // 发送指令
        NettyManager.getInstance().sendMessage(
                NettyMessage.MODULE_PTZ,
                isPress ? (byte) 0x01 : (byte) 0x03,
                isPress ? data : dataStop
        );
    }
    //endregion

    //region标记A B点
    private boolean isMarkA = false;
    private int axisXA;
    private int axisYA;
    private int axisXB;
    private int axisYB;

    public void controlCpMark(boolean isMarkA) {
        this.isMarkA = isMarkA;
        NettyManager.getInstance().sendMessage(
                NettyMessage.MODULE_PTZ,
                (byte) 0x07,
                new byte[]{ (byte) 0x00 }
        );

        NettyManager.getInstance().sendMessage(
                NettyMessage.MODULE_PTZ,
                (byte) 0x07,
                new byte[]{ (byte) 0x01 }
        );
    }
    //endregion

    // region 移动到A B点
    public void controlMoveMark(boolean isPointA) {
        byte[] dataX = new byte[5];
        dataX[0] = (byte) 0x00;
        byte[] bytesX;
        if (isPointA) {
            bytesX = NettyMessageUtils.intToBytes(axisXA);
        } else {
            bytesX = NettyMessageUtils.intToBytes(axisXB);
        }
        System.arraycopy(bytesX, 0, dataX, 1, 4);

        byte[] dataY = new byte[5];
        dataY[0] = (byte) 0x01;
        byte[] bytesY;
        if (isPointA) {
            bytesY = NettyMessageUtils.intToBytes(axisYA);
        } else {
            bytesY = NettyMessageUtils.intToBytes(axisYB);
        }
        System.arraycopy(bytesY, 0, dataY, 1, 4);

        NettyManager.getInstance().sendMessage(
                NettyMessage.MODULE_PTZ,
                (byte) 0x04,
                dataX
        );

        NettyManager.getInstance().sendMessage(
                NettyMessage.MODULE_PTZ,
                (byte) 0x04,
                dataY
        );
    }
    //endregion
}
