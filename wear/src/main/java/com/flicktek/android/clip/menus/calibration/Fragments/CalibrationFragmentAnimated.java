package com.flicktek.android.clip.menus.calibration.Fragments;

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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.flicktek.android.clip.FlicktekCommands;
import com.flicktek.android.clip.FlicktekManager;
import com.flicktek.android.clip.MainActivity;
import com.flicktek.android.clip.R;
import com.flicktek.android.clip.wearable.common.Constants;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Random;

public class CalibrationFragmentAnimated extends Fragment implements View.OnClickListener {
    private String TAG = "CalibrationEnter";

    private MainActivity mainActivity;
    private Button b_close;
    private int status;

    private boolean bFinishedCalibration = false;

    private TextView tv_status;
    private TextView tv_title;
    private ImageView iv_splash;
    private ImageView iv_number;
    private RelativeLayout rl_number_container;

    private int iteration = 1;
    private int gestures_left = 0;
    private int gesture_number = 0;

    public static final int GESTURE_ENTER = 0;
    public static final int GESTURE_HOME = 1;
    public static final int GESTURE_UP = 2;
    public static final int GESTURE_DOWN = 3;
    public static final int GESTURE_EXTRA = 4;

    private static int G_NUMB_MAX = 0;
    private static int G_ITER_MAX = 0;

    /**
     * Resources to use on this gesture calibration
     */
    private int res_layout_calibration;
    private int title;
    private int animation_success;
    private int animation_error;
    private int res_main_icon;

    public static CalibrationFragmentAnimated newInstance(int gesture_number) {
        CalibrationFragmentAnimated myFragment = new CalibrationFragmentAnimated();

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

        FlicktekManager.setCalibrationMode(true);

        animation_success = R.anim.calibration_enter;
        animation_error = R.anim.calibration_error;

        switch (gesture_number) {
            case GESTURE_ENTER:
                res_layout_calibration = R.layout.fragment_calib_white;
                title = R.string.calibration_enter;
                animation_success = R.anim.calibration_enter;
                res_main_icon = R.drawable.ic_enter_circle_black;
                break;
            case GESTURE_HOME:
                title = R.string.calibration_home;
                res_layout_calibration = R.layout.fragment_calib_black;
                animation_success = R.anim.calibration_enter;
                res_main_icon = R.drawable.ic_home_circle_black;
                break;
            case GESTURE_UP:
                title = R.string.calibration_up;
                res_layout_calibration = R.layout.fragment_calib_white;
                animation_success = R.anim.calibration_up;
                res_main_icon = R.drawable.ic_up_circle_black;
                break;
            case GESTURE_DOWN:
                title = R.string.calibration_down;
                res_layout_calibration = R.layout.fragment_calib_black;
                animation_success = R.anim.calibration_down;
                res_main_icon = R.drawable.ic_down_circle_black;
                break;

        }

        Log.v(TAG, "------- ONCREATE VIEW " + getString(title) + "---------------");

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

        if (gesture_number == GESTURE_ENTER) {
            G_NUMB_MAX = FlicktekCommands.getInstance().getGesturesNumber();
            G_ITER_MAX = FlicktekCommands.getInstance().getIterationsNumber();
        }

        iv_number = (ImageView) rootView.findViewById(R.id.counting_icon);

        rl_number_container = (RelativeLayout) rootView.findViewById(R.id.counter_container);

        mainActivity.sendMessageToHandheld(mainActivity.getApplicationContext(),
                Constants.FLICKTEK_CLIP.ANALYTICS_CALIBRATION, "CALIBRATE_GESTURE " + getString(title));

        return rootView;
    }

    private boolean anim_running = false;

