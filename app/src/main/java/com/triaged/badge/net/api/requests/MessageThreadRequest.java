package com.triaged.badge.net.api.requests;

import com.triaged.badge.models.MessageThread;

/**
 * Created by Sadegh Kazemy on 9/11/14.
 */
public class MessageThreadRequest {

    MessageThread messageThread;

    public MessageThreadRequest(MessageThread messageThread) {
        this.messageThread = messageThread;
    }
}
