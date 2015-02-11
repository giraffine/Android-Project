package giraffine.dimmer;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class SettingAbout extends DialogPreference{

	public SettingAbout(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.setting_about);
	}
	@Override
	public void onBindDialogView (View view)
	{
		setDialogTitle("Dimmer Open Source Project");
		String string = 
				"Welcome to contribute Dimmer."
				+ "\n"
				+ "\nSource Code on GitHub:"
				+ "\nhttps://github.com/giraffine"
				+ "\n"
				+ "\nDimmer Develop Group:"
				+ "\nhttp://goo.gl/03dsL1";
				
		TextView about = (TextView)view.findViewById(R.id.aboutText);
		about.setText(string);
	}
}
