package com.jannik_kuehn.common.module.updater.download.sources;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jannik_kuehn.common.module.updater.download.DownloadSource;
import com.jannik_kuehn.common.module.updater.version.Version;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class ModrinthReleaseSource implements ReleaseUpdateSource {

    private final String apiUrl;

    private final String jarFileName;

    public ModrinthReleaseSource(String apiUrl, final String jarFileName) {
        this.apiUrl = apiUrl;
        this.jarFileName = jarFileName;
    }

    @Override
    public Map<Version, String> getReleaseVersions(Version currentVersion) throws IOException {
        final Map<Version, String> releaseVersions = new HashMap<>();
        final JsonArray releaseArray = new Gson().fromJson(new DownloadSource().getFromUrl(URI.create(apiUrl).toURL()), JsonArray.class);
        for (JsonElement release : releaseArray) {
            final JsonObject releaseObject = release.getAsJsonObject();
            final Version version = new Version(releaseObject.get("version_number").getAsString());
            final JsonArray assets = releaseObject.getAsJsonArray("files");
            for (JsonElement asset : assets) {
                JsonObject assetObject = asset.getAsJsonObject();
                String assetName = assetObject.get("filename").getAsString();
                if (assetName.equalsIgnoreCase(jarFileName)) {
                    final String downloadURL = assetObject.get("url").getAsString();
                    releaseVersions.put(version, downloadURL);
                }
            }
        }

        return releaseVersions;
    }
}
