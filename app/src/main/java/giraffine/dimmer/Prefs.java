package giraffine.dimmer;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.WindowManager;

import java.util.Set;

public class Prefs {

    private static Context mContext = null;
    private static String PREFER = "settings";
    private static String AUTOMODE = "automode";
    private static String FAVORMASKVALUE = "favormaskvalue";
    private static String PROXIMITYMAX = "proximitymax";
    private static String PROXIMITYMIN = "proximitymin";
    private static String PREF_COMPATIBLE = "pref_compatible";

    public static String PREF_AUTOMODE = "pref_automode";
    public static String PREF_THRESHOLD_DIM = "pref_threshold_dim";
    public static String PREF_THRESHOLD_DIM_LOWEST = "pref_threshold_dim_lowest";
    public static String PREF_THRESHOLD_BRIGHT = "pref_threshold_bright";
    public static String PREF_SPEED_DIM = "pref_speed_dim";
    public static String PREF_SPEED_BRIGHT = "pref_speed_bright";
    public static String PREF_WIDGETMODE = "pref_widgetmode";
    public static String PREF_ABOUT = "pref_about";
    public static String PREF_ALARM_DIM = "pref_alarm_dim";
    public static String PREF_ALARM_BRIGHT = "pref_alarm_bright";
    public static String PREF_NOTIFY_STEP = "pref_notify_step";
    public static String PREF_NOTIFY_RANGE = "pref_notify_range";
    public static String PREF_NOTIFY_LOWER = "pref_notify_lower";
    public static String PREF_NOTIFY_UPPER = "pref_notify_upper";
    public static String PREF_NOTIFY_LAYOUT = "pref_notify_layout";
    public static String PREF_NOTIFY_PRIORITY = "pref_notify_priority";
    public static String PREF_AP_LIST = "pref_ap_list";
    public static String PREF_BUTTON_BACKLIGHT = "pref_button_backlight";
    public static String PREF_COLORMODE = "pref_colormode";
    public static String PREF_COLOR_VALUE = "pref_color_value";

    private static SharedPreferences mPrefer = null;