    public void animate_number() {
        if (anim_running)
            return;

        final Animation anim = AnimationUtils.loadAnimation(
                mainActivity.getApplicationContext(),
                R.anim.calibration_number_rotation_out);

        rl_number_container.setVisibility(View.VISIBLE);
        rl_number_container.startAnimation(anim);

        gestures_left = G_ITER_MAX - iteration + 1;

        int value = 0;
        switch (gesture_number) {
            case GESTURE_ENTER:
                value = FlicktekManager.GESTURE_ENTER;
                break;
            case GESTURE_HOME:
                value = FlicktekManager.GESTURE_HOME;
                break;
            case GESTURE_UP:
                value = FlicktekManager.GESTURE_UP;
                break;
            case GESTURE_DOWN:
                value = FlicktekManager.GESTURE_DOWN;
                break;
        }
        FlicktekCommands.getInstance().onGestureChanged(value);

        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                anim_running = true;
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                //Log.v(TAG, "Animation finished");
                anim.setAnimationListener(null);

                Animation anim = AnimationUtils.loadAnimation(
                        mainActivity.getApplicationContext(),
                        R.anim.calibration_number_rotation_in);

                int res;
                switch (gestures_left) {
                    case 1:
                        res = R.drawable.ic_looks_one_black_48dp;
                        break;
                    case 2:
                        res = R.drawable.ic_looks_two_black_48dp;
                        break;
                    case 3:
                        res = R.drawable.ic_looks_3_black_48dp;
                        break;
                    case 4:
                        res = R.drawable.ic_looks_4_black_48dp;
                        break;
                    case 5:
                        res = R.drawable.ic_looks_5_black_48dp;
                        break;
                    default:
                        res = R.drawable.ic_looks_6_black_48dp;
                        break;
                }

                iv_number.setImageResource(res);
                rl_number_container.setVisibility(View.VISIBLE);
                rl_number_container.startAnimation(anim);
                anim_running = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

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

        FlicktekCommands.getInstance().onGestureChanged(0);

        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (enter) {
                    if (gesture_number == GESTURE_ENTER)
                        FlicktekCommands.getInstance().startCalibration();
                    else
                        sendStart();
                }
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                Log.d(TAG, "Animation ended. GESTURE " + gesture_number + " Enter " + enter);
            }
        });
        return anim;
    }

    private void updateUi() {
        //Log.d(TAG, "updateUi: ");
        mainActivity.runOnUiThread(new Runnable() {
            public void run() {
                switch (status) {
                    case FlicktekCommands.GESTURE_STATUS_RECORDING:
                        if (iv_splash.getVisibility() == View.INVISIBLE) {
                            iv_splash.setVisibility(View.INVISIBLE);
                            animate_icon(animation_success);
                        }
                        return;
                    case FlicktekCommands.GESTURE_STATUS_OK:
                        tv_status.setText("Repeat gesture!");
                        tv_title.setText("OK!");
                        animate_number();
                        animate_icon(animation_success);
                        break;
                    case FlicktekCommands.GESTURE_STATUS_ERROR1:
                        tv_status.setText("Not similar");
                        tv_title.setText("Try again!");
                        animate_icon(animation_error);
                        break;
                    case FlicktekCommands.GESTURE_STATUS_ERROR2:
                        tv_status.setText("Let's start again");
                        tv_title.setText("Try again...");
                        animate_icon(animation_error);
                        break;
                    case FlicktekCommands.GESTURE_STATUS_OKREPETITION:
                        tv_status.setText("Repeat gesture!");
                        animate_number();

                        Random r = new Random();
                        int i = r.nextInt() % 3;
                        String greats;
                        switch (i) {
                            case 0:
                                greats = "Well done!";
                                break;
                            case 1:
                                greats = "Great!";
                                break;
                            default:
                                greats = "Looks good!";
                                break;
                        }

                        tv_title.setText(greats);
                        animate_icon(animation_success);
                        break;
                    case FlicktekCommands.GESTURE_STATUS_OKGESTURE:
                        tv_status.setText("OK!");
                        tv_title.setText("Perfect!");
                        animate_icon(animation_success);
                        break;
                }

                sendStart();
            }
        });
    }

    public void close() {
        Log.d(TAG, "close: ");
        FlicktekManager.setCalibrationMode(false);
        FlicktekCommands.getInstance().stopCalibration();

        int pop_flag;
        if (bFinishedCalibration) {
            pop_flag = FragmentManager.POP_BACK_STACK_INCLUSIVE;         // Previous menu to calibration
        } else {
            pop_flag = 0; // Pop until the start calibration message
        }

        mainActivity.getFragmentManager().popBackStack("CalibrationFragment", pop_flag);
        FlicktekManager.backMenu(mainActivity);
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

    private void sendStart() {
        Log.d(TAG, "sendStart: " + gesture_number);
        FlicktekCommands.getInstance().writeGestureStatusStart();
    }

    @Subscribe
    public void onCalibrationWritten(FlicktekCommands.onCalibrationWritten _status) {
        Log.d(TAG, "onCalibrationWritten: " + gesture_number);

        status = _status.status;
        switch (_status.status) {
            case FlicktekCommands.STATUS_EXEC:
                break;
            case FlicktekCommands.STATUS_CALIB:
                sendStart();
                break;
        }
    }

    @Subscribe
    public void onGestureStatusNotify(FlicktekCommands.onGestureStatusEvent _status) {
        Log.d(TAG, "onGestureStatusNotify: " + _status.status.toString());
        status = _status.status;
        updateStatus();
    }

    public void showNextFragment() {
        if (gesture_number == G_NUMB_MAX - 1) {
            Log.v(TAG, "Finished calibration");
            bFinishedCalibration = true;
            close();
            return;
        }

        final Fragment _fragment = newInstance(gesture_number + 1);
        Log.d(TAG, "showFragment: ");
        mainActivity.runOnUiThread(new Runnable() {

            public void run() {
                try {
                    FragmentManager fragmentManager = getFragmentManager();
                    Log.d(TAG, "Animated Start getBackStackEntryCount: " + fragmentManager.getBackStackEntryCount());

                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out_left);
                    transaction.addToBackStack("CalibrationAnimated " + gesture_number);
                    //transaction.disallowAddToBackStack();
                    transaction.replace(R.id.container, _fragment);
                    Log.d(TAG, "Animated End   getBackStackEntryCount: " + fragmentManager.getBackStackEntryCount());
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

    private void updateStatus() {
        switch (status) {
            case FlicktekCommands.GESTURE_STATUS_OK:
                Log.d(TAG, "updateStatus: GESTURE_STATUS_OK");
                if (iteration < G_ITER_MAX) {
                    iteration++;
                } else {
                    showNextFragment();
                }

                mainActivity.sendMessageToHandheld(mainActivity.getApplicationContext(),
                        Constants.FLICKTEK_CLIP.ANALYTICS_CALIBRATION, "STATUS_OK " + getString(title));
                break;
            case FlicktekCommands.GESTURE_STATUS_RECORDING:
                // FADEIN
                Log.d(TAG, "updateStatus: GESTURE_STATUS_RECORDING");
                break;
            case FlicktekCommands.GESTURE_STATUS_ERROR1:
                Log.d(TAG, "updateStatus: GESTURE_STATUS_ERROR1");
                iteration = 2;
                mainActivity.sendMessageToHandheld(mainActivity.getApplicationContext(),
                        Constants.FLICKTEK_CLIP.ANALYTICS_CALIBRATION, "ERROR1 " + getString(title));
                break;
            case FlicktekCommands.GESTURE_STATUS_ERROR2:
                Log.d(TAG, "updateStatus: GESTURE_STATUS_ERROR2");
                iteration = 1;
                mainActivity.sendMessageToHandheld(mainActivity.getApplicationContext(),
                        Constants.FLICKTEK_CLIP.ANALYTICS_CALIBRATION, "ERROR 2 " + getString(title));
                break;
            case FlicktekCommands.GESTURE_STATUS_OKREPETITION:
                Log.d(TAG, "updateStatus: GESTURE_STATUS_OKREPETITION");
                iteration++;
                mainActivity.sendMessageToHandheld(mainActivity.getApplicationContext(),
                        Constants.FLICKTEK_CLIP.ANALYTICS_CALIBRATION, "OK_REPETITION" + getString(title));
                break;
            case FlicktekCommands.GESTURE_STATUS_OKGESTURE:
                Log.d(TAG, "updateStatus: GESTURE_STATUS_OKGESTURE");
                iteration = 1;
                showNextFragment();
                mainActivity.sendMessageToHandheld(mainActivity.getApplicationContext(),
                        Constants.FLICKTEK_CLIP.ANALYTICS_CALIBRATION, "OK_GESTURE " + getString(title));
                return;
            case FlicktekCommands.GESTURE_STATUS_OKCALIBRATION:
                Log.d(TAG, "updateStatus: GESTURE_STATUS_OKCALIBRATION");
                bFinishedCalibration = true;
                close();
                mainActivity.sendMessageToHandheld(mainActivity.getApplicationContext(),
                        Constants.FLICKTEK_CLIP.ANALYTICS_CALIBRATION, "OK_CALIBRATION " + getString(title));
                return;
        }

        updateUi();
    }

    public void onClick(View _view) {
        Log.d(TAG, "onClick: ");
        if (_view == b_close)
            close();
    }
}
