package giraffine.dimmer;

import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
	private ImageButton mSettings;
	
	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			onResume();
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Prefs.init(this);
		boolean showActivity = !Prefs.getWidgetMode()
				|| getIntent().getAction().equalsIgnoreCase(DimmerService.ACTIONNOTIFICATION);
		
		if(showActivity)
			setTheme(R.style.AppTheme);
		else
			setTheme(android.R.style.Theme_Dialog);
		
		setContentView(R.layout.activity_dimmer);

		if(showActivity)
			getActionBar().hide();
		
		mIndex = (TextView)findViewById(R.id.index);
		mSettings = (ImageButton)findViewById(R.id.settings);
		mSettings.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startSettings();
			}
		});
		
		startDimmerService(!showActivity);
		
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

		if(!showActivity)
			finish();
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
	}
	public void onPause ()
	{
		super.onPause();
		finish();
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
	public void changeLevel(String action, int i)
	{
		Intent startServiceIntent = new Intent();
		startServiceIntent.setComponent(DimmerService.COMPONENT);
		startServiceIntent.setAction(action);
		startServiceIntent.putExtra(action, i);
        startService(startServiceIntent);
	}
	public void startSettings()
	{
		startActivity(new Intent(this, SettingsActivity.class));
	}
	public void startDimmerService(boolean switchDim)
	{
		Intent startServiceIntent = new Intent();
		startServiceIntent.setComponent(DimmerService.COMPONENT);
		if(switchDim)
			startServiceIntent.setAction(DimmerService.SWITCHDIM);
        startService(startServiceIntent);
	}
}
