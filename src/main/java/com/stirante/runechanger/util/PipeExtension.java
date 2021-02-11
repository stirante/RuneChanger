package com.stirante.runechanger.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stirante.justpipe.Pipe;
import com.stirante.justpipe.function.IOFunction;

public class PipeExtension {

    public static final IOFunction<Pipe, JsonElement> JSON_ELEMENT = pipe -> JsonParser.parseString(pipe.toString());
    public static final IOFunction<Pipe, JsonObject> JSON_OBJECT = pipe -> JSON_ELEMENT.apply(pipe).getAsJsonObject();
    public static final IOFunction<Pipe, JsonArray> JSON_ARRAY = pipe -> JSON_ELEMENT.apply(pipe).getAsJsonArray();

}
