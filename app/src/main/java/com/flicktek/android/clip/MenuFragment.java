package com.flicktek.android.clip;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.flicktek.android.ConnectionEvents.ConnectedEvent;
import com.flicktek.android.ConnectionEvents.ConnectingEvent;
import com.flicktek.android.ConnectionEvents.DisconnectedEvent;
import com.google.android.gms.analytics.HitBuilders;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

/**
 * Generic menu fragment inheritable
 */
public class MenuFragment extends Fragment implements AdapterView.OnItemClickListener {
    protected static final String ARG_JSON_NAME = "jsonName";
    protected static final String ARG_MENU_NAME = "menuName";

    private String TAG = "MenuFragment";
    protected MainActivity mainActivity;

    protected ListView lvMenu;
    protected MenuAdapter menuAdapter;
    protected int menuIndex;
    protected AppModel menuSelectedModel;
    protected TextView tv_current_menu;

    private TextView tv_battery;
    private ImageView iv_battery;
    private LinearLayout ll_battery;

    protected String jsonName = null;
    protected String menuName = null;

    // Default bundle constructor as google best practices

    public static MenuFragment newInstance(String menuName, String jsonName) {
        MenuFragment myFragment = new MenuFragment();

        Bundle args = new Bundle();
        args.putString(ARG_JSON_NAME, jsonName);
        args.putString(ARG_MENU_NAME, menuName);
        myFragment.setArguments(args);
        return myFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        jsonName = getArguments().getString(ARG_JSON_NAME);
        menuName = getArguments().getString(ARG_MENU_NAME);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        init();

        View rootView;
        rootView = inflater.inflate(R.layout.fragment_dashboard_rect, container, false);

        if (menuName != null) {
            tv_current_menu = (TextView) rootView.findViewById(R.id.tv_current_menu);
            tv_current_menu.setText(menuName);
        } else {
            menuName = "MenuFragment";
            tv_current_menu.setVisibility(View.GONE);
        }

        lvMenu = (ListView) rootView.findViewById(R.id.lv_dashboard_menu);

        // --- Battery layouts and display ---
        ll_battery = (LinearLayout) rootView.findViewById(R.id.ll_battery);
        tv_battery = (TextView) rootView.findViewById(R.id.tv_battery_level);
        iv_battery = (ImageView) rootView.findViewById(R.id.iv_battery);

        ll_battery.setVisibility(View.INVISIBLE);
        // --- Battery layouts and display ---

        initList();

        mainActivity.mTracker.setScreenName("MenuFragment " + menuName);
        mainActivity.mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        return rootView;
    }

    public void init() {
        Log.d(TAG, "init: ");
        mainActivity = ((MainActivity) getActivity());
    }

    public void initList() {
        Log.d(TAG, "initList: ");
        menuIndex = 0;
        ArrayList<AppModel> list = new ArrayList<AppModel>();

        menuAdapter = new MenuAdapter(mainActivity, R.layout.item_menu_rect, list);

        try {
            menuAdapter.populateFromJson(jsonName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        menuAdapter.add(new AppModel("Back", null,
                getResources().getDrawable(R.drawable.ic_arrow_back_black_48dp, mainActivity.getTheme()),
                AppModel.BACK_APPLICATION));

        lvMenu.setAdapter(menuAdapter);
        lvMenu.setOnItemClickListener(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        menuIndex = prefs.getInt(menuName, 0);

        changeCurrentMenuIndex(menuIndex);
    }

    public void onStart() {
        Log.d(TAG, "onStart: ");
        super.onStart();
    }

    @Override
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

    private void updateUi() {
        Log.d(TAG, "updateUi - index " + Integer.toString(menuIndex));
        if (menuSelectedModel != null) {
            menuSelectedModel.setSelected(false);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (menuIndex == menuAdapter.getCount() - 1)
            prefs.edit().putInt(menuName, 0).apply();
        else
            prefs.edit().putInt(menuName, menuIndex).apply();

        if (menuIndex >= 0) {
            menuSelectedModel = (AppModel) lvMenu.getItemAtPosition(menuIndex);
            menuSelectedModel.setSelected(true);
        }

        menuAdapter.notifyDataSetChanged();
        lvMenu.smoothScrollToPosition(menuIndex);

        mainActivity.updateBattery(ll_battery, tv_battery, iv_battery);
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        changeCurrentMenuIndex(position);
        openCurrentItem();
    }

    //actions
    public void changeCurrentMenuIndex(int _newMenuIndex) {
        menuIndex = _newMenuIndex;
        updateUi();
    }

    public void openCurrentItem() {
        Log.d(TAG, "openCurrentItem: Games");
        AppModel appModel = menuAdapter.getAppModel(menuIndex);
        if (appModel != null)
            appModel.performAction(mainActivity);
        else
            Log.e(TAG, "openCurrentItem: Missing model!");
    }

    boolean exit_pressed = false;

    public void back() {
        mainActivity.onBackPressed();
    }

    private int oldViewState = 0;
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLinkLost(DisconnectedEvent event) {
        if (tv_current_menu!=null) {
            tv_current_menu.setText("DISCONNECTED");
            oldViewState = tv_current_menu.getVisibility();
            tv_current_menu.setVisibility(View.VISIBLE);
            ll_battery.setVisibility(View.GONE);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConnecting(ConnectingEvent event) {
        if (tv_current_menu!=null)
            tv_current_menu.setText("CONNECTING");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLinkRestablished(ConnectedEvent connectedEvent) {
        if (tv_current_menu!=null) {
            tv_current_menu.setText(menuName);
            if (oldViewState == View.GONE)
                tv_current_menu.setVisibility(View.GONE);

            ll_battery.setVisibility(View.VISIBLE);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGesturePerformed(FlicktekCommands.onGestureEvent gestureEvent) {
        switch (gestureEvent.status) {
            case FlicktekManager.GESTURE_UP:
                if (menuIndex > 0)
                    changeCurrentMenuIndex(menuIndex - 1);
                else
                    changeCurrentMenuIndex(menuAdapter.getCount() - 1);
                break;
            case FlicktekManager.GESTURE_DOWN:
                if (menuIndex < menuAdapter.getCount() - 1)
                    changeCurrentMenuIndex(menuIndex + 1);
                else
                    changeCurrentMenuIndex(0);
                break;
            case FlicktekManager.GESTURE_ENTER:
                openCurrentItem();
                break;

            case FlicktekManager.GESTURE_HOME:
                if (!exit_pressed) {
                    mainActivity.runOnUiThread(
                            new Runnable() {
                                public void run() {
                                    Toast.makeText(mainActivity.getApplicationContext(),
                                            "Perform home again to go back!",
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                } else {
                    back();
                }
                exit_pressed = true;
                return;
        }

        exit_pressed = false;

        /*Toast toast = Toast.makeText(mainActivity.getApplicationContext(),
                FlicktekManager.getGestureString(gestureEvent.status),
                Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.RIGHT, 0, 0);
        toast.show();
        */
    }
}
