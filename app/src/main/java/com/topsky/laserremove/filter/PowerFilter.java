package com.topsky.laserremove.filter;

import android.text.InputFilter;
import android.text.Spanned;

public class PowerFilter implements InputFilter {

    private int minValue = 10;
    private int maxValue = 100;

    public PowerFilter(int minValue, int maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {
        try {
            String replacement = source.toString();
            String existing = dest.toString();
            String newText = existing.substring(0, dstart) + replacement + existing.substring(dend);

            if (newText.isEmpty()) {
                return null;
            }

            int value = Integer.parseInt(newText);

            if (value > maxValue) {
                return "";
            }

            if (newText.length() == 3 && value < minValue) {
                return "";
            }
        } catch (NumberFormatException e) {
            return "";
        }
        return null;
    }
}

