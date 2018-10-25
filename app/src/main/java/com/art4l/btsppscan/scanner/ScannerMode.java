package com.art4l.btsppscan.scanner;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({
        ScannerMode.TILE_INPUT,
        ScannerMode.CONTINUOUS_INPUT,
        ScannerMode.UNKNOWN,
        ScannerMode.ERROR
})
public @interface ScannerMode {
    int UNKNOWN = 0;
    int TILE_INPUT = 1;
    int CONTINUOUS_INPUT = 2;
    int ERROR = 3;
}