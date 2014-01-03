package giraffine.dimmer;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.util.Log;

public class AlarmUtil {

	private Context mContext;
	public AlarmUtil(Context context)
	{
		mContext = context;
	}
	public void update()
	{
		// get latest alarm to trigger
	}
	public boolean nowToDim()
	{
		// check current time is in dim or bright
		return false;
	}
	private Calendar getLatestAlarm()
	{
		Calendar dim = null;
		Calendar bright = null;
		Calendar now = Calendar.getInstance();
		if(getAlarmOnOff(Prefs.PREF_ALARM_DIM))
			dim = getAlarmTime(Prefs.PREF_ALARM_DIM);
		if(getAlarmOnOff(Prefs.PREF_ALARM_BRIGHT))
			bright = getAlarmTime(Prefs.PREF_ALARM_BRIGHT);
		
		return now;
	}
	private void alarmMode(int hour, int minute)
	{
		TimeZone timezone = TimeZone.getDefault();
		Calendar rightNow = Calendar.getInstance();
		rightNow.set(Calendar.HOUR_OF_DAY, hour);
		rightNow.set(Calendar.MINUTE, minute);
		rightNow.set(Calendar.SECOND, 0);

		Log.e(Dimmer.TAG, "Next Alarm: " + rightNow.getTime().toGMTString());
		
		Intent intent = new Intent(DimmerService.ALARMMODE);
		intent.setComponent(DimmerService.COMPONENT);
		PendingIntent pi = PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_ONE_SHOT);
		AlarmManager alarmManager = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
		alarmManager.cancel(pi);
		alarmManager.set(AlarmManager.RTC, rightNow.getTimeInMillis(), pi);
	}
	public static boolean getAlarmOnOff(String type)
	{
		if(Prefs.getAlarm(type).startsWith("-"))
			return false;
		else
			return true;
	}
	public static void getAlarmTime(String type, int[] hourminute)
	{
		String time = Prefs.getAlarm(type);
		hourminute[0] = Integer.valueOf(time.substring(1, 3));
		hourminute[1] = Integer.valueOf(time.substring(4, 6));
	}
	public static String getAlarmTime(String type, Context context)
	{
		String time = Prefs.getAlarm(type);
		Calendar rightNow = Calendar.getInstance();
		rightNow.set(Calendar.HOUR_OF_DAY, Integer.valueOf(time.substring(1, 3)));
		rightNow.set(Calendar.MINUTE, Integer.valueOf(time.substring(4, 6)));
		rightNow.set(Calendar.SECOND, 0);
		java.text.DateFormat dateformat = DateFormat.getTimeFormat(context);
		return dateformat.format(rightNow.getTime());
	}
	public static Calendar getAlarmTime(String type)
	{
		String time = Prefs.getAlarm(type);
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(time.substring(1, 3)));
		cal.set(Calendar.MINUTE, Integer.valueOf(time.substring(4, 6)));
		cal.set(Calendar.SECOND, 0);
		return cal;
	}
	public static void setAlarm(String type, boolean isOn, int hour, int minute)
	{
		Prefs.setAlarm(type, String.format("%s%02d:%02d", isOn?"+":"-", hour, minute));
	}

}
