/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.flicktek.clip;

import android.app.AlarmManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Debug;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.flicktek.clip.ble.BleProfileService;
import com.flicktek.clip.menus.AppModel;
import com.flicktek.clip.menus.MediaFragment;
import com.flicktek.clip.menus.MenuFragment;
import com.flicktek.clip.menus.notification.NotificationFragment;
import com.flicktek.clip.uart.UARTCommandsAdapter;
import com.flicktek.clip.uart.UARTProfile;
import com.flicktek.clip.uart.domain.Command;
import com.flicktek.clip.wearable.common.Constants;
import com.flicktek.clip.wearable.common.NotificationModel;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends WearableActivity implements UARTCommandsAdapter.OnCommandSelectedListener, GoogleApiClient.ConnectionCallbacks,
        DataApi.DataListener, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener, FlicktekManager.BackMenu {
    private static final String TAG = "MainActivity";

    public static boolean isRound;

    public GoogleApiClient mGoogleApiClient;
    private UARTProfile mProfile;

    private AlarmManager mAmbientStateAlarmManager;
    private PendingIntent mAmbientStatePendingIntent;

    private BleProfileService.LocalBinder mBleProfileServiceBinder;

    private void setActivityFlags() {
        final Window windows = getWindow();
        windows.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        windows.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        windows.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        windows.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        windows.addFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }

    private BroadcastReceiver mServiceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            boolean cancelCalibration = false;

            switch (action) {
                case BleProfileService.BROADCAST_CONNECTION_STATE: {
                    final int state = intent.getIntExtra(BleProfileService.EXTRA_CONNECTION_STATE, BleProfileService.STATE_DISCONNECTED);
                    switch (state) {
                        case BleProfileService.STATE_LINK_LOSS:
                            Log.v(TAG, "************ LINK LOST ****************");
                            Toast.makeText(MainActivity.this, "Temporarily disconnected from device", Toast.LENGTH_LONG).show();
                            cancelCalibration = true;
                            sendMessageToHandheld(getApplicationContext(), Constants.FLICKTEK_CLIP.DEVICE_CONNECTION_STATE,
                                    "LinkLoss");
                            break;
                        case BleProfileService.STATE_DISCONNECTED:
                            Log.v(TAG, "************ DISCONNECTED ****************");
                            Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                            cancelCalibration = true;
                            sendMessageToHandheld(getApplicationContext(), Constants.FLICKTEK_CLIP.DEVICE_CONNECTION_STATE,
                                    "Disconnected");
                            finish();
                            break;
                        case BleProfileService.STATE_CONNECTED:
                            Log.v(TAG, "************ CONNECTED ****************");
                            Toast.makeText(MainActivity.this, "Connected!", Toast.LENGTH_SHORT).show();
                            sendMessageToHandheld(getApplicationContext(), Constants.FLICKTEK_CLIP.DEVICE_CONNECTION_STATE,
                                    "Connected");
                            break;
                        case BleProfileService.STATE_CONNECTING:
                            Log.v(TAG, "************ CONNECTING ****************");
                            Toast.makeText(MainActivity.this, "Connecting to device", Toast.LENGTH_LONG).show();
                            sendMessageToHandheld(getApplicationContext(), Constants.FLICKTEK_CLIP.DEVICE_CONNECTION_STATE,
                                    "Connecting");
                            break;
                    }
                    break;
                }
                case BleProfileService.BROADCAST_ERROR: {
                    final String message = intent.getStringExtra(BleProfileService.EXTRA_ERROR_MESSAGE);
                    // final int errorCode = intent.getIntExtra(BleProfileService.EXTRA_ERROR_CODE, 0);
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    // TODO error handing
                    break;
                }
                case UARTProfile.BROADCAST_DATA_RECEIVED: {
                    if (FlicktekManager.mDebugLevel >= FlicktekManager.DEBUG_CRAZY) {
                        final String message = intent.getStringExtra(UARTProfile.EXTRA_DATA);
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                    break;
                }
            }

            if (cancelCalibration && FlicktekManager.getInstance().isCalibrating()) {
                Log.v(TAG, "######## DASHBOARD ########");
                getFragmentManager().popBackStack("Dashboard", 0);
                Fragment fragment = MenuFragment.newInstance("Dashboard", "json_dashboard");
                showFragment(fragment, "DashBoard", true);
                sendMessageToHandheld(getApplicationContext(), Constants.FLICKTEK_CLIP.DEVICE_CONNECTION_STATE,
                        "Disconnected_while_calibration");
            }
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.setIntent(intent);
    }

    // Check if the screen is off and we get aria to sleep after a some time so we save energy
    private BroadcastReceiver mScreenIsOn = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Log.v(TAG, "---------- Screen is off --------");
                FlicktekCommands.getInstance().setApplicationPaused(true);
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                Log.v(TAG, "---------- Screen is on --------");
                FlicktekCommands.getInstance().setApplicationPaused(false);
            }
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            mBleProfileServiceBinder = (BleProfileService.LocalBinder) service;
            mProfile = (UARTProfile) mBleProfileServiceBinder.getProfile();

            sendMessageToHandheld(getApplicationContext(), Constants.FLICKTEK_CLIP.DEVICE_MAC_ADDRESS,
                    FlicktekManager.getInstance().getMacAddress());
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            mBleProfileServiceBinder = null;
            mProfile = null;
            Log.v(TAG, "TODO Check this disconnection");
            //FlicktekManager.getInstance().onDisconnected();
            finish();
        }
    };

    private View mDecorView;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    // This snippet hides the system bars.
    private void hideSystemUI() {
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        mDecorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        // Singleton initialize
        FlicktekCommands.getInstance().init(getApplicationContext());
        super.onCreate(savedInstanceState);
        mDecorView = getWindow().getDecorView();

        setContentView(R.layout.activity_main_stub);
        FlicktekCommands.getInstance().vibration_long();

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub_main);
        stub.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {

            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                stub.onApplyWindowInsets(windowInsets);
                isRound = windowInsets.isRound();

                setActivityFlags();
                hideSystemUI();
                Log.v(TAG, "Stub override. IsRound? " + isRound);
                return windowInsets;
            }
        });

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                boolean displayDashboard = true;
                try {
                    Bundle extras = getIntent().getExtras();
                    if (extras != null) {
                        String notification_key = extras.getString(Constants.FLICKTEK_CLIP.NOTIFICATION_KEY_ID);
                        if (notification_key != null) {
                            NotificationModel notificationModel = FlicktekManager.getInstance().getNotificationModelByKey(notification_key);
                            if (notificationModel != null) {
                                Log.v(TAG, "+ Resume notification [" + notification_key + "]");
                                newNotificationFragment(notificationModel);
                                displayDashboard = false;
                            } else {
                                Log.v(TAG, "+ Notification missing [" + notification_key + "]");
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (displayDashboard) {
                    Fragment fragment = MenuFragment.newInstance("Dashboard", "json_dashboard");
                    showFragment(fragment, "Dashboard", true);
                }

                FlicktekCommands.getInstance().onQueryForCalibration();

                stub.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                    @Override
                    public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                        int chinHeight = insets.getSystemWindowInsetBottom();
                        // chinHeight = 30;
                        return insets;
                    }
                });
            }
        });

        // Check if the WEAR device is connected to the UART device itself, or by the phone.
        // Binding will fail if we are using phone as proxy as the service has not been started before.
        final Intent service = new Intent(this, BleProfileService.class);
        bindService(service, mServiceConnection, 0);

        // Configure Google API client
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Register the broadcast receiver that will listen for events from the device
        final IntentFilter filter = new IntentFilter();
        filter.addAction(BleProfileService.BROADCAST_CONNECTION_STATE);
        filter.addAction(BleProfileService.BROADCAST_ERROR);
        filter.addAction(UARTProfile.BROADCAST_DATA_RECEIVED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mServiceBroadcastReceiver, filter);

        final IntentFilter filterScreen = new IntentFilter();
        filterScreen.addAction(Intent.ACTION_SCREEN_OFF);
        filterScreen.addAction(Intent.ACTION_SCREEN_ON);
        LocalBroadcastManager.getInstance(this).registerReceiver(mScreenIsOn, filter);

        sendMessageToHandheld(getApplicationContext(), Constants.FLICKTEK_CLIP.ANALYTICS_SCREEN, "MainActivity");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.unregisterConnectionCallbacks(this);
        mGoogleApiClient.unregisterConnectionFailedListener(this);
        mGoogleApiClient = null;

        // unbind if we were binded to the service.
        unbindService(mServiceConnection);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mServiceBroadcastReceiver);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        Log.d(TAG, "******************** onEnterAmbient ******************");
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        Log.d(TAG, "******************** onExitAmbient ********************");
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        Log.d(TAG, "onUpdateAmbient");
        Log.d(TAG, "******************** onUpdateAmbient ********************");
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();

        setAmbientEnabled();

        mAmbientStateAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent ambientStateIntent = new Intent(getApplicationContext(), MainActivity.class);

        mAmbientStatePendingIntent = PendingIntent.getActivity(
                getApplicationContext(),
                0,
                ambientStateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(final Bundle bundle) {
        Log.v(TAG, "onConnected");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    /**
     * Creates a fragment based on a new notification that arrived
     *
     * @param notificationModel
     */
    public void newNotificationFragment(NotificationModel notificationModel) {
        NotificationFragment notificationFragment = NotificationFragment.newInstance(notificationModel);
        getFragmentManager().popBackStackImmediate("Notification", FragmentManager.POP_BACK_STACK_INCLUSIVE);
        showFragment(notificationFragment, "Notification", false);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNotificationEvent(FlicktekCommands.onNotificationEvent notificationEvent) {
        Log.v(TAG, "onNotificationEvent");
        newNotificationFragment(notificationEvent.model);
    }

    // We try to launch once the calibration if we are not calibrated
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNotCalibrated(FlicktekCommands.onNotCalibrated notCalibrated) {
        Log.v(TAG, "onNotCalibrated");
        if (!Debug.isDebuggerConnected()) {
            getFragmentManager().popBackStack("Dashboard", 0);
            newFragment("menus.calibration.CalibrationFragmentScroll");

            sendMessageToHandheld(getApplicationContext(), Constants.FLICKTEK_CLIP.DEVICE_STATE,
                    "NotCalibrated");
        } else {
            Log.v(TAG, "Ignoring not calibration");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceReady(FlicktekCommands.onDeviceReady ready) {
        Log.v(TAG, "onDeviceReady");
        FlicktekCommands.getInstance().onQueryForCalibration();

        sendMessageToHandheld(getApplicationContext(), Constants.FLICKTEK_CLIP.DEVICE_STATE,
                "DeviceReady");
    }

    @Override
    public void onResume() {
        Log.v(TAG, "onResume");
        EventBus.getDefault().register(this);
        FlicktekCommands.getInstance().setApplicationFocus(true);
        FlicktekCommands.getInstance().setApplicationPaused(false);
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.v(TAG, "onPause");
        super.onPause();
        FlicktekCommands.getInstance().setApplicationFocus(false);
        FlicktekCommands.getInstance().setApplicationPaused(true);
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onConnectionSuspended(final int cause) {
        finish();
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
        finish();
    }

    @Override
    public void onDataChanged(final DataEventBuffer dataEventBuffer) {
        for (final DataEvent event : dataEventBuffer) {
            final DataItem item = event.getDataItem();
            String path = item.getUri().getEncodedPath();
            if (path.equals(Constants.FLICKTEK_CLIP.COUNT_PATH)) {
                Log.v(TAG, "Ping count from Smartphone");
                return;
            }
        }
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        // If the activity is binded to service it means that it has connected directly to the device. We ignore messages from the handheld.
        if (mProfile != null)
            return;

        switch (messageEvent.getPath()) {
            case Constants.UART.DEVICE_LINKLOSS:
            case Constants.UART.DEVICE_DISCONNECTED: {
                finish();
                break;
            }
        }
    }

    @Override
    public void onCommandSelected(final Command command) {
        // Send command to handheld if the watch is not connected directly to the UART device.
        final Command.Eol eol = command.getEol();
        String text = command.getCommand();
        switch (eol) {
            case CR_LF:
                text = text.replaceAll("\n", "\r\n");
                break;
            case CR:
                text = text.replaceAll("\n", "\r");
                break;
        }

        if (mProfile != null)
            mProfile.send(text);
        else
            sendMessageToHandheld(this, Constants.UART.COMMAND, text);
    }

    /**
     * Sends the given command to the handheld.
     *
     * @param command the message
     */
    public void sendMessageToHandheld(final @NonNull Context context, final @NonNull String route, final @NonNull String command) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final GoogleApiClient client = new GoogleApiClient.Builder(context)
                        .addApi(Wearable.API)
                        .build();
                client.blockingConnect();

                final NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(client).await();
                for (Node node : nodes.getNodes()) {
                    final MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(client, node.getId(),
                            route, command.getBytes()).await();
                    if (!result.getStatus().isSuccess()) {
                        Log.w(TAG, "Failed to send " + Constants.UART.COMMAND + " to " + node.getDisplayName());
                    }
                }
                client.disconnect();
            }
        }).start();
    }

    /**
     * Sends the given command to the handheld.
     *
     * @param command the message
     */
    public void sendGestureToHandheld(int touch) {
        Log.v(TAG, "-------- GESTURE TO HANDHELD " + touch + "--------------");
        sendMessageToHandheld(getApplicationContext(), Constants.FLICKTEK_CLIP.GESTURE, Integer.toString(touch));
    }

    public void backFragment() {
        Log.d(TAG, "showFragment");
        sendMessageToHandheld(getApplicationContext(), Constants.FLICKTEK_CLIP.ANALYTICS_SCREEN, "Back");

        runOnUiThread(new Runnable() {

            public void run() {
                FragmentManager fragmentManager = getFragmentManager();

                if (fragmentManager.getBackStackEntryCount() > 1) {
                    fragmentManager.popBackStack();
                    Log.d(TAG, "BackStackEntryCount: " + fragmentManager.getBackStackEntryCount());
                    fragmentManager.beginTransaction().commit();

                    Log.i(TAG, "-------- BEGIN BACK FRAGMENT --------");
                    for (int entry = 0; entry < fragmentManager.getBackStackEntryCount(); entry++) {
                        FragmentManager.BackStackEntry backStackEntryAt = fragmentManager.getBackStackEntryAt(entry);
                        Log.i(TAG, "Fragment: " + backStackEntryAt.getId() + " " + backStackEntryAt.getName());
                    }
                    Log.i(TAG, "---------- END BACK FRAGMENT --------");

                } else {
                    finish(); // Closes app
                }
            }
        });
    }

    public void showFragment(final Fragment _fragment, final String _fragment_name, final boolean isNewView) {
        Log.d(TAG, "showFragment " + _fragment.getClass().getCanonicalName());
        runOnUiThread(new Runnable() {

            public void run() {
                try {
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction transaction = fragmentManager.beginTransaction();

                    if (isNewView == false) {
                        transaction.setCustomAnimations(R.animator.fade_in_right, R.animator.fade_out_left,
                                R.animator.fade_in_left, R.animator.fade_out_right);
                    } else {
                        transaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);
                    }

                    String fragment_name = _fragment_name;

                    if (fragment_name == null) {
                        fragment_name = _fragment.getClass().getCanonicalName();
                    }

                    transaction.replace(R.id.container, _fragment).addToBackStack(fragment_name);
                    transaction.commit();

                    Log.i(TAG, "-------- BEGIN SHOW FRAGMENT --------");
                    for (int entry = 0; entry < fragmentManager.getBackStackEntryCount(); entry++) {
                        FragmentManager.BackStackEntry backStackEntryAt = fragmentManager.getBackStackEntryAt(entry);
                        Log.i(TAG, "Fragment: " + backStackEntryAt.getId() + " " + backStackEntryAt.getName());
                    }
                    Log.i(TAG, "---------- END SHOW FRAGMENT --------");

                    sendMessageToHandheld(getApplicationContext(), Constants.FLICKTEK_CLIP.ANALYTICS_SCREEN, _fragment_name);
                } catch (Exception e) {
                    e.printStackTrace();
                    finish();
                }
            }
        });
    }

    /**
     * Creates a fragment searching by class name
     *
     * @param appModel
     */
    public void newFragment(String classFragment) {
        String packageName = getPackageName();
        String className = packageName + "." + classFragment;

        Fragment myFragment = null;

        // Build Fragment from class name
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
            Constructor<?> ctor = null;
            ctor = clazz.getConstructor();
            Object object = ctor.newInstance();
            myFragment = (Fragment) object;
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException |
                IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return;
        }

        showFragment(myFragment, className, false);
    }

    /**
     * Creates a fragment by looking at the class on file
     *
     * @param appModel
     */
    public void newFragment(AppModel appModel) {
        String classFragment = appModel.getFragmentClass();
        newFragment(classFragment);
    }

    /**
     * Creates a fragment of Media type and gets the data from the AppModel to generate the
     * controller
     *
     * @param appModel
     */
    public void newMediaFragment(AppModel appModel) {
        MediaFragment mediaFragment = MediaFragment.newInstance(appModel.getConfiguration().toString());
        showFragment(mediaFragment, appModel.getName(), false);
    }

    /**
     * Battery display and levels
     * Store the old battery levels so we don't update in case we already did
     */

    static int old_battery = 0;
    static LinearLayout old_battery_layout = null;

    public void updateBattery(LinearLayout battery_layout, TextView battery_text,
                              ImageView battery_image, int battery_level) {

        if (battery_text == null || battery_layout == null || battery_image == null) {
            Log.v(TAG, "Battery UI interface is not defined");
            return;
        }

        if (battery_level == 0)
            battery_level = FlicktekManager.getInstance().getBatteryLevel();

        if (battery_level == 0)
            return;

        // If we are in a different menu we have to populate the values
        if (old_battery_layout != battery_layout)
            old_battery = 0;

        if (battery_level == old_battery)
            return;

        old_battery = battery_level;
        old_battery_layout = battery_layout;

        battery_text.setText(battery_level + "%");
        battery_layout.setVisibility(View.VISIBLE);

        int res;

        if (battery_level < 5)
            res = R.drawable.ic_batt_empty;
        else if (battery_level < 15)
            res = R.drawable.ic_batt_1;
        else if (battery_level < 30)
            res = R.drawable.ic_batt_2;
        else if (battery_level < 50)
            res = R.drawable.ic_batt_3;
        else if (battery_level < 75)
            res = R.drawable.ic_batt_4;
        else if (battery_level < 90)
            res = R.drawable.ic_batt_5;
        else
            res = R.drawable.ic_batt_full;

        battery_image.setImageResource(res);

        sendMessageToHandheld(getApplicationContext(), Constants.FLICKTEK_CLIP.BATTERY,
                Integer.toString(battery_level));
    }

    public void showToastMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGesturePerformed(FlicktekCommands.onGestureEvent gestureEvent) {
        String gesture = String.valueOf(gestureEvent.status);
        sendMessageToHandheld(this.getApplicationContext(), Constants.FLICKTEK_CLIP.GESTURE, gesture);
    }

    public void shutdown() {
        FlicktekCommands.getInstance().vibration_long();
        FlicktekManager.getInstance().onDisconnected();
        if (mBleProfileServiceBinder != null)
            mBleProfileServiceBinder.disconnect();
    }

    // Read bitmap assets from device

    public Bitmap loadBitmapFromAsset(Asset asset) {
        Log.d(TAG, "loadBitmapFromAsset: ");
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        ConnectionResult result =
                mGoogleApiClient.blockingConnect(2000, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            return null;
        }
        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                mGoogleApiClient, asset).await().getInputStream();

        if (assetInputStream == null) {
            Log.d(TAG, "Requested an unknown Asset from device.");
            return null;
        }
        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }
}
