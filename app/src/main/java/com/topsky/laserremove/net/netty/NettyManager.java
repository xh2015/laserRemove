package com.topsky.laserremove.net.netty;

import java.util.concurrent.CopyOnWriteArrayList;

public class NettyManager implements INettyListener {
    private final NettyClient nettyClient;
    // 消息监听器列表（支持多个组件监听）
    private final CopyOnWriteArrayList<INettyListener> listeners = new CopyOnWriteArrayList<>();
    // 连接状态
    private volatile boolean isConnected = false;

    private NettyManager() {
        nettyClient = new NettyClient();
        nettyClient.setListener(this);
    }

    private static class Holder {
        private static final NettyManager INSTANCE = new NettyManager();
    }

    public static NettyManager getInstance() {
        return Holder.INSTANCE;
    }


    //添加消息监听器
    public void addListener(INettyListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            // 如果已经连接，立即通知
            if (isConnected) {
                listener.onConnected();
            }
        }
    }

    //移除消息监听器
    public void removeListener(INettyListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    //连接服务器
    public void connect() {
        nettyClient.connect();
    }

    //断开连接
    public void disconnect() {
        nettyClient.disconnect();
    }

    //发送消息
    public void sendMessage(NettyMessage message) {
        if (message == null) {
            return;
        }
        nettyClient.sendMessage(message);
    }

    public void sendMessage(byte moduleCode, byte commandCode, byte[] data) {
        NettyMessage message = new NettyMessage(moduleCode, commandCode, data);
        sendMessage(message);
    }

    //获取连接状态
    public boolean isConnected() {
        return isConnected;
    }

    //释放资源
    public void release() {
        listeners.clear();
        nettyClient.release();
    }

    @Override
    public void onConnected() {
        isConnected = true;
        for (INettyListener listener : listeners) {
            if (listener != null) {
                listener.onConnected();
            }
        }
    }

    @Override
    public void onDisconnected() {
        isConnected = false;
        for (INettyListener listener : listeners) {
            if (listener != null) {
                listener.onDisconnected();
            }
        }
    }

    @Override
    public void onConnectFailed(Throwable throwable) {
        isConnected = false;
        for (INettyListener listener : listeners) {
            if (listener != null) {
                listener.onConnectFailed(throwable);
            }
        }
    }

    @Override
    public void onMessageReceived(NettyMessage message) {
        for (INettyListener listener : listeners) {
            if (listener != null) {
                listener.onMessageReceived(message);
            }
        }
    }

    @Override
    public void onSendFailed(NettyMessage message, Throwable throwable) {
        for (INettyListener listener : listeners) {
            if (listener != null) {
                listener.onSendFailed(message, throwable);
            }
        }
    }
}