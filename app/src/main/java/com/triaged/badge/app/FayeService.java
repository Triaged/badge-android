package com.triaged.badge.app;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
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

    protected static final String FAYE_HOST = "wss://badge-messaging.herokuapp.com:443/";
    //protected Fa faye;
    protected SharedPreferences prefs;
    protected boolean fayeConnected = false;
    protected FayeClient faye;
    protected String loggedInUserId;
    protected String authToken;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = PreferenceManager.getDefaultSharedPreferences( this );
        loggedInUserId = prefs.getString( DataProviderService.LOGGED_IN_USER_ID_PREFS_KEY, "" );
        authToken = prefs.getString( DataProviderService.API_TOKEN_PREFS_KEY, "" );
        if( "".equals( loggedInUserId ) ) {
            stopSelf();
        }
        else {
            URI fayeUri = URI.create( FAYE_HOST );
            faye = new FayeClient( new Handler(), fayeUri, String.format( "/users/messages/%s", loggedInUserId ) );
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if( !"".equals( loggedInUserId ) ) {
            JSONObject extension = new JSONObject();
            try {
                extension.put( "user_id", loggedInUserId );
                extension.put( "auth_token", authToken );
                // Looks like this is async so it's safe here.
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

    @Override
    public void messageReceived(JSONObject json) {
        // Do actual work.
        Log.d( LOG_TAG, "Message: " + json.toString() );
    }
}
