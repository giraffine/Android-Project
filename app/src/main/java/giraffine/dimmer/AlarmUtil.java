package giraffine.dimmer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

public class AlarmUtil {

    private Context mContext;
    private Comparator<Calendar> mComparator = new Comparator<Calendar>() {
        @Override
        public int compare(Calendar a, Calendar b) {
            return a.compareTo(b);
        }
    };

    public AlarmUtil(Context context) {
        mContext = context;
    }

    public void update()    // get the latest alarm and setup alarm
    {
        Calendar now = Calendar.getInstance();
        Calendar next = getLatestAlarm(now);
        boolean isAlarmOn = !(next.compareTo(now) == 0);
        setupAlarm(isAlarmOn, next);
    }

    public boolean nowToDim()    // check current time is to dim or bright. called when alarm fired.
    {
        boolean toDim = true;
        boolean toBright = false;
        Calendar dim = null;
        Calendar bright = null;
        if (getAlarmOnOff(Prefs.PREF_ALARM_DIM))
            dim = getAlarmTime(Prefs.PREF_ALARM_DIM);
        if (getAlarmOnOff(Prefs.PREF_ALARM_BRIGHT))
            bright = getAlarmTime(Prefs.PREF_ALARM_BRIGHT);

        if (dim == null && bright == null)
            return toBright;
        else if (dim != null && bright == null)
            return toDim;
        else if (dim == null && bright != null)
            return toBright;
        else {
            Calendar now = Calendar.getInstance();
            int dimCompare = now.compareTo(dim);
            int brightCompare = now.compareTo(bright);
            int dimbrightCompare = dim.compareTo(bright);
            Log.e(Dimmer.TAG, "nowToDim: now: " + now.getTime().toGMTString());
            Log.e(Dimmer.TAG, "nowToDim: dim: " + dim.getTime().toGMTString());
            Log.e(Dimmer.TAG, "nowToDim: bright: " + bright.getTime().toGMTString());
            Log.e(Dimmer.TAG, "nowToDim: dimCompare=" + dimCompare + ", brightCompare=" + brightCompare + ", dimbrightCompare=" + dimbrightCompare);
            if (dimCompare >= 0 && brightCompare < 0) {    // dim ... now ... bright
                return toDim;
            } else if (dimCompare < 0 && brightCompare >= 0) {    // bright ... now ... dim
                return toBright;
            } else {
                if (dimbrightCompare < 0)    // [dim ... bright ... now] or [now ... dim ... bright]
                    return toBright;
                else if (dimbrightCompare > 0)    // [bright ... dim ... now] or [now ... bright ... dim]
                    return toDim;
            }
        }

        return toBright;
    }

    public boolean bootToDim()    // check if [dim ... now ... bright]. called when boot.
    {
        boolean toDim = true;
        boolean toBright = false;
        Calendar dim = null;
        Calendar bright = null;
        if (getAlarmOnOff(Prefs.PREF_ALARM_DIM))
            dim = getAlarmTime(Prefs.PREF_ALARM_DIM);
        if (getAlarmOnOff(Prefs.PREF_ALARM_BRIGHT))
            bright = getAlarmTime(Prefs.PREF_ALARM_BRIGHT);

        if (dim == null || bright == null)
            return toBright;

        Calendar now = Calendar.getInstance();
        int dimCompare = now.compareTo(dim);
        int brightCompare = now.compareTo(bright);
        int dimbrightCompare = dim.compareTo(bright);
        Log.e(Dimmer.TAG, "bootToDim: now: " + now.getTime().toGMTString());
        Log.e(Dimmer.TAG, "bootToDim: dim: " + dim.getTime().toGMTString());
        Log.e(Dimmer.TAG, "bootToDim: bright: " + bright.getTime().toGMTString());
        Log.e(Dimmer.TAG, "bootToDim: dimCompare=" + dimCompare + ", brightCompare=" + brightCompare + ", dimbrightCompare=" + dimbrightCompare);
        if (dimCompare >= 0 && brightCompare < 0) {    // dim ... now ... bright
            return toDim;
        } else if (dimCompare < 0 && brightCompare >= 0) {    // bright ... now ... dim
            return toBright;
        } else {
            if (dimbrightCompare < 0)    // [dim ... bright ... now] or [now ... dim ... bright]
                return toBright;
            else if (dimbrightCompare > 0)    // [bright ... dim ... now] or [now ... bright ... dim]
                return toDim;
        }
        return toBright;
    }

