package giraffine.dimmer;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.util.List;

//import android.support.v4.app.NotificationCompat;

public class DimmerService extends Service implements LightSensor.EventCallback {

    public static boolean DebugMode = false;
    public static String PACKAGENAME = "giraffine.dimmer";
    public static String ACTIONNOTIFICATION = "giraffine.dimmer.Dimmer.action.notification";
    public static ComponentName COMPONENT = new ComponentName(PACKAGENAME, PACKAGENAME + ".DimmerService");
    public static String ADJUSTLEVEL = "adjustLevel";
    public static String FINISHLEVEL = "finishLevel";
    public static String RESETLEVEL = "resetLevel";
    public static String PAUSEFUNCTION = "pauseFunction";
    public static String LAYOUTCHANGE = "layoutChange";
    public static String STATUSBARCHANGE = "statusbarChange";
    public static String STEPLEVELUP = "stepLevelUp";
    public static String STEPLEVELDOWN = "stepLevelDown";
    public static String SWITCHAUTOMODE = "switchAutoMode";
    public static String SWITCHDIM = "switchDim";
    public static String SENSITIVECHANGE = "sensitiveChange";
    public static String ALARMMODE = "alarmMode";
    public static String ALARMCHANGE = "alarmChange";
    public static String COLORCHANGE = "colorChange";
    public static String BOOT = "boot";
    public static String CHANNEL_ID = "dimmer_ch_01";
    public static final int MSG_RESET_LEVEL = 0;
    public static final int MSG_RESET_LEVEL_RESTORE = 1;
    public static final int MSG_RESET_LEVEL_RESTORE_KEEP_NOTIFY = 2;
    public static final int MSG_RESET_ACTING = 3;
    public static final int MSG_ENTER_DIMM = 4;
    public static final int DEFAULTLEVEL = 1000;
    public static int lastLevel = DEFAULTLEVEL;

    private boolean mActing = false;
    private Notification mNotification;
    private RemoteViews mNotiRemoteView = null;
    private Mask mMask = null;
    private boolean mInDimMode = false;
    private LightSensor mLightSensor = null;
    private AlarmUtil mAlarmUtil = null;
    private boolean mKeepSticky = false;
    private boolean mIsPaused = false;
    private boolean mLayoutChanged = true;
    private boolean mUpdateNotifyInfo = true;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public void postNotification(int levelHint, boolean dim) {
        if (mNotiRemoteView == null)
            initNotification();

        if (mLayoutChanged) {
            relayoutNotification();
            mLayoutChanged = false;
        }

        if (mUpdateNotifyInfo) {
            mNotiRemoteView.setTextViewText(R.id.noti_text, levelHint + "");
            for (int i = 0; i < 4; i++) {
                if (dim)
                    mNotiRemoteView.setImageViewResource(SettingNotifyLayout.getNotifyButtonID(i, 2), R.drawable.ic_pause);
                else
                    mNotiRemoteView.setImageViewResource(SettingNotifyLayout.getNotifyButtonID(i, 2), R.drawable.ic_start);
            }
        }

        if (mNotification == null) {
            Intent intent = new Intent(ACTIONNOTIFICATION);
            intent.setClassName(PACKAGENAME, PACKAGENAME + ".SettingsActivity");
            PendingIntent pi = PendingIntent.getActivity(this, 0, intent, 0);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                        "Dimmer Notification Channel",
                        NotificationManager.IMPORTANCE_DEFAULT);

                ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            }

            mNotification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContent(mNotiRemoteView)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentIntent(pi)
                    .setPriority(Prefs.getNotifyPriority() ? -2 : 0)    // adjust priority to control show icon in status bar
                    .build();

