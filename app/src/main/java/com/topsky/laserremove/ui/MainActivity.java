package com.topsky.laserremove.ui;

import android.view.LayoutInflater;

import com.skydroid.fpvplayer.PlayerType;
import com.skydroid.fpvplayer.RtspTransport;
import com.topsky.laserremove.base.BaseActivity;
import com.topsky.laserremove.constant.CommonData;
import com.topsky.laserremove.databinding.ActivityMainBinding;

public class MainActivity extends BaseActivity<ActivityMainBinding> {

    @Override
    protected ActivityMainBinding initViewBinding(LayoutInflater inflater) {
        return ActivityMainBinding.inflate(inflater);
    }

    @Override
    protected void init() {
        initFPV();
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

        //binding.fpvWidget.start();

    }
}