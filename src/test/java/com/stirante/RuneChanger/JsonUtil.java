package com.stirante.RuneChanger;

import com.google.gson.*;

import java.util.concurrent.atomic.AtomicInteger;

public class JsonUtil {
    public static int countJson(JsonObject jsonObject) {
        AtomicInteger jsonCount = new AtomicInteger();
        jsonObject.keySet().forEach(key -> {
            Object keyvalue = jsonObject.get(key);
            if (keyvalue instanceof JsonObject) {
                ((JsonObject) keyvalue).keySet().forEach(key2 -> jsonCount.getAndIncrement());
            }
            jsonCount.getAndIncrement();
        });
        return jsonCount.get();
    }

    public static Gson getStrictGsonObject() {
        return new GsonBuilder().
                registerTypeAdapter(Double.class, (JsonSerializer<Double>) (src, typeOfSrc, context) -> {
                    if(src == src.longValue())
                        return new JsonPrimitive(src.longValue());
                    return new JsonPrimitive(src);
                }).create();
    }
}
