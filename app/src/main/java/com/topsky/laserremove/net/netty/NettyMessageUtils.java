package com.topsky.laserremove.net.netty;

import java.util.Locale;

/**
 * Netty消息工具类
 * 提供字节数组转换、十六进制字符串处理等常用功能
 */
public class NettyMessageUtils {

    /**
     * 将字节数组转换为十六进制字符串
     * 格式：AA BB CC DD
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(String.format(Locale.US, "%02X", bytes[i]));
        }
        return sb.toString();
    }

    /**
     * 将字节数组转换为紧凑的十六进制字符串（无空格）
     * 格式：AABBCCDD
     *
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    public static String bytesToHexCompact(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format(Locale.US, "%02X", b));
        }
        return sb.toString();
    }

    /**
     * 将十六进制字符串转换为字节数组
     * 支持格式："AA BB CC DD" 或 "AABBCCDD"
     *
     * @param hex 十六进制字符串
     * @return 字节数组
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            return new byte[0];
        }

        // 移除空格
        hex = hex.replace(" ", "");

        // 如果长度为奇数，前面补0
        if (hex.length() % 2 != 0) {
            hex = "0" + hex;
        }

        int len = hex.length() / 2;
        byte[] bytes = new byte[len];

        for (int i = 0; i < len; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }

        return bytes;
    }

    /**
     * 将单个字节转换为十六进制字符串
     *
     * @param b 字节
     * @return 十六进制字符串（如：0xAA）
     */
    public static String byteToHex(byte b) {
        return String.format(Locale.US, "0x%02X", b);
    }

    /**
     * 打印字节数组的详细信息（用于调试）
     *
     * @param bytes 字节数组
     * @return 格式化字符串
     */
    public static String dumpBytes(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }

        if (bytes.length == 0) {
            return "empty";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Length: ").append(bytes.length).append(" bytes\n");
        sb.append("Hex: ").append(bytesToHex(bytes)).append("\n");
        sb.append("Decimal: ");

        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                sb.append(" ");
            }
            sb.append(bytes[i] & 0xFF);
        }

        return sb.toString();
    }

    /**
     * 计算字节数组的校验和（所有字节相加取低字节）
     *
     * @param bytes 字节数组
     * @return 校验和
     */
    public static byte calculateChecksum(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return 0;
        }

        int sum = 0;
        for (byte b : bytes) {
            sum += b & 0xFF;
        }

        return (byte) sum;
    }

    /**
     * 验证字节数组的校验和
     *
     * @param bytes 包含校验和的完整字节数组
     * @param checksumIndex 校验和在数组中的位置
     * @return 是否有效
     */
    public static boolean validateChecksum(byte[] bytes, int checksumIndex) {
        if (bytes == null || checksumIndex < 0 || checksumIndex >= bytes.length) {
            return false;
        }

        byte expected = bytes[checksumIndex];
        byte calculated = calculateChecksumExcluding(bytes, checksumIndex);

        return expected == calculated;
    }

    /**
     * 计算排除指定位置的校验和
     *
     * @param bytes 字节数组
     * @param excludeIndex 要排除的位置
     * @return 校验和
     */
    public static byte calculateChecksumExcluding(byte[] bytes, int excludeIndex) {
        if (bytes == null) {
            return 0;
        }

        int sum = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (i != excludeIndex) {
                sum += bytes[i] & 0xFF;
            }
        }

        return (byte) sum;
    }

    /**
     * 从字节数组中提取子数组
     *
     * @param bytes 原始字节数组
     * @param start 起始位置
     * @param length 长度
     * @return 子数组
     */
    public static byte[] subArray(byte[] bytes, int start, int length) {
        if (bytes == null || start < 0 || length <= 0 || start >= bytes.length) {
            return new byte[0];
        }

        int actualLength = Math.min(length, bytes.length - start);
        byte[] result = new byte[actualLength];
        System.arraycopy(bytes, start, result, 0, actualLength);

        return result;
    }

    /**
     * 合并多个字节数组
     *
     * @param arrays 要合并的字节数组
     * @return 合并后的数组
     */
    public static byte[] mergeArrays(byte[]... arrays) {
        if (arrays == null || arrays.length == 0) {
            return new byte[0];
        }

        int totalLength = 0;
        for (byte[] array : arrays) {
            if (array != null) {
                totalLength += array.length;
            }
        }

        byte[] result = new byte[totalLength];
        int offset = 0;

        for (byte[] array : arrays) {
            if (array != null && array.length > 0) {
                System.arraycopy(array, 0, result, offset, array.length);
                offset += array.length;
            }
        }

        return result;
    }

    /**
     * 将short转换为2字节数组（大端序）
     *
     * @param value short值
     * @return 字节数组
     */
    public static byte[] shortToBytes(short value) {
        return new byte[]{
            (byte) ((value >> 8) & 0xFF),
            (byte) (value & 0xFF)
        };
    }

    /**
     * 将2字节数组转换为short（大端序）
     *
     * @param bytes 字节数组
     * @return short值
     */
    public static short bytesToShort(byte[] bytes) {
        if (bytes == null || bytes.length < 2) {
            return 0;
        }

        return (short) (((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF));
    }

    /**
     * 将int转换为4字节数组（大端序）
     *
     * @param value int值
     * @return 字节数组
     */
    public static byte[] intToBytes(int value) {
        return new byte[]{
            (byte) ((value >> 24) & 0xFF),
            (byte) ((value >> 16) & 0xFF),
            (byte) ((value >> 8) & 0xFF),
            (byte) (value & 0xFF)
        };
    }

    /**
     * 将4字节数组转换为int（大端序）
     *
     * @param bytes 字节数组
     * @return int值
     */
    public static int bytesToInt(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return 0;
        }

        return ((bytes[0] & 0xFF) << 24) |
               ((bytes[1] & 0xFF) << 16) |
               ((bytes[2] & 0xFF) << 8) |
               (bytes[3] & 0xFF);
    }

    /**
     * 比较两个字节数组是否相等
     *
     * @param a 字节数组1
     * @param b 字节数组2
     * @return 是否相等
     */
    public static boolean arraysEqual(byte[] a, byte[] b) {
        if (a == b) {
            return true;
        }

        if (a == null || b == null) {
            return false;
        }

        if (a.length != b.length) {
            return false;
        }

        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }

        return true;
    }
}
