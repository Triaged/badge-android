package com.triaged.badge.events;

/**
 * Created by Sadegh Kazemy on 9/16/14.
 */
public class NewMessageEvent {

    public String threadId;
    public String messageId;

    public NewMessageEvent(String threadId, String messageId) {
        this.threadId = threadId;
        this.messageId = messageId;
    }
}
