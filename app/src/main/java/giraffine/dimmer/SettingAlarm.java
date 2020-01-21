package giraffine.dimmer;

import android.content.Context;
import android.content.Intent;
import android.preference.DialogPreference;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;

public class SettingAlarm extends DialogPreference {

    private TimePicker mTimePicker = null;

    public SettingAlarm(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.setting_alarm);
        setNegativeButtonText(getContext().getResources().getString(R.string.pref_alarm_off));
    }

    @Override
    public void onBindDialogView(View view) {
        mTimePicker = (TimePicker) view.findViewById(R.id.timePicker1);
        int[] hourminute = new int[2];
        AlarmUtil.getAlarmTime(getKey(), hourminute);
        mTimePicker.setCurrentHour(hourminute[0]);
        mTimePicker.setCurrentMinute(hourminute[1]);
        mTimePicker.setIs24HourView(DateFormat.is24HourFormat(getContext()));
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        AlarmUtil.setAlarm(getKey(), positiveResult, mTimePicker.getCurrentHour(), mTimePicker.getCurrentMinute());
        updateAlarmSettings();
        changeAlarm();
    }

    public void updateAlarmSettings() {
        if (AlarmUtil.getAlarmOnOff(getKey()))
            setSummary(AlarmUtil.getAlarmTime(getKey(), getContext()));
        else
            setSummary(getContext().getResources().getString(R.string.pref_alarm_off));
    }

    public void changeAlarm() {
        Intent startServiceIntent = new Intent();
        startServiceIntent.setComponent(DimmerService.COMPONENT);
        startServiceIntent.setAction(DimmerService.ALARMCHANGE);
        getContext().startService(startServiceIntent);
    }
}
