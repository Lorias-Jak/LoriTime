package com.jannik_kuehn.common.module.updater;

import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.module.updater.download.sources.DevUpdateSource;
import com.jannik_kuehn.common.module.updater.download.sources.ReleaseUpdateSource;
import com.jannik_kuehn.common.module.updater.version.Strategy;
import com.jannik_kuehn.common.module.updater.version.Version;
import com.jannik_kuehn.common.module.updater.version.VersionComparator;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class UpdateSourceHandler {

    private final LoriTimeLogger log;

    private final List<ReleaseUpdateSource> releaseUpdateSources;

    private final List<DevUpdateSource> developmentUpdateSources;

    public UpdateSourceHandler(LoriTimeLogger log, List<ReleaseUpdateSource> releaseUpdateSources, List<DevUpdateSource> developmentUpdateSources) {
        this.log = log;
        this.releaseUpdateSources = releaseUpdateSources;
        this.developmentUpdateSources = developmentUpdateSources;
    }

    public Pair<Version, String> searchUpdate(Strategy updateStrategy, final String indicator, final Version currentVersion) {
        VersionComparator versionComparator = new VersionComparator(updateStrategy, indicator);
        Pair<Version, String> latestVersion = Pair.of(currentVersion, null);
        latestVersion = searchUpdateFor(latestVersion, releaseUpdateSources, versionComparator,
                releaseUpdateSources -> releaseUpdateSources.getReleaseVersions(currentVersion));
        if (currentVersion.hasQualifier() && currentVersion.getQualifier().equals("-DEV")) {
            if (latestVersion.getValue() == null) {
                latestVersion = searchUpdateFor(latestVersion, developmentUpdateSources, versionComparator,
                        developmentUpdateSources -> developmentUpdateSources.getDevelopmentVersions(currentVersion));
            }
        }
        return latestVersion;
    }

    private <T> Pair<Version, String> searchUpdateFor(final Pair<Version, String> latest, final List<T> updateSources,
                                                      final VersionComparator versionComparator, final UpdateSourceConsumer<T> consumer) {
        Pair<Version, String> currentLatest = latest;
        for (final T updateSource : updateSources) {
            try {
                for (final Map.Entry<Version, String> entry : consumer.consume(updateSource).entrySet()) {
                    if (versionComparator.isOtherNewerThanCurrent(latest.getKey(), entry.getKey())) {
                        currentLatest = Pair.of(entry.getKey(), entry.getValue());
                    }
                }
            } catch (Exception e) {
                log.error("Could not fetch versions from source: " + e.getMessage(), e);
            }
        }
        return currentLatest;
    }

    @FunctionalInterface
    private interface UpdateSourceConsumer<T> {
        Map<Version, String> consume(T source) throws IOException;
    }
}
