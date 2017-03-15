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

package com.flicktek.clip.wearable;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.flicktek.clip.FlicktekClipApplication;
import com.flicktek.clip.ClipIntents;
import com.flicktek.clip.FlicktekCommands;
import com.flicktek.clip.FlicktekManager;
import com.flicktek.clip.LaunchActivity;
import com.flicktek.clip.MainActivity;
import com.flicktek.clip.uart.UARTService;
import com.flicktek.clip.wearable.common.Constants;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The main listener for messages from Wearable devices. There may be only one such service per application so it has to handle messages from all profiles.
 */
public class WearListenerService extends WearableListenerService {
    private static final String TAG = "WEARABLE_SERVICE";

    GoogleApiClient mGoogleApiClient;

    public static boolean mApplicationActive = false;

    public static String mMacDeviceConnected = "MAC_ADDRESS";
    public static int mBatteryLevel = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "------------- WearListenerService onCreate [" + mLastFragment + "] --------- ");
    }

    public void keepAlive(boolean value) {
        if (value)
            startService(new Intent(getApplicationContext(), getClass()));
        else
            stopSelf();
    }

    @Override
    public void onRebind(Intent intent) {
        keepAlive(false);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    private static String mLastFragment = "";

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        final String message = new String(messageEvent.getData());

        String path = "PATH";
        String text = "TEXT";

        try {
            path = messageEvent.getPath();
            text = new String(messageEvent.getData());

            Log.v(TAG, "WearListenerService " + path + " " + text);

            FlicktekClipApplication application = (FlicktekClipApplication) getApplication();
            Tracker tracker = application.getDefaultTracker();

            if (path.equals(Constants.FLICKTEK_CLIP.PHONE_CALL_NUMBER)) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    Log.v(TAG, "Missing CALL NUMBER permission");
                    return;
                }

                String number = text.replaceAll("\\s+","");
                Log.v(TAG, "Call phone number " + number);

                Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number , null));
                callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(callIntent);
            }

            if (path.equals(Constants.FLICKTEK_CLIP.DEVICE_CONNECTION_STATE)) {
                tracker.send(new HitBuilders.EventBuilder()
                        .setCategory("DeviceState")
                        .setLabel(mMacDeviceConnected)
                        .setAction(text)
                        .build());
                return;
            }

            if (path.equals(Constants.FLICKTEK_CLIP.DEVICE_MAC_ADDRESS)) {
                mMacDeviceConnected = text;
                tracker.send(new HitBuilders.EventBuilder()
                        .setCategory("DeviceState")
                        .setLabel(mMacDeviceConnected)
                        .setAction("MacAddress")
                        .setValue(1)
                        .build());

                tracker.setScreenName("MAC " + mMacDeviceConnected);
                tracker.send(new HitBuilders.ScreenViewBuilder().build());
                return;
            }

            if (path.equals(Constants.FLICKTEK_CLIP.ANALYTICS_CALIBRATION)) {
                tracker.send(new HitBuilders.EventBuilder()
                        .setCategory("Calibration")
                        .setAction(text)
                        .setLabel(mMacDeviceConnected)
                        .build());
                return;
            }

            if (path.equals(Constants.FLICKTEK_CLIP.BATTERY)) {
                try {
                    int battery_level = Integer.valueOf(text);
                    tracker.send(new HitBuilders.EventBuilder()
                            .setCategory("Battery")
                            .setAction("Battery")
                            .setLabel(mMacDeviceConnected)
                            .setValue(battery_level)
                            .build());
                } catch (NumberFormatException f) {

                }
                return;
            }

            if (path.equals(Constants.FLICKTEK_CLIP.ANALYTICS_SCREEN)) {
                String pack_name = getPackageName();
                if (text.startsWith(pack_name)) {
                    text = text.substring(pack_name.length() + 1, text.length());
                }
                tracker.setScreenName(text);
                tracker.send(new HitBuilders.ScreenViewBuilder().build());
                return;
            }

            if (path.equals(Constants.FLICKTEK_CLIP.GESTURE)) {
                int value = -1;
                boolean isNumber = false;

                try {
                    value = Integer.valueOf(text);
                    isNumber = true;
                } catch (NumberFormatException f) {
                    return;
                }

                if (isNumber) {
                    if (!mApplicationActive) {
                        String gesture = "";
                        switch (value) {
                            case 1:
                                gesture = "ENTER";
                                break;
                            case 2:
                                gesture = "HOME";
                                break;
                            case 3:
                                gesture = "UP";
                                break;
                            case 4:
                                gesture = "DOWN";
                                break;
                        }
                        Log.v(TAG, "Gesture: " + gesture + " Broadcast gesture ");
                        ClipIntents.openBroadcastIntent(this, ClipIntents.ACTION_URI_GESTURE, gesture);
                    } else {
                        // We propagate the gesture through the event system
                        FlicktekCommands.getInstance().onGestureChanged(value);
                        Log.v(TAG, "Gesture: " + FlicktekManager.getGestureString(value) +
                                " Application is active " + value);
                    }
                }
                return;
            }

            // Check to see if the message is to start an activity
            if (path.equals(Constants.FLICKTEK_CLIP.LAUNCH_FRAGMENT)) {
                // We don't want to relaunch the same fragment on and on
                if (!mLastFragment.contentEquals(text)) {
                    Intent startIntent = new Intent(this, MainActivity.class);

                    Map<String, List<String>> params = new HashMap<String, List<String>>();
                    String[] urlParts = text.split("\\?");

                    startIntent.putExtra("launch", urlParts[0]);
                    if (urlParts.length > 1) {
                        String query = urlParts[1];
                        for (String param : query.split("&")) {
                            String pair[] = param.split("=");
                            String key = URLDecoder.decode(pair[0], "UTF-8");
                            String value = "";
                            if (pair.length > 1) {
                                value = URLDecoder.decode(pair[1], "UTF-8");
                                startIntent.putExtra(key, value);
                            }
                        }
                    }

                    startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(startIntent);
                    mLastFragment = text;
                }
                return;
            } else {
                mLastFragment = "";
            }

            // Check to see if the message is to start an activity
            if (path.equals(Constants.FLICKTEK_CLIP.LAUNCH_ACTIVITY)) {
                Intent res = new Intent();
                String mPackage = getPackageName();
                String mClass = "." + text;
                res.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                res.setComponent(new ComponentName(mPackage, mPackage + mClass));
                startActivity(res);
                return;
            }

            if (path.equals(Constants.FLICKTEK_CLIP.LAUNCH_INTENT)) {
                Log.v(TAG, "Launch Intent! " + text);

                // If we are the same package name we will launch the activity internally
                // Otherwise it is some application that we want to launch
                String packageName = getPackageName();
                if (!text.startsWith(packageName) && !text.startsWith("android.intent.action")) {
                    PackageManager pm = getPackageManager();
                    Intent intent = pm.getLaunchIntentForPackage(text);
                    if (intent != null) {
                        startActivity(intent);
                        return;
                    }
                }

                Map<String, List<String>> params = new HashMap<String, List<String>>();
                String[] urlParts = text.split("\\?");
                if (urlParts.length > 1) {
                    final Intent intent = new Intent(urlParts[0]);
                    String query = urlParts[1];
                    for (String param : query.split("&")) {
                        String pair[] = param.split("=");
                        String key = URLDecoder.decode(pair[0], "UTF-8");
                        String value = "";
                        if (pair.length > 1) {
                            value = URLDecoder.decode(pair[1], "UTF-8");
                            intent.putExtra(key, value);
                        }
                    }
                    getApplicationContext().sendBroadcast(intent);
                    return;
                }

                final Intent intent = new Intent(text);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("launch", text);
                startActivity(intent);
                return;
            }

            // Check to see if the message is to start an activity
            if (path.equals(Constants.FLICKTEK_CLIP.START_ACTIVITY_PATH)) {
                Log.v(TAG, "Launch application");
                Intent startIntent = new Intent(this, LaunchActivity.class);
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startIntent);
                return;
            }

        } catch (
                Exception e
                )

        {
            Log.d(TAG, "onMessageReceived Failed [" + path + "] " + text + " " + e.toString());
        }

        switch (messageEvent.getPath())

        {
            case Constants.ACTION_DISCONNECT: {
                // A disconnect message was sent. The information which profile should be disconnected is in the data.
                final String profile = new String(messageEvent.getData());

                switch (profile) {
                    // Currently only UART profile has Wear support
                    case Constants.UART.PROFILE: {
                        final Intent disconnectIntent = new Intent(UARTService.ACTION_DISCONNECT);
                        disconnectIntent.putExtra(UARTService.EXTRA_SOURCE, UARTService.SOURCE_WEARABLE);
                        sendBroadcast(disconnectIntent);
                        break;
                    }
                }
                break;
            }
            case Constants.UART.COMMAND: {
                final String command = new String(messageEvent.getData());

                final Intent intent = new Intent(UARTService.ACTION_SEND);
                intent.putExtra(UARTService.EXTRA_SOURCE, UARTService.SOURCE_WEARABLE);
                intent.putExtra(Intent.EXTRA_TEXT, command);
                sendBroadcast(intent);
            }
            default:
                super.onMessageReceived(messageEvent);
                break;
        }
    }
}
