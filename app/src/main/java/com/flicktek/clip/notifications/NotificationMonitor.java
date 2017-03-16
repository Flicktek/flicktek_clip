package com.flicktek.clip.notifications;

import android.app.ActivityManager;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.flicktek.clip.wearable.common.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.List;

public class NotificationMonitor extends NotificationListenerService implements
        MessageApi.MessageListener,
        DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, CapabilityApi.CapabilityListener {
    private static final String TAG = "NotificationMonitor";
    private static final String TAG_PRE = "[" + NotificationMonitor.class.getSimpleName() + "] ";
    private static final int EVENT_UPDATE_CURRENT_NOS = 0;
    public static final String ACTION_NLS_CONTROL = "com.flicktek.clip.NLSCONTROL";
    public static List<StatusBarNotification[]> mCurrentNotifications = new ArrayList<StatusBarNotification[]>();
    public static int mCurrentNotificationsCounts = 0;
    public static StatusBarNotification mPostedNotification;
    public static StatusBarNotification mRemovedNotification;
    private CancelNotificationReceiver mReceiver = new CancelNotificationReceiver();
    // String a;

    public GoogleApiClient mGoogleApiClient;

    private void toggleNotificationListenerService() {
        //Log.d(TAG, "toggleNotificationListenerService() called");
        ComponentName thisComponent = new ComponentName(this, /*getClass()*/ NotificationMonitor.class);
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    private Handler mMonitorHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_UPDATE_CURRENT_NOS:
                    updateCurrentNotifications();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "Google API Client was connected");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.CapabilityApi.addListener(
                mGoogleApiClient, this, Uri.parse("wear://"), CapabilityApi.FILTER_REACHABLE);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "Connection to Google API client was suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {

    }

    class CancelNotificationReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action;
            if (intent != null && intent.getAction() != null) {
                action = intent.getAction();
                if (action.equals(ACTION_NLS_CONTROL)) {
                    String command = intent.getStringExtra("command");
                    if (TextUtils.equals(command, "cancel_last")) {
                        if (mCurrentNotifications != null && mCurrentNotificationsCounts >= 1) {
                            StatusBarNotification sbnn = getCurrentNotifications()[mCurrentNotificationsCounts - 1];
                            cancelNotification(sbnn.getPackageName(), sbnn.getTag(), sbnn.getId());
                        }
                    } else if (TextUtils.equals(command, "cancel_all")) {
                        cancelAllNotifications();
                    }
                }
            }
        }
    }

    private static final String PREFERENCE_LAST_NOTIF_ID = "PREFERENCE_LAST_NOTIF_ID";

    private static int getNextNotifId(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        int id = sharedPreferences.getInt(PREFERENCE_LAST_NOTIF_ID, 0) + 1;
        if (id == Integer.MAX_VALUE) { id = 0; } // isn't this over kill ??? hahaha!!  ^_^
        sharedPreferences.edit().putInt(PREFERENCE_LAST_NOTIF_ID, id).apply();
        return id;
    }

    /**
     * Builds a DataItem that on the wearable will be interpreted as a request to show a
     * notification. The result will be a notification that only shows up on the wearable.
     */
    public void buildWearableNotification(StatusBarNotification sbn) {
        String text = "";

        if (mGoogleApiClient.isConnected()) {
            String pack = sbn.getPackageName();
            Bundle extras = sbn.getNotification().extras;
            String notificationTitle = extras.getString(Notification.EXTRA_TITLE);
            if (notificationTitle == null)
                return;

            String path = Constants.FLICKTEK_CLIP.NOTIFICATION_PATH;
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);

            Notification notification = sbn.getNotification().clone();

            int id = getNextNotifId(this);
            Log.i(TAG, "+ Notification ID " + id + " Key "+ sbn.getKey());

            putDataMapRequest.getDataMap().putString(
                    Constants.FLICKTEK_CLIP.NOTIFICATION_KEY_ID,
                    sbn.getKey());

            putDataMapRequest.getDataMap().putString(
                    Constants.FLICKTEK_CLIP.NOTIFICATION_KEY_TITLE,
                    notificationTitle);

            CharSequence notificationText = extras.getCharSequence(Notification.EXTRA_TEXT);
            if (notificationText != null)
                text = notificationText.toString();

            putDataMapRequest.getDataMap().putString(
                    Constants.FLICKTEK_CLIP.NOTIFICATION_KEY_CONTENT,
                    text);

            CharSequence notificationSubText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
            if (notificationSubText != null)
                text = notificationSubText.toString();

            putDataMapRequest.getDataMap().putString(
                    Constants.FLICKTEK_CLIP.NOTIFICATION_KEY_CONTENT,
                    text);

            try {
                Bitmap notificationLargeIcon = ((Bitmap) extras.getParcelable(Notification.EXTRA_LARGE_ICON));
                Log.i(TAG, "notificationLargeIcon is null: " + (notificationLargeIcon == null));

                int id_small_icon = extras.getInt(Notification.EXTRA_SMALL_ICON);
                if (id_small_icon > 0) {
                    Log.i(TAG, "notificationSmallIcon is " + id_small_icon);
                    Context remotePackageContext = null;
                    Bitmap bmp = null;
                    try {
                        remotePackageContext = getApplicationContext().createPackageContext(pack, 0);
                        /*
                        Drawable icon = remotePackageContext.getResources().getDrawable(id_small_icon);
                        if (icon != null) {
                            bmp = ((BitmapDrawable) icon).getBitmap();
                        }
                        */
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            request.setUrgent();
            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.e(TAG, "buildWatchOnlyNotification(): Failed to set the data, "
                                        + "status: " + dataItemResult.getStatus().getStatusCode());
                            }
                        }
                    });
        } else {
            Log.e(TAG, "buildWearableOnlyNotification(): no Google API Client connection");
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //ensureCollectorRunning();
        //toggleNotificationListenerService();

        logNLS("onCreate...");

        try {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();

            mGoogleApiClient.connect();
        } catch (Exception e) {
            Log.d(TAG, "No wearable support");
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // TODO unbind from service on disconnection so we don't leak this.
        unregisterReceiver(mReceiver);
    }

    private void ensureCollectorRunning() {
        ComponentName collectorComponent = new ComponentName(this, /*NotificationListenerService Inheritance*/ NotificationMonitor.class);
        Log.v(TAG, "ensureCollectorRunning collectorComponent: " + collectorComponent);
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        boolean collectorRunning = false;
        List<ActivityManager.RunningServiceInfo> runningServices = manager.getRunningServices(Integer.MAX_VALUE);
        if (runningServices == null) {
            Log.w(TAG, "ensureCollectorRunning() runningServices is NULL");
            return;
        }
        for (ActivityManager.RunningServiceInfo service : runningServices) {
            if (service.service.equals(collectorComponent)) {
                Log.w(TAG, "ensureCollectorRunning service - pid: " + service.pid +
                        ", currentPID: " + Process.myPid() +
                        ", clientPackage: " + service.clientPackage +
                        ", clientCount: " + service.clientCount
                        + ", clientLabel: " + ((service.clientLabel == 0) ? "0" : "(" + getResources().getString(service.clientLabel) + ")"));
                if (service.pid == Process.myPid() /*&& service.clientCount > 0 && !TextUtils.isEmpty(service.clientPackage)*/) {
                    collectorRunning = true;
                }
            }
        }
        if (collectorRunning) {
            Log.d(TAG, "ensureCollectorRunning: collector is running");
            return;
        }
        Log.d(TAG, "ensureCollectorRunning: collector not running, reviving...");
    }

    public boolean isBound() {
        return isBound;
    }

    public void onServiceConnected() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_NLS_CONTROL);
        registerReceiver(mReceiver, filter);
        mMonitorHandler.sendMessage(mMonitorHandler.obtainMessage(EVENT_UPDATE_CURRENT_NOS));
    }

    @Override
    public IBinder onBind(Intent intent) {
        isBound = true;
        String action = intent.getAction();
        Log.d(TAG, "onBind: " + action);

        if (SERVICE_INTERFACE.equals(action)) {
            Log.d(TAG, "Bound by system");
            return super.onBind(intent);
        } else {
            Log.d(TAG, "Bound by application");
            return binder;
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        updateCurrentNotifications();
        mPostedNotification = sbn;

        String pack = sbn.getPackageName();
        Bundle extras = sbn.getNotification().extras;
        String notificationTitle = extras.getString(Notification.EXTRA_TITLE);
        if (notificationTitle == null)
            return;

        CharSequence notificationText = extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence notificationSubText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);

        logNLS("------------ onNotificationPosted ----------");
        //logNLS("+ Have " + mCurrentNotificationsCounts + " active notifications");

        Log.d(TAG, pack + " [" + notificationTitle + "] \"" + notificationText + "\"");
        if (notificationSubText != null)
            Log.i(TAG, "       " + notificationSubText);

        buildWearableNotification(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        updateCurrentNotifications();
        logNLS("+ Removed " + sbn.getKey());
        logNLS("  have " + mCurrentNotificationsCounts + " active notifications");
        mRemovedNotification = sbn;
    }

    private boolean isBound = false;

    private final IBinder binder = new ServiceBinder();

    public class ServiceBinder extends Binder {
        public NotificationMonitor getService() {
            return NotificationMonitor.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startid) {
        return START_STICKY;
    }

    private void updateCurrentNotifications() {
        try {
            StatusBarNotification[] activeNos = getActiveNotifications();
            if (activeNos == null)
                return;

            if (mCurrentNotifications.size() == 0) {
                mCurrentNotifications.add(null);
            }
            mCurrentNotifications.set(0, activeNos);
            mCurrentNotificationsCounts = activeNos.length;
        } catch (SecurityException e) {
            logNLS("We cannot call get active notifications");
        } catch (Exception e) {
            logNLS("Should not be here!!");
            e.printStackTrace();
        }
    }

    public static StatusBarNotification[] getCurrentNotifications() {
        if (mCurrentNotifications.size() == 0) {
            logNLS("mCurrentNotifications size is ZERO!!");
            return null;
        }
        return mCurrentNotifications.get(0);
    }

    private static void logNLS(Object object) {
        Log.i(TAG, TAG_PRE + object);
    }
}