package com.stirante.RuneChanger.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stirante.RuneChanger.SetupApiConnection;
import com.stirante.RuneChanger.model.GameSettings.GameSettings;
import com.stirante.lolclient.ClientApi;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

import static com.stirante.RuneChanger.JsonUtil.countJson;

public class GameSettingsTest extends SetupApiConnection {

    private final String API_PATH = "/lol-game-settings/v1/game-settings";
    private ClientApi api = SetupApiConnection.api;

    @Test
    public void verifyGameSettingsClasses() throws IllegalAccessException {
        Map map = null;
        try {
            map = api.executeGet(API_PATH, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(map);
        int fieldCount = 0;
        GameSettings gameSettings = gson.fromJson(json, GameSettings.class);
        for (Field declaredField : gameSettings.getClass().getDeclaredFields()) {
            declaredField.setAccessible(true);
            Object object = declaredField.get(gameSettings);
            if (object == null && !Modifier.isStatic(declaredField.getModifiers())) {
                Assert.fail();
            }

            for (Field declaredSubfield : object.getClass().getDeclaredFields()) {
                declaredSubfield.setAccessible(true);
                Object subObject = declaredSubfield.get(object);
                if (subObject == null && !Modifier.isStatic(declaredSubfield.getModifiers())) {
                    System.out.println("Processing went wrong at: " + declaredSubfield + "\n Json: " + json);
                    Assert.fail();
                }
                fieldCount++;
            }
            fieldCount++;
        }

        int jsonCount = countJson(gson.toJsonTree(map).getAsJsonObject());
        System.out.println(jsonCount + " == jsoncount | " + fieldCount + " == fieldcount");
        Assert.assertTrue(jsonCount == fieldCount);
    }


}
