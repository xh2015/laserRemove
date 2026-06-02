package com.topsky.laserremove.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.blankj.utilcode.util.StringUtils;
import com.topsky.laserremove.R;
import com.topsky.laserremove.net.netty.INettyListener;
import com.topsky.laserremove.net.netty.NettyManager;
import com.topsky.laserremove.net.netty.NettyMessage;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class NettyClientService extends Service implements INettyListener {
    private static final String CHANNEL_ID = "netty_service_channel";
    private static final int NOTIFICATION_ID = 1001;
    private final IBinder binder = new NettyBinder();
    // 外部监听器（由ViewModel设置）
    private INettyListener externalListener;
    // 是否正在启动
    private boolean isServiceStarted = false;

    @Override
    public void onCreate() {
        super.onCreate();
        // 创建通知渠道（Android 8.0+）
        createNotificationChannel();
        // 启动为前台服务
        startForeground(NOTIFICATION_ID, createNotification("正在连接..."));
        // 注册Netty监听器
        NettyManager.getInstance().addListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isServiceStarted) {
            isServiceStarted = true;
            // 连接服务器
            NettyManager.getInstance().connect();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // 停止前台服务
        stopForeground(true);
        // 取消注册监听器
        NettyManager.getInstance().removeListener(this);
        // 断开连接
        NettyManager.getInstance().disconnect();
        NettyManager.getInstance().release();
        isServiceStarted = false;
        externalListener = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, StringUtils.getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("保持TCP连接稳定运行");
        channel.setShowBadge(false);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String statusText) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("激光控制系统")
                .setContentText(statusText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setAutoCancel(false);

        return builder.build();
    }

    //更新通知状态
    private void updateNotification(String statusText) {
        Notification notification = createNotification(statusText);
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, notification);
        }
    }

    //设置外部监听器（由ViewModel调用）
    public void setNettyListener(INettyListener listener) {
        this.externalListener = listener;
        // 如果已经连接，立即通知
        if (isConnected() && externalListener != null) {
            externalListener.onConnected();
        }
    }

    //发送消息
    public void sendMessage(NettyMessage message) {
        NettyManager.getInstance().sendMessage(message);
    }

    //获取连接状态
    public boolean isConnected() {
        return NettyManager.getInstance().isConnected();
    }

    public class NettyBinder extends Binder {
        public NettyClientService getService() {
            return NettyClientService.this;
        }
    }


    @Override
    public void onConnected() {
        // 更新通知
        updateNotification("已连接服务器");
        if (externalListener != null) {
            externalListener.onConnected();
        }
    }

    @Override
    public void onDisconnected() {
        // 更新通知
        updateNotification("连接已断开，正在重连...");
        if (externalListener != null) {
            externalListener.onDisconnected();
        }
    }

    @Override
    public void onConnectFailed(Throwable throwable) {
        // 更新通知
        updateNotification("连接失败，正在重试...");
        if (externalListener != null) {
            externalListener.onConnectFailed(throwable);
        }
    }

    @Override
    public void onMessageReceived(NettyMessage message) {
        if (externalListener != null) {
            externalListener.onMessageReceived(message);
        }
    }

    @Override
    public void onSendFailed(NettyMessage message, Throwable throwable) {
        if (externalListener != null) {
            externalListener.onSendFailed(message, throwable);
        }
    }
}