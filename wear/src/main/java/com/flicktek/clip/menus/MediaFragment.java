package com.flicktek.clip.menus;

import android.app.Fragment;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.flicktek.clip.FlicktekCommands;
import com.flicktek.clip.FlicktekManager;
import com.flicktek.clip.MainActivity;
import com.flicktek.clip.R;
import com.flicktek.clip.util.Helpers;
import com.flicktek.clip.wearable.common.Constants;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MediaFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "MediaFragment";
    private static final String ARG_JSON = "JSON";
    private static final String JSON_CONFIGURATION = "configuration";

    private final int STATUS_PLAY = 0;
    private final int STATUS_NEXT = 1;
    private final int STATUS_PREV = 2;
    private final int STATUS_EXIT = 3;

    private MainActivity mainActivity;
    private Button bClose;
    private ImageView iconPrev;
    private ImageView iconPP;
    private ImageView iconNext;
    private ImageView iCover;
    private RelativeLayout bg_background_image;
    private TextView tv_control;
    private int status;

    private JSONObject config;

    private class MediaAction {
        private String gesture;
        JSONObject config;

        MediaAction(JSONObject config) {
            gesture = "none";
            this.config = config;

            try {
                gesture = config.getString("onGesture");
            } catch (Exception e) {
                Log.v(TAG, "No activity to be launched");
            }
        }

        public String getGesture() {
            return gesture;
        }

        public void performAction() {
            try {
                String intent = config.getString("intent");
                mainActivity.sendMessageToHandheld(mainActivity.getApplicationContext(),
                        Constants.FLICKTEK_CLIP.LAUNCH_INTENT, intent);
            } catch (Exception e) {
                Log.v(TAG, "No activity to be launched");
            }
        }
    }

    private MediaAction media_UP;
    private MediaAction media_DOWN;
    private MediaAction media_ENTER;
    private MediaAction media_HOME;

    // Default bundle constructor as google best practices
    public static MediaFragment newInstance(String jsonString) {
        MediaFragment myFragment = new MediaFragment();
        Bundle args = new Bundle();
        args.putString(ARG_JSON, jsonString);
        myFragment.setArguments(args);
        return myFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = (MainActivity) getActivity();

        String json = getArguments().getString(ARG_JSON);

        try {
            JSONObject parent = new JSONObject(json);
            String configuration_json = parent.getString(JSON_CONFIGURATION);
            config = Helpers.getJsonFromResources((MainActivity) getActivity(), configuration_json);

            try {
                String fragment_name = config.getString("fragment");
                mainActivity.sendMessageToHandheld(mainActivity.getApplicationContext(),
                        Constants.FLICKTEK_CLIP.LAUNCH_FRAGMENT, fragment_name);
            } catch (Exception e) {
                Log.v(TAG, "No activity to be launched");
            }

            try {
                String activity_name = config.getString("activity");
                mainActivity.sendMessageToHandheld(mainActivity.getApplicationContext(),
                        Constants.FLICKTEK_CLIP.LAUNCH_INTENT, activity_name);

            } catch (Exception e) {
                Log.v(TAG, "No activity to be launched");
            }

            try {
                JSONArray menu_items = config.getJSONArray("media_actions");
                for (int i = 0; i < menu_items.length(); i++) {
                    JSONObject item_config = menu_items.getJSONObject(i);
                    MediaAction mediaAction = new MediaAction(item_config);

                    switch (mediaAction.getGesture()) {
                        case "up":
                            media_UP = mediaAction;
                            break;
                        case "down":
                            media_DOWN = mediaAction;
                            break;
                        case "enter":
                            media_ENTER = mediaAction;
                            break;
                        case "home":
                            media_HOME = mediaAction;
                            break;
                    }
                }
            } catch (Exception e) {
                Log.v(TAG, "Error parsing actions " + e.toString());
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed parsing JSON");
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = null;

        rootView = inflater.inflate(R.layout.fragment_media, container, false);

        Typeface mainFont = Typeface.createFromAsset(mainActivity.getAssets(), getString(R.string.main_font));

        bClose = (Button) rootView.findViewById(R.id.b_media_close);
        bClose.setTypeface(mainFont);
        bClose.setOnClickListener(this);

        bClose.setVisibility(View.VISIBLE);

        if (config == null) {
            mainActivity.showToastMessage("Missing configuration");
            return rootView;
        }

        try {
            String title = "Empty";

            TextView tvTitle = (TextView) rootView.findViewById(R.id.tv_media_title);
            try {
                title = config.getString("title");
            } catch (Exception e) {

            }
            tvTitle.setText(title);

            tv_control = (TextView) rootView.findViewById(R.id.tv_control);
            tv_control.setTypeface(mainFont);
            tv_control.setVisibility(View.INVISIBLE);

            iconPrev = (ImageView) rootView.findViewById(R.id.iV);
            iconPrev.setVisibility(View.VISIBLE);
            iconPrev.setOnClickListener(this);

            iconPrev.setOnTouchListener(new View.OnTouchListener() {

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

            iconPP = (ImageView) rootView.findViewById(R.id.iPP);
            iconPP.setVisibility(View.VISIBLE);
            iconPP.setOnClickListener(this);

            iconPP.setOnTouchListener(new View.OnTouchListener() {

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

            iconNext = (ImageView) rootView.findViewById(R.id.iN);
            iconNext.setVisibility(View.VISIBLE);
            iconNext.setOnClickListener(this);

            iconNext.setOnTouchListener(new View.OnTouchListener() {

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

        } catch (Exception e) {

        }

        bg_background_image = (RelativeLayout) rootView.findViewById(R.id.media_background);

        Drawable drawable = Helpers.getDrawableFromResources(mainActivity, "background", config);
        iCover = (ImageView) rootView.findViewById(R.id.imageCover);
        if (drawable != null)
            iCover.setImageDrawable(drawable);

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
                        iconPP.startAnimation(showColor);
                        break;
                    case STATUS_NEXT:
                        iconNext.startAnimation(showColor);
                        break;
                    case STATUS_PREV:
                        iconPrev.startAnimation(showColor);
                        break;
                    case STATUS_EXIT:
                        break;
                }
                ;
            }//run

        });
    }

    public void onClick(View _view) {
        if (_view == bClose) {
            close();
            return;
        }
        if (_view == iconNext) {
            status = STATUS_NEXT;
            mainActivity.sendGestureToHandheld(FlicktekManager.GESTURE_DOWN);
        }
        if (_view == iconPP) {
            status = STATUS_PLAY;
            mainActivity.sendGestureToHandheld(FlicktekManager.GESTURE_ENTER);
        }
        if (_view == iconPrev) {
            status = STATUS_PREV;
            mainActivity.sendGestureToHandheld(FlicktekManager.GESTURE_UP);
        }

        doGesture();
        updateUi();
    }

    public void close() {
        if (media_HOME !=null)
            media_HOME.performAction();

        FlicktekManager.backMenu(mainActivity);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGesturePerformed(FlicktekCommands.onGestureEvent gestureEvent) {
        int gesture = gestureEvent.status;

        switch (gesture) {
            case (FlicktekManager.GESTURE_BACK):
                status = STATUS_EXIT;
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
                if (media_ENTER!=null)
                    media_ENTER.performAction();
                break;
            case (STATUS_NEXT):
                if (media_DOWN!=null)
                    media_DOWN.performAction();
                break;
            case (STATUS_PREV):
                if (media_UP!=null)
                    media_UP.performAction();
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
