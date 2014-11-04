package com.nguyenmp.monitor;

import com.google.gson.Gson;
import com.squareup.okhttp.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final String BASE_URL = "http://astral-casing-728.appspot.com/";
    private static final int TIMEOUT = 5*60*1000; // 5 minute in milliseconds
    public static void main(String[] args) throws IOException {
        // Get the hostname
        Runtime runtime = Runtime.getRuntime();
        Process who = runtime.exec("hostname");
        BufferedReader reader = new BufferedReader(new InputStreamReader(who.getInputStream()));
        String hostname = reader.readLine();
        reader.close();

        // Constantly run who every minute
        while (true) {
            try {
                who = runtime.exec("who");
                reader = new BufferedReader(new InputStreamReader(who.getInputStream()));
                String line;

                List<UploadModel> models = new ArrayList<>();
                while ((line = reader.readLine()) != null) {
                    models.add(parseLine(line, hostname));
                }

                new Uploader(models.toArray(new UploadModel[models.size()])).start();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static UploadModel parseLine(final String line, final String hostname) {
        // [0] username
        // [1] pts/#
        // [2] Date
        // [3] Time
        // [4] Entry point (ip or tty)
        String[] parts = line.split("\\s+");

        String username = parts[0];
        boolean isRemote = !parts[parts.length - 1].contains(":");
        System.out.println(isRemote);

        // Spin up a new thread to post this entry
        UploadModel uploadModel = new UploadModel();
        uploadModel.username = username;
        uploadModel.hostname = hostname;
        uploadModel.isRemote = isRemote;
        return uploadModel;
    }

    public static class UploadModel {
        public String username;
        public String hostname;
        public boolean isRemote;
    }

    public static class Uploader extends Thread {
        private final UploadModel[] models;

        public Uploader(UploadModel[] models) {
            this.models = models;
        }

        @Override
        public void run() {
            // Build body
            RequestBody body = new FormEncodingBuilder()
                    .add("data", new Gson().toJson(models))
                    .build();

            // Build request
            Request request = new Request.Builder()
                    .url(BASE_URL + "/usage")
                    .post(body)
                    .build();

            // Execute request
            try {
                OkHttpClient client = new OkHttpClient();
                client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
