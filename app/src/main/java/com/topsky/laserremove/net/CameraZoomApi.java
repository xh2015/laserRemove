package com.topsky.laserremove.net;

import com.hjq.http.annotation.HttpHeader;
import com.hjq.http.annotation.HttpIgnore;
import com.hjq.http.annotation.HttpRename;
import com.hjq.http.config.IRequestApi;
import com.topsky.laserremove.utils.LocalCacheUtil;

import androidx.annotation.NonNull;

public final class CameraZoomApi implements IRequestApi {

    @NonNull
    @Override
    public String getApi() {
        return "merlin/PtzCtrl.cgi?speed=2&channelno=0&value=0&operation=" + type;
    }

    @HttpHeader
    @HttpRename("Authorization")
    private final String authorization = "Basic " + LocalCacheUtil.getBasicAuthEnc();

    @HttpHeader
    @HttpRename("Content-Type")
    private final String contentType = "application/json";

    @HttpIgnore
    private final int type;

    public CameraZoomApi(int type) {
        this.type = type;
    }


    public static final class Bean {

    }
}
