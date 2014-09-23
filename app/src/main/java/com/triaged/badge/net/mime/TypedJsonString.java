package com.triaged.badge.net.mime;

import retrofit.mime.TypedString;

/**
 * Created by Sadegh Kazemy on 9/19/14.
 */

public class TypedJsonString extends TypedString {
    public TypedJsonString(String body) {
        super(body);
    }

    @Override public String mimeType() {
        return "application/json";
    }
}
