package giraffine.dimmer;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class SettingNotifyStep extends DialogPreference {

    private TextView mPivot = null;
    private SeekBar mSeekBar = null;

    public SettingNotifyStep(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.setting_notify_step);
    }

    @Override
    public void onBindDialogView(View view) {
        mPivot = (TextView) view.findViewById(R.id.pivot_notify);
        mSeekBar = (SeekBar) view.findViewById(R.id.notifySeekBar);
        TextView min = (TextView) view.findViewById(R.id.notify_seekMin);
        TextView max = (TextView) view.findViewById(R.id.notify_seekMax);
        mSeekBar.setMax(9);
        min.setText(String.valueOf(1));
        max.setText(String.valueOf(10));
        mSeekBar.setProgress(Prefs.getNotify(getKey()));
        showPivot(Prefs.getNotify(getKey()));

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.e(Dimmer.TAG, "onProgressChanged: " + progress);
                showPivot(progress + 1);
            }
        });
    }

    private void showPivot(int value) {
        mPivot.setText(String.valueOf(value));
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            int progress = mSeekBar.getProgress();
            Prefs.setNotify(getKey(), progress + 1);
            setSummary(String.valueOf(progress + 1));
        }
    }

}
