package com.topsky.laserremove.net;

import com.hjq.http.config.IRequestApi;

import androidx.annotation.NonNull;

public final class CameraZoomApi implements IRequestApi {

    @NonNull
    @Override
    public String getApi() {
        return "PtzCtrl.cgi?speed=1&channelno=0&value=0&operation=" + type;
    }

    private final int type;

    public CameraZoomApi(int type) {
        this.type = type;
    }

    public static final class Bean {

    }
}
