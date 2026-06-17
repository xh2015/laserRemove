package com.topsky.laserremove.ui;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.blankj.utilcode.util.ColorUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.ScreenUtils;
import com.blankj.utilcode.util.SizeUtils;
import com.hjq.permissions.XXPermissions;
import com.hjq.permissions.permission.PermissionLists;
import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.interfaces.OnInputConfirmListener;
import com.skydroid.fpvplayer.PlayerType;
import com.skydroid.fpvplayer.RtspTransport;
import com.skydroid.fpvplayer.VideoDecoderCallBack;
import com.topsky.laserremove.R;
import com.topsky.laserremove.base.BaseActivity;
import com.topsky.laserremove.constant.CommonData;
import com.topsky.laserremove.databinding.ActivityMainBinding;
import com.topsky.laserremove.enums.SpeedType;
import com.topsky.laserremove.filter.TyPopCallBack;
import com.topsky.laserremove.manager.DataJsonManager;
import com.topsky.laserremove.viewModel.CameraViewModel;
import com.topsky.laserremove.viewModel.CloudPlatformViewModel;
import com.topsky.laserremove.viewModel.LaserControlViewModel;
import com.topsky.laserremove.viewModel.LaserFocusViewModel;
import com.topsky.laserremove.viewModel.NettyViewModel;
import com.topsky.laserremove.viewModel.RecordViewModel;
import com.topsky.laserremove.viewModel.ScreenshotViewModel;
import com.topsky.laserremove.widget.DistanceInputConfirmPopupView;
import com.topsky.laserremove.widget.PanelView;

import java.nio.ByteBuffer;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModelProvider;

public class MainActivity extends BaseActivity<ActivityMainBinding> implements View.OnClickListener, VideoDecoderCallBack, PanelView.PanelListener {
    private RecordViewModel recordViewModel;
    private ScreenshotViewModel screenshotViewModel;
    private CameraViewModel cameraViewModel;
    private NettyViewModel nettyViewModel;
    private CloudPlatformViewModel cloudPlatformViewModel;
    private LaserControlViewModel laserControlViewModel;
    private LaserFocusViewModel laserFocusViewModel;

    @Override
    protected ActivityMainBinding initViewBinding(LayoutInflater inflater) {
        return ActivityMainBinding.inflate(inflater);
    }

    @Override
    protected void init() {
        initViewModel();
        initFPV();
        initView();
        initJsonData();
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
        binding.btnPulseSetting.setOnClickListener(this);
        binding.ivLaserSwitch.setOnClickListener(this);

        initTouchCustomViewCamera();
        initCrosshair();
        initSpeedView();
        initLaserFocus();
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
        } else if (v.getId() == R.id.btn_slow) {
            changeSpeedType(SpeedType.SLOW);
        } else if (v.getId() == R.id.btn_middle) {
            changeSpeedType(SpeedType.MIDDLE);
        } else if (v.getId() == R.id.btn_fast) {
            changeSpeedType(SpeedType.FAST);
        } else if (v.getId() == R.id.tv_power_percent) {
            settingPowerPercent();
        } else if (v.getId() == R.id.tv_distance) {
            settingLaserDistance();
        } else if (v.getId() == R.id.btn_pulse_setting) {
            settingLaserPulse();
        } else if (v.getId() == R.id.iv_laser_switch) {
            laserSwitch();
        } else if (v.getId() == R.id.btn_up) {
            updateCrossHair(0);
        } else if (v.getId() == R.id.btn_down) {
            updateCrossHair(1);
        } else if (v.getId() == R.id.btn_left) {
            updateCrossHair(2);
        } else if (v.getId() == R.id.btn_right) {
            updateCrossHair(3);
        } else if (v.getId() == R.id.btn_cancel) {
            defaultCrosshairCoordinate(currentCrosshairX, currentCrosshairY);
        } else if (v.getId() == R.id.btn_confirm) {
            setCrossHairCoordinate(tempCrosshairX, tempCrosshairY);
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
        laserFocusViewModel = new ViewModelProvider(this).get(LaserFocusViewModel.class);
        cameraViewModel.setLifecycleOwner(this);
        initNettyViewModel();
        setupObservers();
    }

    //设置 LiveData 观察者
    private void setupObservers() {
        //录像 拍照
        setupCaptureVideoImageObservers();
        //相机
        setupCameraObservers();
        //云台
        setupCloudPlatformObservers();
        //激光器
        setupLaserControlObservers();
        //激光焦距
        setupLaserFocusObservers();
    }
    //endregion

