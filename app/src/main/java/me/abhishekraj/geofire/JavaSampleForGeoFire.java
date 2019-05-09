package me.abhishekraj.geofire;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static android.os.Build.VERSION_CODES.O;

public class JavaSampleForGeoFire extends AppCompatActivity implements GeoQueryEventListener {

    DatabaseReference databaseReference;
    GeoFire geoFire;
    TextView currentLocationTextView;
    TextView distanceFromTextLocationTextView;
    public static final int LOCATION_PERMISSION_CODE = 2;
    private static final int LOCATION_SETTINGS_REQUEST_CODE = 1133;
    GeoQueryEventListener geoQueryEventListener;
    TextView enteredOrExitedTextView;
    TextView testLocationValueTextView;
    LocationManager locationManager;
    Location currentLocation, locationUnderTest;
    //handle gps
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationListener mLocationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_java_sample_for_geo_fire);

        locationUnderTest = new Location("Original Location");
        //TODO: Set location value here, around which you want to monitor enter/exit
        locationUnderTest.setLatitude(12.933179);
        locationUnderTest.setLongitude(77.612152);

        currentLocationTextView = findViewById(R.id.text_view_current_location_value);
        distanceFromTextLocationTextView = findViewById(R.id.text_view_distance_from_test_location_value);
        enteredOrExitedTextView = findViewById(R.id.text_view_entered_or_exited_value);
        testLocationValueTextView = findViewById(R.id.text_view_test_location_value);
        testLocationValueTextView.setText("Location under test: " + locationUnderTest.getLatitude() + ", " + locationUnderTest.getLongitude());

        databaseReference = FirebaseDatabase.getInstance().getReference("path_geofire");
        geoFire = new GeoFire(databaseReference);
        geoQueryEventListener = this;
        //handle location callback
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                currentLocationTextView.setText("onLocationChanged, location is: " + location.getLatitude() + ", " + location.getLongitude());
                currentLocation = location;
                //doGeoFireOperation(location);
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
        permissionHandle();


    }

    private void permissionHandle() {
        int DeviceSdkVersion = Build.VERSION.SDK_INT;
        if (DeviceSdkVersion > Build.VERSION_CODES.LOLLIPOP_MR1) {
            if (checkIfLocationPermissionGranted()) {
                getCurrentLocationOfUser();
                sendBroadcastForLocationTracking();
            } else {
                getUsersLocationPermission();
            }
        }
    }

    private void getUsersLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    sendBroadcastForLocationTracking();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    private void sendBroadcastForLocationTracking() {
        Intent broadcastIntent = new Intent(this, GpsTrackerAlarm.class);
        sendBroadcast(broadcastIntent);
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocationOfUser() {
        if (checkIfLocationPermissionGranted()) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                currentLocationTextView.setText("onSuccess, location is: " + location.getLatitude() + ", " + location.getLongitude());
                                currentLocation = location;
                                doGeoFireOperation(location);
                            }
                        }
                    });
        }
    }

    private boolean checkIfLocationPermissionGranted() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return result == PackageManager.PERMISSION_GRANTED;
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
    public void onKeyEntered(String key, GeoLocation geoLocation) {
        enteredOrExitedTextView.setText("Entered");
        showNotification(key, "Entered");
        Toast.makeText(JavaSampleForGeoFire.this, "entered: " + key, Toast.LENGTH_SHORT).show();

        if (currentLocation != null) {
            distanceFromTextLocationTextView.setText("value is: " + (int) locationUnderTest.distanceTo(currentLocation));
        } else {
            distanceFromTextLocationTextView.setText("Entered but current location Null");
        }
        Log.d("my_tag", String.format("Entered: Key %s entered the search area at [%f,%f]", key, geoLocation.latitude, geoLocation.longitude));
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
    public void onKeyExited(String key) {
        Toast.makeText(JavaSampleForGeoFire.this, "Exited: " + key, Toast.LENGTH_SHORT).show();
        Log.d("my_tag", String.format("Exited: Key %s is no longer in the search area", key));
        enteredOrExitedTextView.setText("Exited");
        showNotification(key, "Exited");
        distanceFromTextLocationTextView.setText("value is: " + (int) locationUnderTest.distanceTo(currentLocation));
    }

    @Override
    public void onKeyMoved(String key, GeoLocation geoLocation) {
        enteredOrExitedTextView.setText("Moved");
        showNotification(key, "Moved");
        Toast.makeText(JavaSampleForGeoFire.this, "Moved: " + key, Toast.LENGTH_SHORT).show();
        Log.d("my_tag", String.format("Moved: Key %s moved within the search area to [%f,%f]", key, geoLocation.latitude, geoLocation.longitude));
    }

    @Override
    public void onGeoQueryReady() {
        enteredOrExitedTextView.setText("READY");
        Log.d("my_tag", "Ready: All initial data has been loaded and events have been fired!");
    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        enteredOrExitedTextView.setText("Error: " + error.getMessage());
        Log.d("my_tag", "Error: There was an error with this query: " + error);
    }
}
