package com.topsky.laserremove.ui;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.blankj.utilcode.util.ColorUtils;
import com.blankj.utilcode.util.SizeUtils;
import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.permission.PermissionLists;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.impl.InputConfirmPopupView;
import com.lxj.xpopup.interfaces.OnInputConfirmListener;
import com.skydroid.fpvplayer.PlayerType;
import com.skydroid.fpvplayer.RtspTransport;
import com.skydroid.fpvplayer.VideoDecoderCallBack;
import com.topsky.laserremove.R;
import com.topsky.laserremove.base.BaseActivity;
import com.topsky.laserremove.constant.CommonData;
import com.topsky.laserremove.databinding.ActivityMainBinding;
import com.topsky.laserremove.filter.TyPopCallBack;
import com.topsky.laserremove.viewModel.CameraViewModel;
import com.topsky.laserremove.viewModel.CloudPlatformViewModel;
import com.topsky.laserremove.viewModel.LaserControlViewModel;
import com.topsky.laserremove.viewModel.RecordViewModel;
import com.topsky.laserremove.viewModel.ScreenshotViewModel;
import com.topsky.laserremove.widget.PanelView;

import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModelProvider;

public class MainActivity extends BaseActivity<ActivityMainBinding> implements View.OnClickListener, VideoDecoderCallBack, PanelView.PanelListener {

    private RecordViewModel recordViewModel;
    private ScreenshotViewModel screenshotViewModel;
    private CameraViewModel cameraViewModel;
    private CloudPlatformViewModel cloudPlatformViewModel;
    private LaserControlViewModel laserControlViewModel;

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

        binding.btnMarkA.setOnClickListener(this);
        binding.btnMarkB.setOnClickListener(this);
        binding.btnToA.setOnClickListener(this);
        binding.btnToB.setOnClickListener(this);
        binding.exMenu.setPanelListener(this);

        binding.tvPowerPercent.setOnClickListener(this);
        binding.tvDistance.setOnClickListener(this);

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
        } else if (v.getId() == R.id.btn_mark_a) {
            markPoint(true);
        } else if (v.getId() == R.id.btn_mark_b) {
            markPoint(false);
        } else if (v.getId() == R.id.btn_to_a) {
            moveToPoint(true);
        } else if (v.getId() == R.id.btn_to_b) {
            moveToPoint(false);
        } else if (v.getId() == R.id.tv_power_percent) {
            settingPowerPercent();
        } else if (v.getId() == R.id.tv_distance) {
            settingLaserDistance();
        }
    }
    //endregion

    //region ViewModel
    //每个 ViewModel 都通过 ViewModelProvider 获取，确保受 Lifecycle 管理
    private void initViewModel() {
        recordViewModel = new ViewModelProvider(this).get(RecordViewModel.class);
        screenshotViewModel = new ViewModelProvider(this).get(ScreenshotViewModel.class);
        cameraViewModel = new ViewModelProvider(this).get(CameraViewModel.class);
        cloudPlatformViewModel = new ViewModelProvider(this).get(CloudPlatformViewModel.class);
        laserControlViewModel = new ViewModelProvider(this).get(LaserControlViewModel.class);

        cameraViewModel.setLifecycleOwner(this);
        setupObservers();
    }

    //设置 LiveData 观察者
    private void setupObservers() {
        //录像 拍照
        setupCaptureVideoImageObservers();
        //云台
        setupCloudPlatformObservers();
        //激光器
        setupLaserControlObservers();
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
            binding.llTopStateRecord.setVisibility(isRecording ? View.VISIBLE : View.GONE);
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

    //region 云平台
    //标定A
    private void markPoint(boolean pointA) {
        cloudPlatformViewModel.setCpConnectStatus(pointA);
    }

    private void moveToPoint(boolean toA) {
        laserControlViewModel.setLaserConnectStatus(toA);
    }

    //云台方向控制
    @Override
    public void onStatesChange(int direction, boolean isPress) {
        cloudPlatformViewModel.controlCpDirection(direction, isPress);
    }

    private void setupCloudPlatformObservers() {
        //观察云平台连接状态
        cloudPlatformViewModel.getCpConnectStatus().observe(this, isConnected -> {
            binding.tvCloudConnectState.setText(isConnected ? R.string.ty_connected : R.string.ty_unconnect);
            binding.tvCloudConnectState.setTextColor(ColorUtils.getColor(isConnected ? R.color.color_connected : R.color.color_unconnect));
        });
    }
    //endregion

    // 激光器控制
    private void setupLaserControlObservers() {
        //观察云平台连接状态
        laserControlViewModel.getLaserConnectStatus().observe(this, isConnected -> {
            binding.tvLaserConnectState.setText(isConnected ? R.string.ty_connected : R.string.ty_unconnect);
            binding.tvLaserConnectState.setTextColor(ColorUtils.getColor(isConnected ? R.color.color_connected : R.color.color_unconnect));
        });
    }

    //设置激光功率
    private void settingPowerPercent() {
        showSettingPop(10, 100, 3, getString(R.string.ty_power_setting_title), R.string.ty_power_input_tip, input -> {
            if (input == null || input.isEmpty()) {
                Toast.makeText(MainActivity.this, R.string.ty_power_input_empty_tip, Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int value = Integer.parseInt(input);
                if (value < 10 || value > 100) {
                    Toast.makeText(MainActivity.this, R.string.ty_power_input_tip, Toast.LENGTH_SHORT).show();
                    return;
                }
                binding.tvPowerPercent.setText(input + "%");
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, R.string.ty_power_input_error_tip, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //设置
    private void settingLaserDistance() {
        showSettingPop(10, 9999, 4, getString(R.string.ty_distance_setting_title), R.string.ty_distance_input_tip, input -> {
            if (input == null || input.isEmpty()) {
                Toast.makeText(MainActivity.this, R.string.ty_power_input_empty_tip, Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                int value = Integer.parseInt(input);
                if (value < 10 || value > 9999) {
                    Toast.makeText(MainActivity.this, R.string.ty_distance_input_tip, Toast.LENGTH_SHORT).show();
                    return;
                }
                binding.tvDistance.setText(String.format(getString(R.string.ty_distance_format), input));
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, R.string.ty_power_input_error_tip, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSettingPop(int minValue, int maxValue, int maxLength, String title, @StringRes int inputTip, OnInputConfirmListener confirmListener) {
        InputConfirmPopupView inputConfirm = new XPopup.Builder(this)
                .hasStatusBar(false)
                .hasNavigationBar(false)
                .popupWidth(SizeUtils.dp2px(400))
                .setPopupCallback(new TyPopCallBack(minValue, maxValue, maxLength, inputTip))
                .asInputConfirm(title, "", confirmListener);

        inputConfirm.show();
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