package giraffine.dimmer;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import giraffine.dimmer.RangeSeekBar.OnRangeSeekBarChangeListener;

public class SettingNotifyRange extends DialogPreference {

    private TextView mPivot = null;
    private RangeSeekBar<Integer> mSeekBar = null;

    public SettingNotifyRange(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.setting_notify_range);
    }

    @Override
    public void onBindDialogView(View view) {
        mPivot = (TextView) view.findViewById(R.id.pivot_notify);
        TextView min = (TextView) view.findViewById(R.id.notify_seekMin);
        TextView max = (TextView) view.findViewById(R.id.notify_seekMax);
        min.setText(String.valueOf(1));
        max.setText(String.valueOf(100));

        // create RangeSeekBar as Integer range between 1 and 100
        mSeekBar = new RangeSeekBar<Integer>(1, 100, getContext());
        mSeekBar.setSelectedMinValue(Prefs.getNotify(Prefs.PREF_NOTIFY_LOWER));
        mSeekBar.setSelectedMaxValue(Prefs.getNotify(Prefs.PREF_NOTIFY_UPPER));

        mSeekBar.setNotifyWhileDragging(true);
        mSeekBar.setOnRangeSeekBarChangeListener(new OnRangeSeekBarChangeListener<Integer>() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Integer minValue, Integer maxValue) {
                if (maxValue < 50) {
                    maxValue = 50;
                    mSeekBar.setSelectedMaxValue(50);
                }
                if (minValue > 50) {
                    minValue = 50;
                    mSeekBar.setSelectedMinValue(50);
                }
                showPivot(minValue, maxValue);
            }
        });
        // add RangeSeekBar to pre-defined layout
        ViewGroup group = (ViewGroup) view.findViewById(R.id.notify_root_view);
        group.addView(mSeekBar, 1);

        showPivot(Prefs.getNotify(Prefs.PREF_NOTIFY_LOWER), Prefs.getNotify(Prefs.PREF_NOTIFY_UPPER));
    }

    private void showPivot(int min, int max) {
        mPivot.setText(String.valueOf(min) + " ~ " + String.valueOf(max));
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            int min = mSeekBar.getSelectedMinValue();
            int max = mSeekBar.getSelectedMaxValue();
            Prefs.setNotify(Prefs.PREF_NOTIFY_LOWER, min);
            Prefs.setNotify(Prefs.PREF_NOTIFY_UPPER, max);
            setSummary(String.valueOf(min) + " ~ " + String.valueOf(max));
        }
    }

}
