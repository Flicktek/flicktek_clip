package com.flicktek.clip;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class FlicktekSettings {
    private final String TAG = "FlicktekSettings";

    // Singleton
    private static FlicktekSettings mInstance = null;
    private SharedPreferences mPreferences;

    public static FlicktekSettings getInstance() {
        if (mInstance == null)
            mInstance = new FlicktekSettings();
        return mInstance;
    }

    public void setPreferences(Activity activity) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
    }
}
