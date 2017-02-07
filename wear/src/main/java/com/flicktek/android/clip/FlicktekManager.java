package com.flicktek.android.clip;

import android.util.Log;

public class FlicktekManager {
    private static final String TAG = "FlickTek";

    public final static int DEBUG_DISABLED = 0;
    public final static int DEBUG_ENABLED  = 1;
    public final static int DEBUG_CRAZY    = 10;

    // Debug levels
    public static int mDebugLevel = DEBUG_DISABLED;

    public final static int GESTURE_ENTER = 1;
    public final static int GESTURE_HOME = 2;
    public final static int GESTURE_UP = 3;
    public final static int GESTURE_DOWN = 4;
    public final static int GESTURE_BACK = 5;

    public final static int STATUS_NONE = 1;
    public final static int STATUS_CONNECTING = 4;
    public final static int STATUS_CONNECTED = 5;
    public final static int STATUS_READY = 6;
    public final static int STATUS_DISCONNECTED = 7;
    public final static int STATUS_DISCONNECTING = 8;

    //-------------- CONNECTION -----------------------

    private static boolean mIsConnected = false;
    private static int mStatus = STATUS_NONE;

    public static boolean isConnected() {
        return mIsConnected;
    }

    public static void onConnecting() {
        mStatus = STATUS_CONNECTING;
        mIsConnected = false;
    }

    public static void onConnected() {
        mStatus = STATUS_CONNECTED;
        mIsConnected = true;
    }

    public static void onDisconnected() {
        mStatus = STATUS_DISCONNECTED;
        mIsConnected = false;
    }

    public static void onDeviceReady() {
        mStatus = STATUS_READY;
        mIsConnected = true;
    }

    public static void onDisconnecting() {
        mStatus = STATUS_DISCONNECTING;
        mIsConnected = false;
    }

    public static void sendDeviceMessage(byte[] buf) {

    }

    //------------- GESTURES -------------------------

    public static String getGestureString(int gesture) {
        switch (gesture) {
            case GESTURE_ENTER:
                return "ENTER";
            case GESTURE_HOME:
                return "HOME";
            case GESTURE_UP:
                return "UP";
            case GESTURE_DOWN:
                return "DOWN";
            case GESTURE_BACK:
                return "BACK";
        }
        return "NONE";
    }

    //------------- SMARTWATCH -------------------------

    public final static String PHONE_LAUNCH_INTENT = "/intent";
    public final static String GESTURE_INTENT = "/gesture";

    public static void sendSmartPhoneMessage(String activity_name, String phoneLaunchIntent) {
        Log.v(TAG, "sendSmartPhoneMessage " + activity_name + " Intent " + phoneLaunchIntent);
    }

    /* The interface created a gesture, we have to redirect the message to the Smartphone */
    public static void onTouchGesture(int gesture) {
        Log.v(TAG, "onTouchGesture " + gesture);
        sendSmartPhoneMessage(getGestureString(gesture), GESTURE_INTENT);
    }

    public static void backMenu(MainActivity mainActivity) {
        if (mainActivity == null) {
            return;
        }

        //mainActivity.showToastMessage("Back");
        Log.d(TAG, "getBackStackEntryCount: " + mainActivity.getFragmentManager().getBackStackEntryCount());
        mainActivity.backFragment();
    }

    //------------- BATTERY LEVELS -------------------------

    public static int mBatteryLevel = 0;

    public static int getBatteryLevel() {
        return mBatteryLevel;
    }

    public static void gotoDashboard(MainActivity mainActivity) {

    }
}
