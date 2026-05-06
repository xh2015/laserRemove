package com.topsky.laserremove.ui;

import android.view.LayoutInflater;

import com.topsky.laserremove.base.BaseActivity;
import com.topsky.laserremove.databinding.ActivityMainBinding;

public class MainActivity extends BaseActivity<ActivityMainBinding> {

    @Override
    protected ActivityMainBinding initViewBinding(LayoutInflater inflater) {
        return ActivityMainBinding.inflate(inflater);
    }

    @Override
    protected void init() {

    }
}