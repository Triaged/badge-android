package com.triaged.badge.models;

import java.io.Serializable;

/**
 * Created by Sadegh Kazemy on 9/10/14.
 */
public class MessageThread implements Serializable {

    String name;

    public MessageThread(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

}

