package com.triaged.badge.models;

/**
 * Created by Sadegh Kazemy on 9/19/14.
 */
public class BThread {

    double timestamp;
    String id;
    boolean muted;
    String name;
    Message[] messages;
    Integer[] userIds;

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

    public Integer[] getUserIds() {
        return userIds;
    }

    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMessages(Message[] messages) {
        this.messages = messages;
    }

    public void setUserIds(Integer[] userIds) {
        this.userIds = userIds;
    }
}
