package com.topsky.laserremove.filter;

import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;

import com.lxj.xpopup.core.BasePopupView;
import com.lxj.xpopup.impl.InputConfirmPopupView;
import com.lxj.xpopup.interfaces.SimpleCallback;

import androidx.annotation.StringRes;

public class TyPopCallBack extends SimpleCallback {
    private int minValue;
    private int maxValue;
    private int maxLength;
    @StringRes
    private int inputTip;

    public TyPopCallBack(int minValue, int maxValue, int maxLength, int inputTip) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.maxLength = maxLength;
        this.inputTip = inputTip;
    }

    @Override
    public void onCreated(BasePopupView popupView) {
        super.onCreated(popupView);
        if (popupView instanceof InputConfirmPopupView) {
            EditText editText = ((InputConfirmPopupView) popupView).getEditText();
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            editText.setHint(inputTip);
            editText.setFilters(new InputFilter[]{
                    new InputFilter.LengthFilter(maxLength),
                    new PowerFilter(minValue, maxValue)
            });
        }
    }
}
