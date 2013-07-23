package giraffine.dimmer;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class DimmerService extends Service{

	public static ComponentName COMPONENT = new ComponentName("giraffine.dimmer", "giraffine.dimmer.DimmerService");
	public static String ADJUSTLEVEL = "adjustLevel";
	public static String FINISHLEVEL = "finishLevel";
	public static String PREFER = "Prefer";
	public static String PREFERLEVEL = "PreferLevel";
	public static final int MSG_RESET_LEVEL = 0;
	public static final int MSG_RESET_ACTING = 1;
	public static final int DEFAULTLEVEL = 1000;
	public static int lastLevel = DEFAULTLEVEL;
	
	WindowManager mWindowManager;
	WindowManager.LayoutParams mWindowParams;
	View mMaskView;
	boolean mActing = false;
	Notification mNotification;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public void postNotification()
	{
		if(mNotification == null)
		{
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			intent.setClassName("giraffine.dimmer", "giraffine.dimmer.Dimmer");
			PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);

			mNotification = new Notification.Builder(this)
			.setContentTitle(getText(R.string.app_name))
			.setContentText(getText(R.string.notification_sub))
			.setSmallIcon(R.drawable.ic_launcher)
			.setOngoing(true)
			.setContentIntent(pi)
			.getNotification();
		}
		startForeground(999, mNotification);
	}
	public void removeNotification()
	{
		stopForeground(true);
	}

	@Override
    public void onCreate() {
		
		BrightnessUtil.init(this);
//		lastLevel = BrightnessUtil.getPreferLevel();
		int currentBrightness = BrightnessUtil.getBrightness();
		lastLevel = (int)(((float)currentBrightness)/255*500 + 500);
		sendBroadcast(new Intent(Dimmer.REFRESH_INDEX));

		initMask();
		
        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), true, new ContentObserver(null){
        	public void onChange(boolean selfChange)
        	{
        		if(mActing == false)
        		{
    				mHandler.sendEmptyMessage(MSG_RESET_LEVEL);
        		}
        		return;
        	}
        });
        resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), true, new ContentObserver(null){
        	public void onChange(boolean selfChange)
        	{
        		if(mActing == false)
        		{
    				mHandler.sendEmptyMessage(MSG_RESET_LEVEL);
        		}
        		return;
        	}
        });
/*        
        SensorManager mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor mPressure = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        Log.e(Dimmer.TAG, "Sensor: getMaximumRange=" + mPressure.getMaximumRange() + ", getMinDelay=" + mPressure.getMinDelay());
        mSensorManager.registerListener(new SensorEventListener(){
			@Override
			public void onAccuracyChanged(Sensor arg0, int arg1) {
				Log.e(Dimmer.TAG, "onAccuracyChanged: " + arg0.toString() + ", arg1=" + arg1);
			}
			@Override
			public void onSensorChanged(SensorEvent arg0) {
				Log.e(Dimmer.TAG, "onSensorChanged: " + arg0.values[0]);
			}
        }, mPressure, SensorManager.SENSOR_DELAY_FASTEST);
*/        
    }
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent != null && intent.getAction() != null)
		{
			if(intent.getAction().equals(ADJUSTLEVEL))
			{
				mHandler.removeMessages(MSG_RESET_ACTING);
				mActing = true;
				adjustLevel(intent.getIntExtra(ADJUSTLEVEL, DEFAULTLEVEL), false);
			}
			else if(intent.getAction().equals(FINISHLEVEL))
			{
				int i = intent.getIntExtra(FINISHLEVEL, DEFAULTLEVEL);
				adjustLevel(intent.getIntExtra(FINISHLEVEL, DEFAULTLEVEL), true);
				lastLevel = i;
				BrightnessUtil.setPreferLevel(lastLevel);
				mHandler.removeMessages(MSG_RESET_ACTING);
				mHandler.sendEmptyMessageDelayed(MSG_RESET_ACTING, 1000);
			}
			else if(intent.getAction().equals("stop"))
			{
				stopSelf();
			}
		}
//		Log.e(Dimmer.TAG, "onStartCommand(): " + lastLevel);
		return lastLevel>500 ? START_NOT_STICKY : START_STICKY;
	}
	
	private void initMask()
	{
		mWindowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
		mWindowParams = new WindowManager.LayoutParams();

		mWindowParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
		mWindowParams.flags |= 
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE 
			| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS 
			| WindowManager.LayoutParams.FLAG_FULLSCREEN
			| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
			| WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
			| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
			| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
		mWindowParams.gravity = Gravity.LEFT | Gravity.TOP;
		Point p = new Point();
		mWindowManager.getDefaultDisplay().getSize(p);
		mWindowParams.x = 0;
		mWindowParams.y = 0;
		int length = ( p.x > p.y ? p.x : p.y) +300;
		mWindowParams.width = length;
		mWindowParams.height = length;
		mWindowParams.format = 1;
		mWindowParams.alpha = (float)0;	// default is transparent

		LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mMaskView = inflater.inflate(R.layout.mask_window, null);
//		mMaskView.setAlpha((float)1);	// == android:alpha
		
//		adjustLevel(lastLevel);
		mWindowManager.addView(mMaskView, mWindowParams);
	}
	public void adjustLevel(int i, boolean finished)
	{
		Log.e(Dimmer.TAG, "adjustLevel: " + i);
		if(i > 500)
		{
			float index = ((float)(i - 500))/500;
			if(finished)
				BrightnessUtil.setBrightness((int)(index*255));
			else
				maskBrightness(index);
			removeNotification();
		}
		else
		{
			if(finished)
				BrightnessUtil.setBrightness(0);
			i = 500 - i;
			adjustMask(((float)i/500));
			postNotification();
		}
	}
	public void maskBrightness(float value)
	{
		Log.e(Dimmer.TAG, "maskBrightness: " + value);
		if(value <= 0.01)	return;	// for padfone: value ~0.0 would fully dark screen 
		mWindowParams.screenBrightness = value;
		mWindowManager.updateViewLayout(mMaskView, mWindowParams);
	}
	public void adjustMask(float alpha)
	{
		Log.e(Dimmer.TAG, "adjustMask: " + alpha);
		if(alpha > 0.98) alpha = (float)0.9;
		else if(alpha < 0) alpha = (float)0;
//		mMaskView.setAlpha(alpha);	// control parent window is much safe
		mWindowParams.alpha = alpha; 
		mWindowManager.updateViewLayout(mMaskView, mWindowParams);
	}
	public void resetLevel()
	{
		Log.e(Dimmer.TAG, "resetLevel() lastLevel: " + lastLevel);
		removeNotification();
		Process.killProcess(Process.myPid());
/*
		int currentBrightness = BrightnessUtil.getBrightness(); 
		lastLevel = (int)(((float)currentBrightness)/255*500 + 500);
		Log.e(Dimmer.TAG, "resetLevel() lastLevel: " + lastLevel);
		BrightnessUtil.setPreferLevel(lastLevel);
		adjustMask(0);
		onStartCommand(null, 0 , 0);
*/	}
	Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			 switch (msg.what) {
			 case MSG_RESET_LEVEL:
				 resetLevel();
				 break;
			 case MSG_RESET_ACTING:
				 mActing = false;
				 break;
			 }
		}
	};
}
