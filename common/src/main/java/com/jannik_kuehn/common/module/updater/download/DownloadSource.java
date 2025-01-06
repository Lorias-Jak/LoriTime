package com.jannik_kuehn.common.module.updater.download;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

public class DownloadSource {

    public DownloadSource() {
        // Empty
    }

    public String getFromUrl(URL url) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("It looks like you have made too many requests to the server in too short a time. Wait a little while, then you can download again. Response code: " + responseCode);
            }

            try (InputStream inputStream = connection.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } finally {
            connection.disconnect();
        }
    }
}
