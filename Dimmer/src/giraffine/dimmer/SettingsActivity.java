package giraffine.dimmer;

import android.app.Activity;
import android.os.Bundle;

public class SettingsActivity extends Activity{

	public static boolean showSettings = false;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActionBar() != null)
        	getActionBar().hide();

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
        showSettings = true;
    }
	
	public void onPause ()
	{
		super.onPause();
		showSettings = false;
		finish();
	}
}
