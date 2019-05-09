package me.abhishekraj.geofire;

import android.app.AlarmManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by Abhishek Raj on 5/9/2019.
 */

public class GpsTrackerService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.d("my_tag", "GpsTrackerService onCreate called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//      GpsTrackerAlarm.cancelAlarm(this, (AlarmManager) getSystemService(ALARM_SERVICE));
        GpsTrackerAlarm.scheduleExactAlarm(GpsTrackerService.this, (AlarmManager) getSystemService(ALARM_SERVICE));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}


