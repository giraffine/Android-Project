package giraffine.dimmer;

import android.content.Context;

public class Prefs {

	private static Context mContext;
	private static String PREFER = "settings";
	private static String AUTOMODE = "automode";
	private static String FAVORMASKVALUE = "favormaskvalue";
	private static String PROXIMITYMAX = "proximitymax";
	private static String PROXIMITYMIN = "proximitymin";
	public static void init(Context context)
	{
		mContext = context;
	}
	
	public static boolean isAutoMode()
	{
		return mContext.getSharedPreferences(PREFER, mContext.MODE_WORLD_READABLE).getBoolean(AUTOMODE, true);
	}
	public static void setAutoMode(boolean isON)
	{
		mContext.getSharedPreferences(PREFER, mContext.MODE_WORLD_WRITEABLE).edit().putBoolean(AUTOMODE, isON).commit();
	}
	public static int getFavorMaskValue()
	{
		return mContext.getSharedPreferences(PREFER, mContext.MODE_WORLD_READABLE).getInt(FAVORMASKVALUE, 250);
	}
	public static void setFavorMaskValue(int value)
	{
		mContext.getSharedPreferences(PREFER, mContext.MODE_WORLD_WRITEABLE).edit().putInt(FAVORMASKVALUE, value).commit();
	}
	public static float getProximity(boolean isMax)
	{
		return mContext.getSharedPreferences(PREFER, mContext.MODE_WORLD_READABLE).getFloat(isMax ? PROXIMITYMAX : PROXIMITYMIN, ProximitySensor.DEFAULT_DISTANCE);
	}
	public static void setProximity(boolean isMax, float value)
	{
		mContext.getSharedPreferences(PREFER, mContext.MODE_WORLD_WRITEABLE).edit().putFloat(isMax ? PROXIMITYMAX : PROXIMITYMIN, value).commit();
	}
}
