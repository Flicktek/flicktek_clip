package com.flicktek.android.clip;

import android.app.FragmentManager;
import android.util.Log;

public class FlicktekManager {
    private static final String TAG = "FlickTek";

    public final static int DEBUG_DISABLED = 0;
    public final static int DEBUG_ENABLED = 1;
    public final static int DEBUG_CRAZY = 10;

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

    //---------------- DEVICE -------------------------

    private static String mMacAddress = "";
    private static String mFirmwareVersion = "";
    private static String mFirmwareRevision = "";

    //-------------- DEVICE STATE ---------------------
    // First handshake between devices happened
    private static boolean mIsHandshakeOk = false;
    private static boolean mIsCalibrated = false;
    private static int mReconnectAttempts = 0;
    private static int mLastPing = 0;
    private static int mBatteryLevel = 0;

    //-------------- CONNECTION -----------------------

    private static boolean mIsConnected = false;
    private static int mStatus = STATUS_NONE;

    public static boolean isConnected() {
        return mIsConnected;
    }

    public static void onRelease() {
        Log.v(TAG, "*********** onRelease ***********" + mMacAddress);
        mIsHandshakeOk = false;
        mFirmwareVersion = "";
        mFirmwareRevision = "";
        mReconnectAttempts = 0;
        mLastPing = 0;
        mBatteryLevel = 0;
        mStatus = STATUS_NONE;
        mIsConnected = false;
        mIsCalibrated = false;
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
        mReconnectAttempts++;
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

    public static void backMenu(MainActivity mainActivity) {
        if (mainActivity == null) {
            return;
        }

        FragmentManager fragmentManager = mainActivity.getFragmentManager();

        //mainActivity.showToastMessage("Back");
        Log.d(TAG, "getBackStackEntryCount: " + fragmentManager.getBackStackEntryCount());
        mainActivity.backFragment();
    }

    //------------- Getters and setters -------------------------

    public static int getBatteryLevel() {
        return mBatteryLevel;
    }

    public static void gotoDashboard(MainActivity mainActivity) {

    }

    public static String getFirmwareVersion() {
        return mFirmwareVersion;
    }

    public static void setFirmwareVersion(String mFirmwareVersion) {
        FlicktekManager.mFirmwareVersion = mFirmwareVersion;
    }

    public static String getFirmwareRevision() {
        return mFirmwareRevision;
    }

    public static void setFirmwareRevision(String mFirmwareRevision) {
        FlicktekManager.mFirmwareRevision = mFirmwareRevision;
    }

    public static boolean isHandshakeOk() {
        return mIsHandshakeOk;
    }

    public static void setHandshakeOk(boolean mIsHandshakeOk) {
        FlicktekManager.mIsHandshakeOk = mIsHandshakeOk;
    }

    public static boolean isCalibrated() {
        return mIsCalibrated;
    }

    public static void setCalibration(boolean mIsCalibrated) {
        FlicktekManager.mIsCalibrated = mIsCalibrated;
    }

    public static int getLastPing() {
        return mLastPing;
    }

    public static void setLastPing(int mLastPing) {
        FlicktekManager.mLastPing = mLastPing;
    }

    public void setMacAddress(String mac_address) {
        mMacAddress = mac_address;
    }

    public static String getMacAddress() {
        return mMacAddress;
    }

}
