package com.triaged.badge.net.api.requests;

import com.triaged.badge.models.BThread;

/**
 * Created by Sadegh Kazemy on 9/11/14.
 */
public class MessageBThreadRequest {

    BThread messageThread;

    public MessageBThreadRequest(BThread bThread) {
        this.messageThread = bThread;
    }
}
