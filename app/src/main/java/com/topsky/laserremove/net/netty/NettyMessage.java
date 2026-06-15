package com.topsky.laserremove.net.netty;

/**
 * Netty消息协议封装类
 * 帧格式：帧头(1) + 帧长度(1) + 模组代码(1) + 指令代码(1) + 数据(n) + 帧尾(1) + 校验和(1)
 */
public class NettyMessage {

    /**
     * 帧头
     */
    public static final byte FRAME_HEADER = (byte) 0xAA;

    /**
     * 帧尾
     */
    public static final byte FRAME_TAIL = (byte) 0x55;

    /**
     * 最小帧长度：帧头(1) + 帧长度(1) + 模组代码(1) + 指令代码(1) + 帧尾(1) + 校验和(1) = 6
     */
    public static final int MIN_FRAME_LENGTH = 6;

    /**
     * 最大帧长度
     */
    public static final int MAX_FRAME_LENGTH = 256;

    /**
     * 模组代码 - 云台相关
     */
    public static final byte MODULE_PTZ = (byte) 0xF0;

    /**
     * 模组代码 - 激光测距
     */
    public static final byte MODULE_LASER_DISTANCE = (byte) 0xF1;

    /**
     * 模组代码 - 激光指示
     */
    public static final byte MODULE_LASER_INDICATOR = (byte) 0xF2;

    /**
     * 模组代码 - 人体感应
     */
    public static final byte MODULE_HUMAN_SENSOR = (byte) 0xF4;

    /**
     * 模组代码 - 加速度计
     */
    public static final byte MODULE_ACCELEROMETER = (byte) 0xF5;

    /**
     * 模组代码 - 激光调焦
     */
    public static final byte MODULE_LASER_FOCUS = (byte) 0xF6;

    /**
     * 模组代码 - 激光指令
     */
    public static final byte MODULE_LASER_COMMAND = (byte) 0xF7;

    /**
     * 模组代码 - 激光控制
     */
    public static final byte MODULE_LASER_CONTROL = (byte) 0xF3;

    // 帧头
    private byte header;

    // 帧长度（包含帧头到校验和的所有字节数）
    private byte length;

    // 模组代码
    private byte moduleCode;

    // 指令代码
    private byte commandCode;

    // 数据内容
    private byte[] data;

    // 帧尾
    private byte tail;

    // 校验和
    private byte checksum;

    public NettyMessage() {
        this.header = FRAME_HEADER;
        this.tail = FRAME_TAIL;
    }

    public NettyMessage(byte moduleCode, byte commandCode, byte[] data) {
        this.header = FRAME_HEADER;
        this.moduleCode = moduleCode;
        this.commandCode = commandCode;
        this.data = data;
        this.tail = FRAME_TAIL;
        this.length = calculateLength();
        this.checksum = calculateChecksum();
    }

    /**
     * 计算帧长度
     */
    private byte calculateLength() {
        // 帧长度 = 帧头(1) + 帧长度(1) + 模组代码(1) + 指令代码(1) + 数据长度 + 帧尾(1) + 校验和(1)
        int len = 6 + (data != null ? data.length : 0);
        return (byte) len;
    }

    /**
     * 计算校验和（帧头到帧尾所有字节相加取低字节）
     */
    private byte calculateChecksum() {
        int sum = header;
        sum += length;
        sum += moduleCode;
        sum += commandCode;
        if (data != null) {
            for (byte b : data) {
                sum += b & 0xFF;
            }
        }
        sum += tail;
        return (byte) sum;
    }

    /**
     * 验证校验和
     */
    public boolean validateChecksum() {
        byte calculated = calculateChecksum();
        return checksum == calculated;
    }

    /**
     * 转换为字节数组
     */
    public byte[] toBytes() {
        int dataLen = data != null ? data.length : 0;
        byte[] bytes = new byte[6 + dataLen];

        int index = 0;
        bytes[index++] = header;
        bytes[index++] = length;
        bytes[index++] = moduleCode;
        bytes[index++] = commandCode;

        if (data != null) {
            System.arraycopy(data, 0, bytes, index, data.length);
            index += data.length;
        }

        bytes[index++] = tail;
        bytes[index] = checksum;

        return bytes;
    }

    /**
     * 从字节数组解析消息
     */
    public static NettyMessage fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length < MIN_FRAME_LENGTH) {
            return null;
        }

        NettyMessage message = new NettyMessage();
        int index = 0;

        message.header = bytes[index++];
        message.length = bytes[index++];
        message.moduleCode = bytes[index++];
        message.commandCode = bytes[index++];

        int dataLen = bytes.length - 6; // 减去帧头、长度、模组、指令、帧尾、校验和
        if (dataLen > 0) {
            message.data = new byte[dataLen];
            System.arraycopy(bytes, index, message.data, 0, dataLen);
            index += dataLen;
        }

        message.tail = bytes[index++];
        message.checksum = bytes[index];

        return message;
    }

    /**
     * 判断是否是云台模块消息 (0xF0-0xF7)
     */
    public boolean isPtzModule() {
        return moduleCode >= MODULE_PTZ && moduleCode <= MODULE_LASER_COMMAND;
    }

    /**
     * 判断是否是激光控制模块消息 (0xF8)
     */
    public boolean isLaserControlModule() {
        return moduleCode == MODULE_LASER_CONTROL;
    }

    // Getters and Setters
    public byte getHeader() {
        return header;
    }

    public void setHeader(byte header) {
        this.header = header;
    }

    public byte getLength() {
        return length;
    }

    public void setLength(byte length) {
        this.length = length;
    }

    public byte getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(byte moduleCode) {
        this.moduleCode = moduleCode;
    }

    public byte getCommandCode() {
        return commandCode;
    }

    public void setCommandCode(byte commandCode) {
        this.commandCode = commandCode;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
        this.length = calculateLength();
        this.checksum = calculateChecksum();
    }

    public byte getTail() {
        return tail;
    }

    public void setTail(byte tail) {
        this.tail = tail;
    }

    public byte getChecksum() {
        return checksum;
    }

    public void setChecksum(byte checksum) {
        this.checksum = checksum;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NettyMessage{");
        sb.append("header=").append(String.format("0x%02X", header));
        sb.append(", length=").append(length);
        sb.append(", moduleCode=").append(String.format("0x%02X", moduleCode));
        sb.append(", commandCode=").append(String.format("0x%02X", commandCode));
        sb.append(", data=").append(data != null ? data.length + " bytes" : "null");
        if (data != null) {
            sb.append(", data=").append(NettyMessageUtils.bytesToHex(data));
        }
        sb.append(", tail=").append(String.format("0x%02X", tail));
        sb.append(", checksum=").append(String.format("0x%02X", checksum));
        sb.append("}");
        return sb.toString();
    }
}