package com.topsky.laserremove.enums;

import java.lang.annotation.Retention;

import androidx.annotation.IntDef;

import static java.lang.annotation.RetentionPolicy.SOURCE;

@Retention(SOURCE)
@IntDef({ SpeedType.SLOW,
        SpeedType.MIDDLE,
        SpeedType.FAST })
public @interface SpeedType {
    int SLOW = 0;
    int MIDDLE = 1;
    int FAST = 2;
}
