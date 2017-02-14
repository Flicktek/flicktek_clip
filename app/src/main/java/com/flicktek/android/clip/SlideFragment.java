package com.flicktek.android.clip;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
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

import com.flicktek.android.clip.util.Helpers;
import com.flicktek.android.clip.wearable.WearListenerService;

import org.json.JSONObject;

public class SlideFragment extends Fragment implements View.OnClickListener, WearListenerService.MyGestureListener {
    private static final String TAG = "SlideFragment";
    private static final String ARG_JSON = "JSON";
    private static final String ARG_EXTRA = "EXTRA";
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

    private int slide = 0;
    private int max_slides = 18;

    private JSONObject config;

    // Default bundle constructor as google best practices
    public static SlideFragment newInstance(String jsonString, String extra) {
        SlideFragment myFragment = new SlideFragment();
        Bundle args = new Bundle();
        args.putString(ARG_JSON, jsonString);
        args.putString(ARG_EXTRA, extra);
        myFragment.setArguments(args);
        return myFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() == null)
            return;

        try {
            String configuration_json = getArguments().getString(ARG_JSON);
            config = Helpers.getJsonFromResources((MainActivity) getActivity(), configuration_json);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed parsing JSON");
        }
        WearListenerService.setCustomObjectListener(this);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();
        View rootView = null;

        rootView = inflater.inflate(R.layout.fragment_media, container, false);

        String title = "No title";
        TextView tvTitle = (TextView) rootView.findViewById(R.id.tv_media_title);

        try {
            title = config.getString("title");
        } catch (Exception e) {

        }
        tvTitle.setText(title);

        tv_control = (TextView) rootView.findViewById(R.id.tv_control);
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

        iconPP = (ImageView) rootView.findViewById(R.id.iClose);
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

        bg_background_image = (RelativeLayout) rootView.findViewById(R.id.media_background);

        Drawable drawable = Helpers.getDrawableFromResources(mainActivity, "background", config);

        try {
            String extra = getArguments().getString(ARG_EXTRA);
            slide = Integer.valueOf(extra);

            String name = null;
            if (slide > 0) {
                name = config.getString("slides_name") + slide;
                max_slides = config.getInt("slides_number");
                drawable = Helpers.getDrawableFromResourcesByName(mainActivity, name, config);
            }

        } catch (Exception e) {

        }

        iCover = (ImageView) rootView.findViewById(R.id.imageCover);
        if (drawable != null)
            iCover.setImageDrawable(drawable);

        return rootView;
    }

    @Override
    public void onResume() {
        WearListenerService.mApplicationActive = true;
        WearListenerService.setCustomObjectListener(this);
        super.onResume();
    }

    @Override
    public void onPause() {
        WearListenerService.mApplicationActive = true;
        super.onPause();
    }

    public void showFragment(final Fragment _fragment, final boolean _isNext) {
        Log.d(TAG, "showFragment: ");
        mainActivity.runOnUiThread(new Runnable() {

            public void run() {
                try {
                    FragmentManager fragmentManager = getFragmentManager();
                    FragmentTransaction transaction = fragmentManager.beginTransaction();

                    if (_isNext == true) {
                        transaction.setCustomAnimations(R.animator.fade_in_right, R.animator.fade_out_left);
                    } else {
                        transaction.setCustomAnimations(R.animator.fade_in_left, R.animator.fade_out_right);
                    }

                    transaction.replace(R.id.container, _fragment);
                    transaction.commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void updateUi() {
        mainActivity.runOnUiThread(new Runnable() {

            public void run() {
                Animation showColor = AnimationUtils.loadAnimation(mainActivity.getApplicationContext(),
                        R.anim.fade_in_soft);

                SlideFragment frag;

                switch (status) {
                    case STATUS_PLAY:
                        iconPP.startAnimation(showColor);
                        frag = SlideFragment.newInstance("media_slide", "1");
                        showFragment(frag, false);
                        break;
                    case STATUS_NEXT:
                        iconNext.startAnimation(showColor);
                        slide++;

                        if (slide > max_slides)
                            slide = 1;

                        frag = SlideFragment.newInstance("media_slide", Integer.toString(slide));
                        showFragment(frag, true);
                        break;
                    case STATUS_PREV:
                        iconPrev.startAnimation(showColor);
                        slide--;
                        if (slide < 0)
                            slide = 0;
                        frag = SlideFragment.newInstance("media_slide", Integer.toString(slide));
                        showFragment(frag, false);
                        break;
                    case STATUS_EXIT:
                        break;

                }
                ;
            }//run

        });
    }

    public void onClick(View _view) {
        if (_view == bClose)
            close();
        if (_view == iconNext) {
            status = STATUS_NEXT;
            updateUi();
        }
        if (_view == iconPP) {
            status = STATUS_PLAY;
            updateUi();
        }
        if (_view == iconPrev) {
            status = STATUS_PREV;
            updateUi();
        }
    }

    public void close() {

    }

    @Override
    public void onGestureReceived(final String gesture) {
        if (gesture.equals("DOWN")) {
            status = STATUS_NEXT;
            updateUi();
        } else if (gesture.equals("HOME")) {
            status = STATUS_PLAY;
            updateUi();
        } else if (gesture.equals("UP")) {
            status = STATUS_PREV;
            updateUi();
        }

        mainActivity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    Toast toast = Toast.makeText(mainActivity.getApplicationContext(), gesture, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.TOP | Gravity.RIGHT, 0, 0);
                    toast.show();
                } catch (Exception e) {
                };
            };
        });
    }
}
