package com.triaged.badge.net;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.saulpower.fayeclient.FayeClient;
import com.triaged.badge.app.App;
import com.triaged.badge.app.BuildConfig;
import com.triaged.badge.app.R;
import com.triaged.badge.models.BThread;
import com.triaged.utils.SharedPreferencesUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * Service that maintains a websocket connection to faye
 * and handles incoming messages/publishes outgoing messages.
 *
 * @author Created by jc on 7/28/14.
 */
public class FayeService extends Service implements FayeClient.FayeListener {

    protected static final String LOG_TAG = FayeService.class.getName();

    protected static final String FAYE_HOST = BuildConfig.FAY_SERVER_URL;

    //protected Fa faye;
    protected SharedPreferences prefs;
    protected boolean fayeConnected = false;
    protected FayeClient faye;
    protected int loggedInUserId;
    protected String authToken;
    protected ScheduledExecutorService heartbeatThread;
    private LocalBinding localBinding;
    private ScheduledFuture<?> heartbeatFuture;
    private DataProviderService.LocalBinding dataProviderServiceBinding;
    protected int timesStarted = 0;

    @Override
    public IBinder onBind(Intent intent) {
        return localBinding;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        localBinding = new LocalBinding();
        heartbeatThread = Executors.newSingleThreadScheduledExecutor();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        loggedInUserId = SharedPreferencesUtil.getInteger(R.string.pref_account_id_key, -1);
        authToken = SharedPreferencesUtil.getString(R.string.pref_api_token, "");
        if (loggedInUserId <= 0) {
            stopSelf();
        } else {
            URI fayeUri = URI.create(FAYE_HOST);
            faye = new FayeClient(new Handler(), fayeUri, String.format("/users/messages/%s", loggedInUserId));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Log.d( LOG_TAG, "Faye service object started " + timesStarted++ + " times" );
        if (loggedInUserId > 0) {
            JSONObject extension = new JSONObject();
            try {
                extension.put("user_id", loggedInUserId);
                extension.put("auth_token", authToken);
                // Looks like this is async so it's safe here.
                faye.setFayeListener(this);
                faye.connectToServer(extension);
            } catch (JSONException e) {
                // Oh, bugger off.
                App.gLogger.e("JSON exception trying to construct the dang faye extension for auth");
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(true);
        }
        if (faye != null) {
            faye.destroy();
        }
        faye = null;
        heartbeatThread.shutdownNow();
        super.onDestroy();
    }

    @Override
    public void connectedToServer() {
        fayeConnected = true;
        Log.d(LOG_TAG, "Faye connected to server!");
        // Start heartbeat
        heartbeatFuture = heartbeatThread.scheduleAtFixedRate(new Runnable() {
            JSONObject message;
            String heartbeatChannel;

            {
                message = new JSONObject();
                try {
                    message.put("ping", "pong");
                } catch (JSONException e) {
                    // Not an operation that can fail.
                }
                heartbeatChannel = String.format("/users/heartbeat/%s", loggedInUserId);
            }

            @Override
            public void run() {
                faye.publish(heartbeatChannel, message);
            }
        }, 250, 250, TimeUnit.MILLISECONDS);

    }

    @Override
    public void disconnectedFromServer() {
        fayeConnected = false;
        heartbeatFuture.cancel(true);
        Log.d(LOG_TAG, "Faye disconnected from server, frown emoji");
    }

    @Override
    public void subscribedToChannel(String subscription) {
    }

    @Override
    public void subscriptionFailedWithError(String error) {
        App.gLogger.e("Couldn't subscribe: " + error);
    }

    /**
     * Message format: { “message_thread” : {“id”,  “user_ids” : [], “messages” : [] }
     *
     * @param json
     */
    @Override
    public void messageReceived(final JSONObject json) {
        if (json.has("ping")) {
            return;
        }
        ensureDataServiceBinding();
        try {
            BThread messageThread = App.gson.fromJson(json.getJSONObject("message_thread").toString(), BThread.class);
            dataProviderServiceBinding.upsertThreadAndMessagesAsync(messageThread);
            // Do actual work.

        } catch (JSONException e) {
            Log.w(LOG_TAG, "JSON exception extracting GUID. This is a big surprise.", e);
        }
        // Log.d( LOG_TAG, "Message: " + json.toString() );
    }

    public class LocalBinding extends Binder {
        public void sendMessage(String threadId, JSONObject msg) {
            if (fayeConnected) {
                faye.publish(String.format("/threads/messages/%s", threadId), msg);
            }
        }
    }

    protected void ensureDataServiceBinding() {
        if (dataProviderServiceBinding == null) {
            dataProviderServiceBinding = ((App) getApplication()).dataProviderServiceBinding;
        }
    }
}