            stopForeground(true);    // remove notification first to ensure update
        }
        if (DebugMode)
            mNotification.tickerText = mLightSensor.getCurrentLux() + "";

        mNotification.number = 1;
        startForeground(999, mNotification);
    }

    public void removeNotification() {
        if (mNotification != null)
            mNotification.number = 0;
        stopForeground(true);
    }

    public void initNotification() {
        Intent stepUpIntent = new Intent(this, DimmerService.class);
        stepUpIntent.setAction(STEPLEVELUP);
        PendingIntent piStepUp = PendingIntent.getService(this, 0, stepUpIntent, 0);

        Intent stepDownIntent = new Intent(this, DimmerService.class);
        stepDownIntent.setAction(STEPLEVELDOWN);
        PendingIntent piStepDown = PendingIntent.getService(this, 0, stepDownIntent, 0);

        Intent pauseIntent = new Intent(this, DimmerService.class);
        pauseIntent.setAction(PAUSEFUNCTION);
        PendingIntent piPause = PendingIntent.getService(this, 0, pauseIntent, 0);

        Intent resetIntent = new Intent(this, DimmerService.class);
        resetIntent.setAction(RESETLEVEL);
        PendingIntent piReset = PendingIntent.getService(this, 0, resetIntent, 0);

        // Customization for notification button:
        // Design A: Dynamically add Remoteivew.
        // Design B: Dynamically set visible/invisible Button.
        // Problem A: Fast click button will trigger notification content intent.
        // Problem B: Need list all permutations and combinations (4x4).
        mNotiRemoteView = new RemoteViews(PACKAGENAME, R.layout.notification);

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                PendingIntent pi = null;
                switch (j) {
                    case 0:
                        pi = piStepUp;
                        break;
                    case 1:
                        pi = piStepDown;
                        break;
                    case 2:
                        pi = piPause;
                        break;
                    case 3:
                        pi = piReset;
                        break;
                }
                mNotiRemoteView.setOnClickPendingIntent(SettingNotifyLayout.getNotifyButtonID(i, j), pi);
            }
        }
    }

    public void relayoutNotification() {
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++)
                mNotiRemoteView.setViewVisibility(SettingNotifyLayout.getNotifyButtonID(i, j), View.GONE);
        String order = Prefs.getNotifyLayout();
        Log.e(Dimmer.TAG, "relayoutNotification: order=" + order);
        if (order.startsWith("-"))
            mNotiRemoteView.setViewVisibility(R.id.noti_icon, View.GONE);
        else
            mNotiRemoteView.setViewVisibility(R.id.noti_icon, View.VISIBLE);
        order = order.substring(1);
        for (int i = 0; i < 4; i++) {
            if (order.charAt(i + 4) == '1')
                mNotiRemoteView.setViewVisibility(SettingNotifyLayout.getNotifyButtonID(i, Integer.valueOf(String.valueOf(order.charAt(i)))), View.VISIBLE);
        }
    }

    @Override
    public void onCreate() {
        BrightnessUtil.init(this);
        Prefs.init(this);
//		lastLevel = BrightnessUtil.getPreferLevel();
        int currentBrightness = BrightnessUtil.getBrightness();
        lastLevel = (int) (((float) currentBrightness) / 255 * 500 + 500);
        sendBroadcast(new Intent(Dimmer.REFRESH_INDEX));


        mMask = new Mask(this);
        mLightSensor = new LightSensor(this, this);
        mAlarmUtil = new AlarmUtil(this);

        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS), true, new ContentObserver(null) {
            public void onChange(boolean selfChange) {
                if (mActing == false)
                    mHandler.sendEmptyMessage(MSG_RESET_LEVEL);
                return;
            }
        });
        resolver.registerContentObserver(Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), true, new ContentObserver(null) {
            public void onChange(boolean selfChange) {
                if (mActing == false)
                    mHandler.sendEmptyMessage(MSG_RESET_LEVEL);
                return;
            }
        });
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                if (Prefs.isAutoMode())
                    mLightSensor.monitor(true);
            }
        }, new IntentFilter(Intent.ACTION_SCREEN_ON));
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent arg1) {
                mLightSensor.monitor(false);
            }
        }, new IntentFilter(Intent.ACTION_SCREEN_OFF));

        if (Prefs.isAutoMode()) {
            mLightSensor.monitor(true);
            mKeepSticky = true;
        }

        mAlarmUtil.update();
    }

    public String getForegroundActivity() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> Info = am.getRunningTasks(1);
        ComponentName topActivity = Info.get(0).topActivity;
        return topActivity.getPackageName();
    }

    @Override
    public void onDestroy() {
        Log.e(Dimmer.TAG, "onDestroy()");
        // need restart ASAP: 60 secs
        if (mKeepSticky) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setComponent(COMPONENT);
            PendingIntent pi = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 60000, pi);
        }
    }

    @Override
    public void onLightChanged(int lux) {
        if (DebugMode) postNotification(lastLevel / 10, false);
        if (SettingsActivity.showSettings)
            sendBroadcast(new Intent(SettingsFragment.REFRESH_LUX).putExtra("lux", lux));
    }

    @Override
    public void onEnterDarkLight() {
//		Log.e(Dimmer.TAG, "onDarkLight() mIsAutoMode=" + Prefs.isAutoMode() + ", mInDimmMode=" + mInDimmMode);
        if (!Prefs.isAutoMode() || getDimMode()
                || (Prefs.getApList() != null && Prefs.getApList().size() != 0 && Prefs.getApList().contains(getForegroundActivity()))
                || mIsPaused) return;
        mLightSensor.setFreezeLux();
        mHandler.sendEmptyMessage(MSG_ENTER_DIMM);
    }

    @Override
    public void onLeaveDarkLight() {
//		Log.e(Dimmer.TAG, "onOverDarkLight() mIsAutoMode=" + Prefs.isAutoMode() + ", mInDimmMode=" + mInDimmMode);
        if (!Prefs.isAutoMode() || !getDimMode()) return;
        mHandler.sendEmptyMessage(MSG_RESET_LEVEL_RESTORE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(ADJUSTLEVEL)) {
                mHandler.removeMessages(MSG_RESET_ACTING);
                mActing = true;
                adjustLevel(intent.getIntExtra(ADJUSTLEVEL, DEFAULTLEVEL), false, false);
            } else if (intent.getAction().equals(FINISHLEVEL)) {
                int i = intent.getIntExtra(FINISHLEVEL, DEFAULTLEVEL);
                adjustLevel(intent.getIntExtra(FINISHLEVEL, DEFAULTLEVEL), true, true);
                lastLevel = i;

                if (lastLevel < 500) {
                    mLightSensor.setFreezeLux();
                    Prefs.setFavorMaskValue(lastLevel);
                }

//				Log.e(Dimmer.TAG, "" + LuxUtil.dumpLuxLevel());
            } else if (intent.getAction().equals(PAUSEFUNCTION)) {
                if (getDimMode())
                    mHandler.sendEmptyMessage(MSG_RESET_LEVEL_RESTORE_KEEP_NOTIFY);
                else {
                    mLightSensor.setFreezeLux();
                    mHandler.sendEmptyMessage(MSG_ENTER_DIMM);
                }
            } else if (intent.getAction().equals(RESETLEVEL)) {
                mHandler.sendEmptyMessage(MSG_RESET_LEVEL_RESTORE);
            } else if (intent.getAction().equals(LAYOUTCHANGE)) {
                mLayoutChanged = true;
                if (mNotification != null && mNotification.number == 1) {
                    mUpdateNotifyInfo = false;
                    postNotification(0, false);
                    mUpdateNotifyInfo = true;
                }
            } else if (intent.getAction().equals(STATUSBARCHANGE)) {
                if (mNotification != null) {
                    boolean needPost = mNotification.number == 1;
                    mNotification = null;
                    if (needPost) {
                        mUpdateNotifyInfo = false;
                        postNotification(0, false);
                        mUpdateNotifyInfo = true;
                    }
                }
            } else if (intent.getAction().equals(STEPLEVELUP)) {
                stepLevel(false);
            } else if (intent.getAction().equals(STEPLEVELDOWN)) {
                stepLevel(true);
            } else if (intent.getAction().equals(SWITCHAUTOMODE)) {
                boolean on = intent.getBooleanExtra(SWITCHAUTOMODE, false);
                mLightSensor.monitor(on);
                mKeepSticky = on;
            } else if (intent.getAction().equals(SWITCHDIM)) {
                if (getDimMode())
                    mHandler.sendEmptyMessage(MSG_RESET_LEVEL_RESTORE);
                else {
                    mLightSensor.setFreezeLux();
                    mHandler.sendEmptyMessage(MSG_ENTER_DIMM);
                }
            } else if (intent.getAction().equals(SENSITIVECHANGE)) {
                mLightSensor.updateSensitive();
            } else if (intent.getAction().equals(ALARMCHANGE)) {
                mAlarmUtil.update();
            } else if (intent.getAction().equals(ALARMMODE)) {
                if (!mAlarmUtil.nowToDim())
                    mHandler.sendEmptyMessage(MSG_RESET_LEVEL_RESTORE);
                else {
                    mLightSensor.setFreezeLux();
                    mHandler.sendEmptyMessage(MSG_ENTER_DIMM);
                }
                mAlarmUtil.update();
            } else if (intent.getAction().equals(COLORCHANGE)) {
                if (getDimMode()) {
                    if (intent.hasExtra(COLORCHANGE + "ON"))
                        mMask.adjustColor(intent.getBooleanExtra(COLORCHANGE + "ON", false), Prefs.getColor());
                    else if (Prefs.getColorMode())
                        mMask.adjustColor(true, intent.getIntExtra(COLORCHANGE, 0xFF000000));
                }
            } else if (intent.getAction().equals(BOOT)) {
                if (mAlarmUtil.bootToDim()) {
                    mLightSensor.setFreezeLux();
                    mHandler.sendEmptyMessage(MSG_ENTER_DIMM);
                } else if (!mKeepSticky)
                    trySuicide();
            }
        }
        // Only tf700t: If killed, next launch will NOT result in Activity/Service OnCreate.
        if (android.os.Build.DEVICE.equalsIgnoreCase("tf700t")) return START_STICKY;
