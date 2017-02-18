/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flicktek.android.clip;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.flicktek.android.clip.dropbox.Dropbox;
import com.flicktek.android.clip.wearable.WearListenerService;
import com.flicktek.android.clip.wearable.common.Constants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataApi.DataItemResult;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageApi.SendMessageResult;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Receives its own events using a listener API designed for foreground activities. Updates a data
 * item every second while it is open. Also allows user to take a photo and send that as an asset
 * to the paired wearable.
 */
public class LaunchActivity extends Activity implements
        CapabilityApi.CapabilityListener,
        MessageApi.MessageListener,
        DataApi.DataListener,
        ConnectionCallbacks,
        OnConnectionFailedListener {

    private static final String TAG = "LaunchActivity";

    final int REQUEST_CODE = 1337;

    //Request code for launching the Intent to resolve Google Play services errors.
    private static final int REQUEST_RESOLVE_ERROR = 1000;

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;
    private boolean mCameraSupported = false;

    private ListView mDataItemList;
    private Button mSendPhotoBtn;

    /* Test to send an intent into a different application */
    private Button mBroadcastIntentBtn;

    private ImageView mThumbView;
    private Bitmap mImageBitmap;
    private View mStartActivityBtn;
    private TextView mCount;

    private DataItemAdapter mDataItemListAdapter;

    // Send DataItems.
    private ScheduledExecutorService mGeneratorExecutor;
    private ScheduledFuture<?> mDataItemGeneratorFuture;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LOGD(TAG, "onCreate");
        mCameraSupported = getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        setContentView(R.layout.main_activity);
        setupViews();

        // Stores DataItems received by the local broadcaster or from the paired watch.
        mDataItemListAdapter = new DataItemAdapter(this, R.layout.list_item);
        mDataItemList.setAdapter(mDataItemListAdapter);

        mGeneratorExecutor = new ScheduledThreadPoolExecutor(1);

        try {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        } catch (Exception e) {
            LOGD(TAG, "No wearable support");
        }
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        WearListenerService.mApplicationActive = true;
        mDataItemGeneratorFuture = mGeneratorExecutor.scheduleWithFixedDelay(
                new DataItemGenerator(), 1, 30, TimeUnit.SECONDS);
    }

    @Override
    public void onPause() {
        super.onPause();
        WearListenerService.mApplicationActive = false;
        mDataItemGeneratorFuture.cancel(true /* mayInterruptIfRunning */);
    }

    @Override
    protected void onStop() {
        if (!mResolvingError && (mGoogleApiClient != null) && (mGoogleApiClient.isConnected())) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.CapabilityApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            mImageBitmap = (Bitmap) extras.get("data");
            mThumbView.setImageBitmap(mImageBitmap);
        }

        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                String res = ClipIntents.result(data);
                mDataItemListAdapter.add(new Event("ClipIntents", res));
            } else {
                mDataItemListAdapter.add(new Event("ClipIntents", "Failed " + resultCode));
            }
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        LOGD(TAG, "Google API Client was connected");
        mResolvingError = false;
        mStartActivityBtn.setEnabled(true);
        mSendPhotoBtn.setEnabled(mCameraSupported);
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.CapabilityApi.addListener(
                mGoogleApiClient, this, Uri.parse("wear://"), CapabilityApi.FILTER_REACHABLE);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        LOGD(TAG, "Connection to Google API client was suspended");
        mStartActivityBtn.setEnabled(false);
        mSendPhotoBtn.setEnabled(false);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (!mResolvingError) {

            if (result.hasResolution()) {
                try {
                    mResolvingError = true;
                    result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
                } catch (IntentSender.SendIntentException e) {
                    // There was an error with the resolution intent. Try again.
                    mGoogleApiClient.connect();
                }
            } else {
                Log.e(TAG, "Connection to Google API client has failed");
                mResolvingError = false;
                mStartActivityBtn.setEnabled(false);
                mSendPhotoBtn.setEnabled(false);
                Wearable.DataApi.removeListener(mGoogleApiClient, this);
                Wearable.MessageApi.removeListener(mGoogleApiClient, this);
                Wearable.CapabilityApi.removeListener(mGoogleApiClient, this);

                onStartBluetoothScanActivityClick(null);
                Toast.makeText(getApplicationContext(), "Update services", Toast.LENGTH_LONG);
            }
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        LOGD(TAG, "onDataChanged: " + dataEvents);

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                //mDataItemListAdapter.add(new Event("DataItem Changed", event.getDataItem().toString()));
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                //mDataItemListAdapter.add(new Event("DataItem Deleted", event.getDataItem().toString()));
            }
        }
    }

    static int mCountNumber = 0;

    @Override
    public void onMessageReceived(final MessageEvent messageEvent) {
        LOGD(TAG, "onMessageReceived() A message from watch was received:"
                + messageEvent.getRequestId() + " " + messageEvent.getPath());

        String text = new String(messageEvent.getData());
        String path = messageEvent.getPath();

        int value = -1;
        boolean isNumber = false;

        if (path.equals("/data-item-received")) {
            mCountNumber++;
            mCount.setText(Integer.toString(mCountNumber));
            return;
        }

        try {
            value = Integer.valueOf(text);
            isNumber = true;
        } catch (Exception e) {
            Log.d(TAG, "onMessageReceived Not a number [" + path + "] " + text);
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

                ClipIntents.openBroadcastIntent(this, ClipIntents.ACTION_URI_GESTURE, gesture);
            }

            mDataItemListAdapter.add(new Event(path, Integer.toString(value)));
        } else {
            if (isNumber) {
                mDataItemListAdapter.add(new Event(path, Integer.toString(value)));
            } else {
                mDataItemListAdapter.add(new Event(path, text));
            }
            Log.d(TAG, "onMessageReceived path:" + path + "; message:" + text);
        }
    }

    @Override
    public void onCapabilityChanged(final CapabilityInfo capabilityInfo) {
        LOGD(TAG, "onCapabilityChanged: " + capabilityInfo);

        mDataItemListAdapter.add(new Event("onCapabilityChanged", capabilityInfo.toString()));
    }

    /**
     * Sets up UI components and their callback handlers.
     */
    private void setupViews() {
        mSendPhotoBtn = (Button) findViewById(R.id.sendPhoto);
        mThumbView = (ImageView) findViewById(R.id.imageView);
        mDataItemList = (ListView) findViewById(R.id.data_item_list);
        if (BuildConfig.DEBUG) {
            displayVersion();
            mDataItemList.setVisibility(View.VISIBLE);
        }

        mStartActivityBtn = findViewById(R.id.start_wearable_activity);
        mCount = (TextView) findViewById(R.id.tv_count);
    }

    public void onTakePhotoClick(View view) {
        dispatchTakePictureIntent();
    }

    public void onSendPhotoClick(View view) {
        if (null != mImageBitmap && mGoogleApiClient.isConnected()) {
            sendPhoto(toAsset(mImageBitmap));
        }
    }

    private void sendStartActivityMessage(String node) {
        Wearable.MessageApi.sendMessage(
                mGoogleApiClient, node, Constants.FLICKTEK_CLIP.START_ACTIVITY_PATH, new byte[0]).setResultCallback(
                new ResultCallback<SendMessageResult>() {
                    @Override
                    public void onResult(SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                        }
                    }
                }
        );
    }

    /**
     * Dispatches an {@link Intent} to take a photo. Result will be returned back
     * in onActivityResult().
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    /**
     * Builds an {@link com.google.android.gms.wearable.Asset} from a bitmap. The image that we get
     * back from the camera in "data" is a thumbnail size. Typically, your image should not exceed
     * 320x320 and if you want to have zoom and parallax effect in your app, limit the size of your
     * image to 640x400. Resize your image before transferring to your wearable device.
     */
    private static Asset toAsset(Bitmap bitmap) {
        ByteArrayOutputStream byteStream = null;
        try {
            byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            return Asset.createFromBytes(byteStream.toByteArray());
        } finally {
            if (null != byteStream) {
                try {
                    byteStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Sends the asset that was created from the photo we took by adding it to the Data Item store.
     */
    private void sendPhoto(Asset asset) {
        PutDataMapRequest dataMap = PutDataMapRequest.create(Constants.FLICKTEK_CLIP.IMAGE_PATH);
        dataMap.getDataMap().putAsset(Constants.FLICKTEK_CLIP.IMAGE_KEY, asset);
        dataMap.getDataMap().putLong("time", new Date().getTime());
        PutDataRequest request = dataMap.asPutDataRequest();
        request.setUrgent();

        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataItemResult>() {
                    @Override
                    public void onResult(DataItemResult dataItemResult) {
                        LOGD(TAG, "Sending image was successful: " + dataItemResult.getStatus()
                                .isSuccess());
                    }
                });
    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();

        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }

        return results;
    }

    /**
     * As simple wrapper around Log.d
     */
    private static void LOGD(final String tag, String message) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message);
        }
    }

    public void onSendIntentClick(View view) {
        ClipIntents.openBroadcastIntent(this, ClipIntents.ACTION_URI_GESTURE,
                ClipIntents.EXTRA_GESTURE_ENTER);
    }

    public void onSendIntentWakeupClick(View view) {
        ClipIntents.openBroadcastIntent(this, ClipIntents.ACTION_URI_WAKEUP,
                ClipIntents.EXTRA_GESTURE_ENTER);
    }

    /**
     * Sends an RPC to start a fullscreen Activity on the wearable.
     */
    public void onStartWearableActivityClick(View view) {
        LOGD(TAG, "Generating RPC");

        // Trigger an AsyncTask that will query for a list of connected nodes and send a
        // "start-activity" message to each connected node.
        new StartWearableActivityTask().execute();

        Intent startIntent = new Intent(this, MainActivity.class);
        Bundle bundle = new Bundle();

        bundle.putString("launch", "WebviewFragment");
        startIntent.putExtras(bundle);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startIntent);
    }

    public void onStartVideoActivityClick(View view) {
        Log.v(TAG, "Launch application");
        Intent startIntent = new Intent(this, VideoActivity.class);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startIntent);
    }

    public void onStartSlidesActivityClick(View view) {
        Log.v(TAG, "Launch application");
        Intent startIntent = new Intent(this, MainActivity.class);
        Bundle bundle = new Bundle();
        // We launch a fragment class
        bundle.putString("launch", "com.flicktek.android.clip.slides");
        startIntent.putExtras(bundle);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startIntent);
    }

    public void onStartBluetoothScanActivityClick(View view) {
        Log.v(TAG, "Launch application");
        Intent startIntent = new Intent(this, MainActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("launch", "BluetoothConnect");
        startIntent.putExtras(bundle);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startIntent);
    }

    public void onStartDropboxActivityClick(View view) {
        Log.v(TAG, "Launch dropbox link application");
        Intent startIntent = new Intent(this, Dropbox.class);
        Bundle bundle = new Bundle();
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startIntent);
    }

    public void onStartGesturesActivityClick(View view) {
        Log.v(TAG, "Launch application");
        Intent startIntent = new Intent(this, MainActivity.class);
        Bundle bundle = new Bundle();
        // We launch a fragment class
        bundle.putString("launch", "menus.AnimatedGestures");
        startIntent.putExtras(bundle);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startIntent);
    }

    public void onConnectivityActivityClick(View view) {
        Log.v(TAG, "Launch application");
        Intent startIntent = new Intent(this, MainActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("json", "menu_connectivity");
        startIntent.putExtras(bundle);
        startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startIntent);
    }

    String version = "unknown";

    public void displayVersion() {
        PackageManager manager = getPackageManager();
        PackageInfo info = null;

        try {
            info = manager.getPackageInfo(
                    getPackageName(), 0);

            version = info.versionName;

            if (BuildConfig.DEBUG)
                version += " DEBUG";

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        Toast.makeText(getApplicationContext(),
                "Version " + version,
                Toast.LENGTH_LONG).show();
    }

    public void onVersionClick(View view) {
        displayVersion();
        if (mDataItemList.getVisibility() == View.INVISIBLE) {
            mDataItemList.setVisibility(View.VISIBLE);
            mDataItemListAdapter.add(new Event("Version", version));
        } else {
            mDataItemListAdapter.clear();
            mDataItemList.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * A View Adapter for presenting the Event objects in a list
     */
    private static class DataItemAdapter extends ArrayAdapter<Event> {

        private final Context mContext;

        public DataItemAdapter(Context context, int unusedResource) {
            super(context, unusedResource);
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_item, null);
                convertView.setTag(holder);
                holder.text1 = (TextView) convertView.findViewById(android.R.id.text1);
                holder.text2 = (TextView) convertView.findViewById(android.R.id.text2);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Event event = getItem(position);
            holder.text1.setText(event.title);
            holder.text2.setText(event.text);
            return convertView;
        }

        private class ViewHolder {
            TextView text1;
            TextView text2;
        }
    }

    private class Event {

        String title;
        String text;

        public Event(String title, String text) {
            this.title = title;
            this.text = text;
        }
    }

    private class StartWearableActivityTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... args) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                sendStartActivityMessage(node);
            }
            return null;
        }
    }

    /**
     * Generates a DataItem based on an incrementing count.
     */
    private class DataItemGenerator implements Runnable {
        private int count = 0;

        @Override
        public void run() {

            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Constants.FLICKTEK_CLIP.COUNT_PATH);
            putDataMapRequest.getDataMap().putInt(Constants.FLICKTEK_CLIP.COUNT_KEY, count++);
            PutDataRequest request = putDataMapRequest.asPutDataRequest();
            request.setUrgent();
            LOGD(TAG, "Generating DataItem: " + request);
            if (!mGoogleApiClient.isConnected()) {
                return;
            }
            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataItemResult>() {
                        @Override
                        public void onResult(DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.e(TAG, "ERROR: failed to putDataItem, status code: "
                                        + dataItemResult.getStatus().getStatusCode());
                            }
                        }
                    });
        }
    }
}