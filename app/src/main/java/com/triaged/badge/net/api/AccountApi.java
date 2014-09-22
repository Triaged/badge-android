package com.triaged.badge.net.api;

import com.squareup.okhttp.Response;
import com.triaged.badge.TypedJsonString;
import com.triaged.badge.models.Account;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.Multipart;
import retrofit.http.PATCH;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Part;
import retrofit.mime.TypedFile;

/**
 * Created by Sadegh Kazemy on 9/19/14.
 */
public interface AccountApi {

    @PUT("/v1/account/update_password")
    void changePassword(@Body TypedJsonString typedJsonString, Callback<Response> callback);

    @PATCH("/v1/account")
    void update(@Body TypedJsonString updateDate, Callback<Account> callback);

    @Multipart
    @POST("/v1/account/avatar")
    void postAvatar(@Part("user[avatar]")TypedFile avatar, Callback<Account> callback);
}
