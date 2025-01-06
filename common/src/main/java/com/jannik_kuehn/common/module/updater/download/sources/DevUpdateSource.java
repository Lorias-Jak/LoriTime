package com.jannik_kuehn.common.module.updater.download.sources;

import com.jannik_kuehn.common.module.updater.version.Version;

import java.util.Map;

public interface DevUpdateSource {

    /**
     * Returns a {@link Map} with the latest {@link Version} and the {@link String} download link.
     *
     * @param currentVersion the current developer version of the plugin
     * @return a map with the latest version and the download link
     */
    Map<Version, String> getDevelopmentVersions(Version currentVersion);
}
