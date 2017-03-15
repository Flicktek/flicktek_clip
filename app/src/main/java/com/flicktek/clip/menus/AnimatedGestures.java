package com.flicktek.clip.menus;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.flicktek.clip.FlicktekCommands;
import com.flicktek.clip.FlicktekManager;
import com.flicktek.clip.MainActivity;
import com.flicktek.clip.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class AnimatedGestures extends Fragment implements View.OnClickListener {
    private String TAG = "CalibrationEnter";

    private MainActivity mainActivity;
    private Button b_close;
    private int status;

    private boolean bFinishedCalibration = false;

    private TextView tv_status;
    private TextView tv_title;
    private ImageView iv_splash;

    private int iteration = 1;
    private int gestures_left = 0;
    private int gesture_number = 0;

    /**
     * Resources to use on this gesture calibration
     */
    private int res_layout_calibration;
    private int title;
    private int animation_success;
    private int animation_error;
    private int res_main_icon;

    public static AnimatedGestures newInstance(int gesture_number) {
        AnimatedGestures myFragment = new AnimatedGestures();

        Bundle args = new Bundle();
        args.putInt("gesture", gesture_number);
        myFragment.setArguments(args);
        return myFragment;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        try {
            gesture_number = getArguments().getInt("gesture");
        } catch (Exception e) {

        }

        animation_success = R.anim.calibration_enter;
        animation_error = R.anim.calibration_error;

        switch (gesture_number) {
            case FlicktekManager.GESTURE_ENTER:
                res_layout_calibration = R.layout.fragment_animated_gesture;
                title = R.string.enter;
                animation_success = R.anim.gesture_performed;
                res_main_icon = R.drawable.ic_enter_circle_black;
                break;
            case FlicktekManager.GESTURE_HOME:
                title = R.string.home;
                res_layout_calibration = R.layout.fragment_animated_gesture;
                animation_success = R.anim.gesture_performed;
                res_main_icon = R.drawable.ic_home_circle_black;
                break;
            case FlicktekManager.GESTURE_UP:
                title = R.string.up;
                res_layout_calibration = R.layout.fragment_animated_gesture;
                animation_success = R.anim.gesture_performed;
                res_main_icon = R.drawable.ic_up_circle_black;
                break;
            case FlicktekManager.GESTURE_DOWN:
                title = R.string.down;
                res_layout_calibration = R.layout.fragment_animated_gesture;
                animation_success = R.anim.gesture_performed;
                res_main_icon = R.drawable.ic_down_circle_black;
                break;
            default:
                title = R.string.perform_gesture;
                res_layout_calibration = R.layout.fragment_animated_gesture;
                animation_success = R.anim.calibration_enter;
                res_main_icon = R.drawable.ic_circle_black;
                break;
        }

        try {
            Log.v(TAG, "------- ONCREATE VIEW " + getString(title) + "---------------");
        } catch (Exception e) {
            Log.d(TAG, "crashed");
        }

        Log.d(TAG, "onCreateView: ");
        mainActivity = (MainActivity) getActivity();
        View rootView = null;

        rootView = inflater.inflate(res_layout_calibration, container, false);

        Typeface mainFont = Typeface.createFromAsset(mainActivity.getAssets(), getString(R.string.main_font));

        b_close = (Button) rootView.findViewById(R.id.b_close);
        b_close.setOnClickListener(this);

        iv_splash = (ImageView) rootView.findViewById(R.id.calibration_icon);
        iv_splash.setImageResource(res_main_icon);

        tv_status = (TextView) rootView.findViewById(R.id.tv_status);
        tv_status.setText("");

        tv_title = (TextView) rootView.findViewById(R.id.tv_title);
        tv_title.setText(title);

        animate_icon(animation_success);
        return rootView;
    }

    private boolean anim_running = false;

    public void animate_icon(int animation) {
        Log.v(TAG, "animate_icon " + animation);
        Animation anim = AnimationUtils.loadAnimation(
                mainActivity.getApplicationContext(),
                animation);

        iv_splash.setVisibility(View.VISIBLE);
        iv_splash.startAnimation(anim);

        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Log.v(TAG, "Animation finished");
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    @Override
    public Animator onCreateAnimator(int transit, final boolean enter, int nextAnim) {
        Log.v(TAG, "onCreateAnimator " + nextAnim);
        if (mainActivity == null) {
            Log.v(TAG, "Something went very wrong!");
            return null;
        }

        if (nextAnim == 0) {
            Log.v(TAG, "Animation is missing! FADEOUT");
            nextAnim = R.animator.fade_out;
        }

        final Animator anim = AnimatorInflater.loadAnimator(mainActivity, nextAnim);
        if (anim == null) {
            Log.v(TAG, "Something went wrong!");
            return null;
        }

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "Animation ended. GESTURE " + gesture_number + " Enter " + enter);
            }
        });
        return anim;
    }

    public void close() {
        mainActivity.finish();
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume: " + gesture_number);
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause: " + gesture_number);
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    public void showFragment() {
        final Fragment _fragment = newInstance(gesture_number);
        Log.d(TAG, "showFragment: ");
        mainActivity.runOnUiThread(new Runnable() {

            public void run() {
                try {
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);
                    transaction.replace(R.id.container, _fragment);
                    transaction.commit();

                    Log.i(TAG, "-------- BEGIN ANIMATED FRAGMENT --------");
                    for (int entry = 0; entry < fragmentManager.getBackStackEntryCount(); entry++) {
                        FragmentManager.BackStackEntry backStackEntryAt = fragmentManager.getBackStackEntryAt(entry);
                        Log.i(TAG, "Fragment: " + backStackEntryAt.getId() + " " + backStackEntryAt.getName());
                    }
                    Log.i(TAG, "---------- END ANIMATED FRAGMENT --------");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGesturePerformed(final FlicktekCommands.onGestureEvent gestureEvent) {
        gesture_number = gestureEvent.status;
        showFragment();
    }

    public void onClick(View _view) {
        Log.d(TAG, "onClick: ");
        if (_view == b_close)
            close();
    }
}
