package com.triaged.badge.net.api;


import com.triaged.badge.net.api.requests.MessageThreadRequest;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

/**
 * Created by Sadegh Kazemy on 9/11/14.
 */
public interface MessageThreadApi {

    @PUT("/api/v1/message_threads/{message_thread_id}")
    void setName(@Path("message_thread_id") String threadId,
                 @Body MessageThreadRequest messageThreadRequest,
                 Callback<Response> callback);


    @POST("/api/v1/message_threads/{message_thread_id}/unmute")
    void unMute(@Path("message_thread_id") String threadId, Callback<Response> callback);

    @POST("/api/v1/message_threads/{message_thread_id}/unmute")
    void mute(@Path("message_thread_id") String threadId, Callback<Response> callback);
}
