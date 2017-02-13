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

package com.flicktek.android.clip;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
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

import com.flicktek.android.clip.ble.BleProfileService;
import com.flicktek.android.clip.menus.AppModel;
import com.flicktek.android.clip.menus.MediaFragment;
import com.flicktek.android.clip.menus.MenuFragment;
import com.flicktek.android.clip.uart.UARTCommandsAdapter;
import com.flicktek.android.clip.uart.UARTProfile;
import com.flicktek.android.clip.uart.domain.Command;
import com.flicktek.android.clip.wearable.common.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class MainActivity extends WearableActivity implements UARTCommandsAdapter.OnCommandSelectedListener, GoogleApiClient.ConnectionCallbacks,
        DataApi.DataListener, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener {
    private static final String TAG = "MainActivity";

    public static boolean isRound;

    private GoogleApiClient mGoogleApiClient;
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

            switch (action) {
                case BleProfileService.BROADCAST_CONNECTION_STATE: {
                    final int state = intent.getIntExtra(BleProfileService.EXTRA_CONNECTION_STATE, BleProfileService.STATE_DISCONNECTED);
                    switch (state) {
                        case BleProfileService.STATE_LINK_LOSS:
                            Toast.makeText(MainActivity.this, "Temporarily disconnected from device", Toast.LENGTH_SHORT).show();
                            break;
                        case BleProfileService.STATE_DISCONNECTED:
                            finish();
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
        }
    };

    // Check if the screen is off and we get aria to sleep after a some time so we save energy
    private BroadcastReceiver mScreenIsOn = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                Log.v(TAG, "---------- Screen is off --------");
                FlicktekCommands.getInstance().setApplicationPaused(getApplicationContext(), true);
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                Log.v(TAG, "---------- Screen is on --------");
                FlicktekCommands.getInstance().setApplicationPaused(getApplicationContext(), false);
            }
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(final ComponentName name, final IBinder service) {
            mBleProfileServiceBinder = (BleProfileService.LocalBinder) service;
            mProfile = (UARTProfile) mBleProfileServiceBinder.getProfile();
        }

        @Override
        public void onServiceDisconnected(final ComponentName name) {
            mBleProfileServiceBinder = null;
            mProfile = null;
            FlicktekManager.onDisconnected();
            finish();
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        // Singleton initialize
        FlicktekCommands.getInstance().init(getApplicationContext());

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_stub);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub_main);
        stub.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {

            @Override
            public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                stub.onApplyWindowInsets(windowInsets);
                isRound = windowInsets.isRound();

                setActivityFlags();
                Log.v(TAG, "Stub override. IsRound? " + isRound);
                return windowInsets;
            }
        });

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                Fragment fragment = MenuFragment.newInstance("Dashboard", "json_dashboard");
                showFragment(fragment, true);

                if (!FlicktekManager.isCalibrated()) {
                    FlicktekCommands.getInstance().onQueryForCalibration();
                }

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
        //Log.d(TAG, "onEnterAmbient: ");
    }

    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        Log.d(TAG, "onExitAmbient");
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        Log.d(TAG, "onUpdateAmbient");
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

    // We try to launch once the calibration if we are not calibrated
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNotCalibrated(FlicktekCommands.onNotCalibrated notCalibrated) {
        Log.v(TAG, "onNotCalibrated");
        newFragment("menus.calibration.CalibrationFragmentScroll");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDeviceReady(FlicktekCommands.onDeviceReady ready) {
        Log.v(TAG, "onDeviceReady");
        FlicktekCommands.getInstance().onQueryForCalibration();
    }

    @Override
    public void onResume() {
        FlicktekCommands.getInstance().setApplicationPaused(this, false);
        super.onResume();
        Log.v(TAG, "onResume");
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        Log.v(TAG, "onPause");
        super.onPause();
        FlicktekCommands.getInstance().setApplicationPaused(this, true);
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
            final long id = ContentUris.parseId(item.getUri());

            // Configuration added or edited
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                final DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();

                // Update UI on UI thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "CREATED", Toast.LENGTH_SHORT).show();
                    }
                });
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // Configuration removed

                // Update UI on UI thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "DELETED", Toast.LENGTH_SHORT).show();
                    }
                });
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
            sendMessageToHandheld(this, text);
    }

    /**
     * Sends the given command to the handheld.
     *
     * @param command the message
     */
    private void sendMessageToHandheld(final @NonNull Context context, final @NonNull String command) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final GoogleApiClient client = new GoogleApiClient.Builder(context)
                        .addApi(Wearable.API)
                        .build();
                client.blockingConnect();

                final NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(client).await();
                for (Node node : nodes.getNodes()) {
                    final MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(client, node.getId(), Constants.UART.COMMAND, command.getBytes()).await();
                    if (!result.getStatus().isSuccess()) {
                        Log.w(TAG, "Failed to send " + Constants.UART.COMMAND + " to " + node.getDisplayName());
                    }
                }
                client.disconnect();
            }
        }).start();
    }

    public void backFragment() {
        Log.d(TAG, "showFragment");
        runOnUiThread(new Runnable() {

            public void run() {
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager.getBackStackEntryCount() > 1){
                    fragmentManager.popBackStackImmediate();
                    Log.d(TAG, "BackStackEntryCount: " + fragmentManager.getBackStackEntryCount());
                    fragmentManager.beginTransaction().commit();
                } else {
                    finish(); // Closes app
                }
            }
        });
    }

    public void showFragment(final Fragment _fragment, final boolean isNewView) {
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

                    transaction.replace(R.id.container, _fragment).addToBackStack("MainActivity");
                    transaction.commit();
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

        showFragment(myFragment, false);
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
        showFragment(mediaFragment, false);
    }

    /**
     * Battery display and levels
     * Store the old battery levels so we don't update in case we already did
     */

    static int old_battery = 0;
    static LinearLayout old_battery_layout = null;

    public void updateBattery(LinearLayout battery_layout, TextView battery_text, ImageView battery_image) {
        int battery_level = FlicktekManager.getBatteryLevel();

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
    }

    public void showToastMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void shutdown() {
        FlicktekManager.onDisconnected();
        mBleProfileServiceBinder.disconnect();
    }
}
