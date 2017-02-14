package com.flicktek.android.clip;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.flicktek.android.clip.wearable.MainWearableListenerService;

import java.util.ArrayList;

/**
 * Generic menu fragment inheritable
 */
public class MenuFragment extends Fragment implements AdapterView.OnItemClickListener,
        MainWearableListenerService.MyGestureListener {

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
        MainWearableListenerService.setCustomObjectListener(this);
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
        changeCurrentMenuIndex(0);
    }

    public void onStart() {
        Log.d(TAG, "onStart: ");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume: ");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause: ");
        super.onPause();
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

    @Override
    public void onGestureReceived(String gesture) {
        switch (gesture) {
            case ("UP"):
                if (menuIndex > 0)
                    changeCurrentMenuIndex(menuIndex - 1);
                else
                    changeCurrentMenuIndex(menuAdapter.getCount() - 1);
                break;
            case ("DOWN"):
                if (menuIndex < menuAdapter.getCount() - 1)
                    changeCurrentMenuIndex(menuIndex + 1);
                else
                    changeCurrentMenuIndex(0);
                break;
            case ("ENTER"):
                openCurrentItem();
                break;

            case ("HOME"):
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

        Toast toast = Toast.makeText(mainActivity.getApplicationContext(), gesture, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP | Gravity.RIGHT, 0, 0);
        toast.show();
    }
}
