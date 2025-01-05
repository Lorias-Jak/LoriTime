package com.jannik_kuehn.common.module.updater;

import com.jannik_kuehn.common.module.updater.version.Version;

import java.util.Map;

/**
 * This interface is responsible for providing the versions of the plugin.
 */
public interface UpdateSource {

    /**
     * Returns a {@link Map} with the latest {@link Version} and the {@link String} download link.
     *
     * @param currentVersion the current version of the plugin
     * @return a map with the latest version and the download link
     */
    Map<Version, String> getReleaseVersions(Version currentVersion);
}
