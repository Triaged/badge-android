package com.triaged.badge.net.api;

import android.content.Intent;

import com.triaged.badge.app.App;
import com.triaged.badge.receivers.LogoutReceiver;

import retrofit.ErrorHandler;
import retrofit.RetrofitError;
import retrofit.client.Response;

/**
 * Created by Sadegh Kazemy on 10/6/14.
 */
public class ApiErrorHandler implements ErrorHandler {

    @Override
    public Throwable handleError(RetrofitError cause) {
        Response r = cause.getResponse();
        if (r != null && r.getStatus() == 401) {
            Intent logoutIntent = new Intent(LogoutReceiver.ACTION_LOGOUT);
            logoutIntent.putExtra(LogoutReceiver.RESTART_APP_EXTRA, true);
            App.context().sendBroadcast(logoutIntent);
        }
        return cause;
    }
}
