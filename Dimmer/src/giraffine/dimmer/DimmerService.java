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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

public class DimmerService extends Service implements LightSensor.EventCallback{

	public static boolean DebugMode = false;
	public static String PACKAGENAME = "giraffine.dimmer";
	public static String ACTIONNOTIFICATION = "giraffine.dimmer.Dimmer.action.notification";
	public static ComponentName COMPONENT = new ComponentName(PACKAGENAME, PACKAGENAME+".DimmerService");
	public static String ADJUSTLEVEL = "adjustLevel";
	public static String FINISHLEVEL = "finishLevel";
	public static String RESETLEVEL = "resetLevel";
	public static String STEPLEVELUP = "stepLevelUp";
	public static String STEPLEVELDOWN = "stepLevelDown";
	public static String SWITCHAUTOMODE = "switchAutoMode";
	public static String SWITCHDIM = "switchDim";
	public static final int MSG_RESET_LEVEL = 0;
	public static final int MSG_RESET_LEVEL_RESTORE = 1;
	public static final int MSG_RESET_ACTING = 3;
	public static final int MSG_ENTER_DIMM = 4;
	public static final int DEFAULTLEVEL = 1000;
	public static int lastLevel = DEFAULTLEVEL;

	private boolean mActing = false;
	private Notification mNotification;
	private RemoteViews mNotiRemoteView = null;
	private Mask mMask = null;
	private boolean mInDimmMode = false;
	private LightSensor mLightSensor = null;
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	
	public void postNotification(int levelHint)
	{
		
		if(mNotiRemoteView == null)
		{
			Intent stepUpIntent = new Intent(this, DimmerService.class);
			stepUpIntent.setAction(STEPLEVELUP);
			PendingIntent piStepUp = PendingIntent.getService(this, 0, stepUpIntent, 0);
			
			Intent stepDownIntent = new Intent(this, DimmerService.class);
			stepDownIntent.setAction(STEPLEVELDOWN);
			PendingIntent piStepDown = PendingIntent.getService(this, 0, stepDownIntent, 0);
			
			Intent resetIntent = new Intent(this, DimmerService.class);
			resetIntent.setAction(RESETLEVEL);
			PendingIntent piReset = PendingIntent.getService(this, 0, resetIntent, 0);

			mNotiRemoteView = new RemoteViews(PACKAGENAME, R.layout.notification);
			mNotiRemoteView.setOnClickPendingIntent(R.id.noti_up, piStepUp);
			mNotiRemoteView.setOnClickPendingIntent(R.id.noti_down, piStepDown);
			mNotiRemoteView.setOnClickPendingIntent(R.id.noti_cross, piReset);
		}
		mNotiRemoteView.setTextViewText(R.id.noti_text, levelHint + "");

		if(mNotification == null)
		{
			Intent intent = new Intent(ACTIONNOTIFICATION);
			intent.setClassName(PACKAGENAME, PACKAGENAME+".Dimmer");
			PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);
			
			mNotification = new NotificationCompat.Builder(this)
				.setContent(mNotiRemoteView)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentIntent(pi)
				.build();
		}
		if(DebugMode)
			mNotification.tickerText = mLightSensor.getCurrentLux() + "";