//		Log.e(Dimmer.TAG, "onStartCommand(): " + lastLevel);
//		return Prefs.isAutoMode() ? START_STICKY : (lastLevel>500 ? START_NOT_STICKY : START_STICKY);
        return mKeepSticky ? START_STICKY : (lastLevel > 500 ? START_NOT_STICKY : START_STICKY);
//		return START_STICKY;	//sticky for continuous alive
    }

    private void adjustLevel(int i, boolean setBrightness, boolean postNotify) {
        if (postNotify)
            mIsPaused = false;

        if (i > 500) {
            if (i > 10 * Prefs.getNotify(Prefs.PREF_NOTIFY_UPPER))
                removeNotification();
            else {
                if (postNotify)
                    postNotification(i / 10, false);
            }
            setDimMode(false);
        } else {
            if (postNotify)
                postNotification(i / 10, true);
            setDimMode(true);
        }
        if (setBrightness)
            triggerActingSession();
        mMask.adjustLevel(i, setBrightness);
        mMask.adjustColor((Prefs.getColorMode() && getDimMode()), Prefs.getColor());
    }

    public void resetLevel(boolean restoreBrighnessState, boolean removeNotification) {
        Log.e(Dimmer.TAG, "resetLevel() lastLevel: " + lastLevel);
//        Log.e(Dimmer.TAG, "resetLevel() lastLevel: " + lastLevel +":"+ restoreBrighnessState +":"+ removeNotification);


        mIsPaused = !removeNotification;
        if (restoreBrighnessState) {
            triggerActingSession();
            BrightnessUtil.restoreState();
        }

        int currentBrightness = BrightnessUtil.getBrightness();
        lastLevel = (int) (((float) currentBrightness) / 255 * 500 + 500);
        mMask.removeMask();
        mMask.adjustColor(false, 0);

        boolean needSuicide = true;
        if (removeNotification)
            removeNotification();
        else {
            postNotification(lastLevel / 10, false);
            needSuicide = false;
        }
        setDimMode(false);
        sendBroadcast(new Intent(Dimmer.REFRESH_INDEX));

        Dimmer.collectState = false;
        if (!mKeepSticky && needSuicide)
            trySuicide();
    }

    public void stepLevel(boolean darker) {
        Log.e(Dimmer.TAG, "stepLevel() lastLevel: " + lastLevel + ", darker=" + darker);

        int step = 10 * Prefs.getNotify(Prefs.PREF_NOTIFY_STEP);
        int lowerbound = 10 * Prefs.getNotify(Prefs.PREF_NOTIFY_LOWER);
        int upperbound = 10 * Prefs.getNotify(Prefs.PREF_NOTIFY_UPPER);
        if (darker)
            lastLevel -= step;
        else
            lastLevel += step;
        if (lastLevel > upperbound) lastLevel = upperbound;
        if (lastLevel < lowerbound) lastLevel = lowerbound;

        if (lastLevel >= 500)    // if > 50, need call Mask.maskBrightness to set screenBrightness
            adjustLevel(lastLevel, false, false);
        adjustLevel(lastLevel, true, true);

        if (lastLevel < 500) {
            mLightSensor.setFreezeLux();
            Prefs.setFavorMaskValue(lastLevel);
        }
        sendBroadcast(new Intent(Dimmer.REFRESH_INDEX));
    }

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RESET_LEVEL:
                    resetLevel(false, true);
                    break;
                case MSG_RESET_LEVEL_RESTORE:
                    resetLevel(true, true);
                    break;
                case MSG_RESET_LEVEL_RESTORE_KEEP_NOTIFY:
                    resetLevel(true, false);
                    break;
                case MSG_RESET_ACTING:
                    mActing = false;
                    break;
                case MSG_ENTER_DIMM:
                    mMask.removeMask();
                    Dimmer.collectState = true;
                    BrightnessUtil.collectState();
                    int favorvalue = Prefs.getFavorMaskValue();
                    adjustLevel(favorvalue, true, true);
                    lastLevel = favorvalue;
                    sendBroadcast(new Intent(Dimmer.REFRESH_INDEX));
                    showHint();
                    break;
            }
        }
    };

    public void triggerActingSession() {
        mActing = true;
        mHandler.removeMessages(MSG_RESET_ACTING);
        mHandler.sendEmptyMessageDelayed(MSG_RESET_ACTING, 1000);
    }

    private void setDimMode(boolean dim) {
        mInDimMode = dim;
        mLightSensor.setDimState(dim);
        DimmerWidget.updateDim(this, dim);
    }

    private boolean getDimMode() {
        return mInDimMode;
    }

    private void trySuicide() {
        Log.e(Dimmer.TAG, "trySuicide()");
        // Only tf700t: If killed, next launch will NOT result in Activity/Service OnCreate.
        if (android.os.Build.DEVICE.equalsIgnoreCase("tf700t")) return;
        if (SettingsActivity.showSettings || Dimmer.showMainApp)
            return;
        stopSelf();
        Process.killProcess(Process.myPid());
    }

    private void showHint() {
        try {
            String version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            if (version == null || !version.equalsIgnoreCase(Prefs.getAbout()))
                Toast.makeText(this, R.string.pref_widget_hint, Toast.LENGTH_LONG).show();
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
