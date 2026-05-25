package com.jannik_kuehn.common.command;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayerConverter;
import com.jannik_kuehn.common.api.common.CommonPlayerSender;
import com.jannik_kuehn.common.api.common.CommonServer;
import com.jannik_kuehn.common.api.storage.RecentPlayerIdentity;
import com.jannik_kuehn.common.api.storage.UnifiedStorage;
import com.jannik_kuehn.common.command.completion.RecentPlayerSuggestionCache;
import com.jannik_kuehn.common.command.completion.ScopeSuggestionCache;
import com.jannik_kuehn.common.config.localization.Localization;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.utils.TimeParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoriTimeCompletionTest {

    private static final UUID PLAYER_ID = UUID.fromString("44174cf6-e76c-4994-899c-3387284ecd62");

    @Test
    void loriTimeTabCompletionUsesCachedAndOnlineNamesWithoutStorageLookup() throws StorageException {
        final CompletionContext context = new CompletionContext();
        final CommonPlayerSender onlinePlayer = mock(CommonPlayerSender.class);
        when(context.plugin.getKnownPlayerNames()).thenReturn(Set.of("Lorias_"));
        when(context.server.getOnlinePlayers()).thenReturn(new CommonPlayerSender[]{onlinePlayer});
        when(onlinePlayer.getName()).thenReturn("OnlineUser");
        when(context.source.hasPermission("loritime.see.other")).thenReturn(true);

        final List<String> completions = new LoriTimeCommand(context.plugin, context.localization).handleTabComplete(context.source, "L");

        assertEquals(List.of("Lorias_"), completions, "Expected tab completion to use cached player names");
        verifyNoSuggestionStorageLookup(context.storage);
    }

    @Test
    void modifyTabCompletionUsesCachedAndOnlineNamesWithoutStorageLookup() throws StorageException {
        final CompletionContext context = new CompletionContext();
        final CommonPlayerSender onlinePlayer = mock(CommonPlayerSender.class);
        when(context.plugin.getKnownPlayerNames()).thenReturn(Set.of("Lorias_"));
        when(context.server.getOnlinePlayers()).thenReturn(new CommonPlayerSender[]{onlinePlayer});
        when(onlinePlayer.getName()).thenReturn("OnlineUser");
        when(context.source.hasPermission("loritime.admin")).thenReturn(true);

        final List<String> completions = new LoriTimeModifyCommand(context.plugin, context.localization,
                mock(TimeParser.class)).handleTabComplete(context.source, "modify", "L");

        assertEquals(List.of("Lorias_"), completions, "Expected modify tab completion to use cached player names");
        verifyNoSuggestionStorageLookup(context.storage);
    }

    @Test
    void loriTimeTabCompletionUsesRecentPlayerSuggestionCache() throws StorageException {
        final CompletionContext context = new CompletionContext();
        final RecentPlayerSuggestionCache cache = new RecentPlayerSuggestionCache();
        cache.replaceRecentIdentities(List.of(new RecentPlayerIdentity(PLAYER_ID, "Lorias_", Optional.empty())));
        when(context.plugin.getRecentPlayerSuggestionCache()).thenReturn(cache);
        when(context.server.getOnlinePlayers()).thenReturn(new CommonPlayerSender[0]);
        when(context.source.hasPermission("loritime.see.other")).thenReturn(true);

        final List<String> completions = new LoriTimeCommand(context.plugin, context.localization).handleTabComplete(context.source, "L");

        assertEquals(List.of("Lorias_"), completions, "Expected tab completion to use the recent player cache");
        verifyNoSuggestionStorageLookup(context.storage);
    }

    @ParameterizedTest
    @MethodSource("longScopePrefixCompletionArguments")
    void loriTimeTabCompletionSuggestsOnlyLongScopePrefixes(final String argument,
                                                            final List<String> expected,
                                                            final String message) {
        final CompletionContext context = new CompletionContext();
        when(context.plugin.getKnownPlayerNames()).thenReturn(Set.of());
        when(context.plugin.getRecentPlayerSuggestionCache()).thenReturn(null);
        when(context.server.getOnlinePlayers()).thenReturn(new CommonPlayerSender[0]);
        when(context.source.hasPermission("loritime.see.server")).thenReturn(true);
        when(context.source.hasPermission("loritime.see.world")).thenReturn(true);

        final LoriTimeCommand command = new LoriTimeCommand(context.plugin, context.localization);

        assertEquals(expected, command.handleTabComplete(context.source, argument), message);
    }

    @Test
    void loriTimeTabCompletionUsesLiveServerCandidatesWithoutStorageLookup() throws StorageException {
        final CompletionContext context = new CompletionContext();
        when(context.server.getLiveServerNames()).thenReturn(List.of("survival", "creative"));

        final LoriTimeCommand command = new LoriTimeCommand(context.plugin, context.localization);

        assertEquals(List.of("server:survival"), command.handleTabComplete(context.source, "server:su"),
                "Expected server flag value completion from live runtime context");
        verifyNoSuggestionStorageLookup(context.storage);
    }

    @Test
    void loriTimeTabCompletionUsesLiveWorldCandidatesWithoutStorageLookup() throws StorageException {
        final CompletionContext context = new CompletionContext();
        when(context.server.getLiveWorldNames(Optional.of("survival"), Optional.empty()))
                .thenReturn(List.of("world", "world_nether"));

        final LoriTimeCommand command = new LoriTimeCommand(context.plugin, context.localization);

        assertEquals(List.of("world:world", "world:world_nether"),
                command.handleTabComplete(context.source, "server:survival", "world:wo"),
                "Expected world flag value completion from live runtime context");
        verifyNoSuggestionStorageLookup(context.storage);
    }

    @Test
    void loriTimeTabCompletionUsesCachedServerCandidatesForShortFlags() throws StorageException {
        final CompletionContext context = new CompletionContext();
        context.scopeCache.replaceStoredNames(Set.of("survival", "creative"), Set.of("world", "world_nether"));
        when(context.server.getLiveServerNames()).thenReturn(List.of());

        final LoriTimeCommand command = new LoriTimeCommand(context.plugin, context.localization);

        assertEquals(List.of("s:survival"), command.handleTabComplete(context.source, "s:su"),
                "Expected short server flag values from the cached scope names");
        verifyNoSuggestionStorageLookup(context.storage);
    }

    @Test
    void loriTimeTabCompletionUsesCachedWorldCandidatesForShortFlags() throws StorageException {
        final CompletionContext context = new CompletionContext();
        context.scopeCache.replaceStoredNames(Set.of("survival", "creative"), Set.of("world", "world_nether"));
        when(context.server.getLiveWorldNames(Optional.of("survival"), Optional.empty())).thenReturn(List.of());

        final LoriTimeCommand command = new LoriTimeCommand(context.plugin, context.localization);

        assertEquals(List.of("w:world", "w:world_nether"), command.handleTabComplete(context.source, "s:survival", "w:wo"),
                "Expected short world flag values from the cached scope names");
        verifyNoSuggestionStorageLookup(context.storage);
    }

    @Test
    void loriTimeTabCompletionSuggestsLongTimePrefixOnly() {
        final CompletionContext context = new CompletionContext();
        when(context.plugin.getKnownPlayerNames()).thenReturn(Set.of());
        when(context.plugin.getRecentPlayerSuggestionCache()).thenReturn(null);
        when(context.server.getOnlinePlayers()).thenReturn(new CommonPlayerSender[0]);

        final LoriTimeCommand command = new LoriTimeCommand(context.plugin, context.localization);

        assertEquals(List.of("time:"), command.handleTabComplete(context.source, "t"),
                "Expected long time prefix completion");
    }

    @Test
    void loriTimeTabCompletionDoesNotSuggestTimeValuesOrDuplicateTimeFlag() {
        final CompletionContext context = new CompletionContext();
        when(context.plugin.getKnownPlayerNames()).thenReturn(Set.of());
        when(context.plugin.getRecentPlayerSuggestionCache()).thenReturn(null);
        when(context.server.getOnlinePlayers()).thenReturn(new CommonPlayerSender[0]);

        final LoriTimeCommand command = new LoriTimeCommand(context.plugin, context.localization);

        assertEquals(List.of(), command.handleTabComplete(context.source, "time:3"),
                "Expected no custom time range value suggestions");
        assertEquals(List.of(), command.handleTabComplete(context.source, "t:3d", "t"),
                "Expected no duplicate time flag suggestion");
    }

    private void verifyNoSuggestionStorageLookup(final UnifiedStorage storage) throws StorageException {
        verify(storage, never()).getNameEntries();
        verify(storage, never()).getRecentPlayerIdentities(anyLong());
        verify(storage, never()).getKnownServerNames();
        verify(storage, never()).getKnownWorldNames();
    }

    private static Stream<Arguments> longScopePrefixCompletionArguments() {
        return Stream.of(
                Arguments.of("s", List.of("server:"), "Expected long server prefix completion"),
                Arguments.of("w", List.of("world:"), "Expected long world prefix completion"),
                Arguments.of("s:", List.of(), "Expected short server alias to stay unsuggested")
        );
    }

    private static final class CompletionContext {

        private final LoriTimePlugin plugin = mock(LoriTimePlugin.class);

        private final UnifiedStorage storage = mock(UnifiedStorage.class);

        private final Localization localization = mock(Localization.class);

        private final CommonServer server = mock(CommonServer.class);

        private final CommonPlayerSender source = mock(CommonPlayerSender.class);

        private final ScopeSuggestionCache scopeCache = new ScopeSuggestionCache();

        private CompletionContext() {
            when(plugin.getLoggerFactory()).thenReturn(new LoggerFactory(Logger.getLogger("test")));
            when(plugin.getStorage()).thenReturn(storage);
            when(plugin.getLocalization()).thenReturn(localization);
            when(plugin.getServer()).thenReturn(server);
            when(plugin.getPlayerConverter()).thenReturn(mock(LoriTimePlayerConverter.class));
            when(plugin.getScopeSuggestionCache()).thenReturn(scopeCache);
        }
    }
}
