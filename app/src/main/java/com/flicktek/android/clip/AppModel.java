package com.flicktek.android.clip;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

public class AppModel {
    private static final String TAG = "AppModel";

    // Actions resulting from running the item on the menu
    public static final int NO_VIEW = -1;
    public static final int BACK_APPLICATION = -2;
    public static final int RUN_PACKAGE = -3;

    private String name;
    private String packageName;
    private Drawable icon;
    private boolean isSelected;
    private JSONObject json;
    private int viewId;

    public AppModel(String _name, String _packageName, Drawable _icon, int _viewId) {
        this.name = _name;
        this.packageName = _packageName;
        this.icon = _icon;
        this.viewId = _viewId;
    }

    public String getName() {
        return name;
    }

    public Drawable getIcon() {
        return icon;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setSelected(boolean _isSelected) {
        isSelected = _isSelected;
    }

    public void setConfiguration(JSONObject jsonObj) {
        json = jsonObj;
    }

    public JSONObject getConfiguration() {
        return json;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public int getViewId() {
        return viewId;
    }

    @Nullable
    public String getAction() {
        if (json == null)
            return null;

        String action = null;
        try {
            action = json.getString("action");
        } catch (JSONException e) {
            Log.v(TAG, "No action on object");
            return null;
        }
        return action;
    }

    @Nullable
    public String getTarget() {
        if (json == null)
            return null;

        String target = null;
        try {
            target = json.getString("target");
        } catch (JSONException e) {
            Log.v(TAG, "No target on object");
            return null;
        }
        return target;
    }

    public void back() {
        Log.v(TAG, "Back");
    }

    public void performAction(MainActivity mainActivity) {
        int viewId = getViewId();

        switch (viewId) {
            case AppModel.BACK_APPLICATION:
                back();
                return;
            case AppModel.RUN_PACKAGE:
                String appToLaunch = getPackageName();
                if (appToLaunch != null) {
                    PackageManager pm = mainActivity.getPackageManager();
                    Intent intent = pm.getLaunchIntentForPackage(appToLaunch);
                    mainActivity.startActivity(intent);
                    return;
                }
        }

        if (viewId != AppModel.NO_VIEW) {

            return;
        }

        if (getConfiguration() != null) {
            String target = getTarget();
            if (target != null) {
                if (target.compareTo("media_menu") == 0) {
                    //mainActivity.newMediaFragment(this);
                } else
                if (target.compareTo("fragment_class") == 0) {
                    mainActivity.newFragment(this);
                }

                return;
            }

            String action = getAction();
            if (action != null) {
                // TODO Run intent
                Toast.makeText(mainActivity, "Launching intent " + action, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Toast.makeText(mainActivity, "Missing what to do with current item ", Toast.LENGTH_SHORT).show();
    }

    /**
     * Returns the name of a class that we would like to build to jump into
     * @return The class doesn't contain the package name, the dev has to append it PACKAGE_NAME + Class
     */
    @Nullable
    public String getFragmentClass() {
        if (json == null)
            return null;

        String target = null;
        try {
            target = json.getString("fragment_class");
        } catch (JSONException e) {
            Log.v(TAG, "No target on object");
            return null;
        }
        return target;
    }
}