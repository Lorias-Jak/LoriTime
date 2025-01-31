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

/**
 * This class is responsible for providing the versions of the plugin from Modrinth.
 */
public class ModrinthReleaseSource implements ReleaseUpdateSource {

    /**
     * The API URL of the Modrinth project.
     */
    private final String apiUrl;

    /**
     * The name of the jar file.
     */
    private final String jarFileName;

    /**
     * Creates a new instance of the {@link ModrinthReleaseSource}.
     *
     * @param apiUrl      The API URL of the Modrinth project
     * @param jarFileName The name of the jar file
     */
    public ModrinthReleaseSource(final String apiUrl, final String jarFileName) {
        this.apiUrl = apiUrl;
        this.jarFileName = jarFileName;
    }

    @Override
    public Map<Version, String> getReleaseVersions(final Version currentVersion) throws IOException {
        final Map<Version, String> releaseVersions = new HashMap<>();
        final JsonArray releaseArray = new Gson().fromJson(new DownloadSource().getFromUrl(URI.create(apiUrl).toURL()), JsonArray.class);
        for (final JsonElement release : releaseArray) {
            final JsonObject releaseObject = release.getAsJsonObject();
            final Version version = new Version(releaseObject.get("version_number").getAsString());
            final JsonArray assets = releaseObject.getAsJsonArray("files");
            for (final JsonElement asset : assets) {
                final JsonObject assetObject = asset.getAsJsonObject();
                final String assetName = assetObject.get("filename").getAsString();
                if (assetName.equalsIgnoreCase(jarFileName)) {
                    final String downloadURL = assetObject.get("url").getAsString();
                    releaseVersions.put(version, downloadURL);
                }
            }
        }

        return releaseVersions;
    }
}
