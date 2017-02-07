package com.flicktek.android.CasEvents;


public class GestureStatusEvent {
    public Integer status;
    public Integer value;
    public Integer unit;
    public Integer decimal;

    public GestureStatusEvent(int status) {
        this.value = status;
        this.unit = status % 10;
        this.status = this.unit;
        status = status / 10;
        this.decimal = status % 10;
    }

}






