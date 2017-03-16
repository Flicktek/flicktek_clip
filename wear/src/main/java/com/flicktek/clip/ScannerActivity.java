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

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.flicktek.clip.ble.BleProfileService;
import com.flicktek.clip.ble.DevicesAdapter;
import com.flicktek.clip.wearable.common.Constants;


public class ScannerActivity extends Activity {
    private static final String TAG = "ScannerActivity";

    private static final int PERMISSION_REQUEST_LOCATION = 1;
    private static final int PERMISSION_READ_CONTACTS = 2;

    private DevicesAdapter mDeviceAdapter;
    private View mHeader;

    private BroadcastReceiver mServiceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();

            switch (action) {
                case BleProfileService.BROADCAST_CONNECTION_STATE: {
                    final int state = intent.getIntExtra(BleProfileService.EXTRA_CONNECTION_STATE, BleProfileService.STATE_DISCONNECTED);
                    switch (state) {
                        case BleProfileService.STATE_LINK_LOSS:
                            Log.v(TAG, "------------- STATE LINK LOSS -- ---------------");
                            break;
                        case BleProfileService.STATE_DISCONNECTED:
                            Log.v(TAG, "------------- STATE DISCONNECTED ---------------");
                            mDeviceAdapter.setConnectingPosition(-1);
                            break;
                    }
                    break;
                }
                case BleProfileService.BROADCAST_DEVICE_READY: {
                    Log.v(TAG, "--------------- DEVICE READY -------------------");
                    final Intent activity = new Intent(ScannerActivity.this, MainActivity.class); //UARTConfigurationsActivity.class
                    startActivity(activity);
                    finish();
                    break;
                }
                case BleProfileService.BROADCAST_DEVICE_NOT_SUPPORTED: {
                    Log.v(TAG, "------------- DEVICE NOT SUPPORTED -------------");
                    Toast.makeText(ScannerActivity.this, R.string.devices_list_device_not_supported, Toast.LENGTH_SHORT).show();
                    mDeviceAdapter.setConnectingPosition(-1);
                    break;
                }
                case BleProfileService.BROADCAST_ERROR: {
                    final String message = intent.getStringExtra(BleProfileService.EXTRA_ERROR_MESSAGE);
                    // final int errorCode = intent.getIntExtra(BleProfileService.EXTRA_ERROR_CODE, 0);
                    Toast.makeText(ScannerActivity.this, message, Toast.LENGTH_SHORT).show();
                    // TODO error handing
                    break;
                }
                case BleProfileService.BROADCAST_BOND_STATE: {
                    mDeviceAdapter.notifyDataSetChanged(); // TODO check this. Bonding was never tested.
                    break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_with_header);

        // Get the list component from the layout of the activity
        final WearableListView listView = (WearableListView) findViewById(R.id.devices_list);
        listView.setAdapter(mDeviceAdapter = new DevicesAdapter(listView));
        listView.setClickListener(mOnRowClickListener);
        listView.addOnScrollListener(mOnScrollListener);

        // The header will be moved as the list is scrolled
        mHeader = findViewById(R.id.header);

        // Register a broadcast receiver that will listen for events from the service.
        LocalBroadcastManager.getInstance(this).registerReceiver(mServiceBroadcastReceiver, BleProfileService.makeIntentFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mServiceBroadcastReceiver);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        Log.v(TAG, "onRequestPermissionsResult " + requestCode);
        switch (requestCode) {
            case PERMISSION_REQUEST_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mDeviceAdapter.startLeScan();
                } else {
                    Toast.makeText(ScannerActivity.this, "Location permission required for bluetooth to work", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;

            case PERMISSION_READ_CONTACTS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(ScannerActivity.this, "Read contacts are required for the addressbook to work", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.v(TAG, "onResume");

        if (FlicktekManager.isConnected()) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra(BleProfileService.EXTRA_DEVICE_ADDRESS, "Relaunch");

            // If we have a notification we just pass it on to the application!
            try {
                Bundle config = getIntent().getExtras();
                String notification = config.getString(Constants.FLICKTEK_CLIP.NOTIFICATION_KEY_ID);
                if (notification != null)
                    intent.putExtra(Constants.FLICKTEK_CLIP.NOTIFICATION_KEY_ID, notification);
            } catch (Exception e) {
                //e.printStackTrace();
            }

            startActivity(intent);
            Log.v(TAG, "Finish and remove activity");
            finishAndRemoveTask();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_LOCATION);
                return;
            }

            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_READ_CONTACTS);
                return;
            }

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_LOCATION);
                return;
            }
        }
        mDeviceAdapter.startLeScan();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDeviceAdapter.stopLeScan();
    }

    /**
     * List click listener.
     */
    private WearableListView.ClickListener mOnRowClickListener = new WearableListView.ClickListener() {
        @Override
        public void onClick(final WearableListView.ViewHolder holder) {
            final DevicesAdapter.ItemViewHolder viewHolder = (DevicesAdapter.ItemViewHolder) holder;
            final BluetoothDevice device = viewHolder.getDevice();

            if (device != null) {
                mDeviceAdapter.stopLeScan();
                mDeviceAdapter.setConnectingPosition(holder.getAdapterPosition());

                // Start the service that will connect to selected device
                final Intent service = new Intent(ScannerActivity.this, BleProfileService.class);
                service.putExtra(BleProfileService.EXTRA_DEVICE_ADDRESS, device.getAddress());
                startService(service);
            } else {
                mDeviceAdapter.startLeScan();
            }
        }

        @Override
        public void onTopEmptyRegionClick() {
            // do nothing
        }
    };

    /**
     * The following code ensures that the title scrolls as the user scrolls up or down the list/
     */
    private WearableListView.OnScrollListener mOnScrollListener = new WearableListView.OnScrollListener() {
        @Override
        public void onAbsoluteScrollChange(final int i) {
            if (i > 0)
                mHeader.setY(-i);
            else
                mHeader.setY(0);
        }

        @Override
        public void onScroll(final int i) {
            // Placeholder
        }

        @Override
        public void onScrollStateChanged(final int i) {
            // Placeholder
        }

        @Override
        public void onCentralPositionChanged(final int i) {
            // Placeholder
        }
    };
}
