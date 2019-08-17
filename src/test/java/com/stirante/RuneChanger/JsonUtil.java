package com.stirante.RuneChanger;

import com.google.gson.JsonObject;

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
}
