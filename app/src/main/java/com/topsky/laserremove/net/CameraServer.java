package com.topsky.laserremove.net;

import com.hjq.http.config.IRequestServer;
import com.topsky.laserremove.constant.CommonData;

import androidx.annotation.NonNull;

public class CameraServer implements IRequestServer {
    @NonNull
    @Override
    public String getHost() {
        return CommonData.BASE_URL;
    }
}
