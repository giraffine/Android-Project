package giraffine.dimmer;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class SettingAbout extends DialogPreference {

    public SettingAbout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.setting_about);
    }

    @Override
    public void onBindDialogView(View view) {
        super.onBindDialogView(view);
        setDialogTitle("Dimmer Open Source Project");
        String string =
                "Welcome to contribute Dimmer."
                        + "\nOriginal project by:"
                        + "\nhttps://github.com/giraffine"
                        + "\n"
                        + "\nSource Code on GitHub:"
                        + "\nhttps://github.com/webhat/Dimmer";

        TextView about = (TextView) view.findViewById(R.id.aboutText);
        about.setText(string);
    }
}
