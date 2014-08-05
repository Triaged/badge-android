package com.triaged.badge.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.triaged.badge.data.CompanySQLiteHelper;
import com.triaged.badge.data.Contact;
import com.triaged.badge.data.OfficeLocation;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Provider;
import java.util.Date;

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
    public static final String LOCATION_ALARM_ACTION = "com.triaged.badge.app.LOCATION_WAKEUP";

    protected static final float OFFICE_DISTANCE_THRESHOLD_M = 150f;

    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;

    private PowerManager powerMgr;
    private PowerManager.WakeLock wakeLock;
    protected Handler handler;

    volatile boolean logLocations;
    OutputStream locationLog;

    public static class WakeupReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            context.startService( new Intent( context, LocationTrackingService.class ) );
        }
    }

    public static void scheduleAlarm( Context context ) {
        Intent alarmIntent = new Intent( LOCATION_ALARM_ACTION );
        PendingIntent scheduledAlarmIntent = PendingIntent.getBroadcast( context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT );
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService( Context.ALARM_SERVICE );
        alarmMgr.setRepeating( AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 60000, scheduledAlarmIntent );

    }

    public static void clearAlarm( DataProviderService.LocalBinding dataProviderServiceBinding, Context context ) {
        Intent alarmIntent = new Intent( LOCATION_ALARM_ACTION );
        PendingIntent scheduledAlarmIntent = PendingIntent.getBroadcast( context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT );
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService( Context.ALARM_SERVICE );
        alarmMgr.cancel( scheduledAlarmIntent );

        // In many cases (phone shutting down), toggling the share location preference,
        // the user is still logged in and may be associated with an office.
        // We try to clear that so that they don't end up "stuck" in that office forever.
        if( dataProviderServiceBinding != null ) {
            Contact user = dataProviderServiceBinding.getLoggedInUser();
            if( user != null &&  user.currentOfficeLocationId > 0 )
                dataProviderServiceBinding.checkOutOfOffice( user.currentOfficeLocationId );
        }

    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();

        mLocationRequest.setInterval(2000);

        // Consume limited power... cell and wifi
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        mLocationRequest.setFastestInterval(1000);

        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);

        powerMgr = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = powerMgr.newWakeLock( PowerManager.PARTIAL_WAKE_LOCK, LocationTrackingService.class.getName() );

        handler = new Handler();
        logLocations = false;
        try {
            locationLog = new FileOutputStream("/sdcard/locations.txt", true );
            logLocations = true;
        }
        catch( IOException e ) {
            Log.w( LOG_TAG, "Couldn't open location debug log", e );
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        wakeLock.acquire();
        mLocationClient.connect();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if( wakeLock != null && wakeLock.isHeld() ) {
            wakeLock.release();
        }
        if( logLocations ) {
            logLocations = false;
            try {
                locationLog.close();
            }
            catch( IOException e ) {
                Log.w( LOG_TAG, "Exception closing location log file.", e );
            }
        }
        super.onDestroy();
        if( mLocationClient.isConnected() ) {
            mLocationClient.removeLocationUpdates(this);
        }
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
        // Log.d( LOG_TAG, "Location service: " + location.getLatitude() + ", " + location.getLongitude() + " with accuracy " + location.getAccuracy() );

        if( logLocations ) {
            String line = new Date().toString() + ": New location: " + location.getLatitude() + "," + location.getLongitude() + " accuracy " + location.getAccuracy() + "\n";
            try {
                locationLog.write(line.getBytes());
                locationLog.flush();
            }
            catch( IOException e ) {
                // Oh stuff it.
            }
        }

        if( location.getAccuracy() > 300f ) {
            String msg = "Accuracy of " + location.getAccuracy() + " isn't going to cut it.";
            // TODO delete me
            //Toast.makeText( this, msg, Toast.LENGTH_LONG ).show();
            // Log.d( LOG_TAG, msg );
            return;
        }

        // Check against each office.
        final DataProviderService.LocalBinding dataProviderServiceBinding = ((BadgeApplication)getApplication()).dataProviderServiceBinding;
        if( dataProviderServiceBinding != null && dataProviderServiceBinding.isInitialized() && dataProviderServiceBinding.getLoggedInUser() != null ) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    Cursor officeLocations = dataProviderServiceBinding.getOfficeLocationsCursor();
                    Location officeLocation = new Location(LocationManager.NETWORK_PROVIDER );
                    while( officeLocations.moveToNext() ) {
                        String latStr = Contact.getStringSafelyFromCursor( officeLocations, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_LAT );
                        String lngStr = Contact.getStringSafelyFromCursor( officeLocations, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_LNG );
                        int officeId = Contact.getIntSafelyFromCursor( officeLocations, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_ID );
                        String officeName = Contact.getStringSafelyFromCursor( officeLocations, CompanySQLiteHelper.COLUMN_OFFICE_LOCATION_NAME );
                        if( latStr != null && !"".equals( latStr ) ) {
                            officeLocation.setLatitude( Float.parseFloat( latStr ) );
                            officeLocation.setLongitude( Float.parseFloat( lngStr ) );
                            float distance = officeLocation.distanceTo( location );
                            if( logLocations ) {
                                String line = "Distance to " + officeName + " is " + distance + " meters\n";
                                try {
                                    locationLog.write(line.getBytes());
                                    locationLog.flush();
                                }
                                catch( IOException e ) {
                                    // Oh stuff it.
                                }
                            }
                            if( distance < OFFICE_DISTANCE_THRESHOLD_M ) {
                                // CHECK IN!
                                dataProviderServiceBinding.checkInToOffice( officeId );
                            }
                            else {
                                dataProviderServiceBinding.checkOutOfOffice( officeId );
                            }
                        }
                    }
                    // final String msg = "Phone location checked against office locations! " + location.getLatitude() + ", " + location.getLongitude();
//                    handler.post( new Runnable() {
//                        @Override
//                        public void run() {
//                            Toast.makeText( LocationTrackingService.this, msg, Toast.LENGTH_LONG ).show();
//                        }
//                    });
                    // Log.i( LOG_TAG, msg );
                    stopSelf();
                    return null;
                }
            }.execute();
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
            Log.e(LOG_TAG, "GMS unavailable");
            return false;
        }
    }

}
