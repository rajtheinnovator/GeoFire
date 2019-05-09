package me.abhishekraj.geofire;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

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

public class JavaSampleForGeoFire extends AppCompatActivity {

    DatabaseReference databaseReference;
    GeoFire geoFire;
    TextView currentLocationTextView;
    TextView distanceFromTextLocationTextView;
    public static final int LOCATION_PERMISSION_CODE = 2;
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
        locationUnderTest.setLatitude(28.646022);
        locationUnderTest.setLongitude(77.355979);

        currentLocationTextView = findViewById(R.id.text_view_current_location_value);
        distanceFromTextLocationTextView = findViewById(R.id.text_view_distance_from_test_location_value);
        enteredOrExitedTextView = findViewById(R.id.text_view_entered_or_exited_value);
        testLocationValueTextView = findViewById(R.id.text_view_test_location_value);
        testLocationValueTextView.setText("Location under test: " + locationUnderTest.getLatitude() + ", " + locationUnderTest.getLongitude());

        databaseReference = FirebaseDatabase.getInstance().getReference("path_geofire");
        geoFire = new GeoFire(databaseReference);
        geoQueryEventListener = new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation geoLocation) {
                enteredOrExitedTextView.setText("Entered");

                if (currentLocation != null) {
                    distanceFromTextLocationTextView.setText("value is: " + (int) locationUnderTest.distanceTo(currentLocation));
                } else {
                    distanceFromTextLocationTextView.setText("Entered but current location Null");
                }
                Log.d("my_tag_activity", String.format("Entered: Key %s entered the search area at [%f,%f]", key, geoLocation.latitude, geoLocation.longitude));
            }

            @Override
            public void onKeyExited(String key) {
                Log.d("my_tag_activity", String.format("Exited: Key %s is no longer in the search area", key));
                enteredOrExitedTextView.setText("Exited");
                distanceFromTextLocationTextView.setText("value is: " + (int) locationUnderTest.distanceTo(currentLocation));
            }

            @Override
            public void onKeyMoved(String key, GeoLocation geoLocation) {
                enteredOrExitedTextView.setText("Moved");
                if (currentLocation != null) {
                    distanceFromTextLocationTextView.setText("value is: " + (int) locationUnderTest.distanceTo(currentLocation));
                }
                Log.d("my_tag_activity", String.format("Moved: Key %s moved within the search area to [%f,%f]", key, geoLocation.latitude, geoLocation.longitude));
            }

            @Override
            public void onGeoQueryReady() {
                if (currentLocation != null) {
                    currentLocationTextView.setText(currentLocation.getLatitude() + ", " + currentLocation.getLongitude());
                }
                enteredOrExitedTextView.setText("READY");
                Log.d("my_tag_activity", "Ready: All initial data has been loaded and events have been fired!");
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
                enteredOrExitedTextView.setText("Error: " + error.getMessage());
                Log.d("my_tag_activity", "Error: There was an error with this query: " + error);
            }
        };
        /*
        handle location callback
        Acquire a reference to the system Location Manager
        */
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                currentLocationTextView.setText("current: " + location.getLatitude() + ", " + location.getLongitude());
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
        permissionHandle();

        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(locationUnderTest.getLatitude(),
                locationUnderTest.getLongitude()), 0.05);
        geoQuery.addGeoQueryEventListener(geoQueryEventListener);
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
                    /*
                    permission was granted, yay! Do the
                    location-related task you need to do.
                    */
                    sendBroadcastForLocationTracking();
                    getCurrentLocationOfUser();
                } else {
                    Toast.makeText(this, "App can't work properly without location permission", Toast.LENGTH_SHORT).show();
                    finish();
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
                            }
                        }
                    });
        }
    }

    private boolean checkIfLocationPermissionGranted() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return result == PackageManager.PERMISSION_GRANTED;
    }
}
