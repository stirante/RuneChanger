package com.stirante.runechanger.model.log;

import com.google.gson.Gson;
import com.stirante.runechanger.util.AnalyticsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class LogRequest {
    private static final Logger log = LoggerFactory.getLogger(LogRequest.class);

    private static final String URL = "https://api.runechanger.stirante.com/v1/upload";

    public String logContent;

    public LogRequest(String log) {
        this.logContent = log;
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
            log.error("Exception occurred while submitting logs", e);
            AnalyticsUtil.addCrashReport(e, "Exception occurred while submitting logs", false);
            return null;
        }
    }

}