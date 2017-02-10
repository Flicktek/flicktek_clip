package com.flicktek.android.clip;


import android.content.Context;
import android.util.Log;

import com.flicktek.android.clip.uart.UARTProfile;

import org.greenrobot.eventbus.EventBus;

import static android.os.Debug.isDebuggerConnected;

public class FlicktekCommands extends UARTProfile {
    private final String TAG = "FlicktekCommands";

    // First handshake between devices happened
    public boolean mIsHandshakeOk = false;

    // Singleton
    private static FlicktekCommands mInstance = null;

    public static FlicktekCommands getInstance() {
        if (mInstance == null)
            mInstance = new FlicktekCommands();
        return mInstance;
    }

    // Settings command values
    public final static int SET_NUMBER_GESTURE = 0xC0;
    public final static int SET_NUMBER_REPETITION = 0xC1;

    // Calibration attribute present
    public final static int CALIBRATION_IS_PRESENT = 1;
    public final static int CALIBRATION_IS_NOT_PRESENT = 0;

    // Calibration mode values
    public final static int CALIBRATION_MODE_NONE = 0;
    public final static int STATUS_CALIB = 1;
    public final static int STATUS_EXEC = 2;
    public final static int STATUS_SLEEP = 3;
    public final static int STATUS_IDLE = 0;
    public final static int STATUS_PRECALIB_AMP = 4;
    public final static int STATUS_PRECALIB_CAD = 5;
    public final static int STATUS_PRECALIB_SIM = 6;
    public final static int STATUS_PRECALIB_DEB = 7;

    // Gesture status values
    public final static int GESTURE_STATUS_NONE = 0;
    public final static int GESTURE_STATUS_STARTED = 1;
    public final static int GESTURE_STATUS_RECORDING = 2;
    public final static int GESTURE_STATUS_OK = 3;
    public final static int GESTURE_STATUS_ERROR1 = 4;
    public final static int GESTURE_STATUS_ERROR2 = 5;
    public final static int GESTURE_STATUS_ERROR3 = 6;
    public final static int GESTURE_STATUS_OKREPETITION = 7;
    public final static int GESTURE_STATUS_OKGESTURE = 8;
    public final static int GESTURE_STATUS_OKCALIBRATION = 9;
    public final static int GESTURE_STATUS_OKCAMP = 10;
    public final static int GESTURE_STATUS_OKCAD = 11;
    public final static int GESTURE_STATUS_OKCSIM = 12;

    // Command list
    public final static char COMMAND_START = '{';
    public final static char COMMAND_END = '}';

    public final static char COMMAND_GESTURE = 'G';
    public final static char COMMAND_CAS_GESTURE_STATUS = 'S';
    public final static char COMMAND_CAS_ERROR = 'E';
    public final static char COMMAND_CAS_GESTURE_QUALITY = 'Q';
    public final static char COMMAND_CAS_GESTURE_FEEDBACK = 'F';
    public final static char COMMAND_CAS_IS_CALIBRATED = 'C';
    public final static char COMMAND_OK = 'O';

    public final static char COMMAND_CAS_WRITE = 'W';
    public final static char COMMAND_SETTING = 'T';
    public final static char COMMAND_SETTING_DATA = 'D';
    public final static char COMMAND_PING = 'P';
    public final static char COMMAND_DEBUG = 'd';
    public final static char COMMAND_VERSION = 'V';

    public final static int VERSION_COMPILATION = 0;
    public final static int VERSION_REVISION = 1;

    // Calibration
    private int numGestures;
    private int numRepetitions;
    private int calibrationStatus;
    private int gestureStatus;
    private int currentGestureIndex;
    private int currentGestureIteration;

    public FlicktekCommands() {
        Log.d(TAG, "FlicktekCommands");
    }

    public void init(Context context) {
        Log.d(TAG, "init: ");
        numGestures = context.getResources().getInteger(R.integer.calibration_gestures);
        numRepetitions = context.getResources().getInteger(R.integer.calibration_iterations);
    }

    public void onQueryForCalibration() {
        Log.v(TAG, "-------------- IS CALIBRATED -------------------");
        writeSingleCommand(COMMAND_CAS_IS_CALIBRATED, 0);
    }

    @Override
    public void onReadyToSendData(boolean ready) {
        Log.v(TAG, "onReadyToSendData " + ready);
        Log.v(TAG, "---------- LETS REPORT WE ARE ALIVE-------------");
        writeSingleCommand(COMMAND_OK, 1);
    }

