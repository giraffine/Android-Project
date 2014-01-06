package giraffine.dimmer;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class LightSensor implements ProximitySensor.EventCallback{

	private static final int MSG_BASE = 100;
	private static final int MSG_ENTER_DARKLIGHT = 1 + MSG_BASE;
	private static final int MSG_LEAVE_DARKLIGHT = 2 + MSG_BASE;
	private static final int MSG_ENSURE_COVERED = 3 + MSG_BASE;
	private static final int MSG_SENSOR_INPUT = 4 + MSG_BASE;
	
	private Context mContext;
	private SensorManager mSensorManager;
	private Sensor mSensorLight;
	private SensorEventListener mSensorEventListener;
	private int mCurrentLux;
	private int mFreezeLux = 9999999;
	private EventCallback mEventCallback = null;
	private ProximitySensor mProximitySensor = null;
	private boolean mDimState = false;
	private int mDelayEnterDark = 5000;
	private int mDelayLeaveDark = 2000;
	private int mDelayCheckCover = 3000;
	
	interface EventCallback
	{
		public void onLightChanged(int lux);
		public void onEnterDarkLight();
		public void onLeaveDarkLight();
	}

	public LightSensor(Context context, EventCallback eventcallback)
	{
		LuxUtil.init(context);
		mProximitySensor = new ProximitySensor(context, this);
		mContext = context;
		mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
		if(mSensorManager != null)
			mSensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		mEventCallback = eventcallback;
		updateSensitive();
	}
	public static boolean hasLightSensor(Context context)
	{
		SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		Sensor sensorLight = null;
		if(sensorManager != null)
			sensorLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
	    if(sensorManager == null || sensorLight == null)
	    	return false;
	    else
	    	return true;
	}
	public boolean monitor(boolean isOn)
	{
		if(!isOn)
		{
			mProximitySensor.monitor(false);
			mHandler.removeMessages(MSG_ENTER_DARKLIGHT);
			mHandler.removeMessages(MSG_LEAVE_DARKLIGHT);
			mHandler.removeMessages(MSG_ENSURE_COVERED);
			mHandler.removeMessages(MSG_SENSOR_INPUT);
		}
		
	    if(mSensorManager == null || mSensorLight == null)
	    	return false;

		if(mSensorEventListener == null)
			mSensorEventListener = new SensorEventListener(){
				@Override
				public void onAccuracyChanged(Sensor arg0, int arg1) {
					Log.e(Dimmer.TAG, "onAccuracyChanged: " + arg0.toString() + ", arg1=" + arg1);
				}
				@Override
				public void onSensorChanged(SensorEvent arg0) {
					Log.e(Dimmer.TAG, "onSensorChanged: " + arg0.values[0]);
					mHandler.removeMessages(MSG_SENSOR_INPUT);
					mHandler.sendMessage(mHandler.obtainMessage(MSG_SENSOR_INPUT, (int)arg0.values[0], 0));
				}
	        };
        if(isOn)
        	mSensorManager.registerListener(mSensorEventListener, mSensorLight, SensorManager.SENSOR_DELAY_FASTEST);
        else
        {
        	mSensorManager.unregisterListener(mSensorEventListener);
        	Point bound = new Point();
        	LuxUtil.getBoundaryLevel(bound);
        	mCurrentLux = bound.x;
        }
        return true;
	}
	public int getCurrentLux()
	{
		return mCurrentLux;
	}
	public void setFreezeLux()
	{
		mFreezeLux = mCurrentLux;
	}
	public void setDimState(boolean dim)
	{
		mDimState = dim;
	}
	public void updateSensitive()
	{
		switch(Prefs.getSpeed(Prefs.PREF_SPEED_DIM))
		{
		case 1:	mDelayEnterDark = 1000;	break;
		case 2:	mDelayEnterDark = 3000;	break;
		case 3:	mDelayEnterDark = 5000;	break;
		case 4:	mDelayEnterDark = 7000;	break;
		case 5:	mDelayEnterDark = 10000;break;
		}
		switch(Prefs.getSpeed(Prefs.PREF_SPEED_BRIGHT))
		{
		case 1:	mDelayLeaveDark = 500;	break;
		case 2:	mDelayLeaveDark = 1000;	break;
		case 3:	mDelayLeaveDark = 2000;	break;
		case 4:	mDelayLeaveDark = 5000;	break;
		case 5:	mDelayLeaveDark = 10000;break;
		}
		mDelayCheckCover = (int)(mDelayEnterDark * 0.6);
	}
	private boolean meetDarkThreshold(int lux)
	{
		if(Prefs.getThresholdDimLowest())
			return LuxUtil.isLowestLevel(lux);
		else
			return lux <= Prefs.getThresholdDim();
	}
	private void sensorInput(int lux)
	{
		mCurrentLux = lux;
		mEventCallback.onLightChanged(lux);
		LuxUtil.setLuxLevel(mCurrentLux);
		
//		Log.e(Dimmer.TAG, "sensorInput: " + LuxUtil.dumpLuxLevel());
		
		if(!mDimState)
		{
			// need include proximity sensor to avoid cover situation
			if(meetDarkThreshold(mCurrentLux))
			{
				mProximitySensor.monitor(true);
				mHandler.sendEmptyMessageDelayed(MSG_ENTER_DARKLIGHT, mDelayEnterDark);
			}
			else
			{
				mProximitySensor.monitor(false);
				mHandler.removeMessages(MSG_ENTER_DARKLIGHT);
			}
		}
		else
		{
			mProximitySensor.monitor(false);
			if(mCurrentLux > mFreezeLux + Prefs.getThresholdBright())
			{
				if(!mHandler.hasMessages(MSG_LEAVE_DARKLIGHT))
					mHandler.sendEmptyMessageDelayed(MSG_LEAVE_DARKLIGHT, mDelayLeaveDark);
			}
			else
				mHandler.removeMessages(MSG_LEAVE_DARKLIGHT);
		}
	}
	private Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_SENSOR_INPUT:
				sensorInput(msg.arg1);
				break;
			case MSG_ENTER_DARKLIGHT:
				mProximitySensor.monitor(false);
				mEventCallback.onEnterDarkLight();
				break;
			case MSG_LEAVE_DARKLIGHT:
				mProximitySensor.monitor(false);
				mEventCallback.onLeaveDarkLight();
				break;
			case MSG_ENSURE_COVERED:
				mProximitySensor.monitor(false);
				mHandler.removeMessages(MSG_ENTER_DARKLIGHT);
				break;
			}
		}
	};
	@Override
	public void onNear() {
		mHandler.sendEmptyMessageDelayed(MSG_ENSURE_COVERED, mDelayCheckCover);
	}
	@Override
	public void onFar() {
		mHandler.removeMessages(MSG_ENSURE_COVERED);
	}
}
