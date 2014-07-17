package com.triaged.badge.app;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.triaged.badge.data.CompanySQLiteHelper;
import com.triaged.badge.data.Contact;
import com.triaged.badge.data.OfficeLocation;

import java.security.Provider;

/**
 * Tracks location in the background if hte user has enabled.
 * Should update very infrequently and never use GPS, only low
 * power locations
 *
 * @author Created by jc on 7/17/14.
 */
public class LocationTrackingService extends Service implements LocationListener,
                                                                GooglePlayServicesClient.ConnectionCallbacks,
                                                                GooglePlayServicesClient.OnConnectionFailedListener
{


    protected static final String LOG_TAG = LocationTrackingService.class.getName();

    public static final String TRACK_LOCATION_PREFS_KEY = "trackLocation";

    protected static final float OFFICE_DISTANCE_THRESHOLD_M = 100f;

    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();

        // Two minutes should be fairly real time.
        mLocationRequest.setInterval(120000);

        // Consume limited power... cell and wifi
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        // Set the interval ceiling to two minutes
        mLocationRequest.setFastestInterval(60000);

        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mLocationClient.connect();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mLocationClient.removeLocationUpdates(this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        if( servicesConnected() ) {
            mLocationClient.requestLocationUpdates(mLocationRequest, this);
        }
    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onLocationChanged( final Location location) {
        // Check against each office.
        final DataProviderService.LocalBinding dataProviderServiceBinding = ((BadgeApplication)getApplication()).dataProviderServiceBinding;
        if( dataProviderServiceBinding != null && dataProviderServiceBinding.isInitialized() ) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    Cursor officeLocations = dataProviderServiceBinding.getOfficeLocationsCursor();
                    Location officeLocation = new Location(LocationManager.NETWORK_PROVIDER );
                    while( officeLocations.moveToNext() ) {
                        String latStr = Contact.getStringSafelyFromCursor( officeLocations, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_LAT );
                        String lngStr = Contact.getStringSafelyFromCursor( officeLocations, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_LAT );
                        int officeId = Contact.getIntSafelyFromCursor( officeLocations, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_ID );
                        if( latStr != null && !"".equals( latStr ) ) {
                            officeLocation.setLatitude( Float.parseFloat( latStr ) );
                            officeLocation.setLongitude( Float.parseFloat( lngStr ) );
                            float distance = officeLocation.distanceTo( location );
                            if( distance < OFFICE_DISTANCE_THRESHOLD_M ) {
                                // CHECK IN!
                                dataProviderServiceBinding.checkInToOffice( officeId );
                            }
                            else {
                                dataProviderServiceBinding.checkOutOfOffice( officeId );
                            }
                        }
                    }
                    return null;
                }
            };
        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // Continue
            return true;
        } else {
            // Google Play services was not available for some reason
            // Display an error dialog
            Log.d(LOG_TAG, "GMS unavailable");
            return false;
        }
    }

}
