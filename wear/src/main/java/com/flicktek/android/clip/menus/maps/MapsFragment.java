package com.flicktek.android.clip.menus.maps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.wearable.view.DismissOverlayView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.flicktek.android.clip.FlicktekCommands;
import com.flicktek.android.clip.FlicktekManager;
import com.flicktek.android.clip.MainActivity;
import com.flicktek.android.clip.R;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class MapsFragment extends MapFragment implements OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener, LocationListener {
    private String TAG = "Maps";

    private MainActivity mainActivity;

    private static final LatLng FLICKTEK = new LatLng(51.5074, 0.1278);

    /**
     * Overlay that shows a short help text when first launched. It also provides an option to
     * exit the app.
     */
    private DismissOverlayView mDismissOverlay;

    /**
     * The map. It is initialized when the map has been fully loaded and is ready to be used.
     *
     * @see #onMapReady(com.google.android.gms.maps.GoogleMap)
     */
    private GoogleMap mMap;
    private MapFragment mMapFragment;
    private LocationManager locationManager;

    private static final long MIN_TIME = 400;
    private static final float MIN_DISTANCE = 1000;

    private float zoomLevel = 10;
    private Location myLocation;

    //fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        mainActivity = (MainActivity) getActivity();
        Typeface mainFont = Typeface.createFromAsset(mainActivity.getAssets(), getString(R.string.main_font));

        // Obtain the DismissOverlayView and display the intro help text.
        mDismissOverlay = (DismissOverlayView) mainActivity.findViewById(R.id.dismiss_overlay);
        mDismissOverlay.setIntroText("Long click to exit map");
        mDismissOverlay.showIntroIfNecessary();

        // Obtain the MapFragment and set the async listener to be notified when the map is ready.
        getMapAsync(this);

        mainActivity = (MainActivity) getActivity();
        locationManager = (LocationManager) mainActivity.getSystemService(mainActivity.LOCATION_SERVICE);

        //You can also use LocationManager.GPS_PROVIDER and LocationManager.PASSIVE_PROVIDER
        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return rootView;
        } else {
            if (locationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER))
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME, MIN_DISTANCE, this);

            if (locationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER))
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }

        return rootView;
    }

    public void close() {
        Log.d(TAG, "close: ");
        FlicktekManager.backMenu(mainActivity);
    }

    @Subscribe
    public void onGesturePerformed(FlicktekCommands.onGestureEvent gestureEvent) {
        Log.d(TAG, "onGesturePerformed: ");
        final int gesture = gestureEvent.status;
        mainActivity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    switch (gesture) {
                        case (FlicktekManager.GESTURE_UP):
                            zoomLevel = zoomLevel + 2;
                            mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomLevel));
                            break;
                        case (FlicktekManager.GESTURE_DOWN):
                            zoomLevel = zoomLevel - 2;
                            mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomLevel));
                            break;
                        case (FlicktekManager.GESTURE_ENTER):
                            myLocation = LocationServices.FusedLocationApi.
                                    getLastLocation(mainActivity.mGoogleApiClient);

                            Log.d(TAG, "handleGesture: ENTER");
                            if (myLocation == null) {
                                Log.d(TAG, "handleGesture: NULL");
                                break;
                            }
                            mMap.animateCamera(CameraUpdateFactory.
                                    newLatLngZoom(new LatLng(myLocation.getLatitude(),
                                            myLocation.getLongitude()), zoomLevel));
                            break;
                        case (FlicktekManager.GESTURE_HOME):
                            close();
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void onResume() {
        Log.d(TAG, "onResume: ");
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause: ");
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    public void onClick(View _view) {
        Log.d(TAG, "onClick: ");
    }

    @Override
    public void onLocationChanged(Location location) {
        myLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 10);
        mMap.animateCamera(cameraUpdate);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));

        locationManager.removeUpdates(this);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Log.v(TAG, "Dismiss overlay");
        mDismissOverlay.show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Map is ready to be used.
        mMap = googleMap;

        // Set the long click listener as a way to exit the map.
        mMap.setOnMapLongClickListener(this);

        // Add a marker with a title that is shown in its info window.
        mMap.addMarker(new MarkerOptions().position(FLICKTEK)
                .title("FlickTek"));

        mMap.setMyLocationEnabled(true);

        Location location = mMap.getMyLocation();

        if (location == null) {
            Log.d(TAG, "onMapReady: Flicktek");
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(FLICKTEK, 10));
            return;
        }

        // Move the camera to show the marker.
        LatLng myposition = new LatLng(location.getLatitude(), location.getLongitude());

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myposition, 10));
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(SYDNEY, 10));
    }
}
