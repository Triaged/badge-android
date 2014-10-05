package com.triaged.badge.events;

import org.json.JSONObject;

/**
 * Created by Sadegh Kazemy on 10/1/14.
 */
public class MessageForFayEvent {
    JSONObject messageObject;
    String threadId;

    public MessageForFayEvent(String threadId, JSONObject msg) {
        this.messageObject = msg;
        this.threadId = threadId;
    }

    public JSONObject getMessage() {
        return messageObject;
    }

    public String getThreadId() {
        return threadId;
    }
}
