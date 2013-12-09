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
	
	private boolean mActing = false;
	private Notification mNotification;
	private Mask mMask = null;

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

		mMask = new Mask(this);
		
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
	private void adjustLevel(int i, boolean finished)
	{
		if(i > 500)
			removeNotification();
		else
			postNotification();
		mMask.adjustLevel(i, finished);
	}
	public void resetLevel()
	{
		Log.e(Dimmer.TAG, "resetLevel() lastLevel: " + lastLevel);
		removeNotification();
		stopSelf();
		Process.killProcess(Process.myPid());
	}
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
