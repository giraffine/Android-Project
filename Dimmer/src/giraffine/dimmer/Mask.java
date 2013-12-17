package giraffine.dimmer;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

public class Mask {
	
	private Context mContext;
	private WindowManager mWindowManager;
	private WindowManager.LayoutParams mWindowParams;
	private int maskLength = 0;
	private View mMaskView;

	public Mask(Context context){
		mContext = context;
		initMask();
	}
	private void initMask()
	{
		mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		mWindowParams = new WindowManager.LayoutParams();

		mWindowParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
		mWindowParams.flags |= 
			WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE 
			| WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS 
			| WindowManager.LayoutParams.FLAG_FULLSCREEN	// remove to keep status bar out of mask
			| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
			| WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
			| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
			| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
		mWindowParams.gravity = Gravity.LEFT | Gravity.TOP;
		Point p = new Point();
		mWindowManager.getDefaultDisplay().getSize(p);
		mWindowParams.x = 0;
		mWindowParams.y = 0;
		maskLength = ( p.x > p.y ? p.x : p.y) +300;
		mWindowParams.width = maskLength;
		mWindowParams.height = maskLength;
		mWindowParams.format = 1;
		mWindowParams.alpha = (float)0;	// default is transparent

		LayoutInflater inflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mMaskView = inflater.inflate(R.layout.mask_window, null);
//		mMaskView.setAlpha((float)1);	// == android:alpha
		
		mWindowManager.addView(mMaskView, mWindowParams);
	}
	public void adjustLevel(int i, boolean setBrightness)
	{
		Log.e(Dimmer.TAG, "adjustLevel: " + i);
		if(i > 500)
		{
			float index = ((float)(i - 500))/500;
			if(setBrightness)
				BrightnessUtil.setBrightness((int)(index*255));
			else
				maskBrightness(index);
		}
		else
		{
			if(setBrightness)
				BrightnessUtil.setBrightness(0);
			i = 500 - i;
			adjustMask(((float)i/500));
		}
	}
	public void removeMask()
	{
		Log.e(Dimmer.TAG, "removeMask");
		mWindowParams.width = 1;	// reduce memory usage
		mWindowParams.height = 1;	// reduce memory usage
		mWindowParams.screenBrightness = -1;	// use the system preferred screen brightness
		mWindowParams.alpha = 0;
		mWindowManager.updateViewLayout(mMaskView, mWindowParams);
	}
	private void maskBrightness(float value)
	{
		Log.e(Dimmer.TAG, "maskBrightness: " + value);
		if(value <= 0.01)	return;	// for padfone: value ~0.0 would fully dark screen
		mWindowParams.width = 1;	// reduce memory usage
		mWindowParams.height = 1;	// reduce memory usage
		mWindowParams.screenBrightness = value;
		if(mWindowParams.alpha != 0)	mWindowParams.alpha = 0;
		mWindowManager.updateViewLayout(mMaskView, mWindowParams);
	}
	private void adjustMask(float alpha)
	{
		Log.e(Dimmer.TAG, "adjustMask: " + alpha);
		if(alpha > 0.98) alpha = (float)0.9;
		else if(alpha < 0) alpha = (float)0;
//		mMaskView.setAlpha(alpha);	// control parent window is much safe
		mWindowParams.width = maskLength;
		mWindowParams.height = maskLength;
		mWindowParams.alpha = alpha; 
		// this will cause fully dark screen
//		if(mWindowParams.screenBrightness != 0)	mWindowParams.screenBrightness = (float)0;
		mWindowManager.updateViewLayout(mMaskView, mWindowParams);
	}
}
