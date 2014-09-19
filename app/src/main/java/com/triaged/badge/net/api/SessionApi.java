package com.triaged.badge.net.api;

import com.triaged.badge.models.Account;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.POST;
import retrofit.mime.TypedInput;

/**
 * Created by Sadegh Kazemy on 9/19/14.
 */
public interface SessionApi {

    @POST("/v1/sessions")
    void login(@Body TypedInput requestBody, Callback<Account> callback);
}
