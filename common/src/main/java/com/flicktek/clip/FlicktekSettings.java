package com.flicktek.clip;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

public class FlicktekSettings {
    private final String TAG = "FlicktekSettings";

    // Singleton
    private static FlicktekSettings mInstance = null;
    private SharedPreferences mPreferences;

    public static final String APPLICATION_VERSION = "application_version";
    public static final String APPLICATION_REVISION = "application_revision";
    public static final String APPLICATION_VERSION_CODE = "application_code";

    public static final String FIRMWARE_VERSION = "firmware_version";
    public static final String FIRMWARE_REVISION = "firmware_revision";

    // Device to connect automatically
    public static final String DEVICE_MAC_SELECTED = "mac_address_device";

    public boolean isDemo() {
        return false;
    }

    public static FlicktekSettings getInstance() {
        if (mInstance == null)
            mInstance = new FlicktekSettings();
        return mInstance;
    }

    public void setPreferences(Activity activity) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
    }

    public boolean putString(String key, String value) {
        if (mPreferences == null)
            return false;

        mPreferences.edit().putString(key, value).apply();
        return true;
    }

    public boolean putInt(String key, int value) {
        if (mPreferences == null)
            return false;

        mPreferences.edit().putInt(key, value).apply();
        return true;
    }

    @Nullable
    public String getString(String key, String default_value) {
        if (mPreferences==null)
            return default_value;

        return mPreferences.getString(key, default_value);
    }

    @Nullable
    public int getInt(String key, int default_value) {
        if (mPreferences==null)
            return default_value;

        return mPreferences.getInt(key, default_value);
    }
}
