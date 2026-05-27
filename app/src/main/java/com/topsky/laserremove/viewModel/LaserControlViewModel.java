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

    //功率
    private final MutableLiveData<Integer> laserPower = new MutableLiveData<>(10);

    public LiveData<Integer> getLaserPower() {
        return laserPower;
    }

    //距离
    private final MutableLiveData<Integer> laserDistance = new MutableLiveData<>(10);

    public LiveData<Integer> getLaserDistance() {
        return laserDistance;
    }

    //脉冲
    private final MutableLiveData<Integer> laserPulse = new MutableLiveData<>(10);

    public LiveData<Integer> getLaserPulse() {
        return laserPulse;
    }

    public void setLaserConnectStatus(boolean isConnected) {
        laserConnectStatus.postValue(isConnected);
    }

    public LaserControlViewModel(@NonNull Application application) {
        super(application);
    }

    public void setLaserPower(int power) {
        laserPower.postValue(power);
    }

    public void setLaserDistance(int distance) {
        laserDistance.postValue(distance);
    }

    public void setLaserPulse(int pulse) {
        laserPulse.postValue(pulse);
    }
}
