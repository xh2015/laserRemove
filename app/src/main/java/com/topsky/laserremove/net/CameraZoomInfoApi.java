package com.topsky.laserremove.net;

import com.hjq.http.annotation.HttpHeader;
import com.hjq.http.annotation.HttpRename;
import com.hjq.http.config.IRequestApi;
import com.topsky.laserremove.utils.LocalCacheUtil;

import androidx.annotation.NonNull;

public final class CameraZoomInfoApi implements IRequestApi {

    @NonNull
    @Override
    public String getApi() {
        return "merlin/Image_GetZoomRation.cgi";
    }

    @HttpHeader
    @HttpRename("Authorization")
    private final String authorization = "Basic " + LocalCacheUtil.getBasicAuthEnc();

    @HttpHeader
    @HttpRename("Content-Type")
    private final String contentType = "application/json";


    public CameraZoomInfoApi() {

    }


    public static final class Bean {
        public Zoom Zoom;
    }

    public static final class Zoom {
        public String ZoomRation;
    }
}
