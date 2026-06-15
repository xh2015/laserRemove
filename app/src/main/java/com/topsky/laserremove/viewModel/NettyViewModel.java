package com.topsky.laserremove.viewModel;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.topsky.laserremove.base.BaseViewModel;
import com.topsky.laserremove.net.netty.INettyListener;
import com.topsky.laserremove.net.netty.NettyMessage;
import com.topsky.laserremove.service.NettyClientService;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class NettyViewModel extends BaseViewModel implements INettyListener {
    // Netty服务绑定状态
    private final MutableLiveData<Boolean> serviceBound = new MutableLiveData<>(false);

    // 连接状态
    private final MutableLiveData<Boolean> connected = new MutableLiveData<>(false);

    public LiveData<Boolean> getConnected() {
        return connected;
    }

    // 接收到的消息（用于分发）
    private final MutableLiveData<NettyMessage> receivedMessage = new MutableLiveData<>();

    // Service引用
    @SuppressLint("StaticFieldLeak")
    private NettyClientService nettyService;

    // Service连接
    private ServiceConnection serviceConnection;

    public NettyViewModel(@NonNull Application application) {
        super(application);
        initServiceConnection();
    }

    //初始化Service连接
    private void initServiceConnection() {
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                NettyClientService.NettyBinder binder = (NettyClientService.NettyBinder) service;
                nettyService = binder.getService();
                nettyService.setNettyListener(NettyViewModel.this);
                serviceBound.postValue(true);

                // 同步当前连接状态
                if (nettyService.isConnected()) {
                    onConnected();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                serviceBound.postValue(false);
                connected.postValue(false);
                nettyService = null;
            }
        };
    }

    //启动并绑定Netty服务
    public void startAndBindService() {
        Intent intent = new Intent(getApplication(), NettyClientService.class);
        getApplication().startService(intent);
        getApplication().bindService(intent, serviceConnection, 0);
    }

    //解绑Netty服务
    public void unbindService() {
        if (serviceBound.getValue() != null && serviceBound.getValue()) {
            getApplication().unbindService(serviceConnection);
            serviceBound.postValue(false);
        }
    }

    //发送消息
    public void sendMessage(NettyMessage message) {
        if (nettyService != null) {
            nettyService.sendMessage(message);
        }
    }

    //发送消息
    public void sendMessage(byte moduleCode, byte commandCode, byte[] data) {
        sendMessage(new NettyMessage(moduleCode, commandCode, data));
    }


    //分发消息到对应的ViewModel
    public void dispatchMessage(CloudPlatformViewModel cloudPlatformViewModel,
                                LaserControlViewModel laserControlViewModel) {
        receivedMessage.observeForever(message -> {
            if (message == null) {
                return;
            }

            byte moduleCode = message.getModuleCode();

            if (moduleCode == NettyMessage.MODULE_LASER_CONTROL) {
                //0xF3: 激光控制消息
                if (laserControlViewModel != null) {
                    laserControlViewModel.handleNettyMessage(message);
                }
            } else {
                //云台相关消息
                if (cloudPlatformViewModel != null) {
                    cloudPlatformViewModel.handleNettyMessage(message);
                }
            }
        });
    }

    @Override
    public void onConnected() {
        connected.postValue(true);
    }

    @Override
    public void onDisconnected() {
        connected.postValue(false);
    }

    @Override
    public void onConnectFailed(Throwable throwable) {
        connected.postValue(false);
    }

    @Override
    public void onMessageReceived(NettyMessage message) {
        receivedMessage.postValue(message);
    }

    @Override
    public void onSendFailed(NettyMessage message, Throwable throwable) {
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        unbindService();
    }
}