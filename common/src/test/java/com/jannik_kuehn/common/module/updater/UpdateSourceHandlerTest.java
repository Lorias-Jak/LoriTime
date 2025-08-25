package com.jannik_kuehn.common.module.updater;

import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.module.updater.download.sources.DevUpdateSource;
import com.jannik_kuehn.common.module.updater.download.sources.ReleaseUpdateSource;
import com.jannik_kuehn.common.module.updater.version.Strategy;
import com.jannik_kuehn.common.module.updater.version.Version;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UpdateSourceHandler}.
 */
class UpdateSourceHandlerTest {

    private static LoriTimeLogger mockedLogger() {
        return Mockito.mock(LoriTimeLogger.class);
    }

    @Test
    @SuppressWarnings("PMD.UnitTestContainsTooManyAsserts")
    void testSearchUpdateRelease() {
        final Version current = new Version("1.2.3");
        final ReleaseUpdateSource release = cur -> Map.of(
                new Version("1.2.4"), "https://example.com/1.2.4",
                new Version("1.3.0"), "https://example.com/1.3.0"
        );
        final LoriTimeLogger logger = Mockito.mock(LoriTimeLogger.class);
        final UpdateSourceHandler handler = new UpdateSourceHandler(logger, List.of(release), List.of());

        final Pair<Version, String> latest = handler.searchUpdate(Strategy.MINOR, "", current);
        assertNotNull(latest, "The latest version should not be null");
        assertEquals("1.3.0", latest.getLeft().getVersionString(), "The Version should be the same ('1.3.0')");
        assertEquals("https://example.com/1.3.0", latest.getRight(), "The URL should be the same ('https://example.com/1.3.0')");
    }

    @Test
    void testSearchUpdateDev() {
        final Version current = new Version("1.2.3-DEV-5");
        final ReleaseUpdateSource none = cur -> new HashMap<>();
        final DevUpdateSource dev = cur -> Map.of(new Version("1.2.3-DEV-7"), "https://example.com/1.2.3-DEV-7");

        final UpdateSourceHandler handler = new UpdateSourceHandler(mockedLogger(), List.of(none), List.of(dev));
        final Pair<Version, String> latest = handler.searchUpdate(Strategy.MINOR, "DEV-", current);
        assertEquals("1.2.3-DEV-7", latest.getLeft().getVersionString(), "The Version should be the same ('1.2.3-DEV-7')");
        assertEquals("https://example.com/1.2.3-DEV-7", latest.getRight(), "The URL should be the same ('https://example.com/1.2.3-DEV-7')");
    }

    @Test
    void testSearchUpdateWithFallback() {
        final Version current = new Version("1.0.0");
        final ReleaseUpdateSource faulty = new ReleaseUpdateSource() {
            @Override
            public Map<Version, String> getReleaseVersions(final Version currentVersion) throws IOException {
                throw new UnknownHostException("host down");
            }
        };
        final ReleaseUpdateSource working = cur -> Map.of(new Version("1.0.1"), "ok");

        final UpdateSourceHandler handler = new UpdateSourceHandler(mockedLogger(), List.of(faulty, working), List.of());
        final Pair<Version, String> latest = handler.searchUpdate(Strategy.MINOR, "", current);
        assertEquals("1.0.1", latest.getLeft().getVersionString(), "The Version should be the same ('1.0.1')");
        assertEquals("ok", latest.getRight(), "The URL should be the same ('ok')");
    }
}
