package com.triaged.badge.net;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.saulpower.fayeclient.FayeClient;
import com.triaged.badge.app.App;
import com.triaged.badge.app.BuildConfig;
import com.triaged.badge.app.MessageProcessor;
import com.triaged.badge.app.R;
import com.triaged.badge.events.MessageForFayEvent;
import com.triaged.badge.events.ReceiptForFayEvent;
import com.triaged.badge.models.BThread;
import com.triaged.badge.models.Receipt;
import com.triaged.utils.SharedPreferencesHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;


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
    private ScheduledFuture<?> heartbeatFuture;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);

        heartbeatThread = Executors.newSingleThreadScheduledExecutor();
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        loggedInUserId = SharedPreferencesHelper.instance().getInteger(R.string.pref_account_id_key, -1);
        authToken = SharedPreferencesHelper.instance().getString(R.string.pref_api_token, "");
        if (loggedInUserId <= 0) {
            stopSelf();
        } else {
            URI fayeUri = URI.create(FAYE_HOST);
            faye = new FayeClient(new Handler(), fayeUri, String.format("/users/messages/%s", loggedInUserId));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
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
    @DebugLog
    public void messageReceived(final JSONObject json) {
        if (json.has("message_thread")) {
            try {
                JSONObject msgJsonObj = json.getJSONObject("message_thread");
                BThread messageThread = App.gson.fromJson(msgJsonObj.toString(), BThread.class);
                MessageProcessor.getInstance().upsertThreadAndMessages(messageThread, true);
            } catch (JSONException e) {
                Log.w(LOG_TAG, "JSON exception extracting GUID. This is a big surprise.", e);
            }
        } else if (json.has("receipts")) {
            try {
                JSONArray receiptJsonObj = json.getJSONArray("receipts");
                Receipt[] receipts = App.gson.fromJson(receiptJsonObj.toString(), Receipt[].class);
                MessageProcessor.getInstance().storeReceipts(receipts);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void onEvent(MessageForFayEvent event) {
        if (fayeConnected) {
            faye.publish(String.format("/threads/messages/%s", event.getThreadId()), event.getMessage());
        }
    }

    public void onEvent(ReceiptForFayEvent event) {
        if (fayeConnected) {
            List<Receipt> threadReceipts = new ArrayList<Receipt>();
            String previousThreadId = event.getReceipts().get(0).getThreadId();
            for (Receipt receipt: event.getReceipts()) {
                if (previousThreadId.equals(receipt.getThreadId())) {
                    threadReceipts.add(receipt);
                } else {
                    prepareAndSendReceipts(threadReceipts, previousThreadId);
                    previousThreadId = receipt.getThreadId();
                    threadReceipts.clear();
                }
            }
            if (threadReceipts.size() > 0) {
                prepareAndSendReceipts(threadReceipts, previousThreadId);
            }
        }
    }

    private void prepareAndSendReceipts(List<Receipt> threadReceipts, String threadId) {
        JsonElement jsonStr = App.gson.toJsonTree(threadReceipts);
        JsonObject jsonObject = new JsonObject();
        try {
            jsonObject.add("receipts", jsonStr);
            jsonObject.addProperty("guid", "do_not_need_guid");
            if (fayeConnected) {
                JSONObject jj = new JSONObject(jsonObject.toString());
                faye.publish(String.format("/threads/receipts/%s", threadId), jj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
