package com.topsky.laserremove.net.netty;

import com.blankj.utilcode.util.LogUtils;
import com.topsky.laserremove.BuildConfig;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

/**
 * Netty消息解码器
 * 处理TCP拆包粘包问题
 * 基于帧头帧尾的协议解析
 */
public class NettyMessageDecoder extends ByteToMessageDecoder {

    private static final String TAG = "NettyMessageDecoder";

    // 当前解析状态
    private enum DecodeState {
        FIND_HEADER,      // 查找帧头
        READ_LENGTH,      // 读取帧长度
        READ_CONTENT,     // 读取内容
        READ_CHECKSUM     // 读取校验和
    }

    private DecodeState state = DecodeState.FIND_HEADER;
    private byte frameLength;
    private int contentRead;
    private ByteBuf tempBuffer;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (BuildConfig.DEBUG) {
            // 标记读索引,方便查看数据
            in.markReaderIndex();

            // 打印所有可读字节的十六进制数据
            if (in.readableBytes() > 0) {
                byte[] data = new byte[in.readableBytes()];
                in.getBytes(in.readerIndex(), data);
                StringBuilder sb = new StringBuilder();
                for (byte b : data) {
                    sb.append(String.format("%02X ", b));
                }
                LogUtils.d(TAG, "Raw data received: [" + sb.toString().trim() + "]");
            }

            // 恢复读索引
            in.resetReaderIndex();
        }

        while (in.isReadable()) {
            switch (state) {
                case FIND_HEADER:
                    if (in.readableBytes() >= 1) {
                        byte header = in.readByte();
                        if (header == NettyMessage.FRAME_HEADER) {
                            // 找到帧头，开始读取长度
                            state = DecodeState.READ_LENGTH;
                            if (tempBuffer != null) {
                                tempBuffer.release();
                                tempBuffer = null;
                            }
                        } else {
                            // 不是帧头，丢弃
                            LogUtils.d(TAG, "Discarding invalid byte: " + String.format("0x%02X", header));
                        }
                    } else {
                        return;
                    }
                    break;

                case READ_LENGTH:
                    if (in.readableBytes() >= 1) {
                        frameLength = in.readByte();
                        // 验证帧长度是否合理
                        if (frameLength < NettyMessage.MIN_FRAME_LENGTH || frameLength > NettyMessage.MAX_FRAME_LENGTH) {
                            LogUtils.e(TAG, "Invalid frame length: " + frameLength);
                            state = DecodeState.FIND_HEADER;
                            continue;
                        }
                        // 分配临时缓冲区，减去已读取的2字节（帧头+长度）
                        int contentLength = frameLength - 2;
                        tempBuffer = ctx.alloc().buffer(contentLength);
                        contentRead = 0;
                        state = DecodeState.READ_CONTENT;
                    } else {
                        return;
                    }
                    break;

                case READ_CONTENT:
                    int remaining = tempBuffer.capacity() - contentRead;
                    int toRead = Math.min(remaining, in.readableBytes());

                    if (toRead > 0) {
                        // 使用 writeBytes 方法，会自动更新 writerIndex
                        tempBuffer.writeBytes(in, toRead);
                        contentRead += toRead;
                    }

                    if (contentRead == tempBuffer.capacity()) {
                        // 内容读取完毕，验证帧尾和校验和
                        tempBuffer.readerIndex(tempBuffer.capacity() - 2);
                        byte tail = tempBuffer.readByte();
                        byte checksum = tempBuffer.readByte();

                        if (tail != NettyMessage.FRAME_TAIL) {
                            LogUtils.e(TAG, "Invalid frame tail: " + String.format("0x%02X", tail));
                            state = DecodeState.FIND_HEADER;
                            tempBuffer.release();
                            tempBuffer = null;
                            continue;
                        }

                        // 构建完整消息并验证校验和
                        byte[] fullMessage = new byte[frameLength];
                        fullMessage[0] = NettyMessage.FRAME_HEADER;
                        fullMessage[1] = frameLength;
                        tempBuffer.readerIndex(0);
                        tempBuffer.readBytes(fullMessage, 2, tempBuffer.capacity());

                        NettyMessage message = NettyMessage.fromBytes(fullMessage);
                        if (message != null && message.validateChecksum()) {
                            out.add(message);
                            //LogUtils.d(TAG, "Decoded message: " + message);
                        } else {
                            LogUtils.e(TAG, "Checksum validation failed");
                        }

                        tempBuffer.release();
                        tempBuffer = null;
                        state = DecodeState.FIND_HEADER;
                    } else {
                        return;
                    }
                    break;
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LogUtils.e(TAG, "Decoder exception: " + cause.getMessage());
        if (tempBuffer != null) {
            tempBuffer.release();
            tempBuffer = null;
        }
        state = DecodeState.FIND_HEADER;
    }

    @Override
    protected void handlerRemoved0(ChannelHandlerContext ctx) {
        if (tempBuffer != null) {
            tempBuffer.release();
            tempBuffer = null;
        }
    }
}