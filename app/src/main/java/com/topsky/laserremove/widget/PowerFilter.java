package com.topsky.laserremove.widget;

import android.text.InputFilter;
import android.text.Spanned;

public class PowerFilter implements InputFilter {

    private static final int MIN_VALUE = 10;
    private static final int MAX_VALUE = 100;

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

            if (value > MAX_VALUE) {
                return "";
            }

            if (newText.length() == 3 && value < MIN_VALUE) {
                return "";
            }
        } catch (NumberFormatException e) {
            return "";
        }
        return null;
    }
}

