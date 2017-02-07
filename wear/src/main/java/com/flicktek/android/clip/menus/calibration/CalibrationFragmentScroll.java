package com.flicktek.android.clip.menus.calibration;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.flicktek.android.clip.FlicktekCommands;
import com.flicktek.android.clip.FlicktekManager;
import com.flicktek.android.clip.MainActivity;
import com.flicktek.android.clip.R;
import com.flicktek.android.clip.menus.calibration.Fragments.CalibrationFragmentAnimated;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class CalibrationFragmentScroll extends Fragment implements View.OnClickListener {
    private String TAG = "CalibrationScroll";

    private MainActivity mainActivity;
    private TextView tvStatus;
    private TextView tvLoader;
    private ImageView ivWelcomeImage;
    private Button bClose;
    private Button bStart;
    private int status;
    private boolean isLaunched = false;

    //fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: ");
        mainActivity = (MainActivity) getActivity();
        View rootView = null;

        rootView = inflater.inflate(R.layout.fragment_calib_welcome, container, false);
        Typeface mainFont = Typeface.createFromAsset(mainActivity.getAssets(), getString(R.string.main_font));

        bClose = (Button) rootView.findViewById(R.id.b_close);
        bClose.setOnClickListener(this);

        bStart = (Button) rootView.findViewById(R.id.b_start);
        bStart.setOnClickListener(this);

        ivWelcomeImage = (ImageView) rootView.findViewById(R.id.welcome);
        ivWelcomeImage.setOnClickListener(this);

        return rootView;
    }

    public void close() {
        Log.d(TAG, "close: ");
        FlicktekManager.backMenu(mainActivity);
    }

    @Subscribe
    public void onGesturePerformed(FlicktekCommands.onGestureEvent gestureEvent) {
        Log.d(TAG, "onGesturePerformed: ");
        int gesture = gestureEvent.status;
        switch (gesture) {
            case (FlicktekManager.GESTURE_UP):
            case (FlicktekManager.GESTURE_DOWN):
            case (FlicktekManager.GESTURE_ENTER):
                showEnterFragment();
                break;
            case (FlicktekManager.GESTURE_HOME):
                close();
                break;
        }
    }

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

    public void showEnterFragment() {
        final Fragment _fragment = new CalibrationFragmentAnimated();
        Log.d(TAG, "showFragment: ");
        mainActivity.runOnUiThread(new Runnable() {

            public void run() {
                try {
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction transaction = fragmentManager.beginTransaction();

                    transaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);

                    transaction.replace(R.id.container, _fragment).addToBackStack("CalibrationFragment");
                    transaction.commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void onClick(View _view) {
        Log.d(TAG, "onClick: ");
        if (_view == bClose) close();
        if (_view == bStart || _view == ivWelcomeImage) {
            showEnterFragment();
        }
    }
}
