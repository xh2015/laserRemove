package com.topsky.laserremove.viewModel;

import android.app.Application;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PathUtils;
import com.topsky.laserremove.base.BaseViewModel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * 截图 ViewModel
 * 负责截图相关的所有业务逻辑
 */
public class ScreenshotViewModel extends BaseViewModel {
    // 截图结果消息
    private final MutableLiveData<String> screenshotMessage = new MutableLiveData<>();

    public LiveData<String> getScreenshotMessage() {
        return screenshotMessage;
    }

    // 视频帧数据缓存
    private ByteBuffer latestYUVData;
    private int latestWidth;
    private int latestHeight;

    public ScreenshotViewModel(@NonNull Application application) {
        super(application);
    }

    //更新视频帧数据
    public void updateFrameData(ByteBuffer data, int width, int height, int pixelFormat) {
        if (data == null) {
            return;
        }
        // 保存最新的帧数据用于截图
        latestYUVData = ByteBuffer.allocateDirect(data.remaining());
        latestYUVData.put(data.duplicate());
        latestWidth = width;
        latestHeight = height;
    }

    /**
     * 执行截图
     */
    public void takeScreenshot() {
        if (latestYUVData == null) {
            screenshotMessage.postValue("暂无视频帧数据，请确保RTSP流已启动");
            LogUtils.e("截图失败：没有可用的视频帧数据");
            return;
        }

        try {
            String screenshotPath = PathUtils.getExternalPicturesPath() + "/laser_" + System.currentTimeMillis() + ".jpg";

            new Thread(() -> {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();

                    latestYUVData.rewind();
                    byte[] yuvData = new byte[latestYUVData.remaining()];
                    latestYUVData.get(yuvData);

                    YuvImage yuvImage = new YuvImage(
                            yuvData,
                            ImageFormat.NV21,
                            latestWidth,
                            latestHeight,
                            null
                    );

                    yuvImage.compressToJpeg(
                            new Rect(0, 0, latestWidth, latestHeight),
                            90,
                            baos
                    );

                    byte[] jpegData = baos.toByteArray();

                    File file = new File(screenshotPath);
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(jpegData);
                    fos.flush();
                    fos.close();

                    screenshotMessage.postValue("截图已保存: " + file.getName());
                    LogUtils.i("截图成功: " + screenshotPath);

                } catch (Exception e) {
                    LogUtils.e("截图保存失败: " + e.getMessage());
                    screenshotMessage.postValue("截图保存失败: " + e.getMessage());
                }
            }).start();

        } catch (Exception e) {
            LogUtils.e("截图失败: " + e.getMessage());
            screenshotMessage.postValue("截图失败: " + e.getMessage());
        }
    }

    //清理资源
    public void cleanup() {
        clearFrameData();
    }

    private void clearFrameData() {
        latestYUVData = null;
        latestWidth = 0;
        latestHeight = 0;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cleanup();
        LogUtils.i("ScreenshotViewModel 已清理");
    }
}