    public void onDeviceRespondedToConnection() {
        Log.v(TAG, "------------- REQUEST VERSION ------------------");
        writeSingleCommand(COMMAND_VERSION, VERSION_COMPILATION);
        writeSingleCommand(COMMAND_VERSION, VERSION_REVISION);
    }

    public void onGestureChanged(int value) {
        int _value = value;
        switch (_value) {
            case 1:
                _value = FlicktekManager.GESTURE_ENTER;
                break;
            case 2:
                _value = FlicktekManager.GESTURE_HOME;
                break;
            case 3:
                _value = FlicktekManager.GESTURE_UP;
                break;
            case 4:
                _value = FlicktekManager.GESTURE_DOWN;
                break;
            case 5:
                _value = FlicktekManager.GESTURE_BACK;
                break;
        }
        Log.d(TAG, "onGestureChanged: " + value + " = " + _value);
        EventBus.getDefault().post(new onGestureEvent(_value));
    }

    @Override
    protected void onDataArrived(byte[] buf_str) {
        String str = new String(buf_str);
        Log.v(TAG, "RX: [" + str + "]");
        onCommandArrived(buf_str);
    }

    //---------- Write commands -----------------------------------------------------

    public void writeSingleCommand(char command, int value) {
        byte buf[] = new byte[4];
        buf[0] = COMMAND_START;
        buf[1] = (byte) command;

        if (value < '0')
            value += '0';

        buf[2] = (byte) value;
        buf[3] = COMMAND_END;
        send(buf);
    }

    public void writeStatus_Ping() {
        Log.v(TAG, "writeStatus_Ping");
        writeSingleCommand(COMMAND_PING, 1);
    }

    public void writeStatus_Sleep() {
        Log.v(TAG, "writeStatus_Sleep");
        writeSingleCommand(COMMAND_CAS_WRITE, STATUS_SLEEP);
    }

    public void writeStatus_Exec() {
        Log.v(TAG, "writeStatus_Exec");
        writeSingleCommand(COMMAND_CAS_WRITE, STATUS_EXEC);
    }

    public void writeStatus_Calib() {
        Log.v(TAG, "writeStatus_Calib");
        writeSingleCommand(COMMAND_CAS_WRITE, STATUS_CALIB);
    }

    public void writeGestureStatus(int status) {
        Log.v(TAG, "writeGestureStatus " + status);
        writeSingleCommand(COMMAND_CAS_GESTURE_STATUS, status);
    }

    public void writeSettingsCommand(int value) {
        Log.v(TAG, "writeSettingsCommand " + value);
        switch (value) {
            case SET_NUMBER_GESTURE:
                writeSingleCommand(COMMAND_SETTING_DATA, numGestures);
                break;
            case SET_NUMBER_REPETITION:
                writeSingleCommand(COMMAND_SETTING_DATA, numRepetitions);
                break;
        }
        writeSingleCommand(COMMAND_SETTING, value);
    }

    public void writeGestureStatusStart() {
        Log.v(TAG, "writeGestureStatusStart");
        writeSingleCommand(COMMAND_CAS_GESTURE_STATUS, GESTURE_STATUS_STARTED);
    }

    public void writeStatus_Pre_Amp() {
        Log.v(TAG, "writeStatus_Pre_Amp");
        writeSingleCommand(COMMAND_CAS_WRITE, STATUS_PRECALIB_AMP);
    }

    public void writeStatus_Pre_Deb() {
        Log.v(TAG, "writeStatus_Pre_Deb");
        writeSingleCommand(COMMAND_CAS_WRITE, STATUS_PRECALIB_DEB);
    }

    public void writeStatus_Pre_Cad() {
        Log.v(TAG, "writeStatus_Pre_Cad");
        writeSingleCommand(COMMAND_CAS_WRITE, STATUS_PRECALIB_CAD);
    }

    public void writeStatus_Pre_Sim() {
        Log.v(TAG, "writeStatus_Pre_Sim");
        writeSingleCommand(COMMAND_CAS_WRITE, STATUS_PRECALIB_SIM);
    }

    //----------------------------------------------------------------------------

    public void setGesture(int value) {
        Log.d(TAG, "setGesture: ");
        setGesturesNumber(value);
        writeSettingsCommand(SET_NUMBER_GESTURE);
    }

