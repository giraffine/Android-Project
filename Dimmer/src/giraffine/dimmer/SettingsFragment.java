package giraffine.dimmer;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment implements OnPreferenceClickListener, OnPreferenceChangeListener{
	
	public static final String REFRESH_LUX = "refreshLux";
	private CheckBoxPreference mPrefAutoMode = null;
	private CheckBoxPreference mPrefWidgetMode = null;
	private ListPreference mPrefSpeedDim = null;
	private ListPreference mPrefSpeedBright = null;
	private Preference mPrefThresholdDim = null;
	private Preference mPrefThresholdBright = null;
	private Preference mPrefApList = null;
	private Preference mPrefAlarmDim = null;
	private Preference mPrefAlarmBright = null;
	private Preference mPrefNotifyStep = null;
	private Preference mPrefNotifyRange = null;
	private Preference mPrefNotifyLayout = null;
	private CheckBoxPreference mPrefNotifyPriority = null;
	private CheckBoxPreference mPrefColorMode = null;
	private Preference mPrefColorPicker = null;
	
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			showLuxInfo(arg1.getIntExtra("lux", 0));
		}
	};

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Prefs.init(getActivity());

        addPreferencesFromResource(R.xml.preference);
        
        mPrefAutoMode = (CheckBoxPreference)findPreference(Prefs.PREF_AUTOMODE);
        mPrefAutoMode.setOnPreferenceClickListener(this);
        
        mPrefWidgetMode = (CheckBoxPreference)findPreference(Prefs.PREF_WIDGETMODE);
        mPrefWidgetMode.setOnPreferenceClickListener(this);
        mPrefWidgetMode.setChecked(Prefs.getWidgetMode());

        mPrefSpeedDim = (ListPreference)findPreference(Prefs.PREF_SPEED_DIM);
        mPrefSpeedDim.setOnPreferenceChangeListener(this);
        mPrefSpeedBright = (ListPreference)findPreference(Prefs.PREF_SPEED_BRIGHT);
        mPrefSpeedBright.setOnPreferenceChangeListener(this);
        
        mPrefThresholdDim = findPreference(Prefs.PREF_THRESHOLD_DIM);
        mPrefThresholdBright = findPreference(Prefs.PREF_THRESHOLD_BRIGHT);
        
        mPrefApList = findPreference(Prefs.PREF_AP_LIST);
        
        mPrefAlarmDim = findPreference(Prefs.PREF_ALARM_DIM);
        mPrefAlarmBright = findPreference(Prefs.PREF_ALARM_BRIGHT);
        
        mPrefNotifyStep = findPreference(Prefs.PREF_NOTIFY_STEP);
        mPrefNotifyRange = findPreference(Prefs.PREF_NOTIFY_RANGE);
        mPrefNotifyLayout = findPreference(Prefs.PREF_NOTIFY_LAYOUT);
        mPrefNotifyPriority = (CheckBoxPreference)findPreference(Prefs.PREF_NOTIFY_PRIORITY);
        mPrefNotifyPriority.setOnPreferenceClickListener(this);
        
        mPrefColorMode = (CheckBoxPreference)findPreference(Prefs.PREF_COLORMODE);
        mPrefColorMode.setOnPreferenceClickListener(this);
        mPrefColorPicker = findPreference(Prefs.PREF_COLOR_VALUE);
        
        updateAutoSettings();
        updateAlarmSettings();
        updateNotifySettings();
        updateColorSettings();
        
        try {
        	String version = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
            Preference about = findPreference(Prefs.PREF_ABOUT);
			about.setTitle("Version: " + version);
			Prefs.setAbout(version);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
        
        getActivity().registerReceiver(mBroadcastReceiver, new IntentFilter(REFRESH_LUX));
//        showAutoModeDetail(false);
    }
	public void onDestroy ()
	{
		super.onDestroy();
		getActivity().unregisterReceiver(mBroadcastReceiver);
	}
	public void showLuxInfo(int lux)
	{
		mPrefAutoMode.setSummaryOn(getActivity().getResources().getString(R.string.pref_auto_lux_state)
				+ " " + String.valueOf(lux) + " lux");
	}
	@Override
	public boolean onPreferenceClick(Preference pref) {
		if(pref.getKey().equalsIgnoreCase(Prefs.PREF_AUTOMODE))
		{
			changeAutoMode(mPrefAutoMode.isChecked());
			mPrefThresholdDim.setEnabled(mPrefAutoMode.isChecked());
			mPrefThresholdBright.setEnabled(mPrefAutoMode.isChecked());
			mPrefSpeedDim.setEnabled(mPrefAutoMode.isChecked());
			mPrefSpeedBright.setEnabled(mPrefAutoMode.isChecked());
			mPrefApList.setEnabled(mPrefAutoMode.isChecked());
			return true;
		}
		else if(pref.getKey().equalsIgnoreCase(Prefs.PREF_WIDGETMODE))
		{
			if(mPrefWidgetMode.isChecked())
				Toast.makeText(getActivity(), R.string.pref_widget_hint, Toast.LENGTH_LONG).show();
		}
		else if(pref.getKey().equalsIgnoreCase(Prefs.PREF_NOTIFY_PRIORITY))
		{
			pref.getEditor().commit();
			changeStatusBarIcon();
		}
		else if(pref.getKey().equalsIgnoreCase(Prefs.PREF_COLORMODE))
		{
			pref.getEditor().commit();
			updateColorSettings();
			changeColorMode(mPrefColorMode.isChecked());
		}
		return false;
	}
	@Override
	public boolean onPreferenceChange(Preference pref, Object newValue) {
		if(pref.getKey().equalsIgnoreCase(Prefs.PREF_SPEED_DIM))
		{
			CharSequence[] entries = mPrefSpeedDim.getEntries();
			mPrefSpeedDim.setSummary(entries[Integer.valueOf((String)newValue)-1]);
			changeSensitive();
			return true;
		}
		else if(pref.getKey().equalsIgnoreCase(Prefs.PREF_SPEED_BRIGHT))
		{
			CharSequence[] entries = mPrefSpeedBright.getEntries();
			mPrefSpeedBright.setSummary(entries[Integer.valueOf((String)newValue)-1]);
			changeSensitive();
			return true;
		}
		return false;
	}
	
	public void changeAutoMode(boolean on)
	{
		Intent startServiceIntent = new Intent();
		startServiceIntent.setComponent(DimmerService.COMPONENT);
		startServiceIntent.setAction(DimmerService.SWITCHAUTOMODE);
		startServiceIntent.putExtra(DimmerService.SWITCHAUTOMODE, on);
		getActivity().startService(startServiceIntent);
	}
	public void changeSensitive()
	{
		Intent startServiceIntent = new Intent();
		startServiceIntent.setComponent(DimmerService.COMPONENT);
		startServiceIntent.setAction(DimmerService.SENSITIVECHANGE);
		getActivity().startService(startServiceIntent);
	}
	public void changeStatusBarIcon()
	{
		Intent startServiceIntent = new Intent();
		startServiceIntent.setComponent(DimmerService.COMPONENT);
		startServiceIntent.setAction(DimmerService.STATUSBARCHANGE);
		getActivity().startService(startServiceIntent);
	}
	public void changeColorMode(boolean enable)
	{
		Intent startServiceIntent = new Intent();
		startServiceIntent.setComponent(DimmerService.COMPONENT);
		startServiceIntent.setAction(DimmerService.COLORCHANGE);
		startServiceIntent.putExtra(DimmerService.COLORCHANGE+"ON", enable);
		getActivity().startService(startServiceIntent);
	}
	public void updateAutoSettings()
	{
		if(!LightSensor.hasLightSensor(getActivity()))
		{
			changeAutoMode(false);
			mPrefAutoMode.setChecked(false);
			mPrefAutoMode.setEnabled(false);
			showAutoModeDetail(false);
			mPrefAutoMode.setSummaryOff(getActivity().getResources().getString(R.string.pref_auto_summary)
					+ "\n\n" + getActivity().getResources().getString(R.string.pref_auto_not_support));
			return;
		}
		mPrefThresholdDim.setEnabled(mPrefAutoMode.isChecked());
		mPrefThresholdDim.setSummary(Prefs.getThresholdDimLowest() ? 
				getActivity().getResources().getString(R.string.pref_threshold_dim_lowest) :
				getActivity().getResources().getString(R.string.pref_threshold_dim_lux) + " < "+ Prefs.getThresholdDim() + " lux" );
		mPrefThresholdBright.setEnabled(mPrefAutoMode.isChecked());
		mPrefThresholdBright.setSummary(getActivity().getResources().getString(R.string.pref_threshold_bright_diff) 
				+ " > "+ Prefs.getThresholdBright() + " lux" );
		mPrefSpeedDim.setEnabled(mPrefAutoMode.isChecked());
		mPrefSpeedBright.setEnabled(mPrefAutoMode.isChecked());
		mPrefSpeedDim.setSummary(mPrefSpeedDim.getEntry());
		mPrefSpeedBright.setSummary(mPrefSpeedBright.getEntry());
		
		mPrefApList.setEnabled(mPrefAutoMode.isChecked());
		mPrefApList.setSummary(SettingApList.getSummary(Prefs.getApList(), getActivity().getPackageManager()));
	}
	public void updateAlarmSettings()
	{
		if(AlarmUtil.getAlarmOnOff(Prefs.PREF_ALARM_DIM))
			mPrefAlarmDim.setSummary(AlarmUtil.getAlarmTime(Prefs.PREF_ALARM_DIM, getActivity()));
		else
			mPrefAlarmDim.setSummary(getActivity().getResources().getString(R.string.pref_alarm_off));
		
		if(AlarmUtil.getAlarmOnOff(Prefs.PREF_ALARM_BRIGHT))
			mPrefAlarmBright.setSummary(AlarmUtil.getAlarmTime(Prefs.PREF_ALARM_BRIGHT, getActivity()));
		else
			mPrefAlarmBright.setSummary(getActivity().getResources().getString(R.string.pref_alarm_off));
	}
	public void updateNotifySettings()
	{
		mPrefNotifyStep.setSummary(String.valueOf(Prefs.getNotify(Prefs.PREF_NOTIFY_STEP)));
		mPrefNotifyRange.setSummary(String.valueOf(Prefs.getNotify(Prefs.PREF_NOTIFY_LOWER))
				+ " ~ " + String.valueOf(Prefs.getNotify(Prefs.PREF_NOTIFY_UPPER)));
		mPrefNotifyLayout.setSummary(SettingNotifyLayout.getSummary(Prefs.getNotifyLayout()));
	}
	public void updateColorSettings()
	{
		mPrefColorPicker.setEnabled(mPrefColorMode.isChecked());
	}
	public void showAutoModeDetail(boolean show)
	{
		if(show)
		{
	        ((PreferenceCategory)findPreference("pref_automode_category")).addPreference(mPrefThresholdDim);
	        ((PreferenceCategory)findPreference("pref_automode_category")).addPreference(mPrefThresholdBright);
	        ((PreferenceCategory)findPreference("pref_automode_category")).addPreference(mPrefSpeedDim);
	        ((PreferenceCategory)findPreference("pref_automode_category")).addPreference(mPrefSpeedBright);
	        ((PreferenceCategory)findPreference("pref_automode_category")).addPreference(mPrefApList);
		}
		else
		{
	        ((PreferenceCategory)findPreference("pref_automode_category")).removePreference(mPrefThresholdDim);
	        ((PreferenceCategory)findPreference("pref_automode_category")).removePreference(mPrefThresholdBright);
	        ((PreferenceCategory)findPreference("pref_automode_category")).removePreference(mPrefSpeedDim);
	        ((PreferenceCategory)findPreference("pref_automode_category")).removePreference(mPrefSpeedBright);
	        ((PreferenceCategory)findPreference("pref_automode_category")).removePreference(mPrefApList);
		}
	}
}
