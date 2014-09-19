package com.triaged.badge.net;

import com.triaged.badge.app.App;
import com.triaged.badge.net.api.DeviceApi;
import com.triaged.badge.net.api.MessageThreadApi;
import com.triaged.badge.net.api.ReceiptApi;

/**
 * Created by Sadegh Kazemy on 9/19/14.
 */
public interface RestClient {

    public final DeviceApi deviceApi = App.restAdapterMessaging.create(DeviceApi.class);
    public final MessageThreadApi messageThreadApi = App.restAdapterMessaging.create(MessageThreadApi.class);
    public final ReceiptApi receiptApi = App.restAdapterMessaging.create(ReceiptApi.class);

}
