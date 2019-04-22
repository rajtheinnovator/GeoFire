package me.abhishekraj.geofire;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.FirebaseError;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class JavaSampleForGeoFire extends AppCompatActivity implements GeoQueryEventListener {

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 100;
    DatabaseReference databaseReference;
    GeoFire geoFire;
    TextView currentLocationTextView;
    TextView distanceFromTextLocationTextView;
    GeoQueryEventListener geoQueryEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_java_sample_for_geo_fire);

        currentLocationTextView = findViewById(R.id.text_view_current_location_value);
        distanceFromTextLocationTextView = findViewById(R.id.text_view_distance_from_test_location_value);
        databaseReference = FirebaseDatabase.getInstance().getReference("path_geofire");
        geoFire = new GeoFire(databaseReference);
        geoQueryEventListener = this;
        requestLocationPermission();
    }

    private void requestLocationPermission() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            doGeoFireOperation();
        }
    }

    private void doGeoFireOperation() {
        geoFire.setLocation("firebase-hq",
                new GeoLocation(12.933192,77.612153), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (error != null) {
                            Log.d("my_tag", "There was an error saving the location to GeoFire: " + error);
                        } else {
                            Log.d("my_tag", "Location saved on server successfully!");
                        }
                    }
                });
        // creates a new query around [37.7832, -122.4056] with a radius of 0.01 kilometers
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(12.933192,77.612153), 0.01);
        geoQuery.addGeoQueryEventListener(geoQueryEventListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    doGeoFireOperation();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    @Override
    public void onKeyEntered(String key, GeoLocation geoLocation) {
        currentLocationTextView.setText("Entered: " + geoLocation.latitude + " " + geoLocation.longitude);
        Toast.makeText(JavaSampleForGeoFire.this, "entered: " + key, Toast.LENGTH_SHORT).show();
        Location location = new Location("Point A");
        location.setLatitude(geoLocation.latitude);
        location.setLongitude(geoLocation.longitude);
        Location originalLocation = new Location("Original Location");
        originalLocation.setLongitude(77.612153);
        originalLocation.setLatitude(12.933192);
        distanceFromTextLocationTextView.setText("" + (int) originalLocation.distanceTo(location));
        Log.d("my_tag", String.format("Key %s entered the search area at [%f,%f]", key, geoLocation.latitude, geoLocation.longitude));
    }

    @Override
    public void onKeyExited(String key) {
        currentLocationTextView.setText("Exited");
        Toast.makeText(JavaSampleForGeoFire.this, "Exited: " + key, Toast.LENGTH_SHORT).show();
        Log.d("my_tag", String.format("Key %s is no longer in the search area", key));
    }

    @Override
    public void onKeyMoved(String key, GeoLocation geoLocation) {
        currentLocationTextView.setText("Moved");
        Toast.makeText(JavaSampleForGeoFire.this, "Moved: " + key, Toast.LENGTH_SHORT).show();
        Log.d("my_tag", String.format("Key %s moved within the search area to [%f,%f]", key, geoLocation.latitude, geoLocation.longitude));
    }

    @Override
    public void onGeoQueryReady() {
        Log.d("my_tag", "All initial data has been loaded and events have been fired!");
    }

    @Override
    public void onGeoQueryError(DatabaseError error) {
        Log.d("my_tag", "There was an error with this query: " + error);
    }
}