    public void setRepetition(Integer value) {
        Log.d(TAG, "setRepetition: " + value.toString());
        setIterationsNumber(value);
        writeSettingsCommand(SET_NUMBER_REPETITION);
    }

    public int getGesturesNumber() {
        Log.d(TAG, "getGesturesNumber: " + Integer.toString(numGestures));
        return numGestures;
    }

    public void setGesturesNumber(int val) {
        Log.d(TAG, "setGesturesNumber: " + Integer.toString(val));
        numGestures = val;
    }

    public int getIterationsNumber() {
        Log.d(TAG, "getIterationsNumber: " + Integer.toString(numRepetitions));
        return numRepetitions;
    }

    public void setIterationsNumber(int val) {
        Log.d(TAG, "setIterationsNumber: " + Integer.toString(val));
        numRepetitions = val;
    }

    public void startCalibration() {
        Log.d(TAG, "startCalibration: ");
        calibrationStatus = CALIBRATION_MODE_NONE;
        gestureStatus = GESTURE_STATUS_NONE;
        currentGestureIndex = 1;
        currentGestureIteration = 1;
        writeStatus_Calib();

        if (isDebuggerConnected()) {
            Log.d(TAG, "------- DEBUG ACTIVE --------");
            writeSingleCommand(COMMAND_DEBUG, 1);
        }
    }

    public void nextCalibrationStep() {
        Log.d(TAG, "nextCalibrationStep: ");
        writeGestureStatus(GESTURE_STATUS_STARTED);
    }

    public void onCalibrationModeWritten(int _value) {
        Log.d(TAG, "onCalibrationModeWritten " + _value);
        //This info is only used so far for the calibration therefore I move it to the calibration status only
        calibrationStatus = _value;
        if (_value == STATUS_CALIB) {
            EventBus.getDefault().post(new onCalibrationWritten(_value));
            Log.d(TAG, "onCalibrationModeWritten: calibration mode scritto correttamente " + _value);
        }

        if (_value == STATUS_EXEC) {
            Log.d(TAG, "onCalibrationModeWritten: execution mode scritto correttamente " + _value);
        }
    }

    public void repeatCalibrationStep() {
        Log.d(TAG, "repeatCalibrationStep: EMPTY ");
    }

    public void stopCalibration() {
        Log.d(TAG, "stopCalibration: ");
        //TODO: implement the control on the Calibration_Attribute
        writeStatus_Exec();
    }

    public int getCalibrationStatus() {
        Log.d(TAG, "getCalibrationStatus: ");
        return calibrationStatus;
    }

    public int getGestureStatus() {
        Log.d(TAG, "getGestureStatus: ");
        return gestureStatus;
    }

    public int getGestureIndex() {
        Log.d(TAG, "getGestureIndex: ");
        return currentGestureIndex;
    }

    public int getGestureIteration() {
        Log.d(TAG, "getGestureIteration: ");
        return currentGestureIteration;
    }

    public void onGestureStatusWritten(int _value) {
        Log.d(TAG, "onGestureStatusWritten: ");
        if (_value == GESTURE_STATUS_STARTED) {
            gestureStatus = GESTURE_STATUS_STARTED;
            EventBus.getDefault().post(new onCalibrationStepStarted(currentGestureIndex, currentGestureIteration));
            return;
        }
    }

    public void onGestureStatusFeedback(int _value) {
        switch (_value) {
            case GESTURE_STATUS_NONE:
                Log.v(TAG, "GESTURE_STATUS_NONE");
                break;
            case GESTURE_STATUS_STARTED:
                Log.v(TAG, "GESTURE_STATUS_STARTED");
                break;
            case GESTURE_STATUS_RECORDING:
                Log.v(TAG, "GESTURE_STATUS_RECORDING");
                break;
            case GESTURE_STATUS_OK:
                Log.v(TAG, "GESTURE_STATUS_OK");
                break;
            case GESTURE_STATUS_ERROR1:
                Log.v(TAG, "GESTURE_STATUS_ERROR1");
                break;
            case GESTURE_STATUS_ERROR2:
                Log.v(TAG, "GESTURE_STATUS_ERROR2");
                break;
            case GESTURE_STATUS_ERROR3:
                Log.v(TAG, "GESTURE_STATUS_ERROR3");
                break;
            case GESTURE_STATUS_OKREPETITION:
                Log.v(TAG, "GESTURE_STATUS_OKREPETITION");
                break;
            case GESTURE_STATUS_OKGESTURE:
                Log.v(TAG, "GESTURE_STATUS_OKGESTURE");
                break;
            case GESTURE_STATUS_OKCALIBRATION:
                Log.v(TAG, "GESTURE_STATUS_OKCALIBRATION");
                break;
            case GESTURE_STATUS_OKCAMP:
                Log.v(TAG, "GESTURE_STATUS_OKCAMP");
                break;
            case GESTURE_STATUS_OKCAD:
                Log.v(TAG, "GESTURE_STATUS_OKCAD");
                break;
            case GESTURE_STATUS_OKCSIM:
                Log.v(TAG, "GESTURE_STATUS_OKCSIM");
                break;
        }
    }

