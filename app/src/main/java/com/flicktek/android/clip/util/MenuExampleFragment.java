package com.flicktek.android.clip.util;

import android.os.Bundle;
import android.util.Log;
import android.widget.AdapterView;

import com.flicktek.android.clip.AppModel;
import com.flicktek.android.clip.MenuAdapter;
import com.flicktek.android.clip.MenuFragment;
import com.flicktek.android.clip.R;

import java.util.ArrayList;

/**
 * Generic menu fragment inheritable
 */
public class MenuExampleFragment extends MenuFragment implements AdapterView.OnItemClickListener {
    private String TAG = "MenuFragment";

    // Default bundle constructor as google best practices

    public static MenuExampleFragment newInstance(String menuName, String jsonName) {
        MenuExampleFragment myFragment = new MenuExampleFragment();

        Bundle args = new Bundle();
        args.putString(ARG_JSON_NAME, jsonName);
        args.putString(ARG_MENU_NAME, menuName);
        myFragment.setArguments(args);
        return myFragment;
    }
    @Override
    public void initList() {
        Log.d(TAG, "initList: ");
        menuIndex = 0;
        ArrayList<AppModel> list = new ArrayList<AppModel>();

        menuAdapter = new MenuAdapter(mainActivity, R.layout.item_menu_rect, list);

        menuAdapter.add(new AppModel("Back", null,
                getResources().getDrawable(R.drawable.ic_arrow_back_black_48dp, mainActivity.getTheme()),
                AppModel.BACK_APPLICATION));

        lvMenu.setAdapter(menuAdapter);
        lvMenu.setOnItemClickListener(this);
        changeCurrentMenuIndex(0);
    }

    @Override
    public void openCurrentItem() {
        Log.d(TAG, "openCurrentItem: Games");
        AppModel appModel = menuAdapter.getAppModel(menuIndex);
        if (appModel != null)
            appModel.performAction(mainActivity);
        else
            Log.e(TAG, "openCurrentItem: Missing model!");
    }
}
