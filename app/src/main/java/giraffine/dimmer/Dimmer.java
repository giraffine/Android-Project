package giraffine.dimmer;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

public class Dimmer extends Activity {

    public static String TAG = "Dimmer";
    public static final String REFRESH_INDEX = "refreshIndex";
    public static boolean showMainApp = false;
    public static boolean collectState = false;
    private TextView mIndex;

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            onResume();
        }
    };

    public void checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Log.v("App", "Requesting Overlay Permission");
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    public void checkFileWritePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                Log.v("App", "Requesting Write Settings");
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Prefs.init(this);
        boolean showActivity = !Prefs.getWidgetMode()
                || DimmerService.ACTIONNOTIFICATION.equalsIgnoreCase(getIntent().getAction());

        if (showActivity) showMainApp = true;

        if (showActivity)
            setTheme(R.style.AppTheme);
        else
            setTheme(android.R.style.Theme_Dialog);

        setContentView(R.layout.activity_dimmer);

        mIndex = (TextView) findViewById(R.id.index);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            checkFileWritePermissions();
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.SYSTEM_ALERT_WINDOW)
                != PackageManager.PERMISSION_GRANTED) {
            checkDrawOverlayPermission();
        }

        if (Settings.canDrawOverlays(this)) {
            startDimmerService(!showActivity);
        }

        RelativeLayout background = (RelativeLayout) findViewById(R.id.relativelayout);
        background.setOnTouchListener(new View.OnTouchListener() {
            private int downx = 0;
            private int downy = 0;
            private int pivot = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Log.e(TAG, "MotionEvent.ACTION_DOWN: (" + event.getX() + ", " + event.getY() + "), (" + event.getRawX() + ", " + event.getRawY() + ")");
                        downx = (int) event.getRawX();
                        downy = (int) event.getRawY();
                        pivot = 0;
                        if (!collectState)    // Only at the first time adjustment, try collect state to keep original setting
                        {
                            BrightnessUtil.collectState();
                            collectState = true;
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
//					Log.e(TAG, "MotionEvent.ACTION_MOVE: (" + event.getX() + ", " + event.getY() + "), (" + event.getRawX() + ", " + event.getRawY() + ")");
                        int diffx = (int) event.getRawX() - downx;
                        int diffy = (int) event.getRawY() - downy;

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

        if (!showActivity)
            finish();
    }

    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
        showMainApp = false;
    }

    public void onResume() {
        super.onResume();
        showIndex(DimmerService.lastLevel);
    }

    public void onPause() {
        super.onPause();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.dimmer_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startSettings();
                return true;
        }
        return false;
    }

    public void showIndex(int i) {
        mIndex.setText(String.valueOf(i / 10));
    }

    public void changeLevel(String action, int i) {
        Intent startServiceIntent = new Intent();
        startServiceIntent.setComponent(DimmerService.COMPONENT);
        startServiceIntent.setAction(action);
        startServiceIntent.putExtra(action, i);
        startService(startServiceIntent);
    }

    public void startSettings() {
        startActivity(new Intent(this, SettingsActivity.class));
    }

    public void startDimmerService(boolean switchDim) {
        Intent startServiceIntent = new Intent();
        startServiceIntent.setComponent(DimmerService.COMPONENT);
        if (switchDim)
            startServiceIntent.setAction(DimmerService.SWITCHDIM);
        startService(startServiceIntent);
    }
}
