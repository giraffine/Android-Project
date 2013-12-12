package giraffine.dimmer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class ProximitySensor {

	private Context mContext;
	private SensorManager mSensorManager;
	private Sensor mSensorProximity;
	private SensorEventListener mSensorEventListener;
	private float mMax = 5;
	private float mMin = 5;
	private float mCurrent;
	
	public ProximitySensor(Context context)
	{
		mContext = context;
		mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
		if(mSensorManager != null)
			mSensorProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
	}
	public boolean hasProximitySensor()
	{
	    if(mSensorManager == null || mSensorProximity == null)
	    	return false;
	    else
	    	return true;
	}
	public boolean monitor(boolean isOn)
	{
		Log.e(Dimmer.TAG, "ProximitySensor.monitor  isOn=" + isOn);
	    if(mSensorManager == null || mSensorProximity == null)
	    	return false;

		if(mSensorEventListener == null)
			mSensorEventListener = new SensorEventListener(){
				@Override
				public void onAccuracyChanged(Sensor arg0, int arg1) {
					Log.e(Dimmer.TAG, "Proximity:: onAccuracyChanged: " + arg0.toString() + ", arg1=" + arg1);
				}
				@Override
				public void onSensorChanged(SensorEvent arg0) {
					Log.e(Dimmer.TAG, "Proximity:: onSensorChanged: " + arg0.values[0]);
					mCurrent = arg0.values[0];
					if(mMax < mCurrent)	mMax = mCurrent;
					if(mMin > mCurrent)	mMin = mCurrent;
				}
	        };
        if(isOn)
        	mSensorManager.registerListener(mSensorEventListener, mSensorProximity, SensorManager.SENSOR_DELAY_FASTEST);
        else
        	mSensorManager.unregisterListener(mSensorEventListener);
        return true;
	}
	public boolean isCovered()
	{
		if(!hasProximitySensor())
			return false;
		if(mCurrent == mMin)
			return true;
		return false;
	}
}
