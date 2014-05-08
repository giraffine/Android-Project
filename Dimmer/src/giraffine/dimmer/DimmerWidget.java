package giraffine.dimmer;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class DimmerWidget extends AppWidgetProvider{

	private static RemoteViews mWidgetView;
	private static RemoteViews getWidgetView(Context context)
	{
		if(mWidgetView == null)
		{
			Intent intent = new Intent(context, DimmerService.class);
			intent.setAction(DimmerService.SWITCHDIM);
			PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);
			mWidgetView = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
			mWidgetView.setOnClickPendingIntent(R.id.widgetImage, pendingIntent);
		}
		return mWidgetView;
	}
	public static void updateDim(Context context, boolean dim)
	{
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, DimmerWidget.class));
		if(appWidgetIds.length < 1)
			return;
		if(dim)
			getWidgetView(context).setImageViewResource(R.id.widgetImage, R.drawable.ic_widget);
		else
			getWidgetView(context).setImageViewResource(R.id.widgetImage, R.drawable.ic_launcher);
		appWidgetManager.updateAppWidget(appWidgetIds, getWidgetView(context));
	}
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		if(appWidgetIds.length < 1)
			return;
		appWidgetManager.updateAppWidget(appWidgetIds, getWidgetView(context));
	}
}
