package com.topsky.laserremove.net.netty;

public interface INettyListener {

    void onConnected();

    void onDisconnected();

    void onConnectFailed(Throwable throwable);

    void onMessageReceived(NettyMessage message);

    void onSendFailed(NettyMessage message, Throwable throwable);
}