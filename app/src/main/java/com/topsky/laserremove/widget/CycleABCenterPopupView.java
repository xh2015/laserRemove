package com.topsky.laserremove.widget;

import android.content.Context;
import android.view.View;

import com.blankj.utilcode.util.ScreenUtils;
import com.lxj.xpopup.core.CenterPopupView;
import com.topsky.laserremove.R;

import androidx.annotation.NonNull;

public class CycleABCenterPopupView extends CenterPopupView {

    public CycleABCenterPopupView(@NonNull Context context) {
        super(context);
    }

    @Override
    protected int getImplLayoutId() {
        return R.layout.pop_recycle;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        findViewById(R.id.btnStop).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnStopListener != null) {
                    mOnStopListener.onStopCycle();
                }
            }
        });
    }

    @Override
    protected int getPopupHeight() {
        return ScreenUtils.getAppScreenHeight() * 3 / 4;
    }

    @Override
    protected int getPopupWidth() {
        return ScreenUtils.getAppScreenWidth() * 3 / 4;
    }

    private OnStopListener mOnStopListener;

    public void setOnStopListener(OnStopListener listener) {
        mOnStopListener = listener;
    }

    public interface OnStopListener {
        void onStopCycle();
    }
}
