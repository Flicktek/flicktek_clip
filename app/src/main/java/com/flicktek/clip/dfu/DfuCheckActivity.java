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
package com.flicktek.clip.dfu;

import android.Manifest;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.flicktek.clip.AppHelpFragment;
import com.flicktek.clip.PermissionRationaleFragment;
import com.flicktek.clip.R;
import com.flicktek.clip.dfu.fragment.UploadCancelFragment;
import com.flicktek.clip.dfu.settings.SettingsActivity;
import com.flicktek.clip.dfu.settings.SettingsFragment;
import com.flicktek.clip.scanner.ScannerFragment;
import com.flicktek.clip.utility.FileHelper;

import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuProgressListenerAdapter;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

/**
 * DfuActivity is the main DFU activity It implements DFUManagerCallbacks to receive callbacks from DFUManager class It implements
 * DeviceScannerFragment.OnDeviceSelectedListener callback to receive callback when device is selected from scanning dialog The activity supports portrait and
 * landscape orientations
 */
public class DfuCheckActivity extends AppCompatActivity implements ScannerFragment.OnDeviceSelectedListener,
		UploadCancelFragment.CancelFragmentListener {

	private static final String TAG = "DfuActivity";

	private static final String FIRMWARE_VERSION = "v290317-1025";
	private static final int FIRMWARE_RAW_RESOURCE = R.raw.clip_firmware;

	private static final String PREFS_DEVICE_NAME = "com.flicktek.clip.dfu.PREFS_DEVICE_NAME";

	private static final String DATA_DEVICE = "device";
	private static final String DATA_STATUS = "status";

	private static final int ENABLE_BT_REQ = 0;

	private TextView mDeviceNameView;
	private TextView mDeviceFirmwareView;
	private TextView mInstallVersionView;

	private TextView mTextPercentage;
	private TextView mTextUploading;
	private ProgressBar mProgressBar;

	private Button mUploadButton;

	private BluetoothDevice mSelectedDevice;

	private final DfuProgressListener mDfuProgressListener = new DfuProgressListenerAdapter() {
		@Override
		public void onDeviceConnecting(final String deviceAddress) {
			mProgressBar.setIndeterminate(true);
			mTextPercentage.setText(R.string.dfu_status_connecting);
		}

		@Override
		public void onDfuProcessStarting(final String deviceAddress) {
			mProgressBar.setIndeterminate(true);
			mTextPercentage.setText(R.string.dfu_status_starting);
		}

		@Override
		public void onEnablingDfuMode(final String deviceAddress) {
			mProgressBar.setIndeterminate(true);
			mTextPercentage.setText(R.string.dfu_status_switching_to_dfu);
		}

		@Override
		public void onFirmwareValidating(final String deviceAddress) {
			mProgressBar.setIndeterminate(true);
			mTextPercentage.setText(R.string.dfu_status_validating);
		}

		@Override
		public void onDeviceDisconnecting(final String deviceAddress) {
			mProgressBar.setIndeterminate(true);
			mTextPercentage.setText(R.string.dfu_status_disconnecting);
		}

		@Override
		public void onDfuCompleted(final String deviceAddress) {
			mTextPercentage.setText(R.string.dfu_status_completed);
			// let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					onTransferCompleted();

					// if this activity is still open and upload process was completed, cancel the notification
					final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					manager.cancel(DfuService.NOTIFICATION_ID);
				}
			}, 200);
		}

		@Override
		public void onDfuAborted(final String deviceAddress) {
			mTextPercentage.setText(R.string.dfu_status_aborted);
			// let's wait a bit until we cancel the notification. When canceled immediately it will be recreated by service again.
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					onUploadCanceled();

					// if this activity is still open and upload process was completed, cancel the notification
					final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					manager.cancel(DfuService.NOTIFICATION_ID);
				}
			}, 200);
		}

		@Override
		public void onProgressChanged(final String deviceAddress, final int percent, final float speed, final float avgSpeed, final int currentPart, final int partsTotal) {
			mProgressBar.setIndeterminate(false);
			mProgressBar.setProgress(percent);
			mTextPercentage.setText(getString(R.string.dfu_uploading_percentage, percent));
			if (partsTotal > 1)
				mTextUploading.setText(getString(R.string.dfu_status_uploading_part, currentPart, partsTotal));
			else
				mTextUploading.setText(R.string.dfu_status_uploading);
		}

		@Override
		public void onError(final String deviceAddress, final int error, final int errorType, final String message) {
			showErrorMessage(message);

			// We have to wait a bit before canceling notification. This is called before DfuService creates the last notification.
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					// if this activity is still open and upload process was completed, cancel the notification
					final NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					manager.cancel(DfuService.NOTIFICATION_ID);
				}
			}, 200);
		}
	};

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_feature_dfu_automatic_update);
		isBLESupported();
		if (!isBLEEnabled()) {
			showBLEDialog();
		}
		setGUI();

		// Try to create sample files
		if (FileHelper.newSamplesAvailable(this)) {
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
				FileHelper.createSamples(this);
			} else {
				final DialogFragment dialog = PermissionRationaleFragment.getInstance(R.string.permission_sd_text, Manifest.permission.WRITE_EXTERNAL_STORAGE);
				dialog.show(getSupportFragmentManager(), null);
			}
		}

		// restore saved state
		if (savedInstanceState != null) {
			mSelectedDevice = savedInstanceState.getParcelable(DATA_DEVICE);
			mUploadButton.setEnabled(true);
		}
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelable(DATA_DEVICE, mSelectedDevice);
	}

	private void setGUI() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
        setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		toolbar.setVisibility(View.GONE);

		mDeviceNameView = (TextView) findViewById(R.id.device_name);
		mInstallVersionView = (TextView) findViewById(R.id.install_firmware_version);
		mInstallVersionView.setText(FIRMWARE_VERSION);

		mDeviceFirmwareView = (TextView) findViewById(R.id.device_firmware_version);


		mUploadButton = (Button) findViewById(R.id.action_upload);
		mUploadButton.setEnabled(true);

		mTextPercentage = (TextView) findViewById(R.id.textviewProgress);
		mTextUploading = (TextView) findViewById(R.id.textviewUploading);
		mProgressBar = (ProgressBar) findViewById(R.id.progressbar_file);

		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		if (isDfuServiceRunning()) {
			// Restore image file information
			mDeviceNameView.setText(preferences.getString(PREFS_DEVICE_NAME, ""));
			mDeviceFirmwareView.setText("Version N/A");
			showProgressBar();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		DfuServiceListenerHelper.registerProgressListener(this, mDfuProgressListener);
	}

	@Override
	protected void onPause() {
		super.onPause();
		DfuServiceListenerHelper.unregisterProgressListener(this, mDfuProgressListener);
	}

	private void isBLESupported() {
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			showToast(R.string.no_ble);
			finish();
		}
	}

	private boolean isBLEEnabled() {
		final BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
		final BluetoothAdapter adapter = manager.getAdapter();
		return adapter != null && adapter.isEnabled();
	}

	private void showBLEDialog() {
		final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(enableIntent, ENABLE_BT_REQ);
	}

	private void showDeviceScanningDialog() {
		final ScannerFragment dialog = ScannerFragment.getInstance(null); // Device that is advertising directly does not have the GENERAL_DISCOVERABLE nor LIMITED_DISCOVERABLE flag set.
		dialog.show(getSupportFragmentManager(), "scan_fragment");
	}

	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		getMenuInflater().inflate(R.menu.settings_and_about, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();
				break;
			case R.id.action_about:
				final AppHelpFragment fragment = AppHelpFragment.getInstance(R.string.dfu_about_text);
				fragment.show(getSupportFragmentManager(), "help_fragment");
				break;
			case R.id.action_settings:
				final Intent intent = new Intent(this, SettingsActivity.class);
				startActivity(intent);
				break;
		}
		return true;
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (resultCode != RESULT_OK)
			return;
	}

	/**
	 * Callback of UPDATE/CANCEL button on DfuActivity
	 */
	public void onUpload() {
		// Save current state in order to restore it if user quit the Activity
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		final SharedPreferences.Editor editor = preferences.edit();
		editor.putString(PREFS_DEVICE_NAME, mSelectedDevice.getName());
		editor.apply();

		showProgressBar();

		final boolean keepBond = preferences.getBoolean(SettingsFragment.SETTINGS_KEEP_BOND, false);
		final boolean forceDfu = preferences.getBoolean(SettingsFragment.SETTINGS_ASSUME_DFU_NODE, true);
		boolean enablePRNs = preferences.getBoolean(SettingsFragment.SETTINGS_PACKET_RECEIPT_NOTIFICATION_ENABLED, Build.VERSION.SDK_INT < Build.VERSION_CODES.M);
		String value = preferences.getString(SettingsFragment.SETTINGS_NUMBER_OF_PACKETS, String.valueOf(4));
		int numberOfPackets;
		try {
			numberOfPackets = Integer.parseInt(value);
		} catch (final NumberFormatException e) {
			numberOfPackets = DfuServiceInitiator.DEFAULT_PRN_VALUE;
		}

		// This doesn't work on the version of the firmware we have v132
		//numberOfPackets = 4;
		//enablePRNs = true;

		final DfuServiceInitiator starter = new DfuServiceInitiator(mSelectedDevice.getAddress())
				.setDeviceName(mSelectedDevice.getName())
				.setKeepBond(keepBond)
				.setForceDfu(forceDfu)
				.setPacketsReceiptNotificationsEnabled(enablePRNs)
				.setPacketsReceiptNotificationsValue(numberOfPackets)
				.setUnsafeExperimentalButtonlessServiceInSecureDfuEnabled(false);

		starter.setZip(FIRMWARE_RAW_RESOURCE);
		starter.start(this, DfuService.class);
	}

	private void showUploadCancelDialog() {
		final LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
		final Intent pauseAction = new Intent(DfuService.BROADCAST_ACTION);
		pauseAction.putExtra(DfuService.EXTRA_ACTION, DfuService.ACTION_PAUSE);
		manager.sendBroadcast(pauseAction);

		final UploadCancelFragment fragment = UploadCancelFragment.getInstance();
		fragment.show(getSupportFragmentManager(), TAG);
	}

	/**
	 * Callback of CONNECT/DISCONNECT button on DfuActivity
	 */
	public void onConnectClicked(final View view) {
		if (isDfuServiceRunning()) {
			showUploadCancelDialog();
			return;
		}

		if (isBLEEnabled()) {
			showDeviceScanningDialog();
		} else {
			showBLEDialog();
		}
	}

	@Override
	public void onDeviceSelected(final BluetoothDevice device, final String name, final String version) {
		mSelectedDevice = device;
		mDeviceNameView.setText(name != null ? name : getString(R.string.not_available));
		mDeviceNameView.setVisibility(View.VISIBLE);

		mDeviceFirmwareView.setText(version);
		onUpload();
	}

	@Override
	public void onDialogCanceled() {
		// do nothing
	}

	private void showProgressBar() {
		mProgressBar.setVisibility(View.VISIBLE);
		mTextPercentage.setVisibility(View.VISIBLE);
		mTextPercentage.setText(null);
		mTextUploading.setText(R.string.dfu_status_uploading);
		mTextUploading.setVisibility(View.VISIBLE);
		mUploadButton.setEnabled(true);
		mUploadButton.setText(R.string.dfu_action_upload_cancel);
	}

	private void onTransferCompleted() {
		clearUI(false);
		//showToast(R.string.dfu_success);
		mTextUploading.setText("Transfer completed!");
	}

	public void onUploadCanceled() {
		clearUI(false);
		//showToast(R.string.dfu_aborted);
		mTextUploading.setText("Cancelled!");
	}

	@Override
	public void onCancelUpload() {
		mProgressBar.setIndeterminate(true);
		mTextUploading.setText(R.string.dfu_status_aborting);
		mTextPercentage.setText(null);
	}

	private void showErrorMessage(final String message) {
		clearUI(false);
		//showToast("Upload failed: " + message);
		mUploadButton.setEnabled(true);
		mTextUploading.setText("Failed " + message);
	}

	private void clearUI(final boolean clearDevice) {
		mProgressBar.setVisibility(View.INVISIBLE);
		mTextPercentage.setVisibility(View.INVISIBLE);
		mUploadButton.setEnabled(true);
		mUploadButton.setText(R.string.dfu_action_upload);
		if (clearDevice) {
			mSelectedDevice = null;
		}
		//mDeviceNameView.setText(R.string.dfu_default_name);
	}

	private void showToast(final int messageResId) {
		Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
	}

	private void showToast(final String message) {
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
	}

	private boolean isDfuServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (DfuService.class.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}
}
