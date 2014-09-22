package com.triaged.badge.net.api;

import com.triaged.badge.models.Department;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;
import retrofit.mime.TypedInput;

/**
 * Created by Sadegh Kazemy on 9/22/14.
 */
public interface DepartmentApi {

    @POST("/v1/departments")
    void create(@Body TypedInput requestBody, Callback<Department> callback);

    @GET("/v1/departments/{id}")
    void get(@Path("id") String id, Callback<Department> callback);

}
