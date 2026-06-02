package com.topsky.laserremove.net.netty;

import com.blankj.utilcode.util.LogUtils;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Netty消息编码器
 * 将NettyMessage对象转换为字节数组发送
 */
public class NettyMessageEncoder extends MessageToByteEncoder<NettyMessage> {

    private static final String TAG = "NettyMessageEncoder";

    @Override
    protected void encode(ChannelHandlerContext ctx, NettyMessage msg, ByteBuf out) {
        if (msg == null) {
            LogUtils.e(TAG, "Message is null");
            return;
        }

        byte[] bytes = msg.toBytes();
        out.writeBytes(bytes);
        
        LogUtils.d(TAG, "Encoded message: " + msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LogUtils.e(TAG, "Encoder exception: " + cause.getMessage());
        ctx.fireExceptionCaught(cause);
    }
}