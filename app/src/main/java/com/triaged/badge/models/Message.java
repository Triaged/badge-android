package com.triaged.badge.models;

/**
 * Created by Sadegh Kazemy on 9/18/14.
 */
public class Message {

    String id;
    String body;
    String authorId;
    String guid;
    double timestamp;
    Receipt[] readReceipts;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(double timestamp) {
        this.timestamp = timestamp;
    }

    public Receipt[] getReadReceipts() {
        return readReceipts;
    }

    public void setReadReceipts(Receipt[] readReceipts) {
        this.readReceipts = readReceipts;
    }
}
