package giraffine.dimmer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class TaskerReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getAction().equalsIgnoreCase("giraffine.dimmer.TOGGLE_ON_OFF"))
		{
			startDimmer(context, DimmerService.SWITCHDIM, null);
		}
		else if(intent.getAction().equalsIgnoreCase("giraffine.dimmer.TOGGLE_PAUSE_RESUME"))
		{
			startDimmer(context, DimmerService.PAUSEFUNCTION, null);
		}
		else if(intent.getAction().equalsIgnoreCase("giraffine.dimmer.STOP"))
		{
			startDimmer(context, DimmerService.RESETLEVEL, null);
		}
		else if(intent.getAction().equalsIgnoreCase("giraffine.dimmer.STEP_UP"))
		{
			startDimmer(context, DimmerService.STEPLEVELUP, null);
		}
		else if(intent.getAction().equalsIgnoreCase("giraffine.dimmer.STEP_DOWN"))
		{
			startDimmer(context, DimmerService.STEPLEVELDOWN, null);
		}
	}
	public void startDimmer(Context context, String action, Bundle bundle)
	{
		Intent startServiceIntent = new Intent();
		startServiceIntent.setComponent(DimmerService.COMPONENT);
		startServiceIntent.setAction(action);
		context.startService(startServiceIntent);
	}

}
