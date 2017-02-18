package com.flicktek.android.clip;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.flicktek.android.clip.util.Helpers;
import com.flicktek.android.clip.wearable.WearListenerService;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class WebviewFragment extends Fragment {
    private static final String TAG = "SlideFragment";
    private static final String ARG_JSON = "JSON";
    private static final String ARG_EXTRA = "EXTRA";
    private static final String JSON_CONFIGURATION = "configuration";

    private MainActivity mainActivity;
    private JSONObject config;

    private WebView webview;

    public static WebviewFragment newInstance(String jsonString, String extra) {
        WebviewFragment myFragment = new WebviewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_JSON, jsonString);
        args.putString(ARG_EXTRA, extra);
        myFragment.setArguments(args);
        return myFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() == null)
            return;

        try {
            String configuration_json = getArguments().getString(ARG_JSON);
            config = Helpers.getJsonFromResources((MainActivity) getActivity(), configuration_json);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed parsing JSON");
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();
        View rootView = null;
        rootView = inflater.inflate(R.layout.fragment_webview, container, false);

        webview = (WebView) rootView.findViewById(R.id.webview);

        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setDatabaseEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false);

        // Disable opening new windows! We don't want to leave our application
        webview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //view.loadUrl(url);
                return true;
            }
        });

        if (getArguments() == null) {
            webview.loadUrl("file:///android_asset/reaction/index.html");
            return rootView;
        }

        try {
            String assets = getArguments().getString("assets");
            if (assets == null) {
                assets = "documentation";
            }

            webview.loadUrl("file:///android_asset/" + assets + "/index.html");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed parsing JSON");
        }

        return rootView;
    }

    @Override
    public void onResume() {
        WearListenerService.mApplicationActive = true;
        EventBus.getDefault().register(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        WearListenerService.mApplicationActive = true;
        EventBus.getDefault().unregister(this);
        super.onPause();
        if (webview != null) {
            webview.destroy();
            webview = null;
        }
    }

    public void close() {

    }

    Timer longTimer;

    synchronized void simulateKey(final int key, long timeout) {
        if (webview == null)
            return;
        webview.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, key));
        if (longTimer != null) {
            longTimer.cancel();
            longTimer = null;
        }

        if (longTimer == null) {
            longTimer = new Timer();
            longTimer.schedule(new TimerTask() {
                public void run() {
                    longTimer.cancel();
                    longTimer = null;
                    if (webview != null)
                        webview.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, key));
                }
            }, timeout);
        }
    }

    public void click_button(int gesture) {
        Log.v(TAG, "Evaluate javascript");
        //webview.evaluateJavascript("on_gesture(" + gesture + ");", null);
        switch (gesture) {
            case FlicktekManager.GESTURE_UP:
                simulateKey(KeyEvent.KEYCODE_DPAD_LEFT, 150);
                webview.pageUp(false);
                break;
            case FlicktekManager.GESTURE_DOWN:
                simulateKey(KeyEvent.KEYCODE_DPAD_RIGHT, 150);
                webview.pageDown(false);
                //webview.evaluateJavascript("window.scrollTo(0,500)", null);
                break;
            case FlicktekManager.GESTURE_ENTER:
                simulateKey(KeyEvent.KEYCODE_ENTER, 150);
                break;
            case FlicktekManager.GESTURE_HOME:
                simulateKey(KeyEvent.KEYCODE_SPACE, 150);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGesturePerformed(final FlicktekCommands.onGestureEvent gestureEvent) {
        click_button(gestureEvent.status);
    }
}
