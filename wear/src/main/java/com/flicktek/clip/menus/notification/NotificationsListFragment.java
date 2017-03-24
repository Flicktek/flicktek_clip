package com.flicktek.clip.menus.notification;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.flicktek.clip.FlicktekCommands;
import com.flicktek.clip.FlicktekManager;
import com.flicktek.clip.MainActivity;
import com.flicktek.clip.R;
import com.flicktek.clip.wearable.common.NotificationModel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

/**
 * Generic menu fragment inheritable
 */
public class NotificationsListFragment extends Fragment implements AdapterView.OnItemClickListener {
    private String TAG = "MenuFragment";
    private MainActivity mainActivity;

    private ListView lvMenu;
    private NotificationListAdapter menuAdapter;
    private int menuIndex;
    private NotificationModel menuSelectedModel;

    private String menuName = "Notifications";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        mainActivity.setMenuName(menuName);

        lvMenu = (ListView) rootView.findViewById(R.id.lv_dashboard_menu);
        initList();

        mainActivity.initializeBatteryDisplay();
        return rootView;
    }

    public void init() {
        Log.d(TAG, "init: ");
        mainActivity = ((MainActivity) getActivity());
    }

    private void initList() {
        Log.d(TAG, "initList: ");

        List<NotificationModel> list = FlicktekManager.getInstance().getNotifications();

        menuAdapter = new NotificationListAdapter(mainActivity, R.layout.item_notification_rect, list);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        menuIndex = prefs.getInt(menuName, 0);

        lvMenu.setAdapter(menuAdapter);
        lvMenu.setOnItemClickListener(this);

        if (menuIndex < list.size())
            changeCurrentMenuIndex(menuIndex);
        else
            menuIndex = 0;

        if (list.size() == 0) {

        }

        menuAdapter.add(new NotificationModel("Back", "", "application_back",
                getResources().getDrawable(R.drawable.ic_arrow_back_black_48dp, mainActivity.getTheme()),
                null));
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

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (menuIndex == menuAdapter.getCount() - 1)
            prefs.edit().putInt(menuName, min_value).apply();
        else
            prefs.edit().putInt(menuName, menuIndex).apply();

        if (menuIndex >= 0) {
            menuSelectedModel = (NotificationModel) lvMenu.getItemAtPosition(menuIndex);
            menuSelectedModel.setSelected(true);
        }

        menuAdapter.notifyDataSetChanged();
        lvMenu.smoothScrollToPosition(menuIndex);

        mainActivity.updateBattery(0);
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
        NotificationModel notificationModel = menuAdapter.getNotificationModel(menuIndex);
        if (notificationModel != null)
            notificationModel.performAction(mainActivity);
        else
            Log.e(TAG, "openCurrentItem: Missing model!");
    }

    boolean exit_pressed = false;

    int min_value = 0;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGesturePerformed(FlicktekCommands.onGestureEvent gestureEvent) {
        Log.d(TAG, "onGesturePerformed: " + gestureEvent.status.toString() + " index " + Integer.toString(menuIndex));
        int gesture = gestureEvent.status;

        if (menuAdapter.hasHeader) {
            min_value = 1;
        }

        switch (gesture) {
            case (FlicktekManager.GESTURE_UP):
                if (menuIndex > min_value) {
                    changeCurrentMenuIndex(menuIndex - 1);
                } else {
                    menuAdapter.disable_header();
                    changeCurrentMenuIndex(menuAdapter.getCount() - 1);
                }
                break;
            case (FlicktekManager.GESTURE_DOWN):
                if (menuIndex < menuAdapter.getCount() - 1) {
                    changeCurrentMenuIndex(menuIndex + 1);
                } else {
                    menuAdapter.disable_header();
                    changeCurrentMenuIndex(min_value);
                }
                break;
            case (FlicktekManager.GESTURE_ENTER):
                openCurrentItem();
                break;

            case (FlicktekManager.GESTURE_HOME):
                exit_pressed = !FlicktekManager.getInstance().mIsDoubleGestureHomeExit;

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
                    FlicktekManager.getInstance().backMenu(mainActivity);
                }
                exit_pressed = true;
                return;
        }

        exit_pressed = false;
    }
}
