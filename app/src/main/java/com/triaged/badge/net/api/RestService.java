package com.triaged.badge.net.api;

import com.squareup.okhttp.Response;
import com.triaged.badge.TypedJsonString;
import com.triaged.badge.app.App;
import com.triaged.badge.models.Account;
import com.triaged.badge.models.Department;
import com.triaged.badge.models.OfficeLocation;
import com.triaged.badge.net.api.requests.DeviceRequest;
import com.triaged.badge.net.api.requests.MessageThreadRequest;
import com.triaged.badge.net.api.requests.ReceiptsReportRequest;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.PATCH;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.mime.TypedFile;
import retrofit.mime.TypedInput;

/**
 * Created by Sadegh Kazemy on 9/19/14.
 */
public class RestService {

    public static RestService mInstance;
    private static MessagingService mMessaging;
    private static BadgeService mBadge;

    public static RestService instance() {
        synchronized (RestService.class) {
            if (mInstance == null) {
                if (mMessaging == null || mBadge == null) {
                    throw new IllegalStateException("Please first prepare the rest service by calling prepare() method");
                }
                mInstance = new RestService();
            }
        }
        return mInstance;
    }

    private RestService() { }

    public static void prepare(RestAdapter messagingAdapter, RestAdapter badgeAdapter) {
        mMessaging = messagingAdapter.create(MessagingService.class);
        mBadge = badgeAdapter.create(BadgeService.class);
    }

    public MessagingService messaging() {
        return mMessaging;
    }

    public BadgeService badge() {
        return mBadge;
    }


    public interface MessagingService {
        @PUT("/api/v1/message_threads/{message_thread_id}")
        void threadSetName(@Path("message_thread_id") String threadId,
                           @Body MessageThreadRequest messageThreadRequest,
                           Callback<retrofit.client.Response> callback);

        @POST("/api/v1/message_threads/{message_thread_id}/unmute")
        void unmuteThread(@Path("message_thread_id") String threadId, Callback<retrofit.client.Response> callback);

        @POST("/api/v1/message_threads/{message_thread_id}/mute")
        void muteThread(@Path("message_thread_id") String threadId, Callback<retrofit.client.Response> callback);


        @POST("/api/v1/read_receipts")
        void reportReceipts(@Body ReceiptsReportRequest receiptsReportRequest, Callback<retrofit.client.Response> callback);
    }

    public interface BadgeService {
        @PUT("/v1/account/update_password")
        void changePassword(@Body TypedJsonString typedJsonString, Callback<Response> callback);

        @PATCH("/v1/account")
        void updateAccount(@Body TypedJsonString updateDate, Callback<Account> callback);

        @Multipart
        @POST("/v1/account/avatar")
        void postAvatar(@Part("user[avatar]") TypedFile avatar, Callback<Account> callback);


        @POST("/v1/departments")
        void createDepartment(@Body TypedInput requestBody, Callback<Department> callback);

        @GET("/v1/departments/{id}")
        void getDepartment(@Path("id") String id, Callback<Department> callback);


        @POST("v1/devices")
        void registerDevice(@Body DeviceRequest deviceRequest);

        @DELETE("v1/devices/{device_id}/sign_out")
        void signOut(@Path("device_id") int deviceId);


        @POST("/v1/sessions")
        void login(@Body TypedInput requestBody, Callback<Account> callback);


        @POST("/v1/office_locations")
        void createOfficeLocation(@Body TypedInput typedInput, Callback<OfficeLocation> callback);
    }

}
