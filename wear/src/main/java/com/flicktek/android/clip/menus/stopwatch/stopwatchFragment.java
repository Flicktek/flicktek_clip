package com.flicktek.android.clip.menus.stopwatch;

import android.app.Fragment;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.flicktek.android.clip.FlicktekCommands;
import com.flicktek.android.clip.FlicktekManager;
import com.flicktek.android.clip.MainActivity;
import com.flicktek.android.clip.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class stopwatchFragment extends Fragment implements View.OnClickListener {
    private static String TAG = "StopwatchActivity";

    private final int START = 1;
    private final int PAUSE = 2;
    private final int RESET = 3;

    private MainActivity mainActivity;
    private stopwatch swatch = new stopwatch();
    private int status;

    // View elements in stopwatch.xml
    private TextView m_elapsedTime;
    private Button m_start;
    private Button m_pause;
    private Button m_reset;
    private Button b_close;

    // Timer to update the elapsedTime display
    private final long mFrequency = 100;    // milliseconds
    private final int TICK_WHAT = 2;

    private Handler mHandler = new Handler() {
        public void handleMessage(Message m) {
            updateElapsedTime();
            sendMessageDelayed(Message.obtain(this, TICK_WHAT), mFrequency);
        }
    };

    private boolean stopThread = false;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mainActivity = (MainActivity) getActivity();

        View rootView = null;
        rootView = inflater.inflate(R.layout.fragment_stopwatch, container, false);

        Typeface mainFont = Typeface.createFromAsset(mainActivity.getAssets(), getString(R.string.main_font));

        m_elapsedTime = (TextView) rootView.findViewById(R.id.ElapsedTime);

        m_start = (Button) rootView.findViewById(R.id.StartButton);
        m_pause = (Button) rootView.findViewById(R.id.PauseButton);
        m_reset = (Button) rootView.findViewById(R.id.ResetButton);
        b_close = (Button) rootView.findViewById(R.id.ResetButton);

        m_start.setTypeface(mainFont);
        m_pause.setTypeface(mainFont);
        m_reset.setTypeface(mainFont);

        m_start.setOnClickListener(this);
        m_pause.setOnClickListener(this);
        m_reset.setOnClickListener(this);
        b_close.setOnClickListener(this);

        mHandler.sendMessageDelayed(Message.obtain(mHandler, TICK_WHAT), mFrequency);
        return rootView;
    }

    private void updateUi() {
        mainActivity.runOnUiThread(new Runnable() {

            public void run() {
                if (stopThread) return;
                if (swatch != null)
                    m_elapsedTime.setText(long2string((swatch.getElapsedTime())));

                switch (status) {
                    case START:
                        showPauseLapButtons();
                        break;
                    case PAUSE:
                        showStartResetButtons();
                        break;
                    case RESET:
                        break;
                }
            }//run

        });
    }

    public void onClick(View _view) {

        if (_view == b_close) close();
        if (_view == m_start) onStartClicked();
        if (_view == m_pause) onPauseClicked();
        if (_view == m_reset) onResetClicked();

    }//onClick

    private void showCorrectButtons() {
        Log.d(TAG, "showCorrectButtons");

        if (swatch != null) {
            if (swatch.isRunning()) {
                showPauseLapButtons();
            } else {
                showStartResetButtons();
            }
        }
    }

    private void showPauseLapButtons() {
        Log.d(TAG, "showPauseLapButtons");

        m_start.setVisibility(View.GONE);
        m_reset.setVisibility(View.GONE);
        m_pause.setVisibility(View.VISIBLE);
    }

    private void showStartResetButtons() {
        Log.d(TAG, "showStartResetButtons");

        m_start.setVisibility(View.VISIBLE);
        m_reset.setVisibility(View.VISIBLE);
        m_pause.setVisibility(View.GONE);
    }

    public void onStartClicked() {
        Log.d(TAG, "start button clicked");
        swatch.start();
        status = START;
        updateUi();
    }

    public void onPauseClicked() {
        Log.d(TAG, "pause button clicked");
        swatch.pause();
        status = PAUSE;

        updateUi();

    }

    public void onResetClicked() {
        Log.d(TAG, "reset button clicked");
        swatch.reset();
        status = RESET;
        updateUi();


    }


    public void updateElapsedTime() {
        if (swatch != null)
            updateUi();
    }

    private String long2string(Long number) {
        String text;


        Long hours = number / (1000 * 60 * 60) % 24;
        Long min = (number - hours) / (1000 * 60) % 60;
        Long sec = (number - hours - min) / (1000) % 60;
        Long msec = (number / 100) % 10;
        text = hours.toString() + ":" + min.toString() + ":" + sec.toString() + ":" + msec.toString();

        return text;
    }

    public void close() {
        mainActivity.backFragment();
    }

    boolean exit_pressed = false;

    //Aria Service events
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGesturePerformed(FlicktekCommands.onGestureEvent gestureEvent) {
        int gesture = gestureEvent.status;
        switch (gesture) {
            case (FlicktekManager.GESTURE_UP):
                onResetClicked();
                break;
            case (FlicktekManager.GESTURE_DOWN):
                onResetClicked();
                break;
            case (FlicktekManager.GESTURE_ENTER):
                if (swatch.isRunning())
                    onPauseClicked();
                else
                    onStartClicked();
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
        updateUi();
    }

    public class GestureTimer extends CountDownTimer {
        private boolean isRunning;

        public GestureTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            isRunning = true;
        }

        @Override
        public void onFinish() {
            //isRunning = false;
            // updateUi();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        stopThread = false;
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        stopThread = true;
        EventBus.getDefault().unregister(this);
    }
}
