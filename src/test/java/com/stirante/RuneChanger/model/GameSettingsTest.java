package com.stirante.RuneChanger.model;

import com.google.gson.*;
import com.stirante.RuneChanger.SetupApiConnection;
import com.stirante.RuneChanger.model.GameSettings.GameSettings;
import com.stirante.lolclient.ClientApi;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Map;

import static com.stirante.RuneChanger.JsonUtil.countJson;
import static com.stirante.RuneChanger.JsonUtil.getStrictGsonObject;

/**
 * If the gson classes get outdated you can use these settings https://gyazo.com/f2157720d112c33bcf2268b04e070778?token=4f34116248804a826504445f24889559
 * with this website http://www.jsonschema2pojo.org
 */
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

        Gson gson = getStrictGsonObject();
        String json = gson.toJson(map);
        GameSettings gameSettings = gson.fromJson(json, GameSettings.class);
        String exportedJson = gson.toJson(gameSettings);
        Assert.assertTrue(exportedJson.equals(json));

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
            }
        }
    }


}
