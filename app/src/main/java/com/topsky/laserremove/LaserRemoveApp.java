package com.topsky.laserremove;

import com.blankj.utilcode.util.LogUtils;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonToken;
import com.hjq.gson.factory.GsonFactory;
import com.hjq.gson.factory.ParseExceptionCallback;
import com.hjq.http.EasyConfig;
import com.hjq.http.config.IRequestServer;
import com.topsky.laserremove.net.CameraServer;
import com.topsky.laserremove.net.RequestHandler;
import com.topsky.laserremove.utils.LocalCacheUtil;

import androidx.multidex.MultiDexApplication;
import okhttp3.OkHttpClient;

public class LaserRemoveApp extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtils.getConfig()
                .setLogSwitch(BuildConfig.DEBUG);
        initNet();
    }

    private void initNet() {
        // 设置 Json 解析容错监听
        GsonFactory.setParseExceptionCallback(new ParseExceptionCallback() {

            @Override
            public void onParseObjectException(TypeToken<?> typeToken, String fieldName, JsonToken jsonToken) {
                handlerGsonParseException("Object parsing exception: " + typeToken + "#" + fieldName + ", backend return type: " + jsonToken);
            }

            @Override
            public void onParseListItemException(TypeToken<?> typeToken, String fieldName, JsonToken listItemJsonToken) {
                handlerGsonParseException("List parsing exception: " + typeToken + "#" + fieldName + ", backend return item type: " + listItemJsonToken);
            }

            @Override
            public void onParseMapItemException(TypeToken<?> typeToken, String fieldName, String mapItemKey, JsonToken mapItemJsonToken) {
                handlerGsonParseException("Map parsing exception: " + typeToken + "#" + fieldName + ", mapItemKey = " + mapItemKey + ", backend return item type: " + mapItemJsonToken);
            }

            private void handlerGsonParseException(String message) {
                IllegalArgumentException exception = new IllegalArgumentException(message);
                if (BuildConfig.DEBUG) {
                    throw exception;
                }
            }
        });

        // 网络请求框架初始化
        IRequestServer server = new CameraServer();
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .build();

        EasyConfig.with(okHttpClient)
                // 是否打印日志
                .setLogEnabled(BuildConfig.DEBUG)
                // 设置服务器配置（必须设置）
                .setServer(server)
                // 设置请求处理策略（必须设置）
                .setHandler(new RequestHandler(this))
                .into();
    }
}
