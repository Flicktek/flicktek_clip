package com.flicktek.clip.menus;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
public class MenuFragment extends Fragment {
    private static final String ARG_JSON_NAME = "jsonName";
    private static final String ARG_MENU_NAME = "menuName";

    private String TAG = "MenuFragment";
    private MainActivity mainActivity;

    private RecyclerView mRecyclerView;
    private MenuAdapter menuAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private int menuIndex;
    private AppModel menuSelectedModel;

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
        rootView = inflater.inflate(R.layout.fragment_dashboard_recycler, container, false);

        mainActivity.setMenuName(menuName);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.lv_dashboard_menu);

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

    private final RecyclerView.OnItemTouchListener mTouchListener = new RecyclerView.OnItemTouchListener() {
        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            mainActivity.mDetector.onTouchEvent(e);
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        }
    };

    private void initList() {
        Log.d(TAG, "initList: ");

        ArrayList<AppModel> list = new ArrayList<AppModel>();

        menuAdapter = new MenuAdapter(list);
        try {
            menuAdapter.populateFromJson(mainActivity, jsonName);
        } catch (Exception e) {
            e.printStackTrace();
            mainActivity.showToastMessage(e.toString());
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        menuIndex = prefs.getInt(menuName, 0);

        if (menuAdapter.hasHeader && menuIndex == 0)
            menuIndex = 1;

        //mRecyclerView.setHasFixedSize(true);

        mRecyclerView.setAdapter(menuAdapter);
        mLayoutManager = new LinearLayoutManager(mainActivity.getApplicationContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mRecyclerView.addOnItemTouchListener(mTouchListener);

        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(mainActivity.getApplicationContext(),
                        new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        try {
                            changeCurrentMenuIndex(position);
                            openCurrentItem();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                })
        );


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
        menuAdapter.setSelected(menuIndex);
        mRecyclerView.smoothScrollToPosition(menuIndex);
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
        if (menuIndex == menuAdapter.getItemCount() - 1)
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
                    changeCurrentMenuIndex(menuAdapter.getItemCount() - 1);
                }
                break;
            case (FlicktekManager.GESTURE_DOWN):
                if (menuIndex < menuAdapter.getItemCount() - 1) {
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
