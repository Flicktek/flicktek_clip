package com.flicktek.android.ArsEvents;


public class GestureEvent {
	public  Integer status;
	public  Integer quality;

	public GestureEvent(int value) {
		this.status = value;
	}
	public GestureEvent(int value, int quality) {

		this.status = value;
		this.quality = quality;
	}
}
