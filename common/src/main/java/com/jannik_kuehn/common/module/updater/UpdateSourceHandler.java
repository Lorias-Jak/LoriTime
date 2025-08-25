package com.jannik_kuehn.common.module.updater;

import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.module.updater.download.sources.DevUpdateSource;
import com.jannik_kuehn.common.module.updater.download.sources.ReleaseUpdateSource;
import com.jannik_kuehn.common.module.updater.version.Strategy;
import com.jannik_kuehn.common.module.updater.version.Version;
import com.jannik_kuehn.common.module.updater.version.VersionComparator;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

/**
 * Responsible for handling the update sources and searching for updates.
 */
public class UpdateSourceHandler {

    /**
     * The qualifier that is used for development versions.
     */
    public static final String DEV_QUALIFIER = "DEV-";

    /**
     * The {@link LoriTimeLogger} that should be used for logging.
     */
    private final LoriTimeLogger log;

    /**
     * The {@link ReleaseUpdateSource}s that should be used for searching for release updates.
     */
    private final List<ReleaseUpdateSource> releaseUpdateSources;

    /**
     * The {@link DevUpdateSource}s that should be used for searching for development updates.
     */
    private final List<DevUpdateSource> developmentUpdateSources;

    /**
     * Creates a new {@link UpdateSourceHandler}.
     *
     * @param log                      the {@link LoriTimeLogger} that should be used for logging
     * @param releaseUpdateSources     the {@link ReleaseUpdateSource}s that should be used for searching for release updates
     * @param developmentUpdateSources the {@link DevUpdateSource}s that should be used for searching for development updates
     */
    public UpdateSourceHandler(final LoriTimeLogger log, final List<ReleaseUpdateSource> releaseUpdateSources,
                               final List<DevUpdateSource> developmentUpdateSources) {
        this.log = log;
        this.releaseUpdateSources = releaseUpdateSources;
        this.developmentUpdateSources = developmentUpdateSources;
    }

    /**
     * Searches for an update for the given indicator and current version.
     *
     * @param updateStrategy the {@link Strategy} that should be used for comparing the versions
     * @param indicator      the indicator that should be used for searching for updates
     * @param currentVersion the current version of the plugin
     * @return a {@link Pair} with the latest found version and the download link
     */
    public Pair<Version, String> searchUpdate(final Strategy updateStrategy, final String indicator, final Version currentVersion) {
        final VersionComparator versionComparator = new VersionComparator(updateStrategy, indicator);
        Pair<Version, String> latestVersion = Pair.of(currentVersion, null);
        latestVersion = searchUpdateFor(latestVersion, releaseUpdateSources, versionComparator,
                releaseUpdateSources -> releaseUpdateSources.getReleaseVersions(currentVersion));
        if (currentVersion.hasQualifier() && DEV_QUALIFIER.equals(currentVersion.getQualifier()) && latestVersion.getValue() == null) {
            latestVersion = searchUpdateFor(latestVersion, developmentUpdateSources, versionComparator,
                    developmentUpdateSources -> developmentUpdateSources.getDevelopmentVersions(currentVersion));
        }
        return latestVersion;
    }

    private <T> Pair<Version, String> searchUpdateFor(final Pair<Version, String> latest, final List<T> updateSources,
                                                      final VersionComparator versionComparator, final UpdateSourceConsumer<T> consumer) {
        Pair<Version, String> currentLatest = latest;
        for (final T updateSource : updateSources) {
            try {
                for (final Map.Entry<Version, String> entry : consumer.consume(updateSource).entrySet()) {
                    if (versionComparator.isOtherNewerThanCurrent(currentLatest.getKey(), entry.getKey())) {
                        currentLatest = Pair.of(entry.getKey(), entry.getValue());
                    }
                }
            } catch (final UnknownHostException e) {
                log.error("Could not fetch update versions. Maybe the host is currently not available: " + e.getMessage());
            } catch (final IOException e) {
                log.error("Could not fetch versions from source: " + e.getMessage(), e);
            }
        }
        return currentLatest;
    }

    /**
     * A functional interface that consumes a source and returns a map with the versions and download links.
     *
     * @param <T> the type of the source. Either {@link ReleaseUpdateSource} or {@link DevUpdateSource}.
     */
    @FunctionalInterface
    private interface UpdateSourceConsumer<T> {
        /**
         * Consumes the given source and returns a map with the versions and download links.
         *
         * @param source the source that should be consumed
         * @return a map with the versions and download links
         * @throws IOException is thrown if any problem occurred while fetching the versions
         */
        Map<Version, String> consume(T source) throws IOException;
    }
}
