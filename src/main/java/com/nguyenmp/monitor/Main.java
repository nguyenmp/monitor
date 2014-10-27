package com.nguyenmp.monitor;

import com.squareup.okhttp.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {
    private static final String BASE_URL = "http://astral-casing-728.appspot.com/";
    private static final int TIMEOUT = 60*1000; // 1 minute in milliseconds
    public static void main(String[] args) throws IOException {
        // Get the hostname
        Runtime runtime = Runtime.getRuntime();
        Process who = runtime.exec("hostname");
        BufferedReader reader = new BufferedReader(new InputStreamReader(who.getInputStream()));
        String hostname = reader.readLine();
        reader.close();

        // Constantly run who every minute
        while (!false) {
            try {
                who = runtime.exec("who");
                reader = new BufferedReader(new InputStreamReader(who.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    parseLine(line, hostname);
                }
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

    private static void parseLine(final String line, final String hostname) {
        // [0] username
        // [1] pts/#
        // [2] Date
        // [3] Time
        // [4] Entry point (ip or tty)
        String[] parts = line.split("\\s+");

        String username = parts[0];
        boolean isRemote = parts[4].contains(":");

        // Spin up a new thread to post this entry
        new Uploader(username, hostname, isRemote).start();
    }

    public static class Uploader extends Thread {
        private final String username;
        private final String hostname;
        private final boolean isRemote;

        public Uploader(String username, String hostname, boolean isRemote) {
            this.username = username;
            this.hostname = hostname;
            this.isRemote = isRemote;
        }

        @Override
        public void run() {
            // Build body
            RequestBody body = new FormEncodingBuilder()
                    .add("username", username)
                    .add("hostname", hostname)
                    .add("is_remote", String.valueOf(isRemote))
                    .build();

            // Build request
            Request request = new Request.Builder()
                    .url(BASE_URL + "/usage/new")
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