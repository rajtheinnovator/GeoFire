package me.abhishekraj.geofire;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

/**
 * Created by Abhishek Raj on 5/9/2019.
 */

public class GpsTrackerAlarm extends BroadcastReceiver {
    public static void scheduleExactAlarm(Context context, AlarmManager alarms) {
        Log.d("my_taggg", "scheduleExactAlarm called");
        Intent i = new Intent(context, GpsTrackerAlarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarms.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 10 * 1000 - SystemClock.elapsedRealtime() % 1000, pi);
        }
    }

    public static void cancelAlarm(Context context, AlarmManager alarms) {
        Log.d("my_taggg", "cancelAlarm called");
        Intent i = new Intent(context, GpsTrackerAlarm.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
        alarms.cancel(pi);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d("my_taggg", "onReceive called");
        scheduleExactAlarm(context, (AlarmManager) context.getSystemService(Context.ALARM_SERVICE));

        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GpsTracker:GpsTrackerWakelock");
        wl.acquire(1000);
        Handler handler = new Handler();
        Runnable periodicUpdate = new Runnable() {
            @Override
            public void run() {
                context.startService(new Intent(context, GpsTrackerService.class));
            }
        };

        handler.post(periodicUpdate);
        wl.release();
    }
}