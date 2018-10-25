package com.art4l.btsppscan.scanner;

import android.support.annotation.IntDef;
import android.support.annotation.StringDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@StringDef({
        ColorCode.RED,
        ColorCode.GREEN,
        ColorCode.AMBER,
        ColorCode.WHITE,

})
public @interface ColorCode {
    String RED = "RED";
    String GREEN = "GREEN";
    String AMBER = "AMBER";
    String WHITE = "WHITE";
}