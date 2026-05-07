package com.topsky.laserremove.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.PathUtils;
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

        //binding.fpvWidget.start();

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_record) {
            //录屏
            recordOpt();
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

    //region 录屏
    private MP4RecordHelper mp4RecordHelper;
    private boolean isStartRecord = false;

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
        mp4RecordHelper.setBitrate(1500000);//比特率
        //这里最好不能超过30帧，编码的速度没那么快，可能会导致录制时间不符合预期
        mp4RecordHelper.setFrameRate(15);//输出帧率
        mp4RecordHelper.setVideoSize(1280, 720); //输出分辨率
        mp4RecordHelper.setUseSoftEncoder(true);//true:软编码（X264），false:硬编码
        String path = PathUtils.getExternalDownloadsPath() + "/" + System.currentTimeMillis() + ".mp4";
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
//endregion
}