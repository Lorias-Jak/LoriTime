package com.jannik_kuehn.common.module.updater.sources;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jannik_kuehn.common.module.updater.version.Version;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GitHubReleaseSource implements ReleaseUpdateSource {

    public static final String RELEASES_URL = "/releases";

    private final String apiUrl;

    private final String jarFileName;

    public GitHubReleaseSource(String apiUrl, final String jarFileName) {
        this.apiUrl = apiUrl;
        this.jarFileName = jarFileName;
    }

    @Override
    public Map<Version, String> getReleaseVersions(Version currentVersion) throws IOException {
        final Map<Version, String> releaseVersions = new HashMap<>();
        final JsonArray releaseArray = new Gson().fromJson(getFromUrl(URI.create(apiUrl + RELEASES_URL).toURL()), JsonArray.class);
        for (JsonElement release : releaseArray) {
            final JsonObject releaseObject = release.getAsJsonObject();
            final Version version = new Version(releaseObject.get("tag_name").getAsString());
            final JsonArray assets = releaseObject.getAsJsonArray("assets");
            for (JsonElement asset : assets) {
                JsonObject assetObject = asset.getAsJsonObject();
                String assetName = assetObject.get("name").getAsString();
                if (assetName.equalsIgnoreCase(jarFileName)) {
                    final String downloadURL = assetObject.get("browser_download_url").getAsString();
                    releaseVersions.put(version, downloadURL);
                }
            }
        }

        return releaseVersions;
    }

    private String getFromUrl(URL url) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error: " + responseCode);
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
