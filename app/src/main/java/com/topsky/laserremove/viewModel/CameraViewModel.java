package com.topsky.laserremove.viewModel;

import android.app.Application;

import com.blankj.utilcode.util.LogUtils;
import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallbackProxy;
import com.topsky.laserremove.base.BaseViewModel;
import com.topsky.laserremove.net.CameraZoomApi;
import com.topsky.laserremove.net.HttpData;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * 相机控制 ViewModel
 * 负责相机相关的所有业务逻辑（变焦、对焦等）
 */
public class CameraViewModel extends BaseViewModel {
    //光学变倍
    private final MutableLiveData<Integer> cameraZoom = new MutableLiveData<>(10);

    public LiveData<Integer> getCameraZoom() {
        return cameraZoom;
    }

    public CameraViewModel(@NonNull Application application) {
        super(application);
    }

    //相机变焦控制  type 9-放大, 10-缩小, 0-停止
    public void changeCameraZoom(int type) {
        if (lifecycleOwner == null) {
            return;
        }
        EasyHttp.post(lifecycleOwner)
                .api(new CameraZoomApi(type))
                .request(new HttpCallbackProxy<HttpData<CameraZoomApi.Bean>>(null) {

                    @Override
                    public void onHttpSuccess(@NonNull HttpData<CameraZoomApi.Bean> result) {
                        super.onHttpSuccess(result);
                        LogUtils.d("相机变焦成功, type: " + type);
                    }

                    @Override
                    public void onHttpFail(@NonNull Throwable e) {
                        super.onHttpFail(e);
                        LogUtils.e("相机变焦失败: " + e.getMessage());
                    }
                });
    }

    //开始放大
    public void startZoomIn() {
        changeCameraZoom(9);
    }

    //开始缩小
    public void startZoomOut() {
        changeCameraZoom(10);
    }

    //停止控制
    public void stopZoom() {
        changeCameraZoom(0);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        LogUtils.i("CameraViewModel 已清理");
    }
}
