package giraffine.dimmer;

import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceClickListener{
	
	private String PREF_AUTOMODE = "pref_automode";
	private String PREF_ABOUT = "pref_about";
	
	private CheckBoxPreference mPrefAutoMode = null;
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();
        addPreferencesFromResource(R.xml.preference);
        
        mPrefAutoMode = (CheckBoxPreference)findPreference(PREF_AUTOMODE);
        mPrefAutoMode.setOnPreferenceClickListener(this);
        mPrefAutoMode.setChecked(Prefs.isAutoMode());
        
        Preference about = findPreference(PREF_ABOUT);
        try {
			about.setTitle("Version: " + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
    }


	@Override
	public boolean onPreferenceClick(Preference pref) {
		if(pref.getKey().equalsIgnoreCase(PREF_AUTOMODE))
		{
			Prefs.setAutoMode(mPrefAutoMode.isChecked() ? true : false);
			return true;
		}
		return false;
	}
}


