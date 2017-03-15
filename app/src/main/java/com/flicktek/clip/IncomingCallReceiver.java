package com.flicktek.clip;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.Method;

public class IncomingCallReceiver extends BroadcastReceiver {
    public static String TAG = "IncomingCall";
    private ITelephony telephonyService;
    private static String mLastState;

    public interface ITelephony {
        boolean endCall();

        void answerRingingCall();

        void silenceRinger();
    }

    public void onEndCall() {
        Log.v(TAG, "+ HANGUP");

        if (telephonyService == null) {
            Log.v(TAG, "! No telephone service");
            return;
        }

        try {
            telephonyService.silenceRinger();
            telephonyService.endCall();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        String phoneNumber = bundle.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
        String state = bundle.getString(TelephonyManager.EXTRA_STATE);

        Log.e(TAG, "============================ INCOMING CALL ============================");
        Log.e(TAG, " Number " + phoneNumber );
        Log.e(TAG, " State " + state);

        //EXTRA_STATE_IDLE
        //EXTRA_STATE_RINGING
        //EXTRA_STATE_OFFHOOK

        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Class c = Class.forName(tm.getClass().getName());
            Method m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            telephonyService = (ITelephony) m.invoke(tm);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}