    public void onCommandArrived(byte[] buf_str) {
        // Found single value command
        if (buf_str[0] == COMMAND_START && buf_str[3] == COMMAND_END) {
            int cmd = buf_str[1];
            int value = buf_str[2];

            // If it is a number we like it in digital form.
            if (value >= '0' && value <= '9') {
                value -= '0';
            }

            switch (cmd) {
                case COMMAND_GESTURE:
                    onGestureChanged(value);
                    return;
                case COMMAND_CAS_GESTURE_STATUS:
                case COMMAND_CAS_GESTURE_FEEDBACK:
                    onGestureStatusFeedback(value);
                    onGestureStatusWritten(value);
                    EventBus.getDefault().post(new onGestureStatusEvent(value));
                    return;
                case COMMAND_OK:
                    if (value == 'K') {
                        Log.d(TAG, "OK FOUND!");
                        if (!mIsHandshakeOk) {
                            mIsHandshakeOk = true;
                            onDeviceRespondedToConnection();
                            EventBus.getDefault().post(new onDeviceReady());
                        }
                    }
                    break;
            }
            return;
        }

        // Value written correctly
        if (buf_str[0] == 'A' && buf_str[1] == 'C' && buf_str[2] == 'K') {
            int cmd = buf_str[4];
            int value = buf_str[5] - '0';

            switch (cmd) {
                case COMMAND_CAS_IS_CALIBRATED:
                    if (value == 0) {
                        Log.v(TAG, "Aria is not calibrated!");
                        EventBus.getDefault().post(new onNotCalibrated());
                    } else {
                        writeStatus_Exec();
                    }
                    break;
                case COMMAND_CAS_WRITE:
                    if (value == STATUS_CALIB) {
                        onCalibrationModeWritten(value);
                    }
                    return;
                case COMMAND_CAS_GESTURE_STATUS:
                    onGestureStatusFeedback(value);
                    onGestureStatusWritten(value);
                    return;
                default:
                    break;
            }

            return;
        }

        //EventBus.getDefault().post(new CharacterEvent(_value));
    }

    @Override
    protected void release() {
        super.release();
        mIsHandshakeOk = false;
    }

    //------------------------------------------------------------------------------
    // FLICKTEK MESSAGES
    //------------------------------------------------------------------------------

    public class onNotCalibrated {
    }

    public class onCalibrationWritten {
        public int status;

        public onCalibrationWritten(int status) {
            this.status = status;
        }
    }

    public class onCalibrationStepStarted {
        public int currentGestureIndex;
        public int currentGestureIteration;

        public onCalibrationStepStarted(int currentGestureIndex, int currentGestureIteration) {
            this.currentGestureIndex = currentGestureIndex;
            this.currentGestureIteration = currentGestureIteration;
        }
    }

    public class onGestureStatusEvent {
        public Integer status;
        public Integer value;
        public Integer unit;
        public Integer decimal;

        public onGestureStatusEvent(int status) {
            this.value = status;
            this.unit = status % 10;
            this.status = this.unit;
            status = status / 10;
            this.decimal = status % 10;
        }
    }

    public class onCalibrationStepEvent {
    }

    public class onDeviceReady {

    }

    public class onCalibrationAttributeEvent {
        public Integer quality;
        public Integer unit;
        public Integer decimal;

        public onCalibrationAttributeEvent(int quality) {
            this.quality = quality;
            this.unit = quality % 10;
            quality = quality / 10;
            this.decimal = quality % 10;
        }
    }

    public class onGestureEvent {
        public  Integer status;
        public  Integer quality;

        public onGestureEvent(int value) {
            this.status = value;
        }
        public onGestureEvent(int value, int quality) {

            this.status = value;
            this.quality = quality;
        }
    }
}
