package com.flicktek.clip;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Debug;
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

    // General diagnosis information on the about
    public static final String VERSION_BASE_OS = "version_base_os";
    public static final String VERSION_CODENAME = "version_codename";
    public static final String VERSION_PRODUCT = "version_product";
    public static final String VERSION_DEBUG = "version_debugging";

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

    public void saveSettingsInformation(Activity activity) {
        try {
            PackageInfo packageInfo = activity.getPackageManager().getPackageInfo(
                    activity.getPackageName(), 0);

            putString(FlicktekSettings.APPLICATION_VERSION, packageInfo.versionName);
            putInt(FlicktekSettings.APPLICATION_VERSION_CODE, packageInfo.versionCode);

            putString(FlicktekSettings.VERSION_BASE_OS, Build.VERSION.BASE_OS);
            putString(FlicktekSettings.VERSION_CODENAME, Build.VERSION.CODENAME);
            putString(FlicktekSettings.VERSION_PRODUCT, Build.PRODUCT);
            if (!Debug.isDebuggerConnected()) {
                putString(FlicktekSettings.VERSION_DEBUG, "Debugging");
            } else {
                putString(FlicktekSettings.VERSION_DEBUG, "Not debugging");
            }
        } catch (PackageManager.NameNotFoundException e) {
            //Handle exception
        }
    }
    
    public void setPreferencesActivity(Activity activity) {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        saveSettingsInformation(activity);
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
