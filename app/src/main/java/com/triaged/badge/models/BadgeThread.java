package com.triaged.badge.models;

/**
 * Created by Sadegh Kazemy on 9/19/14.
 */
public class BadgeThread {

    double timestamp;
    String id;
    boolean muted;
    String name;
    Message[] messages;
    String [] userIds;

    public double getTimestamp() {
        return timestamp;
    }

    public String getId() {
        return id;
    }

    public boolean isMuted() {
        return muted;
    }

    public Message[] getMessages() {
        return messages;
    }

    public String getName() {
        return name;
    }

    public String[] getUserIds() {
        return userIds;
    }
}
