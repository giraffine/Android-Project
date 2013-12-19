package giraffine.dimmer;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class DialogTrigger extends DialogPreference{

	public static final String REFRESH_LUX = "refreshLux";
	public static ComponentName COMPONENT = new ComponentName(DimmerService.PACKAGENAME, DimmerService.PACKAGENAME+".DialogTrigger");
	
	private TextView mCurrentLux;
	private TextView mPivotLux;
	private SeekBar mSeekBar;
	private ImageButton mButtonIncre;
	private ImageButton mButtonDecre;
	private Switch mSwitchSetLowest;
	private Context mContext;
	private Dialog mDialog;
	private Thread mThread;
	private boolean mStopThread = true;
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			mCurrentLux.setText("Current ambient light: " + String.valueOf(arg1.getIntExtra("lux", 0)) + " lux");
		}
	};
	private int mShift;
	private int mAdjustRegion = 100;
	
	public DialogTrigger(Context context, AttributeSet attrs) {
		super(context, attrs);

		setDialogLayoutResource(R.layout.setting_trigger);
	}
	@Override
	public void onBindDialogView (View view)
	{
		mContext = getContext();
		mDialog = getDialog();
		
		mCurrentLux = (TextView)view.findViewById(R.id.currentLux);
		mPivotLux = (TextView)view.findViewById(R.id.pivotLux);
		mSeekBar = (SeekBar)view.findViewById(R.id.luxSeekBar);
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
		TextView min = (TextView)view.findViewById(R.id.seekMin);
		TextView max = (TextView)view.findViewById(R.id.seekMax);
		min.setText(String.valueOf(bound.x));
		max.setText(String.valueOf(bound.x + mAdjustRegion));

		mSwitchSetLowest.setChecked(Prefs.getTriggerLowest());
		if(Prefs.getTriggerLowest())
		{
			mSeekBar.setProgress(0);
			mPivotLux.setText(String.valueOf(bound.x));
		}
		else
		{
			mSeekBar.setProgress(Prefs.getTriggerValue() - mShift);
			mPivotLux.setText(String.valueOf(Prefs.getTriggerValue()));
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
				mPivotLux.setText(String.valueOf(progress + mShift));
				if(progress != 0)
					mSwitchSetLowest.setChecked(false);
			}
		});
		this.getContext().registerReceiver(mBroadcastReceiver, new IntentFilter(REFRESH_LUX));
	}
/*	
	@Override
	public View onCreateDialogView()
	{
		return super.onCreateDialogView();
	}
*/
	@Override
	public void onDialogClosed(boolean positiveResult)
	{
		this.getContext().unregisterReceiver(mBroadcastReceiver);
		
		if(positiveResult)
		{
			Prefs.setTriggerValue(mSeekBar.getProgress() + mShift);
			Prefs.setTriggerLowest(mSwitchSetLowest.isChecked());
			if(mSwitchSetLowest.isChecked())
				setSummary("Detect lowest ambient light");
			else
				setSummary("Ambient light < "+ String.valueOf(mSeekBar.getProgress() + mShift) + " lux");
		}
	}
}