    //region 录像 拍照
    //视频帧数据回调
    //同时分发给录像和截图 ViewModel
    @Override
    public void onYUV(@NonNull ByteBuffer data, int width, int height, int pixel_format) {
        if (recordViewModel != null) {
            recordViewModel.updateFrameData(data, width, height, pixel_format);
        }
        if (screenshotViewModel != null) {
            screenshotViewModel.updateFrameData(data, width, height, pixel_format);
        }
    }

    //录像
    private void handleRecordClick() {
        if (recordViewModel != null && Boolean.TRUE.equals(recordViewModel.getIsRecording().getValue())) {
            recordViewModel.stopRecord();
        } else {
            checkPermissionAndRecord();
        }
    }

    private void checkPermissionAndRecord() {
        if (XXPermissions.isGrantedPermission(this, PermissionLists.getManageExternalStoragePermission())) {
            if (recordViewModel != null) {
                recordViewModel.startRecord();
            }
        } else {
            XXPermissions.with(this)
                    .permission(PermissionLists.getManageExternalStoragePermission())
                    .request((grantedList, deniedList) -> {
                        if (deniedList.isEmpty()) {
                            if (recordViewModel != null) {
                                recordViewModel.startRecord();
                            }
                        }
                    });
        }
    }

    //拍照
    private void checkPermissionAndCaptureImage() {
        if (XXPermissions.isGrantedPermission(this, PermissionLists.getManageExternalStoragePermission())) {
            if (screenshotViewModel != null) {
                screenshotViewModel.takeScreenshot();
            }
        } else {
            XXPermissions.with(this)
                    .permission(PermissionLists.getManageExternalStoragePermission())
                    .request((grantedList, deniedList) -> {
                        if (deniedList.isEmpty()) {
                            if (screenshotViewModel != null) {
                                screenshotViewModel.takeScreenshot();
                            }
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

        // 观察录像成功消息
        recordViewModel.getRecordPathMessage().observe(this, path -> {
            if (!TextUtils.isEmpty(path)) {
                Toast.makeText(this, getString(R.string.ty_record_success_path) + path, Toast.LENGTH_SHORT).show();
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
    private void setupCameraObservers() {
        cameraViewModel.getCameraZoom().observe(this, zoom -> {
            binding.tvLightTimes.setText(zoom);
        });
    }

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

        cameraViewModel.getCameraInfo();
    }
    //endregion

    //region 云台
    //标定A
    private void markPoint(boolean pointA) {
    }

    private void moveToPoint(boolean toA) {
    }

    //云台方向控制
    @Override
    public void onStatesChange(int direction, boolean isPress) {
        cloudPlatformViewModel.controlCpDirection(direction, isPress);
    }

    private void setupCloudPlatformObservers() {
        //观察云平台
        cloudPlatformViewModel.getConnected().observe(this, connected -> {
            binding.tvCloudConnectState.setText(connected ? R.string.ty_connected : R.string.ty_unconnect);
            binding.tvCloudConnectState.setTextColor(ColorUtils.getColor(connected ? R.color.color_connected : R.color.color_unconnect));
        });

        cloudPlatformViewModel.getVoltage().observe(this, voltage -> {
            binding.tvVoltage.setText(String.format(getString(R.string.ty_voltage), voltage));
        });
    }
    //endregion

    //region 激光器控制
    private boolean laserOn = false;

    private void laserSwitch() {
        laserOn = !laserOn;
        binding.ivLaserSwitch.setSelected(laserOn);
        // 通过Netty发送激光开关指令
        laserControlViewModel.sendLaserSwitch(laserOn);
    }

    private void setupLaserControlObservers() {
        //激光功率
        laserControlViewModel.getLaserPower().observe(this, power -> {
            binding.tvPowerPercent.setText(power + "%");
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
                // 通过Netty发送功率设置指令
                laserControlViewModel.sendLaserPower(value);
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, R.string.ty_power_input_error_tip, Toast.LENGTH_SHORT).show();
            }
        });
    }
    //endregion

    //region 准心设置
    private int stepPx = 5;
    private int screenWidth;
    private int screenHeight;
    private int currentCrosshairX;
    private int currentCrosshairY;
    private int tempCrosshairX;
    private int tempCrosshairY;
    private int crosshairSize;

    private void initCrosshair() {
        binding.btnCrosshairVisible.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                binding.ivCrosshair.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        binding.btnCrosshairVisible.setChecked(true);

        binding.btnCrosshairSetting.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(@NonNull CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    defaultCrosshairCoordinate(currentCrosshairX, currentCrosshairY);
                } else {
                    binding.btnCrosshairVisible.setChecked(true);
                }
                binding.btnUp.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                binding.btnDown.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                binding.btnLeft.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                binding.btnRight.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                binding.btnCancel.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                binding.btnConfirm.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        binding.btnUp.setOnClickListener(this);
        binding.btnDown.setOnClickListener(this);
        binding.btnLeft.setOnClickListener(this);
        binding.btnRight.setOnClickListener(this);
        binding.btnCancel.setOnClickListener(this);
        binding.btnConfirm.setOnClickListener(this);

        screenWidth = ScreenUtils.getScreenWidth();
        screenHeight = ScreenUtils.getScreenHeight();

        binding.ivCrosshair.post(() -> {
            crosshairSize = binding.ivCrosshair.getWidth();
            currentCrosshairX = (screenWidth - crosshairSize) / 2;
            currentCrosshairY = (screenHeight - crosshairSize) / 2;
            defaultCrosshairCoordinate(currentCrosshairX, currentCrosshairY);
        });
    }

    //0 上 1 下 2 左 3 右
    private void updateCrossHair(int direction) {
        if (direction == 0) {
            tempCrosshairY -= stepPx;
        } else if (direction == 1) {
            tempCrosshairY += stepPx;
        } else if (direction == 2) {
            tempCrosshairX -= stepPx;
        } else if (direction == 3) {
            tempCrosshairX += stepPx;
        }

        updateCrossHairCoordinate(tempCrosshairX, tempCrosshairY);
    }

    private void defaultCrosshairCoordinate(int currentCrosshairX, int currentCrosshairY) {
        updateCrossHairCoordinate(currentCrosshairX, currentCrosshairY);
    }

    private void updateCrossHairCoordinate(int x, int y) {
        if (x < 0 || x > screenWidth - crosshairSize) {
            return;
        }
        if (y < 0 || y > screenHeight - crosshairSize) {
            return;
        }

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) binding.ivCrosshair.getLayoutParams();
        lp.leftMargin = x;
        lp.topMargin = y;
        tempCrosshairX = x;
        tempCrosshairY = y;
        binding.ivCrosshair.setLayoutParams(lp);
        changeCrosshairCoordinateTxt(x, y);
    }

    private void changeCrosshairCoordinateTxt(int x, int y) {
        binding.tvCenterXy.setText(String.format(Locale.CHINA, getString(R.string.ty_crosshair_coordinate), x, y));
    }

    private void setCrossHairCoordinate(int x, int y) {
        currentCrosshairX = x;
        currentCrosshairY = y;
    }
    //endregion

    //region 云台速度
    //速度模式
    private int speedType = SpeedType.SLOW;

    private void initSpeedView() {
        binding.btnSlow.setOnClickListener(this);
        binding.btnMiddle.setOnClickListener(this);
        binding.btnFast.setOnClickListener(this);

        binding.btnSlow.setChecked(true);
    }

    private void changeSpeedType(@SpeedType int type) {
        speedType = type;
        LogUtils.d("切换速度模式: " + type);
    }
    //endregion

    //region tcp server相关
    private void initNettyViewModel() {
        if (XXPermissions.isGrantedPermission(this, PermissionLists.getPostNotificationsPermission())) {
            settingNettyViewModel();
        } else {
            XXPermissions.with(this)
                    .permission(PermissionLists.getPostNotificationsPermission())
                    .request((grantedList, deniedList) -> {
                        if (deniedList.isEmpty()) {
                            settingNettyViewModel();
                        }
                    });
        }
    }

    private void settingNettyViewModel() {
        nettyViewModel = new ViewModelProvider(this).get(NettyViewModel.class);//Netty连接状态
        setupNettyObservers();
        // 启动Netty服务并设置消息分发
        nettyViewModel.startAndBindService();
        nettyViewModel.dispatchMessage(cloudPlatformViewModel, laserControlViewModel, laserFocusViewModel);
    }

    private void setupNettyObservers() {
        //连接状态
        nettyViewModel.getConnected().observe(this, connected -> {
            binding.tvLaserConnectState.setText(connected ? R.string.ty_connected : R.string.ty_unconnect);
            binding.tvLaserConnectState.setTextColor(ColorUtils.getColor(connected ? R.color.color_connected : R.color.color_unconnect));
        });
    }
    //endregion

    //region 获取距离和脉冲之间关系的配置json
    private void initJsonData() {
        XXPermissions.with(this)
                .permission(PermissionLists.getManageExternalStoragePermission())
                .request((grantedList, deniedList) -> {
                    if (deniedList.isEmpty()) {
                        DataJsonManager.getInstance().getJsonFromLocal();
                    }
                });
    }
    //endregion

    //region 激光焦距 调焦
    private DistanceInputConfirmPopupView distanceInputConfirmPopupView;
    private Handler focusHandler;
    private Runnable focusRunnable;
    private boolean isFocusing;
    private boolean focusNear;

    private void initLaserFocus() {
        focusHandler = new Handler(Looper.getMainLooper());
        focusRunnable = new Runnable() {
            @Override
            public void run() {
                if (isFocusing) {
                    changeLaserFocus(focusNear);
                    focusHandler.postDelayed(this, 500);
                }
            }
        };

        binding.btnFocusNear.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startFocusRepeat(true);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        stopFocusRepeat();
                        break;
                }
                return false;
            }
        });

        binding.btnFocusFar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        startFocusRepeat(false);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        stopFocusRepeat();
                        break;
                }
                return false;
            }
        });
    }

    private void startFocusRepeat(boolean near) {
        stopFocusRepeat();

        isFocusing = true;
        focusNear = near;

        changeLaserFocus(near);
        focusHandler.postDelayed(focusRunnable, 500);
    }

    private void stopFocusRepeat() {
        isFocusing = false;
        if (focusHandler != null && focusRunnable != null) {
            focusHandler.removeCallbacks(focusRunnable);
        }
    }

    private void changeLaserFocus(boolean near) {
        if (laserFocusViewModel != null) {
            laserFocusViewModel.changeLaserFocus(near);
        }
    }

    private void setupLaserFocusObservers() {
        //激光距离
        laserFocusViewModel.getLaserDistance().observe(this, distance -> {
            binding.tvDistance.setText(String.format(getString(R.string.ty_distance_format), distance));
            if (distanceInputConfirmPopupView != null && distanceInputConfirmPopupView.isShow()) {
                distanceInputConfirmPopupView.setMyContent(String.valueOf(distance));
            }
        });
    }

    //设置距离
    private void settingLaserDistance() {
        showDistancePop(10, 9999, 4, getString(R.string.ty_distance_setting_title), R.string.ty_distance_input_tip, input -> {
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
                laserFocusViewModel.sendLaserPulseByDistance(value);
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, R.string.ty_power_input_error_tip, Toast.LENGTH_SHORT).show();
            }
        });
    }

    //设置脉冲
    private void settingLaserPulse() {
        showSettingPop(10, 9999, 4, getString(R.string.ty_pulse_setting), R.string.ty_distance_input_tip, input -> {
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
                // 通过Netty发送脉冲设置指令
                laserFocusViewModel.sendLaserPulse(value);
            } catch (NumberFormatException e) {
                Toast.makeText(MainActivity.this, R.string.ty_power_input_error_tip, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showSettingPop(int minValue, int maxValue, int maxLength, String title, @StringRes int inputTip, OnInputConfirmListener confirmListener) {
        new XPopup.Builder(this)
                .hasStatusBar(false)
                .hasNavigationBar(false)
                .popupWidth(SizeUtils.dp2px(400))
                .setPopupCallback(new TyPopCallBack(minValue, maxValue, maxLength, inputTip))
                .asInputConfirm(title, "", confirmListener)
                .show();
    }

    private void showDistancePop(int minValue, int maxValue, int maxLength, String title, @StringRes int inputTip, OnInputConfirmListener confirmListener) {
        distanceInputConfirmPopupView = new DistanceInputConfirmPopupView(this, title, "", getString(inputTip), "10", confirmListener, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                laserFocusViewModel.getLaserDistanceByServer();
            }
        });
        new XPopup.Builder(this)
                .hasStatusBar(false)
                .hasNavigationBar(false)
                .popupWidth(SizeUtils.dp2px(400))
                .setPopupCallback(new TyPopCallBack(minValue, maxValue, maxLength, inputTip))
                .asCustom(distanceInputConfirmPopupView)
                .show();
    }
    //endregion

    //region 释放资源
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding.fpvWidget.stop();

        // 解绑Netty服务
        if (nettyViewModel != null) {
            nettyViewModel.unbindService();
        }
    }
    //endregion
}