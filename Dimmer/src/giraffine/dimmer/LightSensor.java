package giraffine.dimmer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class LightSensor {

	public static final int MSG_BASE = 100;
	public static final int MSG_DARKLIGHT = 0 + MSG_BASE;
	public static final int MSG_OVER_DARKLIGHT = 1 + MSG_BASE;
	
	private Context mContext;
	private SensorManager mSensorManager;
	private Sensor mSensorLight;
	private SensorEventListener mSensorEventListener;
	private int mCurrentLux;
	private int mFreezeLux = 9999999;
	private EventCallback mEventCallback = null;
	private ProximitySensor mProximitySensor = null;
	
	interface EventCallback
	{
		public void onLightChanged();
		public void onDarkLight();
		public void onOverDarkLight();
	}

	public LightSensor(Context context, EventCallback eventcallback)
	{
		LuxUtil.init(context);
		mProximitySensor = new ProximitySensor(context);
		mContext = context;
		mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
		if(mSensorManager != null)
			mSensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		mEventCallback = eventcallback;
	}
	public boolean hasLightSensor()
	{
	    if(mSensorManager == null || mSensorLight == null)
	    	return false;
	    else
	    	return true;
	}
	public boolean monitor(boolean isOn)
	{
	    if(mSensorManager == null || mSensorLight == null)
	    	return false;

	    mProximitySensor.monitor(isOn);
	    
		if(mSensorEventListener == null)
			mSensorEventListener = new SensorEventListener(){
				@Override
				public void onAccuracyChanged(Sensor arg0, int arg1) {
					Log.e(Dimmer.TAG, "onAccuracyChanged: " + arg0.toString() + ", arg1=" + arg1);
				}
				@Override
				public void onSensorChanged(SensorEvent arg0) {
					Log.e(Dimmer.TAG, "onSensorChanged: " + arg0.values[0]);
					mCurrentLux = (int)arg0.values[0];
					mEventCallback.onLightChanged();
					LuxUtil.setLuxLevel(mCurrentLux);
					
					// need include proximity sensor to avoid cover situation
					if(LuxUtil.isLowestLevel(mCurrentLux) && !mProximitySensor.isCovered())
					{
						mHandler.sendEmptyMessageDelayed(MSG_DARKLIGHT, 5000);
					}
					else
						mHandler.removeMessages(MSG_DARKLIGHT);
					
					if(mCurrentLux > mFreezeLux*2)
					{
						if(!mHandler.hasMessages(MSG_OVER_DARKLIGHT))
							mHandler.sendEmptyMessageDelayed(MSG_OVER_DARKLIGHT, 2000);
					}
					else
						mHandler.removeMessages(MSG_OVER_DARKLIGHT);
				}
	        };
        if(isOn)
        	mSensorManager.registerListener(mSensorEventListener, mSensorLight, SensorManager.SENSOR_DELAY_FASTEST);
        else
        	mSensorManager.unregisterListener(mSensorEventListener);
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
	
	private Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_DARKLIGHT:
				mEventCallback.onDarkLight();
				break;
			case MSG_OVER_DARKLIGHT:
				mEventCallback.onOverDarkLight();
				break;
			}
		}
	};
}
