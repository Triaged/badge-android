package com.triaged.badge.app;

import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.saulpower.fayeclient.FayeClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;


/**
 * Service that maintains a websocket connection to faye
 * and handles incoming messages/publishes outgoing messages.
 *
 * @author Created by jc on 7/28/14.
 */
public class FayeService extends Service implements FayeClient.FayeListener {

    protected static final String LOG_TAG = FayeService.class.getName();

    protected static final String FAYE_HOST = "ws://badge-messaging-staging.herokuapp.com/streaming";
    //protected Fa faye;
    protected SharedPreferences prefs;
    protected boolean fayeConnected = false;
    protected FayeClient faye;
    protected int loggedInUserId;
    protected String authToken;
    private LocalBinding localBinding;
    private DataProviderService.LocalBinding dataProviderServiceBinding;

    @Override
    public IBinder onBind(Intent intent) {
        return localBinding;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        localBinding = new LocalBinding();
        prefs = PreferenceManager.getDefaultSharedPreferences( this );
        loggedInUserId = prefs.getInt(DataProviderService.LOGGED_IN_USER_ID_PREFS_KEY, -1);
        authToken = prefs.getString(DataProviderService.API_TOKEN_PREFS_KEY, "");
        if( loggedInUserId <= 0 ) {
            stopSelf();
        }
        else {
            URI fayeUri = URI.create( FAYE_HOST );
            faye = new FayeClient( new Handler(), fayeUri, String.format( "/users/messages/%s", loggedInUserId ) );
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if( loggedInUserId > 0 ) {
            JSONObject extension = new JSONObject();
            try {
                extension.put( "user_id", loggedInUserId );
                extension.put( "auth_token", authToken );
                // Looks like this is async so it's safe here.
                faye.setFayeListener( this );
                faye.connectToServer(extension);
            }
            catch( JSONException e ) {
                // Oh, bugger off.
                Log.e( LOG_TAG, "JSON exception trying to construct the dang faye extension for auth" );
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if( fayeConnected ) {
            faye.disconnectFromServer();
        }
        super.onDestroy();
    }

    @Override
    public void connectedToServer() {
        fayeConnected = true;
        Log.d( LOG_TAG, "Faye connected to server!" );
    }

    @Override
    public void disconnectedFromServer() {
        fayeConnected = false;
        Log.d( LOG_TAG, "Faye disconnected to server, frown emoji" );
    }

    @Override
    public void subscribedToChannel(String subscription) {
        Log.d( LOG_TAG, "Faye subscribed to channel!" );
    }

    @Override
    public void subscriptionFailedWithError(String error) {
        Log.e( LOG_TAG, "Couldn't subscribe: " + error );
    }

    /**
     * Message format: { “message_thread” : {“id”,  “user_ids” : [], “messages” : [] }
     *
     * @param json
     */
    @Override
    public void messageReceived( final JSONObject json) {
        ensureDataServiceBinding();
        dataProviderServiceBinding.upsertThreadAndMessages( json );
        // Do actual work.
        Log.d( LOG_TAG, "Message: " + json.toString() );
    }

    public class LocalBinding extends Binder {
        public void sendMessage( JSONObject msg ) {
            if( fayeConnected ) {
                //faye.publish( "" )
            }
        }
    }

    protected void ensureDataServiceBinding() {
        if( dataProviderServiceBinding == null ) {
            dataProviderServiceBinding = ((BadgeApplication) getApplication()).dataProviderServiceBinding;
        }
    }
}
