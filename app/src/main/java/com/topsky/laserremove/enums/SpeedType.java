package com.topsky.laserremove.enums;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import androidx.annotation.IntDef;

import static java.lang.annotation.RetentionPolicy.SOURCE;

@Retention(SOURCE)
@IntDef(value = { SpeedType.SLOW,
        SpeedType.MIDDLE,
        SpeedType.FAST })
@Target({ ElementType.PARAMETER, ElementType.FIELD, ElementType.LOCAL_VARIABLE })
public @interface SpeedType {
    int SLOW = 0;
    int MIDDLE = 1;
    int FAST = 2;
}