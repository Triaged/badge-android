package com.triaged.badge.net.api;

import com.triaged.badge.net.api.requests.DeviceRequest;

import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by Sadegh Kazemy on 9/18/14.
 */
public interface DeviceApi {


    @POST("v1/devices")
    void registerDevice(@Body DeviceRequest deviceRequest);

    @DELETE("v1/devices/{device_id}/sign_out")
    void signOut(@Path("device_id") int deviceId);

}
