package giraffine.dimmer;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

public class DimmerService extends Service implements LightSensor.EventCallback{

	public static boolean DebugMode = false;
	public static String PACKAGENAME = "giraffine.dimmer";
	public static ComponentName COMPONENT = new ComponentName(PACKAGENAME, PACKAGENAME+".DimmerService");
	public static String ADJUSTLEVEL = "adjustLevel";
	public static String FINISHLEVEL = "finishLevel";
	public static String MONITORLIGHT = "monitorLight";
	public static String PREFER = "Prefer";
	public static String PREFERLEVEL = "PreferLevel";
	public static final int MSG_RESET_LEVEL = 0;
	public static final int MSG_RESET_LEVEL_AUTOMODE = 1;
	public static final int MSG_RESET_ACTING = 2;
	public static final int MSG_ENTER_DIMM = 4;
	public static final int DEFAULTLEVEL = 1000;
	public static int lastLevel = DEFAULTLEVEL;

	public static boolean mIsAutoMode = true;
	
	public int mFavorMaskValue = 250;

	private boolean mActing = false;
	private Notification mNotification;
	private Mask mMask = null;
	private boolean mInDimmMode = false;
	private LightSensor mLightSensor = null;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	public void postNotification()
	{
		if(mNotification == null || DebugMode)
		{
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);
			intent.setClassName(PACKAGENAME, PACKAGENAME+".Dimmer");
			PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);

			mNotification = new Notification.Builder(this)
			.setContentTitle(getText(R.string.app_name) + (DebugMode ? " Lux=" + mLightSensor.getCurrentLux() : ""))
			.setContentText(getText(R.string.notification_sub))
			.setSmallIcon(R.drawable.ic_launcher)
			.setOngoing(true)
			.setContentIntent(pi)
			.getNotification();
			
			if(DebugMode)
				mNotification.tickerText = mLightSensor.getCurrentLux() + "";
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

		mMask = new Mask(this);
		mLightSensor = new LightSensor(this, this);
		
        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), true, new ContentObserver(null){
        	public void onChange(boolean selfChange)
        	{
        		if(mActing == false)
    				mHandler.sendEmptyMessage(MSG_RESET_LEVEL);
        		return;
        	}
        });
        resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), true, new ContentObserver(null){
        	public void onChange(boolean selfChange)
        	{
        		if(mActing == false)
    				mHandler.sendEmptyMessage(MSG_RESET_LEVEL);
        		return;
        	}
        });
        registerReceiver(new BroadcastReceiver(){
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				mLightSensor.monitor(true);
			}
        }, new IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(new BroadcastReceiver(){
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				mLightSensor.monitor(false);
			}
        }, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }
	@Override
	public void onDestroy() {
		// need restart ASAP: 5 secs
		Log.e(Dimmer.TAG, "onDestroy()");
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setComponent(COMPONENT);
		intent.setAction(MONITORLIGHT);
		PendingIntent pi = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
		AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 5000, pi);
	}
	@Override
	public void onLightChanged() {
		if(DebugMode)	postNotification();
	}
	@Override
	public void onDarkLight() {
		Log.e(Dimmer.TAG, "onDarkLight() mIsAutoMode=" + mIsAutoMode + ", mInDimmMode=" + mInDimmMode);
		if(!mIsAutoMode || mInDimmMode)	return;
		mLightSensor.setFreezeLux();
		mHandler.sendEmptyMessage(MSG_ENTER_DIMM);
	}
	
	@Override
	public void onOverDarkLight() {
		Log.e(Dimmer.TAG, "onOverDarkLight() mIsAutoMode=" + mIsAutoMode + ", mInDimmMode=" + mInDimmMode);
		if(!mIsAutoMode || !mInDimmMode)	return;
		mHandler.sendEmptyMessage(MSG_RESET_LEVEL_AUTOMODE);
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
				mHandler.removeMessages(MSG_RESET_ACTING);
				mHandler.sendEmptyMessageDelayed(MSG_RESET_ACTING, 1000);

				if(lastLevel < 500)
				{
					mLightSensor.setFreezeLux();
					mFavorMaskValue = lastLevel;
			}

//				Log.e(Dimmer.TAG, "" + LuxUtil.dumpLuxLevel());
			}
			else if(intent.getAction().equals(MONITORLIGHT))
			{
				mLightSensor.monitor(true);
			}
		}
//		Log.e(Dimmer.TAG, "onStartCommand(): " + lastLevel);
		return lastLevel>500 ? START_NOT_STICKY : START_STICKY;
	}
	private void adjustLevel(int i, boolean finished)
	{
		if(i > 500)
		{
			removeNotification();
			mInDimmMode = false;
		}
		else
		{
			postNotification();
			mInDimmMode = true;
		}
		mMask.adjustLevel(i, finished);
	}
	public void resetLevel(boolean enableAutoBrightness)
	{
		Log.e(Dimmer.TAG, "resetLevel() lastLevel: " + lastLevel);
		mActing = true;
		mHandler.removeMessages(MSG_RESET_ACTING);
		mHandler.sendEmptyMessageDelayed(MSG_RESET_ACTING, 1000);
		if(enableAutoBrightness)
		{
			adjustLevel(500, true);
			lastLevel = 500;
			BrightnessUtil.setAutoBrightness(true);
		}
		removeNotification();
		mInDimmMode = false;
		sendBroadcast(new Intent(Dimmer.REFRESH_INDEX));
//		stopSelf();
//		Process.killProcess(Process.myPid());
	}
	Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			 switch (msg.what) {
			 case MSG_RESET_LEVEL:
				resetLevel(false);
				break;
			case MSG_RESET_LEVEL_AUTOMODE:
				resetLevel(true);
				 break;
			 case MSG_RESET_ACTING:
				 mActing = false;
				 break;
			case MSG_ENTER_DIMM:
				mActing = true;
				mHandler.removeMessages(MSG_RESET_ACTING);
				mHandler.sendEmptyMessageDelayed(MSG_RESET_ACTING, 1000);
				adjustLevel(mFavorMaskValue, true);
				lastLevel = mFavorMaskValue;
				sendBroadcast(new Intent(Dimmer.REFRESH_INDEX));
				break;
			 }
		}
	};

}
