package giraffine.dimmer;

import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.widget.Toast;

public class SettingsFragment extends PreferenceFragment implements OnPreferenceClickListener, OnPreferenceChangeListener{
	
	private CheckBoxPreference mPrefAutoMode = null;
	private CheckBoxPreference mPrefWidgetMode = null;
	private ListPreference mPrefSensitiveOn = null;
	private ListPreference mPrefSensitiveOff = null;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preference);
        
        mPrefAutoMode = (CheckBoxPreference)findPreference(Prefs.PREF_AUTOMODE);
        mPrefAutoMode.setOnPreferenceClickListener(this);
        
        mPrefWidgetMode = (CheckBoxPreference)findPreference(Prefs.PREF_WIDGETMODE);
        mPrefWidgetMode.setOnPreferenceClickListener(this);

        mPrefSensitiveOn = (ListPreference)findPreference(Prefs.PREF_SENSITIVE_ON);
        mPrefSensitiveOn.setOnPreferenceChangeListener(this);
        mPrefSensitiveOff = (ListPreference)findPreference(Prefs.PREF_SENSITIVE_OFF);
        mPrefSensitiveOff.setOnPreferenceChangeListener(this);
        
        updateSettings();
        
        Preference about = findPreference(Prefs.PREF_ABOUT);
        try {
			about.setTitle("Version: " + getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
    }
	@Override
	public boolean onPreferenceClick(Preference pref) {
		if(pref.getKey().equalsIgnoreCase(Prefs.PREF_AUTOMODE))
		{
			changeAutoMode(mPrefAutoMode.isChecked());
			mPrefSensitiveOn.setEnabled(mPrefAutoMode.isChecked());
			mPrefSensitiveOff.setEnabled(mPrefAutoMode.isChecked());
			return true;
		}
		else if(pref.getKey().equalsIgnoreCase(Prefs.PREF_WIDGETMODE))
		{
			if(mPrefWidgetMode.isChecked())
				Toast.makeText(getActivity(), R.string.pref_widget_hint, Toast.LENGTH_LONG).show();
		}
		return false;
	}
	@Override
	public boolean onPreferenceChange(Preference pref, Object newValue) {
		if(pref.getKey().equalsIgnoreCase(Prefs.PREF_SENSITIVE_ON))
		{
			CharSequence[] entries = mPrefSensitiveOn.getEntries();
			mPrefSensitiveOn.setSummary(entries[Integer.valueOf((String)newValue)-1]);
			changeSensitive();
			return true;
		}
		else if(pref.getKey().equalsIgnoreCase(Prefs.PREF_SENSITIVE_OFF))
		{
			CharSequence[] entries = mPrefSensitiveOff.getEntries();
			mPrefSensitiveOff.setSummary(entries[Integer.valueOf((String)newValue)-1]);
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
	public void updateSettings()
	{
		mPrefSensitiveOn.setEnabled(mPrefAutoMode.isChecked());
		mPrefSensitiveOff.setEnabled(mPrefAutoMode.isChecked());
		mPrefSensitiveOn.setSummary(mPrefSensitiveOn.getEntry());
		mPrefSensitiveOff.setSummary(mPrefSensitiveOff.getEntry());
	}
}
