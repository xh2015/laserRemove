package com.topsky.laserremove.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.widget.EditText;

import com.lxj.xpopup.XPopup;
import com.lxj.xpopup.impl.InputConfirmPopupView;
import com.lxj.xpopup.interfaces.OnInputConfirmListener;
import com.lxj.xpopup.util.XPopupUtils;
import com.topsky.laserremove.R;

import androidx.annotation.NonNull;

public class DistanceInputConfirmPopupView extends InputConfirmPopupView {
    private String content;
    private OnClickListener clickListener;

    public DistanceInputConfirmPopupView(@NonNull Context context, String title, String desc, String hint, String content, OnInputConfirmListener listener, OnClickListener clickListener) {
        super(context, R.layout.pop_distance_input);
        this.content = content;
        setTitleContent(title, desc, hint);
        inputContent = content;
        setListener(listener, null);
        this.clickListener = clickListener;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        EditText et_input = getEditText();
        et_input.post(() -> {
            if (et_input.getMeasuredWidth() > 0) {
                BitmapDrawable defaultDrawable = XPopupUtils.createBitmapDrawable(this.getContext(), et_input.getMeasuredWidth(), Color.parseColor("#888888"));
                BitmapDrawable focusDrawable = XPopupUtils.createBitmapDrawable(this.getContext(), et_input.getMeasuredWidth(), XPopup.getPrimaryColor());
                et_input.setBackgroundDrawable(XPopupUtils.createSelector(defaultDrawable, focusDrawable));
            }
        });
        findViewById(R.id.btn_distance).setOnClickListener(clickListener);
    }

    public void setMyContent(String content) {
        this.content = content;
        getEditText().setText(content);
    }
}
