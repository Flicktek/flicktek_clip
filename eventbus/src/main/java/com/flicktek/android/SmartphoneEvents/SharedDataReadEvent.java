package com.flicktek.android.SmartphoneEvents;


import com.google.android.gms.wearable.DataMap;

public class SharedDataReadEvent {
	public final String path;
	public final DataMap dataMap;

	public SharedDataReadEvent(String path, DataMap dataMap) {
		this.path = path;
		this.dataMap = dataMap;
	}
}
