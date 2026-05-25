package com.topsky.laserremove.utils;

import android.util.Base64;

import com.topsky.laserremove.constant.CommonData;

public class LocalCacheUtil {
    private static String ENCODED_CREDENTIALS = null;

    public static String getBasicAuthEnc() {
        if (ENCODED_CREDENTIALS == null) {
            //添加 Basic 认证请求头
            String credentials = CommonData.CREDENTIALS; // 格式：用户名:密码
            ENCODED_CREDENTIALS = Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
        }
        return ENCODED_CREDENTIALS;
    }
}
