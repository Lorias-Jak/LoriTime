package com.jannik_kuehn.common.module.updater.download;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Responsible for downloading the source from a given URL.
 */
public class DownloadSource {

    /**
     * Creates a new instance of the {@link DownloadSource}.
     */
    public DownloadSource() {
        // Empty
    }

    /**
     * Downloads the source from the given URL.
     *
     * @param url the {@link URL} where the source code should be downloaded from
     * @return the source code as a {@link String}
     * @throws IOException is thrown if any problem occurred while downloading the source code
     */
    public String getFromUrl(final URL url) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.connect();

            final int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("It looks like you have made too many requests to the server in too short a time. Wait a little while, then you can download again. Response code: " + responseCode);
            }

            try (InputStream inputStream = connection.getInputStream()) {
                return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            }
        } finally {
            connection.disconnect();
        }

    }
}