    private Calendar getLatestAlarm(Calendar now) {
        Calendar dim = null;
        Calendar bright = null;
        Calendar result = now;
        ArrayList<Calendar> list = new ArrayList<Calendar>();
        list.add(now);
        Log.e(Dimmer.TAG, "getLatestAlarm now: " + now.getTime().toGMTString());
        if (getAlarmOnOff(Prefs.PREF_ALARM_DIM)) {
            dim = getAlarmTime(Prefs.PREF_ALARM_DIM);
            if (dim.compareTo(now) <= 0)
                dim.roll(Calendar.DAY_OF_YEAR, true);
            list.add(dim);
            Log.e(Dimmer.TAG, "getLatestAlarm dim: " + dim.getTime().toGMTString());
        }
        if (getAlarmOnOff(Prefs.PREF_ALARM_BRIGHT)) {
            bright = getAlarmTime(Prefs.PREF_ALARM_BRIGHT);
            if (bright.compareTo(now) <= 0)
                bright.roll(Calendar.DAY_OF_YEAR, true);
            list.add(bright);
            Log.e(Dimmer.TAG, "getLatestAlarm bright: " + bright.getTime().toGMTString());
        }
        if (list.size() == 1)
            return result;

        Collections.sort(list, mComparator);
//		for(Calendar cal : list)
//			Log.e(Dimmer.TAG, "SORT#1: " + cal.getTime().toGMTString());

        for (int i = 0; i < list.size(); i++) {
            Calendar cal = list.get(i);
            if (cal.compareTo(now) > 0) {
                result = list.get(i);
                break;
            }
        }

        return result;
    }

    private void setupAlarm(boolean on, Calendar time) {
        Log.e(Dimmer.TAG, "setupAlarm: " + (on ? time.getTime().toGMTString() : "OFF"));

        Intent intent = new Intent(DimmerService.ALARMMODE);
        intent.setComponent(DimmerService.COMPONENT);
        PendingIntent pi = PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pi);
        if (on)
            alarmManager.set(AlarmManager.RTC, time.getTimeInMillis(), pi);
    }

    public static boolean getAlarmOnOff(String type) {
        if (Prefs.getAlarm(type).startsWith("-"))
            return false;
        else
            return true;
    }

    public static void getAlarmTime(String type, int[] hourminute) {
        String time = Prefs.getAlarm(type);
        hourminute[0] = Integer.valueOf(time.substring(1, 3));
        hourminute[1] = Integer.valueOf(time.substring(4, 6));
    }

    public static String getAlarmTime(String type, Context context) {
        String time = Prefs.getAlarm(type);
        Calendar rightNow = Calendar.getInstance();
        rightNow.set(Calendar.HOUR_OF_DAY, Integer.valueOf(time.substring(1, 3)));
        rightNow.set(Calendar.MINUTE, Integer.valueOf(time.substring(4, 6)));
        rightNow.set(Calendar.SECOND, 0);
        java.text.DateFormat dateformat = DateFormat.getTimeFormat(context);
        return dateformat.format(rightNow.getTime());
    }

    public static Calendar getAlarmTime(String type) {
        String time = Prefs.getAlarm(type);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, Integer.valueOf(time.substring(1, 3)));
        cal.set(Calendar.MINUTE, Integer.valueOf(time.substring(4, 6)));
        cal.set(Calendar.SECOND, 0);
        cal.setTimeInMillis(cal.getTimeInMillis() / 1000 * 1000);    // set ms to 0
        return cal;
    }

    public static void setAlarm(String type, boolean isOn, int hour, int minute) {
        Prefs.setAlarm(type, String.format("%s%02d:%02d", isOn ? "+" : "-", hour, minute));
    }

}
