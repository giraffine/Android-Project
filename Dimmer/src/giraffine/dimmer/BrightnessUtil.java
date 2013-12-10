package giraffine.dimmer;

import android.content.Context;
import android.provider.Settings;

public class BrightnessUtil {

	private static Context mContext;
	public static void init(Context context)
	{
		mContext = context;
	}
	public static int getBrightness()
	{
		return Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
	}
	public static void setBrightness(int i)
	{
//		Log.e(Dimmer.TAG, "setBrightness: " + i);
		Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
		Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, i);
	}
	public static void setAutoBrightness(boolean b)
	{
		if(b == true)
			Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
		else
			Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
	}
	public static int getPreferLevel()
	{
		return mContext.getSharedPreferences(DimmerService.PREFER, mContext.MODE_WORLD_WRITEABLE).getInt(DimmerService.PREFERLEVEL, DimmerService.DEFAULTLEVEL);
	}
	public static void setPreferLevel(int level)
	{
		mContext.getSharedPreferences(DimmerService.PREFER, mContext.MODE_WORLD_WRITEABLE).edit().putInt(DimmerService.PREFERLEVEL, level).commit();
	}
}
