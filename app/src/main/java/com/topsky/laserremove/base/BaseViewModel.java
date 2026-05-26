package com.topsky.laserremove.base;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * ViewModel 基类
 * 提供通用的状态管理和错误处理
 */
public abstract class BaseViewModel extends AndroidViewModel {

    private static final String TAG = "BaseViewModel";

    protected LifecycleOwner lifecycleOwner;

    public void setLifecycleOwner(LifecycleOwner lifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner;
    }

    // 错误消息
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    protected void setErrorMessage(String message) {
        errorMessage.postValue(message);
    }

    // 成功消息
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    protected void setSuccessMessage(String message) {
        successMessage.postValue(message);
    }

    public BaseViewModel(@NonNull Application application) {
        super(application);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