		startForeground(999, mNotification);
	}
	public void removeNotification()
	{
		stopForeground(true);
	}

	@Override
    public void onCreate() {
		BrightnessUtil.init(this);
		Prefs.init(this);
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
				if(Prefs.isAutoMode())
				mLightSensor.monitor(true);
			}
        }, new IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(new BroadcastReceiver(){
			@Override
			public void onReceive(Context arg0, Intent arg1) {
				mLightSensor.monitor(false);
			}
        }, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        
        if(Prefs.isAutoMode())
        mLightSensor.monitor(true);
    }
	@Override
	public void onDestroy() {
		// need restart ASAP: 60 secs
		Log.e(Dimmer.TAG, "onDestroy()");
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setComponent(COMPONENT);
		PendingIntent pi = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
		AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 60000, pi);
	}
	@Override
	public void onLightChanged() {
		if(DebugMode)	postNotification(lastLevel/10);
	}
	@Override
	public void onEnterDarkLight() {
//		Log.e(Dimmer.TAG, "onDarkLight() mIsAutoMode=" + Prefs.isAutoMode() + ", mInDimmMode=" + mInDimmMode);
		if(!Prefs.isAutoMode() || mInDimmMode)	return;
		mLightSensor.setFreezeLux();
		mHandler.sendEmptyMessage(MSG_ENTER_DIMM);
	}
	
	@Override
	public void onLeaveDarkLight() {
//		Log.e(Dimmer.TAG, "onOverDarkLight() mIsAutoMode=" + Prefs.isAutoMode() + ", mInDimmMode=" + mInDimmMode);
		if(!Prefs.isAutoMode() || !mInDimmMode)	return;
		mHandler.sendEmptyMessage(MSG_RESET_LEVEL_RESTORE);
	}
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		if(intent != null && intent.getAction() != null)
		{
			if(intent.getAction().equals(ADJUSTLEVEL))
			{
				mHandler.removeMessages(MSG_RESET_ACTING);
				mActing = true;
				adjustLevel(intent.getIntExtra(ADJUSTLEVEL, DEFAULTLEVEL), false, false);
			}
			else if(intent.getAction().equals(FINISHLEVEL))
			{
				int i = intent.getIntExtra(FINISHLEVEL, DEFAULTLEVEL);
				adjustLevel(intent.getIntExtra(FINISHLEVEL, DEFAULTLEVEL), true, true);
				lastLevel = i;

				if(lastLevel < 500)
				{
					mLightSensor.setFreezeLux();
					Prefs.setFavorMaskValue(lastLevel);
				}

//				Log.e(Dimmer.TAG, "" + LuxUtil.dumpLuxLevel());
			}
			else if(intent.getAction().equals(RESETLEVEL))
			{
				mHandler.sendEmptyMessage(MSG_RESET_LEVEL_RESTORE);
			}
			else if(intent.getAction().equals(STEPLEVELUP))
			{
				stepLevel(false);
			}
			else if(intent.getAction().equals(STEPLEVELDOWN))
			{
				stepLevel(true);
			}
			else if(intent.getAction().equals(SWITCHAUTOMODE))
			{
				boolean on = intent.getBooleanExtra(SWITCHAUTOMODE, false);
				if(on)
					mLightSensor.monitor(true);
				else
					mLightSensor.monitor(false);
			}
			else if(intent.getAction().equals(SWITCHDIM))
			{
				if(mInDimmMode)
					mHandler.sendEmptyMessage(MSG_RESET_LEVEL_RESTORE);
				else
				{
					mLightSensor.setFreezeLux();
					mHandler.sendEmptyMessage(MSG_ENTER_DIMM);
				}
			}
		}
//		Log.e(Dimmer.TAG, "onStartCommand(): " + lastLevel);
//		return Prefs.isAutoMode() ? START_STICKY : (lastLevel>500 ? START_NOT_STICKY : START_STICKY);
		return START_STICKY;	//sticky for continuous alive
	}
	private void adjustLevel(int i, boolean setBrightness, boolean postNotify)
	{
		if(i > 500)
		{
			removeNotification();
			mInDimmMode = false;
		}
		else
		{
			if(postNotify)
			postNotification(i/10);
			mInDimmMode = true;
		}
		if(setBrightness)
			triggerActingSession();
		mMask.adjustLevel(i, setBrightness);
	}
	public void resetLevel(boolean restoreBrighnessState)
	{
		Log.e(Dimmer.TAG, "resetLevel() lastLevel: " + lastLevel);

		if(restoreBrighnessState)
		{
			triggerActingSession();
			BrightnessUtil.restoreState();
		}

		int currentBrightness = BrightnessUtil.getBrightness();
		lastLevel = (int)(((float)currentBrightness)/255*500 + 500);
		mMask.removeMask();
		
		removeNotification();
		mInDimmMode = false;
		sendBroadcast(new Intent(Dimmer.REFRESH_INDEX));
//		stopSelf();
//		Process.killProcess(Process.myPid());
	}
	public void stepLevel(boolean darker)
	{
		Log.e(Dimmer.TAG, "stepLevel() lastLevel: " + lastLevel + ", darker=" + darker);

		int step = 50;
		if(darker)
			lastLevel -= step;
		else
			lastLevel += step;
		if(lastLevel > 500)	lastLevel = 500;
		if(lastLevel < 100)	lastLevel = 100;
		
		adjustLevel(lastLevel, true, true);
		
		if(lastLevel < 500)
		{
			mLightSensor.setFreezeLux();
			Prefs.setFavorMaskValue(lastLevel);
		}
		sendBroadcast(new Intent(Dimmer.REFRESH_INDEX));
	}
	Handler mHandler = new Handler(){
		public void handleMessage(Message msg) {
			 switch (msg.what) {
			 case MSG_RESET_LEVEL:
				resetLevel(false);
				break;
			 case MSG_RESET_LEVEL_RESTORE:
				resetLevel(true);
				 break;
			 case MSG_RESET_ACTING:
				 mActing = false;
				 break;
			case MSG_ENTER_DIMM:
				BrightnessUtil.collectState();
				int favorvalue = Prefs.getFavorMaskValue();
				adjustLevel(favorvalue, true, true);
				lastLevel = favorvalue;
				sendBroadcast(new Intent(Dimmer.REFRESH_INDEX));
				break;
			 }
		}
	};
	public void triggerActingSession()
	{
		mActing = true;
		mHandler.removeMessages(MSG_RESET_ACTING);
		mHandler.sendEmptyMessageDelayed(MSG_RESET_ACTING, 1000);
	}
}
