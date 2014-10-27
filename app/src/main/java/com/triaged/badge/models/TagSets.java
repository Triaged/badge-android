package com.triaged.badge.models;

/**
 * Created by Sadegh Kazemy on 10/15/14.
 */
public class TagSets {

    int id;
    String name;
    Tag[] tags;

    public String name() {
        return name;
    }

    public static class Tag {
        int id;
        String name;

        public String name() {
            return name;
        }
    }

}
