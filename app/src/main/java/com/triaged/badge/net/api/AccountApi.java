package com.triaged.badge.net.api;

import com.squareup.okhttp.Response;
import com.triaged.badge.TypedJsonString;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.PUT;

/**
 * Created by Sadegh Kazemy on 9/19/14.
 */
public interface AccountApi {

    @PUT("/v1/account/update_password")
    void changePassword(@Body TypedJsonString typedJsonString, Callback<Response> callback);
}
