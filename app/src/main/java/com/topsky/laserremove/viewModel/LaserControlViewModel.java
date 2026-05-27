package com.topsky.laserremove.viewModel;

import android.app.Application;

import com.topsky.laserremove.base.BaseViewModel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class LaserControlViewModel extends BaseViewModel {
    // 激光连接状态
    private final MutableLiveData<Boolean> laserConnectStatus = new MutableLiveData<>(false);

    public LiveData<Boolean> getLaserConnectStatus() {
        return laserConnectStatus;
    }

    public void setLaserConnectStatus(boolean isConnected) {
        laserConnectStatus.postValue(isConnected);
    }

    public LaserControlViewModel(@NonNull Application application) {
        super(application);
    }

}
