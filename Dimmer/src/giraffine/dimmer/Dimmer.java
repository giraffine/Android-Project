package giraffine.dimmer;

import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class Dimmer extends Activity {

	public static String TAG = "Dimmer";
	public static final String REFRESH_INDEX = "refreshIndex"; 
	private TextView mIndex;
	private ImageButton mAutoMode;
	private View mAutoSetting;
	private TextView mAutoText;
	
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			onResume();
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dimmer);
		mIndex = (TextView)findViewById(R.id.index);
		mAutoMode = (ImageButton)findViewById(R.id.automode);
		mAutoSetting = findViewById(R.id.autosetting);
		mAutoText = (TextView)findViewById(R.id.autotext);

		updateAutoMode();
		
		if(DimmerService.DebugMode)	mAutoMode.setBackgroundColor(Color.CYAN);
		mAutoSetting.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				DimmerService.mIsAutoMode = !DimmerService.mIsAutoMode;
				updateAutoMode();
			}
		});

		mAutoSetting.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction())
				{
				case MotionEvent.ACTION_DOWN:
					mAutoText.setTextColor(Color.YELLOW);
					mAutoMode.setImageResource(R.drawable.auto_pressed);
					break;
				case MotionEvent.ACTION_UP:
					mAutoText.setTextColor(Color.BLACK);
					updateAutoMode();
					break;
				}
				return false;
			}
		});
		

		getActionBar().hide();
		
		Intent startServiceIntent = new Intent();
		startServiceIntent.setComponent(DimmerService.COMPONENT);
        startService(startServiceIntent);
		
		RelativeLayout background = (RelativeLayout)findViewById(R.id.relativelayout);
		background.setOnTouchListener(new View.OnTouchListener() {
			private int downx = 0;
			private int downy = 0;
			private int pivot = 0;
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch(event.getAction())
				{
				case MotionEvent.ACTION_DOWN:
					Log.e(TAG, "MotionEvent.ACTION_DOWN: (" + event.getX() + ", " + event.getY() + "), (" + event.getRawX() + ", " + event.getRawY() + ")");
					downx = (int)event.getRawX();
					downy = (int)event.getRawY();
					pivot = 0;
					break;
				case MotionEvent.ACTION_MOVE:
//					Log.e(TAG, "MotionEvent.ACTION_MOVE: (" + event.getX() + ", " + event.getY() + "), (" + event.getRawX() + ", " + event.getRawY() + ")");
					int diffx = (int)event.getRawX() - downx;
					int diffy = (int)event.getRawY() - downy;
					
					int temp = diffy;
					temp = temp > 1000 ? 1000 : temp;
					temp = temp < -1000 ? -1000 : temp;
					temp = -temp;
					pivot = DimmerService.lastLevel + temp;
					pivot = pivot < 10 ? 10 : pivot;
					pivot = pivot > 1000 ? 1000 : pivot;
					showIndex(pivot);
					changeLevel(DimmerService.ADJUSTLEVEL, pivot);
//					Log.e(TAG, "pivot=" + pivot + ", temp=" + temp);
					break;
				case MotionEvent.ACTION_UP:
					Log.e(TAG, "MotionEvent.ACTION_UP: (" + event.getX() + ", " + event.getY() + "), (" + event.getRawX() + ", " + event.getRawY() + ")");
					showIndex(pivot);
					changeLevel(DimmerService.FINISHLEVEL, pivot);
					break;
				}
				return true;
			}
		});
		registerReceiver(mBroadcastReceiver, new IntentFilter(REFRESH_INDEX));
	}
	protected void onDestroy ()
	{
		super.onDestroy();
		unregisterReceiver(mBroadcastReceiver);
	}
	public void onResume ()
	{
		super.onResume();
		showIndex(DimmerService.lastLevel);
		monitorLight();
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
//		getMenuInflater().inflate(R.menu.dimmer, menu);
		return true;
	}

	public void showIndex(int i)
	{
		mIndex.setText(String.valueOf(i/10));
	}
	public void updateAutoMode()
	{
		if(DimmerService.mIsAutoMode)
		{
			mAutoMode.setImageResource(R.drawable.auto_on);
			mAutoText.setTextColor(Color.BLACK);
		}
		else
		{
			mAutoMode.setImageResource(R.drawable.auto_off);
			mAutoText.setTextColor(Color.LTGRAY);
		}
	}
	public void changeLevel(String action, int i)
	{
		Intent startServiceIntent = new Intent();
		startServiceIntent.setComponent(DimmerService.COMPONENT);
		startServiceIntent.setAction(action);
		startServiceIntent.putExtra(action, i);
        startService(startServiceIntent);
	}
	public void monitorLight()
	{
		Intent startServiceIntent = new Intent();
		startServiceIntent.setComponent(DimmerService.COMPONENT);
		startServiceIntent.setAction(DimmerService.MONITORLIGHT);
        startService(startServiceIntent);
	}
}
