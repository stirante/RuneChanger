package com.stirante.runechanger.model.log;

import com.google.gson.Gson;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class LogRequest {

    private static final String URL = "https://u570wx94k6.execute-api.eu-central-1.amazonaws.com/v1/upload";

    public String log;

    public LogRequest(String log) {
        this.log = log;
    }

    public String submit() {
        try {
            Gson gson = new Gson();
            URL url = new URL(URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestMethod("PUT");
            conn.setDoOutput(true);
            conn.connect();
            OutputStream out = conn.getOutputStream();
            new DataOutputStream(out).write(gson.toJson(this).getBytes());
            out.flush();
            out.close();
            InputStream in = conn.getInputStream();
            LogResponse result = gson.fromJson(new InputStreamReader(in), LogResponse.class);
            in.close();
            conn.disconnect();
            return result.code;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}