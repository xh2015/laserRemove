package com.topsky.laserremove.viewModel;

import android.app.Application;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PathUtils;
import com.topsky.laserremove.base.BaseViewModel;
import com.topsky.laserremove.manager.MP4RecordHelper;

import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * 录像 ViewModel
 * 负责录像相关的所有业务逻辑
 */
public class RecordViewModel extends BaseViewModel implements MP4RecordHelper.MP4RecordCallBack {
    // 录像状态
    private final MutableLiveData<Boolean> isRecording = new MutableLiveData<>(false);

    public LiveData<Boolean> getIsRecording() {
        return isRecording;
    }

    // 是否有有效视频帧（用于判断是否可以录像）
    private final MutableLiveData<Boolean> hasValidFrame = new MutableLiveData<>(false);

    public LiveData<Boolean> getHasValidFrame() {
        return hasValidFrame;
    }

    // 录像管理器
    private MP4RecordHelper mp4RecordHelper;

    public RecordViewModel(@NonNull Application application) {
        super(application);
        initRecordHelper();
    }

    //初始化录像管理器
    private void initRecordHelper() {
        if (mp4RecordHelper == null) {
            mp4RecordHelper = new MP4RecordHelper();
        }
    }

    //更新视频帧数据
    public void updateFrameData(ByteBuffer data, int width, int height, int pixelFormat) {
        if (data == null) {
            return;
        }
        // 保存最新的帧数据用于录像
        hasValidFrame.postValue(true);

        if (mp4RecordHelper != null) {
            mp4RecordHelper.putYUV(data, width, height, pixelFormat);
        }
    }

    //开始录像
    public void startRecord() {
        if (Boolean.FALSE.equals(hasValidFrame.getValue())) {
            setErrorMessage("暂无视频帧数据，请确保RTSP流已启动");
            LogUtils.e("录像失败：没有可用的视频帧数据");
            return;
        }
        mp4RecordHelper.setBitrate(1500000);
        mp4RecordHelper.setFrameRate(15);
        mp4RecordHelper.setVideoSize(1280, 720);
        mp4RecordHelper.setUseSoftEncoder(true);

        String path = PathUtils.getExternalDownloadsPath() + "/laser_" + System.currentTimeMillis() + ".mp4";
        Exception ret = mp4RecordHelper.start(path);

        if (ret == null) {
            isRecording.postValue(true);
            LogUtils.i("开始录像: " + path);
        } else {
            setErrorMessage("录像失败：" + ret.getMessage());
            LogUtils.e("录像失败：" + ret);
        }
    }

    //停止录像
    public void stopRecord() {
        if (mp4RecordHelper != null) {
            mp4RecordHelper.stop(this);
        }
    }


    //录像完成回调
    @Override
    public void onRecordComplete(Exception e) {
        if (e != null) {
            setErrorMessage("停止录像失败：" + e.getMessage());
            LogUtils.e("停止录像失败：" + e);
        }
        isRecording.postValue(false);
    }

    //清理资源
    public void cleanup() {
        if (Boolean.TRUE.equals(isRecording.getValue()) && mp4RecordHelper != null) {
            mp4RecordHelper.stop(null);
        }
        clearFrameData();
    }

    private void clearFrameData() {
        hasValidFrame.postValue(false);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cleanup();
        LogUtils.i("RecordViewModel 已清理");
    }
}
