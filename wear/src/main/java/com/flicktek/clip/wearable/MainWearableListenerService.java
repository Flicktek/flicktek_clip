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

import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.flicktek.clip.FlicktekCommands;
import com.flicktek.clip.FlicktekManager;
import com.flicktek.clip.R;
import com.flicktek.clip.ScannerActivity;
import com.flicktek.clip.uart.UARTConfigurationsActivity;
import com.flicktek.clip.wearable.common.Constants;
import com.flicktek.clip.wearable.common.NotificationModel;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;

import java.util.Iterator;

public class MainWearableListenerService extends com.google.android.gms.wearable.WearableListenerService {
    public static final String TAG = "SmartPhone";

    private static final int UART_SHOW_CONFIGURATIONS = 1;
    private static final int UART_DISCONNECT = 2;

    private static final int UART_NOTIFICATION_ID = 1;

    public void keepAlive(boolean value) {
        if (value)
            startService(new Intent(getApplicationContext(), getClass()));
        else
            stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //Log.v(TAG, "------------- MainWearableListenerService onCreate --------- ");
    }

    @Override
    public void onRebind(Intent intent) {
        keepAlive(false);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    public void notificationDeal(DataEvent dataEvent) {
        DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
        if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
            String title = dataMap.getString(Constants.FLICKTEK_CLIP.NOTIFICATION_KEY_TITLE);
            String content = dataMap.getString(Constants.FLICKTEK_CLIP.NOTIFICATION_KEY_CONTENT);

            String id = dataMap.getString(Constants.FLICKTEK_CLIP.NOTIFICATION_KEY_ID);
            Asset asset = dataMap.getAsset(Constants.FLICKTEK_CLIP.NOTIFICATION_KEY_BITMAP_ASSET);

            Log.v(TAG, "+ " + id + ": Notification [" + title + "] content \"" + content + "\"");

            NotificationModel model = new NotificationModel(title, content, id, null, (Object) asset);
            FlicktekCommands.getInstance().onNotification(model);
        } else if (dataEvent.getType() == DataEvent.TYPE_DELETED) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "+ Notification - DataItem deleted: " + dataEvent.getDataItem().getUri().getPath());
            }

            String id = dataMap.getString(Constants.FLICKTEK_CLIP.NOTIFICATION_KEY_ID);
            Iterator<NotificationModel> iterator = FlicktekManager.getInstance().mNotifications.iterator();
            while (iterator.hasNext()) {
                NotificationModel notification = iterator.next();
                Log.d(TAG, "+ Deleted " + id + "Run inside handleNotificationUpdate: "
                        + notification.getKeyId() + " == " + id);

                if (notification.getKeyId().equals(id)) {
                    Log.d(TAG, "+ Removing notification " + id);
                    iterator.remove();
                }
            }
        }
    }

    @Override
    public void onDataChanged(final DataEventBuffer dataEventBuffer) {
        for (final DataEvent event : dataEventBuffer) {
            final DataItem item = event.getDataItem();
            String path = item.getUri().getEncodedPath();
            if (path.equals(Constants.FLICKTEK_CLIP.NOTIFICATION_PATH)) {
                notificationDeal(event);
                return;
            }
        }
    }

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        final String message = new String(messageEvent.getData());
        Log.v(TAG, "onMessageReceived " + message);
        switch (messageEvent.getPath()) {

            case Constants.FLICKTEK_CLIP.START_ACTIVITY_PATH: {
                final Intent activity = new Intent(getApplicationContext(), ScannerActivity.class);
                activity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(activity);
                break;
            }

            case Constants.UART.DEVICE_CONNECTED: {
                // Disconnect action
                final Intent disconnectIntent = new Intent(ActionReceiver.ACTION_DISCONNECT);
                disconnectIntent.putExtra(ActionReceiver.EXTRA_DATA, Constants.UART.PROFILE);
                final PendingIntent disconnectAction = PendingIntent.getBroadcast(this, UART_DISCONNECT, disconnectIntent, PendingIntent.FLAG_CANCEL_CURRENT);

                // Open action
                final Intent intent = new Intent(this, UARTConfigurationsActivity.class);
                final PendingIntent pendingIntent = PendingIntent.getActivity(this, UART_SHOW_CONFIGURATIONS, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                final NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                        .setContentIntent(pendingIntent)
                        .setOngoing(true)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(getString(R.string.notif_uart_device_connected))
                        .setContentText(message)
                        .addAction(new NotificationCompat.Action(R.drawable.ic_full_bluetooth, getString(R.string.action_disconnect), disconnectAction))
                        .setLocalOnly(true);
                NotificationManagerCompat.from(this).notify(UART_NOTIFICATION_ID, builder.build());
                break;
            }
            case Constants.UART.DEVICE_LINKLOSS:
            case Constants.UART.DEVICE_DISCONNECTED: {
                NotificationManagerCompat.from(this).cancel(UART_NOTIFICATION_ID);
            }
            default:
                super.onMessageReceived(messageEvent);
                break;
        }
    }

    @Override
    public void onPeerDisconnected(final Node peer) {
        super.onPeerDisconnected(peer);
        NotificationManagerCompat.from(this).cancel(UART_NOTIFICATION_ID);
    }
}
