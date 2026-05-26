package com.topsky.laserremove.ui;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.permission.PermissionLists;
import com.skydroid.fpvplayer.PlayerType;
import com.skydroid.fpvplayer.RtspTransport;
import com.skydroid.fpvplayer.VideoDecoderCallBack;
import com.topsky.laserremove.R;
import com.topsky.laserremove.base.BaseActivity;
import com.topsky.laserremove.constant.CommonData;
import com.topsky.laserremove.databinding.ActivityMainBinding;
import com.topsky.laserremove.viewModel.CameraViewModel;
import com.topsky.laserremove.viewModel.RecordViewModel;
import com.topsky.laserremove.viewModel.ScreenshotViewModel;

import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

public class MainActivity extends BaseActivity<ActivityMainBinding> implements View.OnClickListener, VideoDecoderCallBack {

    private RecordViewModel recordViewModel;
    private ScreenshotViewModel screenshotViewModel;
    private CameraViewModel cameraViewModel;

    @Override
    protected ActivityMainBinding initViewBinding(LayoutInflater inflater) {
        return ActivityMainBinding.inflate(inflater);
    }

    @Override
    protected void init() {
        initViewModel();
        initFPV();
        initView();
    }

    //region 初始化
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
            handleRecordClick();
        } else if (v.getId() == R.id.btn_screenshot) {
            checkPermissionAndCaptureImage();
        }
    }
    //endregion

    //region ViewModel
    //每个 ViewModel 都通过 ViewModelProvider 获取，确保受 Lifecycle 管理
    private void initViewModel() {
        recordViewModel = new ViewModelProvider(this).get(RecordViewModel.class);
        screenshotViewModel = new ViewModelProvider(this).get(ScreenshotViewModel.class);
        cameraViewModel = new ViewModelProvider(this).get(CameraViewModel.class);

        cameraViewModel.setLifecycleOwner(this);
        setupObservers();
    }

    //设置 LiveData 观察者
    private void setupObservers() {
        //录像 拍照
        setupCaptureVideoImageObservers();
    }
    //endregion

    //region 录像 拍照
    //视频帧数据回调
    //同时分发给录像和截图 ViewModel
    @Override
    public void onYUV(@NonNull ByteBuffer data, int width, int height, int pixel_format) {
        recordViewModel.updateFrameData(data, width, height, pixel_format);
        screenshotViewModel.updateFrameData(data, width, height, pixel_format);
    }

    //录像
    private void handleRecordClick() {
        if (Boolean.TRUE.equals(recordViewModel.getIsRecording().getValue())) {
            recordViewModel.stopRecord();
        } else {
            checkPermissionAndRecord();
        }
    }

    private void checkPermissionAndRecord() {
        if (XXPermissions.isGrantedPermission(this, PermissionLists.getManageExternalStoragePermission())) {
            recordViewModel.startRecord();
        } else {
            XXPermissions.with(this)
                    .permission(PermissionLists.getManageExternalStoragePermission())
                    .request((grantedList, deniedList) -> {
                        if (deniedList.isEmpty()) {
                            recordViewModel.startRecord();
                        }
                    });
        }
    }

    //拍照
    private void checkPermissionAndCaptureImage() {
        if (XXPermissions.isGrantedPermission(this, PermissionLists.getManageExternalStoragePermission())) {
            screenshotViewModel.takeScreenshot();
        } else {
            XXPermissions.with(this)
                    .permission(PermissionLists.getManageExternalStoragePermission())
                    .request((grantedList, deniedList) -> {
                        if (deniedList.isEmpty()) {
                            screenshotViewModel.takeScreenshot();
                        }
                    });
        }
    }

    private void setupCaptureVideoImageObservers() {
        // 观察录像状态
        recordViewModel.getIsRecording().observe(this, isRecording -> {
            binding.btnRecord.setSelected(isRecording);
            //binding.llTopStateRecord.setVisibility(isRecording ? View.VISIBLE : View.GONE);
        });

        // 观察录像错误消息
        recordViewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });

        // 观察录像时长
        recordViewModel.getRecordDuration().observe(this, duration -> {
            String formattedTime = RecordViewModel.formatDuration(duration);
            binding.tvRecordTime.setText(formattedTime);
        });

        // 观察截图消息
        screenshotViewModel.getScreenshotMessage().observe(this, message -> {
            if (!TextUtils.isEmpty(message)) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        // 观察截图错误消息
        screenshotViewModel.getErrorMessage().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    //endregion

    //region 相机变倍
    @SuppressLint("ClickableViewAccessibility")
    private void initTouchCustomViewCamera() {
        binding.btnZoomBig.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        cameraViewModel.startZoomIn();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        cameraViewModel.stopZoom();
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
                        cameraViewModel.startZoomOut();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        cameraViewModel.stopZoom();
                        break;
                }
                return false;
            }
        });
    }
    //endregion

    //region 释放资源
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.fpvWidget.stop();
    }
    //endregion
}