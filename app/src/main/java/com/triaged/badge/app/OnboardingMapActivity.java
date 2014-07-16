package com.triaged.badge.app;

import android.app.Dialog;
import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.triaged.badge.app.views.PlacesAutocompleteAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Allow user to add a new office location during onboarding flow.
 *
 * Created by Will on 7/14/14.
 */
public class OnboardingMapActivity extends BadgeActivity implements
        AdapterView.OnItemClickListener,
        LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private GoogleMap map = null;
    private AutoCompleteTextView autoCompleteTextView;
    private Button addButton;
    private static final String LOG_TAG = OnboardingMapActivity.class.getName();
    private ImageButton findMeButton = null;
    private String provider;
    private Location myLocation;
    protected DataProviderService.LocalBinding dataProviderServiceBinding;
    protected Address addressToAdd;

    // A request to connect to Location Services
    private LocationRequest mLocationRequest;

    // Stores the current instantiation of the location client in this object
    private LocationClient mLocationClient;

    private DataProviderService.AsyncSaveCallback addNewAddressCallback = new DataProviderService.AsyncSaveCallback() {
        @Override
        public void saveSuccess(int newId) {
            setResult( newId );
            finish();
        }

        @Override
        public void saveFailed(String reason) {
            Toast.makeText( OnboardingMapActivity.this, reason, Toast.LENGTH_SHORT ).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataProviderServiceBinding = ((BadgeApplication)getApplication()).dataProviderServiceBinding;
        addressToAdd = null;
        setContentView(R.layout.activity_onboarding_map);
        TextView backButton = (TextView) findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

        autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.autocomplete_location);
        autoCompleteTextView.setAdapter(new PlacesAutocompleteAdapter(this, android.R.layout.simple_list_item_1));
        autoCompleteTextView.setOnItemClickListener(this);

        addButton = (Button)findViewById( R.id.add_location_button );
        addButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( addressToAdd != null ) {
                    dataProviderServiceBinding.createNewOfficeLocationAsync(
                        addressToAdd.getAddressLine( 0 ).toString(),
                        addressToAdd.getLocality(),
                        addressToAdd.getAdminArea(),
                        addressToAdd.getPostalCode(),
                        addressToAdd.getCountryCode(),
                        addNewAddressCallback
                    );
                }
            }
        });
        findMeButton = (ImageButton) findViewById(R.id.find_me_button);
//
//        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
//        Criteria criteria = new Criteria();
//        provider = locationManager.getBestProvider(criteria, false);
//        myLocation = locationManager.getLastKnownLocation(provider);

        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();
        /*
         * Set the update interval
         */
        mLocationRequest.setInterval(5000);

        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(1000);

        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this, this, this);

        findMeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myLocation != null) {
                    LatLng latLng = new LatLng(myLocation.getLatitude(), myLocation.getLongitude());
                    (new GetAddressFromCoordsTask(OnboardingMapActivity.this)).execute(latLng);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        overridePendingTransition(0,0);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    @Override
    protected void onStop() {
        if (mLocationClient.isConnected()) {
            stopPeriodicUpdates();
        }
        mLocationClient.disconnect();
        super.onStop();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String str = (String) parent.getItemAtPosition(position);
        (new GetAddressTask(OnboardingMapActivity.this)).execute(str);
        InputMethodManager imm = (InputMethodManager)getSystemService(
                Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(autoCompleteTextView.getWindowToken(), 0);
    }

    @Override
    public void onLocationChanged(Location location) {
        myLocation = location;
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (servicesConnected()) {
            startPeriodicUpdates();
        }
    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    /** Given a search string, return a list of locations from Google Places Autocomplete API */
    public class GetAddressTask extends AsyncTask<String, Void, LatLng> {

        Context context;

        public GetAddressTask(Context context) {
            super();
            this.context = context;
        }

        @Override
        protected LatLng doInBackground(String... params) {
            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocationName(params[0], 1);
            } catch (IOException el) {
                el.printStackTrace();
                Log.e(LOG_TAG, "Error retrieving address");
            }
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                addressToAdd = address;
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                return latLng;

            }
            return null;
        }

        @Override
        protected void onPostExecute(LatLng latLng) {
            super.onPostExecute(latLng);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
            // ADD MARKER
        }
    }

    /** Given a set of coordinates, return the location from the Geocoder as a String */
    public class GetAddressFromCoordsTask extends AsyncTask<LatLng, Void, String> {
        Context context;
        LatLng latLng;

        public GetAddressFromCoordsTask(Context context) {
            super();
            this.context = context;
        }

        @Override
        protected String doInBackground(LatLng... params) {
            this.latLng = params[0];
            Geocoder myLocation = new Geocoder(context, Locale.getDefault());
            List<Address> addresses = null;
            try {
                addresses = myLocation.getFromLocation(latLng.latitude, latLng.longitude, 1);
                Address firstResult = addresses.get(0);
                addressToAdd = firstResult;
                StringBuilder strReturnedAddress = new StringBuilder("");

                for (int i = 0; i < firstResult.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(firstResult.getAddressLine(i)).append(", ");
                }
                return strReturnedAddress.toString();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Error retrieving address");
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));
            autoCompleteTextView.setText(s);
            super.onPostExecute(s);
        }
    }

    /**
     * In response to a request to start updates, send a request
     * to Location Services
     */
    private void startPeriodicUpdates() {

        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    /**
     * In response to a request to stop updates, send a request to
     * Location Services
     */
    private void stopPeriodicUpdates() {
        mLocationClient.removeLocationUpdates(this);
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
            // Google Play services was not available for some reason
        } else {
            // Display an error dialog
            Log.d(LOG_TAG, "GMS unavailable");
            return false;
        }
    }

}
