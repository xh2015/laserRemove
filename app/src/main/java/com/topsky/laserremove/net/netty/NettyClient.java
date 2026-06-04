package com.topsky.laserremove.net.netty;

import com.blankj.utilcode.util.LogUtils;

import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

public class NettyClient {
    private static final String TAG = "NettyClient";
    // 服务器地址和端口
    private static final String DEFAULT_HOST = "192.168.1.33";
    //private static final String DEFAULT_HOST = "192.168.0.9";
    private static final int DEFAULT_PORT = 8899;
    // 心跳间隔（秒）
    private static final int HEARTBEAT_INTERVAL = 30;
    // 重连间隔（秒）
    private static final int RECONNECT_DELAY = 4;
    // 最大重连次数
    private static final int MAX_RECONNECT_COUNT = 3000;
    private final String host;
    private final int port;

    private Bootstrap bootstrap;
    private EventLoopGroup eventLoopGroup;
    private Channel channel;

    private INettyListener listener;

    // 连接状态
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean isConnecting = new AtomicBoolean(false);
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);

    // 重连计数器
    private int reconnectCount = 0;

    // 心跳任务
    private Runnable heartbeatTask;
    private volatile boolean heartbeatRunning = false;

    // 线程池
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    public NettyClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    public NettyClient(String host, int port) {
        this.host = host;
        this.port = port;
        init();
    }

    private void init() {
        eventLoopGroup = new NioEventLoopGroup(4);
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new IdleStateHandler(0, HEARTBEAT_INTERVAL, 0, TimeUnit.SECONDS))
                                .addLast(new NettyMessageDecoder())
                                .addLast(new NettyMessageEncoder())
                                .addLast(new NettyClientHandler());
                    }
                });

        LogUtils.d(TAG, "NettyClient initialized, host: " + host + ", port: " + port);
    }

    //设置回调监听器
    public void setListener(INettyListener listener) {
        this.listener = listener;
    }

    //连接服务器
    public void connect() {
        if (isConnected.get() || isConnecting.get()) {
            LogUtils.w(TAG, "Already connected or connecting");
            return;
        }

        isConnecting.set(true);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    LogUtils.d(TAG, "Connecting to " + host + ":" + port);
                    ChannelFuture future = bootstrap.connect(host, port).sync();
                    if (future.isSuccess()) {
                        channel = future.channel();
                        isConnected.set(true);
                        isConnecting.set(false);
                        reconnectCount = 0;
                        LogUtils.d(TAG, "Connected successfully");
                        //startHeartbeat();
                        if (listener != null) {
                            listener.onConnected();
                        }
                        // 阻塞等待连接关闭
                        future.channel().closeFuture().sync();
                    } else {
                        handleConnectFailed(future.cause());
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    handleConnectFailed(e);
                } catch (Exception e) {
                    handleConnectFailed(e);
                }
            }
        });
    }

    //断开连接
    public void disconnect() {
        //stopHeartbeat();
        if (channel != null && channel.isActive()) {
            channel.close().addListener(future -> {
                isConnected.set(false);
                isConnecting.set(false);
                isReconnecting.set(false);
                LogUtils.d(TAG, "Disconnected");
                if (listener != null) {
                    listener.onDisconnected();
                }
            });
        }
    }

    //发送消息
    public void sendMessage(final NettyMessage message) {
        LogUtils.d(TAG, "Sending message: " + message);
        if (!isConnected.get() || channel == null) {
            LogUtils.e(TAG, "Not connected, cannot send message");
            if (listener != null) {
                listener.onSendFailed(message, new IllegalStateException("Not connected"));
            }
            return;
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    channel.writeAndFlush(message).addListener(future -> {
                        if (!future.isSuccess()) {
                            LogUtils.e(TAG, "Send message failed: " + future.cause().getMessage());
                            if (listener != null) {
                                listener.onSendFailed(message, future.cause());
                            }
                        } else {
                            LogUtils.d(TAG, "Message sent: " + message);
                        }
                    });
                } catch (Exception e) {
                    LogUtils.e(TAG, "Send message exception: " + e.getMessage());
                    if (listener != null) {
                        listener.onSendFailed(message, e);
                    }
                }
            }
        });
    }

    //发送心跳包
    private void sendHeartbeat() {
        // 心跳包：帧头 0xAA + 帧长度 0x06 + 模组代码 0xFF + 指令代码 0x00 + 帧尾 0x55 + 校验和
        NettyMessage heartbeat = new NettyMessage((byte) 0xFF, (byte) 0x00, null);
        sendMessage(heartbeat);
    }

    //启动心跳
    private void startHeartbeat() {
        if (heartbeatRunning) {
            return;
        }
        heartbeatTask = new Runnable() {
            @Override
            public void run() {
                while (heartbeatRunning && isConnected.get()) {
                    try {
                        sendHeartbeat();
                        Thread.sleep(HEARTBEAT_INTERVAL * 1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        };

        heartbeatRunning = true;
        executorService.execute(heartbeatTask);
        LogUtils.d(TAG, "Heartbeat started");
    }

    //停止心跳
    private void stopHeartbeat() {
        heartbeatRunning = false;
        if (heartbeatTask != null) {
            heartbeatTask = null;
        }
    }

    //处理连接失败
    private void handleConnectFailed(Throwable cause) {
        isConnected.set(false);
        isConnecting.set(false);
        LogUtils.e(TAG, "Connect failed: " + cause.getMessage());
        if (listener != null) {
            listener.onConnectFailed(cause);
        }
        // 自动重连
        scheduleReconnect();
    }

    //调度重连
    private void scheduleReconnect() {
        if (isReconnecting.get()) {
            return;
        }
        if (reconnectCount >= MAX_RECONNECT_COUNT) {
            LogUtils.e(TAG, "Max reconnect attempts reached, stopping");
            return;
        }
        isReconnecting.set(true);
        reconnectCount++;
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    LogUtils.d(TAG, "Reconnect attempt " + reconnectCount + "/" + MAX_RECONNECT_COUNT +
                            ", waiting " + RECONNECT_DELAY + " seconds");
                    Thread.sleep(RECONNECT_DELAY * 1000);

                    isReconnecting.set(false);

                    if (!isConnected.get()) {
                        LogUtils.d(TAG, "Attempting to reconnect...");
                        connect();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    isReconnecting.set(false);
                }
            }
        });
    }

    //处理连接断开
    private void handleDisconnect() {
        isConnected.set(false);
        LogUtils.d(TAG, "Connection disconnected");
        if (listener != null) {
            listener.onDisconnected();
        }
        // 自动重连
        scheduleReconnect();
    }

    //获取连接状态
    public boolean isConnected() {
        return isConnected.get();
    }

    //释放资源
    public void release() {
        //stopHeartbeat();
        disconnect();

        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }

        LogUtils.d(TAG, "NettyClient released");
    }

    //Netty客户端处理器
    private class NettyClientHandler extends SimpleChannelInboundHandler<NettyMessage> {

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            LogUtils.d(TAG, "Channel active");
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            LogUtils.d(TAG, "Channel inactive");
            handleDisconnect();
        }

        @Override
        public void channelRead0(ChannelHandlerContext ctx, NettyMessage msg) {
            LogUtils.d(TAG, "Message received: " + msg);

            if (listener != null) {
                listener.onMessageReceived(msg);
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            /*if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state() == IdleState.WRITER_IDLE) {
                    // 发送心跳
                    sendHeartbeat();
                }
            }*/
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            LogUtils.e(TAG, "Channel exception: " + cause.getMessage());
            
            // 稳健模式：只在严重网络异常时断开连接
            // 编码/解码等业务异常不影响连接稳定性
            if (cause instanceof IOException || cause instanceof SocketException) {
                LogUtils.w(TAG, "Network exception, closing connection");
                ctx.close();
            } else {
                LogUtils.w(TAG, "Non-critical exception, connection kept alive");
            }
        }
    }
}