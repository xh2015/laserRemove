package com.topsky.laserremove.viewModel;

import android.app.Application;

import com.blankj.utilcode.util.LogUtils;
import com.topsky.laserremove.base.BaseViewModel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class CloudPlatformViewModel extends BaseViewModel {
    // 云台连接状态
    private final MutableLiveData<Boolean> cpConnectStatus = new MutableLiveData<>(false);

    public LiveData<Boolean> getCpConnectStatus() {
        return cpConnectStatus;
    }

    public CloudPlatformViewModel(@NonNull Application application) {
        super(application);
    }

    public void setCpConnectStatus(boolean isConnected) {
        cpConnectStatus.postValue(isConnected);
    }

    //region 云台方向控制 发送转动 0下 1左 2上 3右
    public void controlCpDirection(int direction, boolean isPress) {
    }
    //endregion
}
