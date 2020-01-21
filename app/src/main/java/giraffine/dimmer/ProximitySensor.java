package giraffine.dimmer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class ProximitySensor {

    public static float DEFAULT_DISTANCE = 5;

    private Context mContext;
    private SensorManager mSensorManager;
    private Sensor mSensorProximity;
    private SensorEventListener mSensorEventListener;
    private EventCallback mEventCallback = null;
    private float mMax = Prefs.getProximity(true);
    private float mMin = Prefs.getProximity(false);
    private float mCurrent = DEFAULT_DISTANCE;

    interface EventCallback {
        public void onNear();

        public void onFar();
    }

    public ProximitySensor(Context context, EventCallback callback) {
        mContext = context;
        mEventCallback = callback;
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null)
            mSensorProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    }

    public boolean hasProximitySensor() {
        if (mSensorManager == null || mSensorProximity == null)
            return false;
        else
            return true;
    }

    public boolean monitor(boolean isOn) {
        Log.e(Dimmer.TAG, "ProximitySensor.monitor  isOn=" + isOn);
        if (mSensorManager == null || mSensorProximity == null)
            return false;

        mCurrent = DEFAULT_DISTANCE;

        if (mSensorEventListener == null)
            mSensorEventListener = new SensorEventListener() {
                @Override
                public void onAccuracyChanged(Sensor arg0, int arg1) {
                    Log.e(Dimmer.TAG, "Proximity:: onAccuracyChanged: " + arg0.toString() + ", arg1=" + arg1);
                }

                @Override
                public void onSensorChanged(SensorEvent arg0) {
                    Log.e(Dimmer.TAG, "Proximity:: onSensorChanged: " + arg0.values[0]);
                    mCurrent = arg0.values[0];
                    if (mMax < mCurrent) {
                        mMax = mCurrent;
                        Prefs.setProximity(true, mMax);
                    }
                    if (mMin > mCurrent) {
                        mMin = mCurrent;
                        Prefs.setProximity(false, mMin);
                    }

                    if (mCurrent == mMin)
                        mEventCallback.onNear();
                    else
                        mEventCallback.onFar();
                }
            };
        if (isOn)
            mSensorManager.registerListener(mSensorEventListener, mSensorProximity, SensorManager.SENSOR_DELAY_FASTEST);
        else
            mSensorManager.unregisterListener(mSensorEventListener);
        return true;
    }
}
