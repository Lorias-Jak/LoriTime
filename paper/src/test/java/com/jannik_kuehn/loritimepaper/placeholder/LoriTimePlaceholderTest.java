package com.jannik_kuehn.loritimepaper.placeholder;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.jannik_kuehn.common.LoriTimePlugin;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;

import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoriTimePlaceholderTest {

    private static final UUID PLAYER_ID = UUID.fromString("44174cf6-e76c-4994-899c-3387284ecd62");

    @Test
    void returnsCachedTimeAndRequestsRefresh() {
        final TestCache cache = new TestCache(OptionalLong.of(42L));
        final LoriTimePlaceholder placeholder = new LoriTimePlaceholder(plugin(), cache);

        assertEquals("42", placeholder.onRequest(onlinePlayer(), "unformatted_onlinetime"),
                "Expected cached placeholder time");
        assertEquals(1, cache.refreshRequests.get(), "Expected placeholder render to request a refresh");
    }

    @Test
    void returnsZeroOnCacheMissAndRequestsRefresh() {
        final TestCache cache = new TestCache(OptionalLong.empty());
        final LoriTimePlaceholder placeholder = new LoriTimePlaceholder(plugin(), cache);

        assertEquals("0", placeholder.onRequest(onlinePlayer(), "unformatted_onlinetime"),
                "Expected deterministic fallback on cache miss");
        assertEquals(1, cache.refreshRequests.get(), "Expected placeholder cache miss to request a refresh");
    }

    @Test
    void returnsZeroForOfflinePlayerWithoutRefresh() {
        final TestCache cache = new TestCache(OptionalLong.of(42L));
        final LoriTimePlaceholder placeholder = new LoriTimePlaceholder(plugin(), cache);

        assertEquals("0", placeholder.onRequest(offlinePlayer(), "unformatted_onlinetime"),
                "Expected offline player to use zero-time fallback");
        assertEquals(0, cache.refreshRequests.get(), "Expected offline player to skip refresh");
    }

    @Test
    void returnsZeroForMissingPlayerWithoutRefresh() {
        final TestCache cache = new TestCache(OptionalLong.of(42L));
        final LoriTimePlaceholder placeholder = new LoriTimePlaceholder(plugin(), cache);

        assertEquals("0", placeholder.onRequest(null, "unformatted_onlinetime"),
                "Expected missing player to use zero-time fallback");
        assertEquals(0, cache.refreshRequests.get(), "Expected missing player to skip refresh");
    }

    private LoriTimePlugin plugin() {
        final LoriTimePlugin plugin = mock(LoriTimePlugin.class);
        when(plugin.getLoggerFactory()).thenReturn(new LoggerFactory(Logger.getLogger("test")));
        return plugin;
    }

    private OfflinePlayer onlinePlayer() {
        final OfflinePlayer player = player();
        when(player.isOnline()).thenReturn(true);
        return player;
    }

    private OfflinePlayer offlinePlayer() {
        final OfflinePlayer player = player();
        when(player.isOnline()).thenReturn(false);
        return player;
    }

    private OfflinePlayer player() {
        final OfflinePlayer player = mock(OfflinePlayer.class);
        when(player.getUniqueId()).thenReturn(PLAYER_ID);
        return player;
    }

    private record TestCache(OptionalLong time, AtomicInteger refreshRequests) implements PlaceholderTimeCache {

        private TestCache(final OptionalLong time) {
            this(time, new AtomicInteger());
        }

        @Override
        public OptionalLong getCachedTime(final UUID uniqueId) {
            return time;
        }

        @Override
        public void requestRefresh(final UUID uniqueId) {
            refreshRequests.incrementAndGet();
        }
    }
}
