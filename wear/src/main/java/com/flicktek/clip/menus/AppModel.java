package com.flicktek.clip.menus;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.flicktek.clip.FlicktekManager;
import com.flicktek.clip.MainActivity;
import com.flicktek.clip.wearable.common.Constants;

import org.json.JSONException;
import org.json.JSONObject;

public class AppModel {
    private static final String TAG = "AppModel";

    // Actions resulting from running the item on the menu
    public static final int NO_VIEW = -1;
    public static final int BACK_APPLICATION = -2;
    public static final int RUN_PACKAGE = -3;

    // Items on the list
    public static final String TARGET_MEDIA_CONTROLLER = "media_controller";
    public static final String TARGET_REMOTE_ACTIVITY = "remote_activity";
    public static final String TARGET_FRAGMENT_CLASS = "fragment_class";
    public static final String TARGET_MEDIA_LIST = "menu_list";
    public static final String TARGET_CLOSE = "close";

    private String name;
    private String packageName;
    private Drawable icon;
    private boolean isSelected;
    private JSONObject json;
    private int viewId;
    public boolean isHeader;

    public AppModel(String _name, String _packageName, Drawable _icon, int _viewId) {
        this.name = _name;
        this.packageName = _packageName;
        this.icon = _icon;
        this.viewId = _viewId;
    }

    public String getName() {
        if (name == null) {
            return "AppModel";
        }
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
        if (json == null)
            return;

        try {
            isHeader = json.getBoolean("header");
        } catch (JSONException e) {
        }

        try {
            this.name = json.getString("text");
        } catch (JSONException e) {
        }
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

    // Reads a value from the settings and displays that value
    @Nullable
    public String getDataKey() {
        if (json == null)
            return null;
        try {
            return json.getString("data");
        } catch (JSONException e) {
            return null;
        }
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

    @Nullable
    public String getTargetConfigurationJSON() {
        if (json == null)
            return null;

        String jsonTarget = null;
        try {
            jsonTarget = json.getString("json");
        } catch (JSONException e) {
            Log.v(TAG, "No target on object");
            return null;
        }
        return jsonTarget;
    }

    public void performAction(MainActivity mainActivity) {
        int viewId = getViewId();

        switch (viewId) {
            case AppModel.BACK_APPLICATION:
                FlicktekManager.getInstance().backMenu(mainActivity);
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

        if (getConfiguration() == null) {
            Toast.makeText(mainActivity, "Missing what to do with current item ", Toast.LENGTH_SHORT).show();
            return;
        }

        String target = getTarget();
        if (target == null)
            return;

        String action = getAction();
        switch (target) {
            case TARGET_MEDIA_LIST:
                mainActivity.showFragment(
                        MenuFragment.newInstance(
                                this.name,
                                this.getTargetConfigurationJSON()), getName(), false);
                break;
            case TARGET_MEDIA_CONTROLLER:
                mainActivity.newMediaFragment(this);
                break;
            case TARGET_FRAGMENT_CLASS:
                mainActivity.newFragment(this);
                break;
            case TARGET_CLOSE:
                mainActivity.shutdown();
                break;
            case TARGET_REMOTE_ACTIVITY:
                if (action != null) {
                    Toast.makeText(mainActivity, "Launching internal activity on the device " + action, Toast.LENGTH_SHORT).show();
                    mainActivity.sendMessageToHandheld(mainActivity.getApplicationContext(),
                            Constants.FLICKTEK_CLIP.LAUNCH_ACTIVITY, action);
                }
                break;
            default:
                mainActivity.showToastMessage("Don't have a valid target " + target);
                break;
        }
    }

    /**
     * Returns the name of a class that we would like to build to jump into
     *
     * @return The class doesn't contain the package name, the dev has to append it PACKAGE_NAME + Class
     */
    @Nullable
    public String getFragmentClass() {
        if (json == null)
            return null;

        String target = null;
        try {
            target = json.getString("class");
        } catch (JSONException e) {
            Log.v(TAG, "No target on object");
            return null;
        }
        return target;
    }
}