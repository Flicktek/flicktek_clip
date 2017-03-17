package com.flicktek.clip;

import android.support.annotation.Nullable;
import android.util.Log;

import com.flicktek.clip.ConnectionEvents.ConnectedEvent;
import com.flicktek.clip.ConnectionEvents.ConnectingEvent;
import com.flicktek.clip.ConnectionEvents.DisconnectedEvent;
import com.flicktek.clip.ConnectionEvents.DisconnectingEvent;
import com.flicktek.clip.ConnectionEvents.LinkLossEvent;
import com.flicktek.clip.wearable.common.NotificationModel;

import org.greenrobot.eventbus.EventBus;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

// TODO Change this into a singleton

public class FlicktekManager {
    private static final String TAG = "FlickTek";

    // Singleton
    private static FlicktekManager mInstance = null;

    public static FlicktekManager getInstance() {
        if (mInstance == null)
            mInstance = new FlicktekManager();
        return mInstance;
    }

    public final static int DEBUG_DISABLED = 0;
    public final static int DEBUG_ENABLED = 1;
    public final static int DEBUG_CRAZY = 10;

    // Debug levels
    public static int mDebugLevel = DEBUG_DISABLED;

    public final static int GESTURE_NONE = 0;
    public final static int GESTURE_ENTER = 1;
    public final static int GESTURE_HOME = 2;
    public final static int GESTURE_UP = 3;
    public final static int GESTURE_DOWN = 4;
    public final static int GESTURE_BACK = 5;
    public final static int GESTURE_PHYSICAL_BUTTON = 10;

    public final static int STATUS_NONE = 1;
    public final static int STATUS_CONNECTING = 4;
    public final static int STATUS_CONNECTED = 5;
    public final static int STATUS_READY = 6;
    public final static int STATUS_DISCONNECTED = 7;
    public final static int STATUS_DISCONNECTING = 8;

    //---------------- DEVICE -------------------------

    private static String mDeviceName = "";
    private static String mMacAddress = "";
    private static String mFirmwareVersion = "";
    private static String mFirmwareRevision = "";

    //-------------- DEVICE STATE ---------------------
    // First handshake between devices happened
    private boolean mIsHandshakeOk = false;
    private boolean mIsCalibrated = false;
    private boolean mIsCalibrating = false;
    private int mReconnectAttempts = 0;
    private int mLastPing = 0;
    private int mBatteryLevel = 0;

    //-------------- CONNECTION -----------------------

    private boolean mIsConnected = false;
    private int mStatus = STATUS_NONE;

    // Configures if the menu will go back if you click home
    // Or requires a double input to exit
    public boolean mIsDoubleGestureHomeExit = false;

    public boolean isConnected() {
        return mIsConnected;
    }

    public void onRelease() {
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

    public void onConnecting(String macAddress) {
        mStatus = STATUS_CONNECTING;
        mIsConnected = false;
        setMacAddress(macAddress);
        EventBus.getDefault().post(new ConnectingEvent(macAddress));
    }

    public void setName(String name) {
        mDeviceName = name;
    }

    public void onConnected(String name, String macAddress) {
        mStatus = STATUS_CONNECTED;
        setFirmwareRevision("");
        setFirmwareVersion("");

        mIsConnected = true;
        mIsCalibrating = false;
        mBatteryLevel = 0;
        setMacAddress(macAddress);
        setName(name);
        EventBus.getDefault().post(new ConnectedEvent(name, macAddress));
    }

    public void onDisconnected() {
        FlicktekManager.getInstance().setHandshakeOk(false);
        mStatus = STATUS_DISCONNECTED;
        mIsConnected = false;
        mReconnectAttempts++;
        EventBus.getDefault().post(new DisconnectedEvent());
    }

    public void onLinkloss() {
        EventBus.getDefault().post(new LinkLossEvent());
        onDisconnected();
    }

    public void onDeviceReady() {
        mStatus = STATUS_READY;
        mIsConnected = true;
    }

    public void onDisconnecting() {
        mStatus = STATUS_DISCONNECTING;
        mIsConnected = false;
        EventBus.getDefault().post(new DisconnectingEvent());
    }

    public void sendDeviceMessage(byte[] buf) {

    }

    public boolean isCalibrating() {
        return mIsCalibrating;
    }

    public void setCalibrationMode(boolean calibration) {
        mIsCalibrating = calibration;
    }

    //------------- GESTURES -------------------------

    public String getGestureString(int gesture) {
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

    public void addNotification(NotificationModel model) {
        mNotifications.add(model);
    }

    public interface BackMenu {
        void backFragment();
    }

    //------------- SMARTWATCH -------------------------

    public void backMenu(BackMenu mainActivity) {
        if (mainActivity == null) {
            return;
        }
        mainActivity.backFragment();
    }

    //------------- Getters and setters -------------------------

    public void setBatteryLevel(int value) {
        mBatteryLevel = value;
    }

    public int getBatteryLevel() {
        return mBatteryLevel;
    }

    public String getFirmwareVersion() {
        return mFirmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.mFirmwareVersion = firmwareVersion;
    }

    public String getFirmwareRevision() {
        return mFirmwareRevision;
    }

    public void setFirmwareRevision(String mFirmwareRevision) {
        this.mFirmwareRevision = mFirmwareRevision;
    }

    public boolean isHandshakeOk() {
        return mIsHandshakeOk;
    }

    public void setHandshakeOk(boolean isHandshakeOk) {
        this.mIsHandshakeOk = isHandshakeOk;
    }

    public boolean isCalibrated() {
        return mIsCalibrated;
    }

    public void setCalibration(boolean isCalibrated) {
        mIsCalibrated = isCalibrated;
    }

    public int getLastPing() {
        return mLastPing;
    }

    public void setLastPing(int lastPing) {
        mLastPing = lastPing;
    }

    public void setMacAddress(String mac_address) {
        mMacAddress = mac_address;
    }

    public String getMacAddress() {
        return mMacAddress;
    }

    //------------- Notification system -------------------------

    public List<NotificationModel> mNotifications = new LinkedList<NotificationModel>();

    public List<NotificationModel> getNotifications() {
        Log.d(TAG, "getNotifications: ");
        return mNotifications;
    }

    @Nullable
    public NotificationModel getNotificationModelByKey(String id) {
        Iterator<NotificationModel> iterator = mNotifications.iterator();
        while (iterator.hasNext()) {
            NotificationModel notification = iterator.next();
            if (notification.getKeyId().equals(id)) {
                return notification;
            }
        }
        return null;
    }
}
