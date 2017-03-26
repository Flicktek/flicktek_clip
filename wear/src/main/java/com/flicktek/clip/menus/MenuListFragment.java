package com.flicktek.clip.menus;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.flicktek.clip.FlicktekCommands;
import com.flicktek.clip.FlicktekManager;
import com.flicktek.clip.FlicktekSettings;
import com.flicktek.clip.MainActivity;
import com.flicktek.clip.R;
import com.flicktek.clip.wearable.common.Constants;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;

/**
 * Generic menu fragment inheritable
 */
public class MenuListFragment extends Fragment implements AdapterView.OnItemClickListener {
    private static final String ARG_JSON_NAME = "jsonName";
    private static final String ARG_MENU_NAME = "menuName";

    private String TAG = "MenuFragment";
    private MainActivity mainActivity;

    private ListView lvMenu;
    private MenuListAdapter menuAdapter;
    private int menuIndex;
    private AppModel menuSelectedModel;

    private String jsonName = null;
    private String menuName = null;

    // Default bundle constructor as google best practices

    public static MenuListFragment newInstance(String menuName, String jsonName) {
        MenuListFragment myFragment = new MenuListFragment();

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

        mainActivity.setMenuName(menuName);
        lvMenu = (ListView) rootView.findViewById(R.id.lv_dashboard_menu);

        initList();

        if (FlicktekSettings.getInstance().isDemo())
            mainActivity.sendMessageToHandheld(mainActivity.getApplicationContext(),
                    Constants.FLICKTEK_CLIP.LAUNCH_FRAGMENT, "menus.AnimatedGestures");

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
            menuAdapter = new MenuListAdapter(mainActivity, R.layout.item_menu_round, list);
        } else {
            menuAdapter = new MenuListAdapter(mainActivity, R.layout.item_menu_rect, list);
        }

        try {
            menuAdapter.populateFromJson(jsonName);
        } catch (Exception e) {
            e.printStackTrace();
            mainActivity.showToastMessage(e.toString());
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        menuIndex = prefs.getInt(menuName, 0);

        if (menuAdapter.hasHeader && menuIndex == 0)
            menuIndex = 1;

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

    public static View getChildAtPosition(final AdapterView view, final int position) {
        final int index = position - view.getFirstVisiblePosition();
        if ((index >= 0) && (index < view.getChildCount())) {
            return view.getChildAt(index);
        } else {
            return null;
        }
    }

    public static void smoothScrollToPositionFromTop(final AbsListView view, final int position) {
        View child = getChildAtPosition(view, position);
        // There's no need to scroll if child is already at top or view is already scrolled to its end
        if ((child != null) && ((child.getTop() == 0) || ((child.getTop() > 0) && !view.canScrollVertically(1)))) {
            return;
        }

        view.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(final AbsListView view, final int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    view.setOnScrollListener(null);

                    // Fix for scrolling bug
                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            view.setSelection(position);
                        }
                    });
                }
            }

            @Override
            public void onScroll(final AbsListView view, final int firstVisibleItem, final int visibleItemCount,
                                 final int totalItemCount) { }
        });

        // Perform scrolling to position
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                view.smoothScrollToPositionFromTop(position, 0);
            }
        });
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

        smoothScrollToPositionFromTop(lvMenu, menuIndex);
        //lvMenu.smoothScrollToPosition(menuIndex);
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

        save_preferences();
    }

    boolean exit_pressed = false;

    int min_value = 0;

    void save_preferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (menuIndex == menuAdapter.getCount() - 1)
            prefs.edit().putInt(menuName, min_value).apply();
        else
            prefs.edit().putInt(menuName, menuIndex).apply();
    }

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
                    save_preferences();
                    FlicktekManager.getInstance().backMenu(mainActivity);
                }
                exit_pressed = true;
                return;
        }

        exit_pressed = false;
    }
}
