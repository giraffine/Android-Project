package giraffine.dimmer;

import android.content.Context;
import android.graphics.Point;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class SettingEnterDim extends DialogPreference{

	private TextView mPivotLux;
	private SeekBar mSeekBar;
	private Switch mSwitchSetLowest;
	private int mShift;
	private int mAdjustRegion = 100;
	
	public SettingEnterDim(Context context, AttributeSet attrs) {
		super(context, attrs);

		setDialogLayoutResource(R.layout.setting_enter_dim);
	}
	@Override
	public void onBindDialogView (View view)
	{
		mPivotLux = (TextView)view.findViewById(R.id.pivotLux_dim);
		mSeekBar = (SeekBar)view.findViewById(R.id.luxSeekBar_dim);
		mSwitchSetLowest = (Switch)view.findViewById(R.id.switchSetLowest);
		mSwitchSetLowest.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				if(arg1)
				{
					mSeekBar.setProgress(0);
				}
			}
		});
		
		Point bound = new Point();
		LuxUtil.getBoundaryLevel(bound);
		mShift = bound.x;
		mSeekBar.setMax(mAdjustRegion);
		TextView min = (TextView)view.findViewById(R.id.seekMin_dim);
		TextView max = (TextView)view.findViewById(R.id.seekMax_dim);
		min.setText(String.valueOf(bound.x));
		max.setText(String.valueOf(bound.x + mAdjustRegion));

		mSwitchSetLowest.setChecked(Prefs.getThresholdDimLowest());
		if(Prefs.getThresholdDimLowest())
		{
			mSeekBar.setProgress(0);
			showPivotLux(bound.x);
		}
		else
		{
			mSeekBar.setProgress(Prefs.getThresholdDim() - mShift);
			showPivotLux(Prefs.getThresholdDim());
		}
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
				showPivotLux(progress + mShift);
				if(progress != 0)
					mSwitchSetLowest.setChecked(false);
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
			Prefs.setThresholdDim(mSeekBar.getProgress() + mShift);
			Prefs.setThresholdDimLowest(mSwitchSetLowest.isChecked());
			if(mSwitchSetLowest.isChecked())
				setSummary(getContext().getResources().getString(R.string.pref_threshold_dim_lowest));
			else
				setSummary(getContext().getResources().getString(R.string.pref_threshold_dim_lux)
						+ " < "+ String.valueOf(mSeekBar.getProgress() + mShift) + " lux");
		}
	}
}
