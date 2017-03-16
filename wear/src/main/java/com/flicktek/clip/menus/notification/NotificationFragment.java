package com.flicktek.clip.menus.notification;

import android.app.Fragment;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.flicktek.clip.FlicktekCommands;
import com.flicktek.clip.FlicktekManager;
import com.flicktek.clip.MainActivity;
import com.flicktek.clip.R;
import com.flicktek.clip.wearable.common.NotificationModel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class NotificationFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "MediaFragment";
    private static final String ARG_MODEL_ID = "MODEL_ID";
    private static final String JSON_CONFIGURATION = "configuration";

    private final int STATUS_PLAY = 0;
    private final int STATUS_NEXT = 1;
    private final int STATUS_PREV = 2;
    private final int STATUS_EXIT = 3;

    private MainActivity mainActivity;
    private ImageView iClose;
    private RelativeLayout bg_background_image;
    private TextView tv_control;
    private TextView tv_content;
    private int status;

    private NotificationModel model;

    // Default bundle constructor as google best practices
    public static NotificationFragment newInstance(NotificationModel model) {
        NotificationFragment myFragment = new NotificationFragment();
        Bundle args = new Bundle();

        // We pass the ID araound so we have to query the FlicktekManager for the Model
        args.putString(ARG_MODEL_ID, model.getKeyId());
        myFragment.setArguments(args);
        return myFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = (MainActivity) getActivity();

        if (model == null) {
            String notification_key = getArguments().getString(ARG_MODEL_ID);
            model = FlicktekManager.getNotificationModelByKey(notification_key);
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = null;

        rootView = inflater.inflate(R.layout.fragment_notification, container, false);

        Typeface mainFont = Typeface.createFromAsset(mainActivity.getAssets(), getString(R.string.main_font));

        iClose = (ImageView) rootView.findViewById(R.id.iClose);
        iClose.setVisibility(View.VISIBLE);
        iClose.setOnClickListener(this);

        iClose.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: {
                        ImageView view = (ImageView) v;
                        //overlay is black with transparency of 0x77 (119)
                        view.getDrawable().setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
                        view.invalidate();
                        break;
                    }
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL: {
                        ImageView view = (ImageView) v;
                        //clear the overlay
                        view.getDrawable().clearColorFilter();
                        view.invalidate();
                        break;
                    }
                }

                return false;
            }
        });


        try {
            String title = "Empty";

            TextView tvTitle = (TextView) rootView.findViewById(R.id.tv_title);
            tvTitle.setText(model.getTitle());

            tv_control = (TextView) rootView.findViewById(R.id.tv_control);
            tv_control.setTypeface(mainFont);
            tv_control.setVisibility(View.INVISIBLE);

            tv_content = (TextView) rootView.findViewById(R.id.tv_content);
            tv_content.setTypeface(mainFont);

            if (model.getText() != null)
                tv_content.setText(model.getText());
            else
                tv_content.setVisibility(View.INVISIBLE);

        } catch (Exception e) {

        }

        /*
        bg_background_image = (RelativeLayout) rootView.findViewById(R.id.media_background);

        Drawable drawable = Helpers.getDrawableFromResources(mainActivity, "background", config);
        iCover = (ImageView) rootView.findViewById(R.id.imageCover);
        if (drawable != null)
            iCover.setImageDrawable(drawable);
         */

        return rootView;
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
        mainActivity.runOnUiThread(new Runnable() {

            public void run() {
                Animation showColor = AnimationUtils.loadAnimation(mainActivity.getApplicationContext(),
                        R.anim.fade_in_soft);

                switch (status) {
                    case STATUS_PLAY:
                        iClose.startAnimation(showColor);
                        break;
                    case STATUS_NEXT:
                        //iconNext.startAnimation(showColor);
                        break;
                    case STATUS_PREV:
                        //iconPrev.startAnimation(showColor);
                        break;
                    case STATUS_EXIT:
                        break;
                }
                ;
            }//run

        });
    }

    public void onClick(View _view) {
        if (_view == iClose) {
            mainActivity.backFragment();
            return;
        }
        doGesture();
        updateUi();
    }

    public void close() {
        FlicktekManager.backMenu(mainActivity);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGesturePerformed(FlicktekCommands.onGestureEvent gestureEvent) {
        int gesture = gestureEvent.status;

        switch (gesture) {
            case (FlicktekManager.GESTURE_BACK):
                status = STATUS_EXIT;
                mainActivity.backFragment();
                break;
            case (FlicktekManager.GESTURE_HOME):
                status = STATUS_EXIT;
                break;
            case (FlicktekManager.GESTURE_ENTER):
                status = STATUS_PLAY;
                break;
            case (FlicktekManager.GESTURE_DOWN):
                status = STATUS_NEXT;
                break;
            case (FlicktekManager.GESTURE_UP):
                status = STATUS_PREV;
                break;
        }
        doGesture();
        updateUi();
    }

    boolean exit_pressed = false;

    private void doGesture() {
        switch (status) {
            case (STATUS_EXIT):
                exit_pressed = !FlicktekManager.mIsDoubleGestureHomeExit;

                if (!exit_pressed) {
                    mainActivity.runOnUiThread(
                            new Runnable() {
                                public void run() {
                                    Toast.makeText(mainActivity.getApplicationContext(),
                                            "Perform HOME again to go back!",
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                } else {
                    close();
                }
                exit_pressed = true;
                return;
            case (STATUS_PLAY):
                break;
            case (STATUS_NEXT):
                break;
            case (STATUS_PREV):
                break;
        }
        exit_pressed = false;
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
            isRunning = false;
            updateUi();
        }
    }
}
