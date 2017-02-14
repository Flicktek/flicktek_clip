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

package com.flicktek.android.clip.wearable;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import com.flicktek.android.clip.ClipIntents;
import com.flicktek.android.clip.LaunchActivity;
import com.flicktek.android.clip.MainActivity;
import com.flicktek.android.clip.uart.UARTService;
import com.flicktek.android.clip.wearable.common.Constants;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * The main listener for messages from Wearable devices. There may be only one such service per application so it has to handle messages from all profiles.
 */
public class WearListenerService extends WearableListenerService {
    private static final String TAG = "WEARABLE_SERVICE";

    public static MyGestureListener mListener;

    public static void setCustomObjectListener(MyGestureListener listener) {
        mListener = listener;
    }

    GoogleApiClient mGoogleApiClient;

    public static boolean mApplicationActive = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "------------- WearListenerService onCreate --------- ");
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

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        final String message = new String(messageEvent.getData());
        Log.v(TAG, "WearListenerService " + message);

        String path = "PATH";
        String text = "TEXT";

        try {
            path = messageEvent.getPath();
            text = new String(messageEvent.getData());

            if (path.equals(Constants.FLICKTEK_CLIP.START_MUSIC_PATH)) {
                Intent intent = new Intent("android.intent.action.MUSIC_PLAYER");
                startActivity(intent);

                if (!text.equals("launch"))
                    ClipIntents.openBroadcastIntent(this, "com.android.music.musicservicecommand",
                            "command", text);
                return;
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
                if (!text.startsWith(packageName)) {
                    PackageManager pm = getPackageManager();
                    Intent intent = pm.getLaunchIntentForPackage(text);
                    startActivity(intent);
                    return;
                }

                final Intent intent = new Intent(text);
                intent.putExtra("launch", text);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return;
            }

            // Check to see if the message is to start an activity
            if (path.equals(Constants.FLICKTEK_CLIP.LAUNCH_FRAGMENT)) {
                Intent startIntent = new Intent(this, MainActivity.class);
                startIntent.putExtra("fragment", text);
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startIntent);
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

            if (path.equals(Constants.FLICKTEK_CLIP.START_MUSIC_PATH)) {
                Intent intent = new Intent("android.intent.action.MUSIC_PLAYER");
                startActivity(intent);

                if (!text.equals("launch"))
                    ClipIntents.openBroadcastIntent(this,
                            "com.android.music.musicservicecommand",
                            "command", text);
                return;
            }


            int value = -1;
            boolean isNumber = false;

            try {
                value = Integer.valueOf(text);
                isNumber = true;
            } catch (NumberFormatException f) {

            }

            if (path.equals(Constants.FLICKTEK_CLIP.GESTURE)) {
                if (isNumber) {
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

                    if (!mApplicationActive) {
                        Log.v(TAG, "Gesture: " + gesture + " Broadcast gesture ");
                        ClipIntents.openBroadcastIntent(this, ClipIntents.ACTION_URI_GESTURE, gesture);
                    } else {
                        if (mListener != null)
                            mListener.onGestureReceived(gesture);

                        Log.v(TAG, "Gesture: " + gesture + " Application is active ");
                    }
                }
            }

        } catch (Exception e) {
            Log.d(TAG, "onMessageReceived Failed [" + path + "] " + text + " " + e.toString());
        }

        switch (messageEvent.getPath()) {
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

    public interface MyGestureListener {
        void onGestureReceived(String gesture);
    }
}
