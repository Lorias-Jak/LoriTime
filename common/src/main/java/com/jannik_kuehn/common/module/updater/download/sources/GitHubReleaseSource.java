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
 * This class is responsible for providing the versions of the plugin from GitHub.
 */
public class GitHubReleaseSource implements ReleaseUpdateSource {

    /**
     * The URL addition of the releases.
     */
    public static final String RELEASES_URL = "/releases";

    /**
     * The API URL of the GitHub project.
     */
    private final String apiUrl;

    /**
     * The name of the jar file.
     */
    private final String jarFileName;

    /**
     * Creates a new instance of the {@link GitHubReleaseSource}.
     *
     * @param apiUrl      The API URL of the GitHub project
     * @param jarFileName The name of the jar file
     */
    public GitHubReleaseSource(final String apiUrl, final String jarFileName) {
        this.apiUrl = apiUrl;
        this.jarFileName = jarFileName;
    }

    @Override
    public Map<Version, String> getReleaseVersions(final Version currentVersion) throws IOException {
        final Map<Version, String> releaseVersions = new HashMap<>();
        final JsonArray releaseArray = new Gson().fromJson(new DownloadSource().getFromUrl(URI.create(apiUrl + RELEASES_URL).toURL()), JsonArray.class);
        for (final JsonElement release : releaseArray) {
            final JsonObject releaseObject = release.getAsJsonObject();
            final Version version = new Version(releaseObject.get("tag_name").getAsString());
            final JsonArray assets = releaseObject.getAsJsonArray("assets");
            for (final JsonElement asset : assets) {
                final JsonObject assetObject = asset.getAsJsonObject();
                final String assetName = assetObject.get("name").getAsString();
                if (assetName.equalsIgnoreCase(jarFileName)) {
                    final String downloadURL = assetObject.get("browser_download_url").getAsString();
                    releaseVersions.put(version, downloadURL);
                }
            }
        }

        return releaseVersions;
    }
}
