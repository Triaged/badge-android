package com.triaged.badge.net.api.serializer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.triaged.badge.models.MessageThread;

import java.lang.reflect.Type;

/**
 * Created by Sadegh Kazemy on 9/11/14.
 */
public class MessageThreadTypeAdapter implements JsonSerializer<MessageThread> {

    @Override
    public JsonElement serialize(MessageThread src, Type typeOfSrc, JsonSerializationContext context) {

        JsonObject fields = new JsonObject();

         if (src.name() != null) {
             fields.addProperty("name", src.name());
         }

        JsonObject resultJsonObject = new JsonObject();
        resultJsonObject.add("message_thread", fields);
        return resultJsonObject;
    }
}
