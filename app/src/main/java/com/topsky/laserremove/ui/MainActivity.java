package com.topsky.laserremove.ui;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PathUtils;
import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallbackProxy;
import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.permission.PermissionLists;
import com.skydroid.fpvplayer.PlayerType;
import com.skydroid.fpvplayer.RtspTransport;
import com.skydroid.fpvplayer.VideoDecoderCallBack;
import com.topsky.laserremove.R;
import com.topsky.laserremove.base.BaseActivity;
import com.topsky.laserremove.constant.CommonData;
import com.topsky.laserremove.databinding.ActivityMainBinding;
import com.topsky.laserremove.manager.MP4RecordHelper;
import com.topsky.laserremove.net.CameraZoomApi;
import com.topsky.laserremove.net.HttpData;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;

public class MainActivity extends BaseActivity<ActivityMainBinding> implements View.OnClickListener, VideoDecoderCallBack, MP4RecordHelper.MP4RecordCallBack {

    @Override
    protected ActivityMainBinding initViewBinding(LayoutInflater inflater) {
        return ActivityMainBinding.inflate(inflater);
    }

    @Override
    protected void init() {
        initFPV();
        initView();
        initRecord();
    }

    private void initView() {
        binding.btnRecord.setOnClickListener(this);
        binding.btnScreenshot.setOnClickListener(this);
        initTouchCustomViewCamera();
    }

    private void initFPV() {
        //硬解码
        binding.fpvWidget.setUsingMediaCodec(true);
        //设置播放地址
        binding.fpvWidget.setUrl(CommonData.RTSP_URL);
        //使用云卓播放器
        binding.fpvWidget.setPlayerType(PlayerType.ONLY_SKY);
        //RTSP流TCP/UDP连接
        binding.fpvWidget.setRtspTranstype(RtspTransport.AUTO);
        //配置遥控器型号-可用于播放器根据遥控器型号选择比较合适的参数
        binding.fpvWidget.setRcType(CommonData.G20_TYPE);

        binding.fpvWidget.setVideoDecoderCallBack(this);

        binding.fpvWidget.start();

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_record) {
            //录屏
            recordOpt();
        } else if (v.getId() == R.id.btn_screenshot) {
            //截图
            takeScreenshot();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.fpvWidget.stop();
        if (isStartRecord && mp4RecordHelper != null) {
            mp4RecordHelper.stop(this);
        }
    }

    //region 录像 截图
    private MP4RecordHelper mp4RecordHelper;
    private boolean isStartRecord = false;

    //截图
    private ByteBuffer latestYUVData;
    private int latestWidth;
    private int latestHeight;
    private int latestPixelFormat;
    private boolean hasValidFrame = false;

    private void initRecord() {
        mp4RecordHelper = new MP4RecordHelper();
    }

    private void recordOpt() {
        if (isStartRecord) {
            stopRecord();
        } else {
            if (XXPermissions.isGrantedPermission(this, PermissionLists.getManageExternalStoragePermission())) {
                startRecord();
            } else {
                XXPermissions.with(this)
                        .permission(PermissionLists.getManageExternalStoragePermission())
                        .request((grantedList, deniedList) -> {
                        });
            }
        }
    }

    private void startRecord() {
        if (mp4RecordHelper == null) {
            return;
        }
        if (!hasValidFrame || latestYUVData == null) {
            Toast.makeText(this, "暂无视频帧数据，请确保RTSP流已启动", Toast.LENGTH_SHORT).show();
            LogUtils.e("录像失败：没有可用的视频帧数据");
            return;
        }
        mp4RecordHelper.setBitrate(1500000);//比特率
        //这里最好不能超过30帧，编码的速度没那么快，可能会导致录制时间不符合预期
        mp4RecordHelper.setFrameRate(15);//输出帧率
        mp4RecordHelper.setVideoSize(1280, 720); //输出分辨率
        mp4RecordHelper.setUseSoftEncoder(true);//true:软编码（X264），false:硬编码
        String path = PathUtils.getExternalDownloadsPath() + "/laser_" + System.currentTimeMillis() + ".mp4";
        Exception ret = mp4RecordHelper.start(path);
        if (ret == null) {
            isStartRecord = true;
            switchRecordText();
        } else {
            LogUtils.e("录像失败：" + ret);
        }
    }

    private void stopRecord() {
        if (mp4RecordHelper != null) {
            mp4RecordHelper.stop(this);
        }
    }

    @Override
    public void onYUV(@NonNull ByteBuffer data, int width, int height, int pixel_format) {
        if (mp4RecordHelper != null) {
            mp4RecordHelper.putYUV(data, width, height, pixel_format);
        }

        latestYUVData = ByteBuffer.allocateDirect(data.remaining());
        latestYUVData.put(data.duplicate());
        latestWidth = width;
        latestHeight = height;
        latestPixelFormat = pixel_format;
        hasValidFrame = true;
    }

    @Override
    public void onRecordComplete(Exception e) {
        if (e != null) {
            LogUtils.e("停止录像失败：" + e);
        }
        isStartRecord = false;
        switchRecordText();
    }

    private void switchRecordText() {
        if (isStartRecord) {
            binding.btnRecord.setText("停止录像");
        } else {
            binding.btnRecord.setText("开始录像");
        }
    }

    private void takeScreenshot() {
        if (!hasValidFrame || latestYUVData == null) {
            Toast.makeText(this, "暂无视频帧数据，请确保RTSP流已启动", Toast.LENGTH_SHORT).show();
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

                    runOnUiThread(() -> {
                        Toast.makeText(this, "截图已保存: " + file.getName(), Toast.LENGTH_LONG).show();
                        LogUtils.i("截图成功: " + screenshotPath);
                    });

                } catch (Exception e) {
                    LogUtils.e("截图保存失败: " + e.getMessage());
                    runOnUiThread(() -> {
                        Toast.makeText(this, "截图保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            }).start();

        } catch (Exception e) {
            LogUtils.e("截图失败: " + e.getMessage());
        }
    }
    //endregion

    //region 云台相机操作
    @SuppressLint("ClickableViewAccessibility")
    private void initTouchCustomViewCamera() {
        binding.btnZoomBig.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        changeCameraZoom(9);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        changeCameraZoom(0);
                        break;
                }
                return false;
            }
        });

        binding.btnZoomSmall.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        changeCameraZoom(10);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        changeCameraZoom(0);
                        break;
                }
                return false;
            }
        });
    }

    private void changeCameraZoom(int type) {
        EasyHttp.post(this)
                .api(new CameraZoomApi(type))
                .request(new HttpCallbackProxy<HttpData<CameraZoomApi.Bean>>(MainActivity.this) {

                    @Override
                    public void onHttpSuccess(@NonNull HttpData<CameraZoomApi.Bean> result) {

                    }
                });
    }
    //endregion
}