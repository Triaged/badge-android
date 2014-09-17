package com.triaged.badge.net.api;

import com.triaged.badge.net.api.requests.ReceiptsReportRequest;

import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.POST;

/**
 * Created by Sadegh Kazemy on 9/16/14.
 */
public interface ReceiptApi {

    @POST("/api/v1/read_receipts")
    void reportReceipts(@Body ReceiptsReportRequest receiptsReportRequest, Callback<Response> callback);
}
