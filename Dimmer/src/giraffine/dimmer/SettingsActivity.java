package giraffine.dimmer;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceClickListener{
	
	private String PREF_AUTOMODE = "pref_automode";
	private CheckBoxPreference mPrefAutoMode = null;
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();
        addPreferencesFromResource(R.xml.preference);
        
        mPrefAutoMode = (CheckBoxPreference)findPreference(PREF_AUTOMODE);
        mPrefAutoMode.setOnPreferenceClickListener(this);
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


