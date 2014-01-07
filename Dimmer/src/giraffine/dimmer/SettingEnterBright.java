package giraffine.dimmer;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class SettingEnterBright extends DialogPreference{

	private TextView mPivotLux;
	private SeekBar mSeekBar;
	
	public SettingEnterBright(Context context, AttributeSet attrs) {
		super(context, attrs);

		setDialogLayoutResource(R.layout.setting_enter_bright);
	}
	@Override
	public void onBindDialogView (View view)
	{
		mPivotLux = (TextView)view.findViewById(R.id.pivotLux_bright);
		mSeekBar = (SeekBar)view.findViewById(R.id.luxSeekBar_bright);
		mSeekBar.setMax(99);
		TextView min = (TextView)view.findViewById(R.id.seekMin_bright);
		TextView max = (TextView)view.findViewById(R.id.seekMax_bright);
		min.setText(String.valueOf(1));
		max.setText(String.valueOf(100));

		mSeekBar.setProgress(Prefs.getThresholdBright());
		showPivotLux(Prefs.getThresholdBright());
		
		mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				Log.e(Dimmer.TAG, "onStopTrackingTouch");
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				Log.e(Dimmer.TAG, "onStartTrackingTouch");
			}
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				Log.e(Dimmer.TAG, "onProgressChanged: " + progress);
				showPivotLux(progress + 1);
			}
		});
	}
	private void showPivotLux(int lux)
	{
		mPivotLux.setText(String.valueOf(lux) + " lux");
	}
	@Override
	public void onDialogClosed(boolean positiveResult)
	{
		if(positiveResult)
		{
			Prefs.setThresholdBright(mSeekBar.getProgress() +1);
			setSummary(getContext().getResources().getString(R.string.pref_threshold_bright_diff)
					+ " > "+ String.valueOf(mSeekBar.getProgress() +1) + " lux");
		}
	}
}
