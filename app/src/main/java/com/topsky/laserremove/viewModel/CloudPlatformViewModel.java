package com.topsky.laserremove.viewModel;

import android.app.Application;
import android.widget.Toast;

import com.blankj.utilcode.util.LogUtils;
import com.topsky.laserremove.LaserRemoveApp;
import com.topsky.laserremove.R;
import com.topsky.laserremove.base.BaseViewModel;
import com.topsky.laserremove.enums.SpeedType;
import com.topsky.laserremove.net.netty.NettyManager;
import com.topsky.laserremove.net.netty.NettyMessage;
import com.topsky.laserremove.net.netty.NettyMessageUtils;

import java.util.Timer;
import java.util.TimerTask;

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

    // 是否可以显示循环激光弹窗
    private final MutableLiveData<Boolean> cycleAB = new MutableLiveData<>(false);

    public LiveData<Boolean> getCycleAB() {
        return cycleAB;
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
    //注意：当前在子线程中执行，请勿在子线程中更新UI
    }

    public void onConnectedChange(boolean connected) {
        this.connected.postValue(connected);
    }

    public void onVoltageChange(float voltage) {
        this.voltage.postValue(voltage);
    }

    public void handleNettyMessage(NettyMessage message) {
        //注意：当前在子线程中执行，请勿在子线程中更新UI
        if (message == null) {
            return;
        }

        byte moduleCode = message.getModuleCode();
        byte commandCode = message.getCommandCode();
        if (moduleCode == NettyMessage.MODULE_PTZ && commandCode == 0x05) {
            //运动状态
            byte[] data = message.getData();
            if (data.length < 2) {
                return;
            }
            byte axis = data[0];
            byte status = data[1];

            if (axis == 0x00) {
                if (status == 0x00) {
                    isStopX = true;
                }
            } else {
                if (status == 0x00) {
                    isStopY = true;
                }
            }
            if (isStopY && isStopX) {
                isStop = true;
            }
        } else if (moduleCode == NettyMessage.MODULE_PTZ && commandCode == 0x08) {
            //获取角度
            byte[] data = message.getData();
            if (data.length >= 5) {
                boolean axisY = data[0] == 0x01;
                byte[] bytes = NettyMessageUtils.subArray(data, 1, 4);
                int value = NettyMessageUtils.bytesToInt(bytes);
                if (isMarkA != null && !isRecycling) {
                    //标记
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
                    LogUtils.d(TAG, "获取角度: x=" + (isMarkA ? axisXA : axisXB) + " y=" + (isMarkA ? axisYA : axisYB));
                    isMarkA = null;
                } else {
                    //循环获取
                    if (isStop) {
                        //转轴停止 开始检测是否到达指定点
                        if (axisY) {//y
                            if ((value / 1000f) <= ((targetYAngle / 1000f) + 0.01) && (value / 1000f) >= ((targetYAngle / 1000f) - 0.01)) {
                                isArriveY = true;
                            }
                        } else {//x
                            if ((value / 1000f) <= ((targetXAngle / 1000f) + 0.01) && (value / 1000f) >= ((targetXAngle / 1000f) - 0.01)) {
                                isArriveX = true;
                            }
                        }

                        if (isArriveX && isArriveY) {
                            if (targetXAngle == axisXA) {
                                //移动到了a
                                resetMovementFlags();
                                moveToPoint(axisXB, axisYB);
                            } else {
                                //移动到了b
                                resetMovementFlags();
                                moveToPoint(axisXA, axisYA);
                            }
                        }
                    }
                }
            }
        } else if ((moduleCode == NettyMessage.MODULE_HUMAN_SENSOR || moduleCode == NettyMessage.MODULE_ACCELEROMETER) && commandCode == 0x01) {
            //跌落
            if (isRecycling) {
                stopCycleAB();
                cycleAB.postValue(false);
            } else {
                //1.关闭激光

                //2.停止云台
                stopCloudMove();
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
    private Boolean isMarkA = false;
    private int axisXA;
    private int axisYA;
    private int axisXB;
    private int axisYB;

    public void controlCpMark(Boolean isMarkA) {
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
        moveToPoint(isPointA ? axisXA : axisXB, isPointA ? axisYA : axisYB);
    }

    private void moveToPoint(int x, int y) {
        targetXAngle = x;
        targetYAngle = y;
        byte[] dataX = new byte[5];
        dataX[0] = (byte) 0x00;
        byte[] bytesX = NettyMessageUtils.intToBytes(x);
        System.arraycopy(bytesX, 0, dataX, 1, 4);

        byte[] dataY = new byte[5];
        dataY[0] = (byte) 0x01;
        byte[] bytesY = NettyMessageUtils.intToBytes(y);

        System.arraycopy(bytesY, 0, dataY, 1, 4);

        NettyManager.getInstance().

                sendMessage(
                        NettyMessage.MODULE_PTZ,
                        (byte) 0x04,
                        dataX
                );

        NettyManager.getInstance().
                sendMessage(
                        NettyMessage.MODULE_PTZ,
                        (byte) 0x04,
                        dataY
                );
    }
    //endregion

    //region 循环AB点
    private Timer readCloudTask;
    private float targetXAngle;
    private float targetYAngle;
    private boolean isStopX = false;
    private boolean isStopY = false;
    private boolean isStop = false;
    private boolean isArriveX = false;
    private boolean isArriveY = false;
    private boolean isRecycling = false;

    public void cycleAB() {
        if (axisXA == 0 && axisYA == 0 && axisXB == 0 && axisYB == 0) {
            Toast.makeText(LaserRemoveApp.getInstance(), R.string.ty_cloud_no_mark, Toast.LENGTH_SHORT).show();
            return;
        }
        cycleAB.postValue(true);
        moveToPoint(axisXA, axisYA);
        //检查是否到a
        startReadCloud();
    }

    public void stopCycleAB() {
        isRecycling = false;
        stopReadCloud();
        resetMovementFlags();
    }

    //定时读取当前是否运动和转轴角度
    private void startReadCloud() {
        stopReadCloud();
        if (readCloudTask == null) {
            readCloudTask = new Timer();
        }
        readCloudTask.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!isStop) {
                    obtainMove();
                }
                controlCpMark(null);
            }
        }, 0, 700);
    }

    private void stopReadCloud() {
        if (readCloudTask != null) {
            readCloudTask.cancel();
            readCloudTask = null;
        }
    }

    private void resetMovementFlags() {
        isArriveX = false;
        isArriveY = false;
        isStopX = false;
        isStopY = false;
    }

    private void obtainMove() {
        NettyManager.getInstance().
                sendMessage(
                        NettyMessage.MODULE_PTZ,
                        (byte) 0x05,
                        new byte[]{ (byte) 0x00 }
                );

        NettyManager.getInstance().
                sendMessage(
                        NettyMessage.MODULE_PTZ,
                        (byte) 0x05,
                        new byte[]{ (byte) 0x01 }
                );
    }

    //停止云台转动
    private void stopCloudMove() {
        NettyManager.getInstance().sendMessage(
                NettyMessage.MODULE_PTZ,
                (byte) 0x03,
                new byte[]{ (byte) 0x00 }
        );

        NettyManager.getInstance().sendMessage(
                NettyMessage.MODULE_PTZ,
                (byte) 0x03,
                new byte[]{ (byte) 0x01 }
        );
    }
    //endregion
}