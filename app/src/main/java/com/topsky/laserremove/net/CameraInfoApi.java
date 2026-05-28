package com.topsky.laserremove.net;

import com.hjq.http.annotation.HttpHeader;
import com.hjq.http.annotation.HttpRename;
import com.hjq.http.config.IRequestApi;
import com.topsky.laserremove.utils.LocalCacheUtil;

import androidx.annotation.NonNull;

public final class CameraInfoApi implements IRequestApi {

    @NonNull
    @Override
    public String getApi() {
        return "merlin/Image_GetFocusCfg.cgi";
        //return "merlin/Image_GetZoomRation.cgi";
    }

    @HttpHeader
    @HttpRename("Authorization")
    private final String authorization = "Basic " + LocalCacheUtil.getBasicAuthEnc();

    @HttpHeader
    @HttpRename("Content-Type")
    private final String contentType = "application/json";


    public CameraInfoApi() {

    }


    public static final class Bean {
        public Focus Focus;
    }

    public static final class Focus {
        public int ZoomRatioMax;
    }
}