    public static void init(Context context) {
        if (mContext != null)
            return;
        mContext = context;
        mPrefer = PreferenceManager.getDefaultSharedPreferences(mContext);

        // backward compatible
        if (!mPrefer.getBoolean(PREF_COMPATIBLE, false)) {
            SharedPreferences prefer = mContext.getSharedPreferences(PREFER, Context.MODE_PRIVATE);
            if (prefer.contains(AUTOMODE))
                mPrefer.edit().putBoolean(AUTOMODE, prefer.getBoolean(AUTOMODE, false)).commit();
            if (prefer.contains(FAVORMASKVALUE))
                mPrefer.edit().putInt(FAVORMASKVALUE, prefer.getInt(FAVORMASKVALUE, 250)).commit();
            if (prefer.contains(PROXIMITYMAX))
                mPrefer.edit().putFloat(PROXIMITYMAX, prefer.getFloat(PROXIMITYMAX, ProximitySensor.DEFAULT_DISTANCE)).commit();
            if (prefer.contains(PROXIMITYMIN))
                mPrefer.edit().putFloat(PROXIMITYMIN, prefer.getFloat(PROXIMITYMIN, ProximitySensor.DEFAULT_DISTANCE)).commit();
            mPrefer.edit().putBoolean(PREF_COMPATIBLE, true).commit();
        }
        if (((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth() < 540)
            SettingNotifyLayout.DEFAULT_LAYOUT = "-01231111";
    }

    public static boolean isAutoMode() {
        return mPrefer.getBoolean(PREF_AUTOMODE, false);
    }

    public static int getSpeed(String key) {
        return Integer.valueOf(mPrefer.getString(key, "3"));
    }

    public static boolean getWidgetMode() {
        return mPrefer.getBoolean(PREF_WIDGETMODE, true);
    }

    public static int getFavorMaskValue() {
        return mPrefer.getInt(FAVORMASKVALUE, 250);
    }

    public static void setFavorMaskValue(int value) {
        mPrefer.edit().putInt(FAVORMASKVALUE, value).commit();
    }

    public static float getProximity(boolean isMax) {
        return mPrefer.getFloat(isMax ? PROXIMITYMAX : PROXIMITYMIN, ProximitySensor.DEFAULT_DISTANCE);
    }

    public static void setProximity(boolean isMax, float value) {
        mPrefer.edit().putFloat(isMax ? PROXIMITYMAX : PROXIMITYMIN, value).commit();
    }

    public static int getThresholdDim() {
        return mPrefer.getInt(PREF_THRESHOLD_DIM, 0);
    }

    public static void setThresholdDim(int value) {
        mPrefer.edit().putInt(PREF_THRESHOLD_DIM, value).commit();
    }

    public static boolean getThresholdDimLowest() {
        return mPrefer.getBoolean(PREF_THRESHOLD_DIM_LOWEST, true);
    }

    public static void setThresholdDimLowest(boolean yes) {
        mPrefer.edit().putBoolean(PREF_THRESHOLD_DIM_LOWEST, yes).commit();
    }

    public static int getThresholdBright() {
        return mPrefer.getInt(PREF_THRESHOLD_BRIGHT, 40);
    }

    public static void setThresholdBright(int value) {
        mPrefer.edit().putInt(PREF_THRESHOLD_BRIGHT, value).commit();
    }

    public static String getAlarm(String type) {
        return mPrefer.getString(type, "-00:00");
    }

    public static void setAlarm(String type, String value) {
        mPrefer.edit().putString(type, value).commit();
    }

    public static boolean getNotifyPriority() {
        return mPrefer.getBoolean(PREF_NOTIFY_PRIORITY, false);
    }

    public static int getNotify(String type) {
        int value = 0;
        if (type.equalsIgnoreCase(PREF_NOTIFY_STEP))
            value = 5;
        else if (type.equalsIgnoreCase(PREF_NOTIFY_LOWER))
            value = 10;
        else if (type.equalsIgnoreCase(PREF_NOTIFY_UPPER))
            value = 50;
        return mPrefer.getInt(type, value);
    }

    public static void setNotify(String type, int value) {
        mPrefer.edit().putInt(type, value).commit();
    }

    public static String getNotifyLayout() {
        String r = mPrefer.getString(PREF_NOTIFY_LAYOUT, SettingNotifyLayout.DEFAULT_LAYOUT);
        return (r.length() != SettingNotifyLayout.DEFAULT_LAYOUT.length() ? SettingNotifyLayout.DEFAULT_LAYOUT : r);
    }

    public static void setNotifyLayout(String layout) {
        if (layout.length() != SettingNotifyLayout.DEFAULT_LAYOUT.length())
            layout = SettingNotifyLayout.DEFAULT_LAYOUT;
        mPrefer.edit().putString(PREF_NOTIFY_LAYOUT, layout).commit();
    }

    public static Set<String> getApList() {
        return mPrefer.getStringSet(PREF_AP_LIST, null);
    }

    public static void setApList(Set<String> list) {
        mPrefer.edit().putStringSet(PREF_AP_LIST, list).commit();
    }

    public static boolean getColorMode() {
        return mPrefer.getBoolean(PREF_COLORMODE, false);
    }

    public static int getColor() {
        return mPrefer.getInt(PREF_COLOR_VALUE, ColorPickerPreference.DEFAULT_COLOR);
    }

    public static void setColor(int color) {
        mPrefer.edit().putInt(PREF_COLOR_VALUE, color).commit();
    }

    public static boolean getDisableButtonBacklight() {
        return mPrefer.getBoolean(PREF_BUTTON_BACKLIGHT, true);
    }

    public static String getAbout() {
        return mPrefer.getString(PREF_ABOUT, "");
    }

    public static void setAbout(String value) {
        mPrefer.edit().putString(PREF_ABOUT, value).commit();
    }

}
