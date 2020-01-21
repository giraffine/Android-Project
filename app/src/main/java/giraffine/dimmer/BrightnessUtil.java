package giraffine.dimmer;

import android.content.Context;
import android.provider.Settings;

public class BrightnessUtil {

    private static Context mContext;
    private static int mAutoState;
    private static int mLevelState;

    public static void init(Context context) {
        mContext = context;
    }

    public static int getBrightness() {
        return Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
    }

    public static void setBrightness(int i) {
//		Log.e(Dimmer.TAG, "setBrightness: " + i);
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, i);
    }

    public static void setAutoBrightness(boolean b) {
        if (b == true)
            Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        else
            Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
    }

    private static int getAutoBrightness() {
        return Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }

    public static void collectState() {
        mAutoState = getAutoBrightness();
        mLevelState = getBrightness();
    }

    public static void restoreState() {
        setBrightness(mLevelState);
        setAutoBrightness(mAutoState == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
    }
}
