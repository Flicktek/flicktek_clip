package com.flicktek.android.clip.menus;

import android.app.Fragment;
import android.os.Bundle;
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

import com.flicktek.android.clip.FlicktekCommands;
import com.flicktek.android.clip.FlicktekManager;
import com.flicktek.android.clip.MainActivity;
import com.flicktek.android.clip.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

/**
 * Generic menu fragment inheritable
 */
public class MenuFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String ARG_JSON_NAME = "jsonName";
    private static final String ARG_MENU_NAME = "menuName";

    private String TAG = "MenuFragment";
    private MainActivity mainActivity;

    private ListView lvMenu;
    private MenuAdapter menuAdapter;
    private int menuIndex;
    private AppModel menuSelectedModel;
    private TextView tv_current_menu;

    private TextView tv_battery;
    private ImageView iv_battery;
    private LinearLayout ll_battery;

    private String jsonName = null;
    private String menuName = null;

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
        if (mainActivity.isRound) {
            rootView = inflater.inflate(R.layout.fragment_dashboard_round, container, false);
        } else {
            rootView = inflater.inflate(R.layout.fragment_dashboard_rect, container, false);
        }

        if (menuName != null) {
            tv_current_menu = (TextView) rootView.findViewById(R.id.tv_current_menu);
            tv_current_menu.setText(menuName);
        } else {
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

        return rootView;
    }

    public void init() {
        Log.d(TAG, "init: ");
        mainActivity = ((MainActivity) getActivity());
    }

    private void initList() {
        Log.d(TAG, "initList: ");

        ArrayList<AppModel> list = new ArrayList<AppModel>();

        if (mainActivity.isRound) {
            menuAdapter = new MenuAdapter(mainActivity, R.layout.item_menu_round, list);
        } else {
            menuAdapter = new MenuAdapter(mainActivity, R.layout.item_menu_rect, list);
        }

        try {
            menuAdapter.populateFromJson(jsonName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (menuAdapter.hasHeader)
            menuIndex = 1;
        else
            menuIndex = 0;

        lvMenu.setAdapter(menuAdapter);
        lvMenu.setOnItemClickListener(this);

        changeCurrentMenuIndex(menuIndex);
    }

    public void onStart() {
        Log.d(TAG, "onStart: ");
        super.onStart();
    }

    @Override
    public void onResume() {
        EventBus.getDefault().register(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    private void updateUi() {
        Log.d(TAG, "updateUi - index " + Integer.toString(menuIndex));
        if (menuSelectedModel != null) {
            menuSelectedModel.setSelected(false);
        }

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
    private void changeCurrentMenuIndex(int _newMenuIndex) {
        menuIndex = _newMenuIndex;
        updateUi();
    }

    private void openCurrentItem() {
        Log.d(TAG, "openCurrentItem: Games");
        AppModel appModel = menuAdapter.getAppModel(menuIndex);
        if (appModel != null)
            appModel.performAction(mainActivity);
        else
            Log.e(TAG, "openCurrentItem: Missing model!");
    }

    boolean exit_pressed = false;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGesturePerformed(FlicktekCommands.onGestureEvent gestureEvent) {
        Log.d(TAG, "onGesturePerformed: " + gestureEvent.status.toString() + " index " + Integer.toString(menuIndex));
        int gesture = gestureEvent.status;
        int minValue = 0;

        if (menuAdapter.hasHeader) {
            minValue = 1;
        }

        switch (gesture) {
            case (FlicktekManager.GESTURE_UP):
                if (menuIndex > minValue)
                    changeCurrentMenuIndex(menuIndex - 1);
                else
                    changeCurrentMenuIndex(menuAdapter.getCount() - 1);
                break;
            case (FlicktekManager.GESTURE_DOWN):
                if (menuIndex < menuAdapter.getCount() - 1) {
                    changeCurrentMenuIndex(menuIndex + 1);
                } else {
                    changeCurrentMenuIndex(minValue);
                }
                break;
            case (FlicktekManager.GESTURE_ENTER):
                openCurrentItem();
                break;

            case (FlicktekManager.GESTURE_HOME):
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
                    FlicktekManager.backMenu(mainActivity);
                }
                exit_pressed = true;
                return;
        }

        exit_pressed = false;
    }
}
