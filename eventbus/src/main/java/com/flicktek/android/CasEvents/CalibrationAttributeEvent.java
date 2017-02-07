package com.flicktek.android.CasEvents;


public class CalibrationAttributeEvent {
    public Integer quality;
    public Integer unit;
    public Integer decimal;

    public CalibrationAttributeEvent(int quality) {
        this.quality=quality;
        this.unit = quality % 10;
        quality=quality/10;
        this.decimal = quality % 10;
    }
}
