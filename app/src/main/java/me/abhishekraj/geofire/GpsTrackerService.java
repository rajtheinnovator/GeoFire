package me.abhishekraj.geofire;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static android.os.Build.VERSION_CODES.O;

/**
 * Created by Abhishek Raj on 5/9/2019.
 */

public class GpsTrackerService extends Service implements GeoQueryEventListener {

    DatabaseReference databaseReference;
    GeoFire geoFire;
    LocationManager locationManager;
    Location currentLocation, locationUnderTest;
    GeoQueryEventListener geoQueryEventListener;
    Context context;
    private LocationListener locationListener;

    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        getCurrentLocationOfUser();
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        context = this;
        locationUnderTest = new Location("Original Location");
        //TODO: Set location value here, around which you want to monitor enter/exit
        locationUnderTest.setLatitude(28.646022);
        locationUnderTest.setLongitude(77.355979);
        databaseReference = FirebaseDatabase.getInstance().getReference("path_geofire");
        geoFire = new GeoFire(databaseReference);
        geoQueryEventListener = this;
        //handle location callback
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                currentLocation = location;
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {

            }
        };
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(locationUnderTest.getLatitude(),
                locationUnderTest.getLongitude()), 0.05);
        geoQuery.addGeoQueryEventListener(geoQueryEventListener);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        GpsTrackerAlarm.scheduleExactAlarm(GpsTrackerService.this, (AlarmManager) getSystemService(ALARM_SERVICE));
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocationOfUser() {
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            doGeoFireOperation(location);
                        }
                    }
                });

    }

    private void doGeoFireOperation(Location location) {
        geoFire.setLocation("firebase-hq",
                new GeoLocation(location.getLatitude(), location.getLongitude()), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (error != null) {
                            Log.d("my_tag", "There was an error saving the location to GeoFire: " + error);
                        } else {
                            Log.d("my_tag", "Location saved on server successfully!");
                        }
                    }
                });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void showNotification(String key, String enteredOrExited) {

        if (Build.VERSION.SDK_INT > O) {
            createChannel();
        }

        Intent notificationIntent = new Intent(this, JavaSampleForGeoFire.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "geo_fire_event")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("GeoFireEvent")
                .setContentText(enteredOrExited)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // notificationId is a unique int for each notification that you must define
        notificationManager.notify(101, builder.build());
    }

    @TargetApi(26)
    private void createChannel() {
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
        String name = "GeoFireEvent";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel mChannel = new NotificationChannel("geo_fire_event", name, importance);
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.BLUE);
        notificationManager.createNotificationChannel(mChannel);
    }

    @Override
    public void onKeyEntered(String key, GeoLocation geoLocation) {
        showNotification(key, "Entered");
        Log.d("my_tag", String.format("Entered: Key %s entered the search area at [%f,%f]", key, geoLocation.latitude, geoLocation.longitude));
    }

    @Override
    public void onKeyExited(String key) {
        Log.d("my_tag", String.format("Exited: Key %s is no longer in the search area", key));
        showNotification(key, "Exited");
    }

    @Override
    public void onKeyMoved(String key, GeoLocation geoLocation) {
        showNotification(key, "Moved");
        Log.d("my_tag", String.format("Moved: Key %s moved within the search area to [%f,%f]", key, geoLocation.latitude, geoLocation.longitude));
    }

    @Override
    public void onGeoQueryReady() {
        showNotification("key", "Moved");
        Log.d("my_tag", "Ready: All initial data has been loaded and events have been fired!");
    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        Log.d("my_tag", "Error: There was an error with this query: " + error);
    }
}


