package com.jannik_kuehn.common.command;

import com.github.roleplaycauldron.spellbook.core.logger.LoggerFactory;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.api.storage.TimeScope;
import com.jannik_kuehn.common.command.core.CommandScopes;
import com.jannik_kuehn.common.config.localization.Localization;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.module.updater.Updater;
import com.jannik_kuehn.common.platform.CommonConsoleSender;
import com.jannik_kuehn.common.platform.CommonPlayerSender;
import com.jannik_kuehn.common.platform.CommonSender;
import com.jannik_kuehn.common.platform.CommonServer;
import com.jannik_kuehn.common.player.LoriTimePlayerConverter;
import com.jannik_kuehn.common.player.TrackedLoriTimePlayer;
import com.jannik_kuehn.common.scheduler.PluginScheduler;
import com.jannik_kuehn.common.scheduler.PluginTask;
import com.jannik_kuehn.common.storage.DataStorageManager;
import com.jannik_kuehn.common.storage.contract.AdminStorageMaintenance;
import com.jannik_kuehn.common.storage.contract.TimeAccumulator;
import com.jannik_kuehn.common.storage.contract.UnifiedStorage;
import com.jannik_kuehn.common.storage.database.DatabaseStorage;
import com.jannik_kuehn.common.storage.model.ManualTimeAdjustment;
import com.jannik_kuehn.common.storage.model.PlayerStorageTransferRequest;
import com.jannik_kuehn.common.storage.model.StorageDeleteRequest;
import com.jannik_kuehn.common.storage.model.StorageMaintenanceOperation;
import com.jannik_kuehn.common.storage.model.StorageMaintenancePreview;
import com.jannik_kuehn.common.storage.model.StorageMaintenanceResult;
import com.jannik_kuehn.common.storage.model.StorageMaintenanceScope;
import com.jannik_kuehn.common.storage.model.StorageTransferMapping;
import com.jannik_kuehn.common.storage.model.StorageTransferRequest;
import com.jannik_kuehn.common.storage.model.TimeEntryReason;
import com.jannik_kuehn.common.utils.TimeParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"PMD.CouplingBetweenObjects", "PMD.GodClass", "PMD.UnitTestContainsTooManyAsserts"})
class SenderRoleCommandTest {

    private static final UUID PLAYER_ID = UUID.fromString("44174cf6-e76c-4994-899c-3387284ecd62");

    private static Stream<Arguments> validLookupArguments() {
        return Stream.of(
                Arguments.of(new CommandScopes.LookupRequest(null, null, null), new String[0],
                        "Expected empty lookup to target global self time"),
                Arguments.of(new CommandScopes.LookupRequest("Lorias_", null, null), new String[]{"Lorias_"},
                        "Expected single non-flag token to target a player"),
                Arguments.of(new CommandScopes.LookupRequest(null, "survival", null), new String[]{"server:survival"},
                        "Expected long server flag to parse"),
                Arguments.of(new CommandScopes.LookupRequest(null, "survival", null), new String[]{"s:survival"},
                        "Expected short server flag to parse"),
                Arguments.of(new CommandScopes.LookupRequest("Lorias_", "survival", "world"),
                        new String[]{"world:world", "Lorias_", "server:survival"},
                        "Expected mixed-order world, player, and server flags to parse"),
                Arguments.of(new CommandScopes.LookupRequest(null, null, "world"), new String[]{"w:world"},
                        "Expected short world-only flag to parse")
        );
    }

    private static Stream<Arguments> invalidLookupArguments() {
        return Stream.of(
                Arguments.of(new String[]{"server:survival", "s:creative"}, "Expected duplicate server flags to fail"),
                Arguments.of(new String[]{"world:"}, "Expected empty world value to fail"),
                Arguments.of(new String[]{"scope:value"}, "Expected unknown flag-like token to fail"),
                Arguments.of(new String[]{"Lorias_", "OtherPlayer"}, "Expected multiple player tokens to fail"),
                Arguments.of(new String[]{"server", "survival"}, "Expected legacy server syntax to fail")
        );
    }

    private static Stream<Arguments> invalidTimeRangeArguments() {
        return Stream.of(
                Arguments.of(new String[]{"time:"}, "Expected empty time range to fail"),
                Arguments.of(new String[]{"time:nope"}, "Expected unparsable time range to fail"),
                Arguments.of(new String[]{"time:0d"}, "Expected zero time range to fail"),
                Arguments.of(new String[]{"time:-3d"}, "Expected negative time range to fail"),
                Arguments.of(new String[]{"time:4w-3d"}, "Expected reversed time range to fail"),
                Arguments.of(new String[]{"time:3d", "t:4d"}, "Expected duplicate time range to fail")
        );
    }

    private static TimeParser parser() {
        return new TimeParser.Builder()
                .addUnit(60, "m")
                .addUnit(60 * 60 * 24, "d")
                .addUnit(60 * 60 * 24 * 7, "w")
                .addUnit(60 * 60 * 24 * 30, "mo")
                .build();
    }

    private static Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-05-25T00:00:00Z"), ZoneOffset.UTC);
    }

    @ParameterizedTest
    @MethodSource("validLookupArguments")
    void lookupParserAcceptsFlaggedScopeForms(final CommandScopes.LookupRequest expected,
                                              final String[] arguments, final String message) {
        assertEquals(expected, CommandScopes.parseLookup(arguments), message);
    }

    @ParameterizedTest
    @MethodSource("invalidLookupArguments")
    void lookupParserRejectsInvalidFlaggedScopeForms(final String[] arguments, final String message) {
        assertNull(CommandScopes.parseLookup(arguments), message);
    }

    @Test
    void lookupParserAcceptsSingleDurationTimeRange() {
        final CommandScopes.LookupRequest request = CommandScopes.parseLookup(parser(), fixedClock(), "time:8mo");

        assertEquals(TimeRange.between(Instant.parse("2025-09-27T00:00:00Z"), Instant.parse("2026-05-25T00:00:00Z")),
                request.timeRange(), "Expected single duration to resolve from now backwards");
    }

    @Test
    void lookupParserAcceptsNearToFarTimeRange() {
        final CommandScopes.LookupRequest request = CommandScopes.parseLookup(parser(), fixedClock(),
                "Lorias_", "t:3d-4w", "server:survival");

        assertEquals(new CommandScopes.LookupRequest("Lorias_", "survival", null,
                TimeRange.between(Instant.parse("2026-04-27T00:00:00Z"), Instant.parse("2026-05-22T00:00:00Z")),
                "3d-4w"), request, "Expected near-to-far range to parse with player and scope");
    }

    @ParameterizedTest
    @MethodSource("invalidTimeRangeArguments")
    void lookupParserRejectsInvalidTimeRangeForms(final String[] arguments, final String message) {
        assertNull(CommandScopes.parseLookup(parser(), fixedClock(), arguments), message);
    }

    @Test
    void playerSenderTargetsOwnLoriTimeQuery() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonPlayerSender sender = mock(CommonPlayerSender.class);
        when(sender.hasPermission("loritime.see")).thenReturn(true);
        when(sender.getUniqueId()).thenReturn(PLAYER_ID);
        when(context.storage().getTime(PLAYER_ID, TimeScope.GLOBAL)).thenReturn(OptionalLong.of(45L));
        when(context.playerConverter().getOnlinePlayer(PLAYER_ID))
                .thenReturn(new TrackedLoriTimePlayer(PLAYER_ID, "Lorias_"));

        new LoriTimeCommand(context.plugin(), context.localization()).execute(sender);

        verify(context.playerConverter()).getOnlinePlayer(PLAYER_ID);
        verify(sender).sendMessage(any(TextComponent.class));
    }

    @Test
    void consoleSenderCannotTargetOwnLoriTimeQuery() {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        when(sender.hasPermission("loritime.see")).thenReturn(true);

        new LoriTimeCommand(context.plugin(), context.localization()).execute(sender);

        verify(context.playerConverter(), never()).getOnlinePlayer(any(UUID.class));
        verify(sender).sendMessage(any(TextComponent.class));
    }

    @Test
    void playerSenderCanQueryOwnServerScopedTime() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonPlayerSender sender = mock(CommonPlayerSender.class);
        when(sender.hasPermission("loritime.see.server")).thenReturn(true);
        when(sender.getUniqueId()).thenReturn(PLAYER_ID);
        when(context.storage().getTime(PLAYER_ID, TimeScope.server("survival"))).thenReturn(OptionalLong.of(45L));
        when(context.playerConverter().getOnlinePlayer(PLAYER_ID))
                .thenReturn(new TrackedLoriTimePlayer(PLAYER_ID, "Lorias_"));

        new LoriTimeCommand(context.plugin(), context.localization()).execute(sender, "server:survival");

        verify(context.storage()).getTime(PLAYER_ID, TimeScope.server("survival"));
        verify(sender).sendMessage(any(TextComponent.class));
    }

    @Test
    void playerSenderCanQueryOwnRangedServerScopedTime() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonPlayerSender sender = mock(CommonPlayerSender.class);
        when(sender.hasPermission("loritime.see.server")).thenReturn(true);
        when(sender.hasPermission("loritime.see.timerange")).thenReturn(true);
        when(sender.getUniqueId()).thenReturn(PLAYER_ID);
        when(context.storage().getTime(eq(PLAYER_ID), eq(TimeScope.server("survival")), any(TimeRange.class)))
                .thenReturn(OptionalLong.of(45L));
        when(context.playerConverter().getOnlinePlayer(PLAYER_ID))
                .thenReturn(new TrackedLoriTimePlayer(PLAYER_ID, "Lorias_"));

        new LoriTimeCommand(context.plugin(), context.localization()).execute(sender, "server:survival", "time:3d-4w");

        verify(context.storage()).getTime(eq(PLAYER_ID), eq(TimeScope.server("survival")), any(TimeRange.class));
        verify(sender, atLeastOnce()).sendMessage(any(TextComponent.class));
    }

    @Test
    void playerSenderCannotQueryOwnRangedTimeWithoutTimeRangePermission() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonPlayerSender sender = mock(CommonPlayerSender.class);
        when(sender.hasPermission("loritime.see")).thenReturn(true);
        when(sender.getUniqueId()).thenReturn(PLAYER_ID);
        when(context.playerConverter().getOnlinePlayer(PLAYER_ID))
                .thenReturn(new TrackedLoriTimePlayer(PLAYER_ID, "Lorias_"));

        new LoriTimeCommand(context.plugin(), context.localization()).execute(sender, "time:3d");

        verify(context.storage(), never()).getTime(eq(PLAYER_ID), eq(TimeScope.GLOBAL), any(TimeRange.class));
        verify(sender, atLeastOnce()).sendMessage(any(TextComponent.class));
    }

    @Test
    void otherRangedLookupRequiresOtherTimeRangePermission() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        when(sender.hasPermission("loritime.see.other")).thenReturn(true);
        when(sender.hasPermission("loritime.see.timerange.other")).thenReturn(true);
        when(context.storage().getUuid("Lorias_")).thenReturn(Optional.of(PLAYER_ID));
        when(context.storage().getTime(eq(PLAYER_ID), eq(TimeScope.GLOBAL), any(TimeRange.class)))
                .thenReturn(OptionalLong.of(45L));
        when(context.playerConverter().getOnlinePlayer(PLAYER_ID))
                .thenReturn(new TrackedLoriTimePlayer(PLAYER_ID, "Lorias_"));

        new LoriTimeCommand(context.plugin(), context.localization()).execute(sender, "Lorias_", "time:3d");

        verify(context.storage()).getTime(eq(PLAYER_ID), eq(TimeScope.GLOBAL), any(TimeRange.class));
        verify(sender, atLeastOnce()).sendMessage(any(TextComponent.class));
    }

    @Test
    void otherRangedLookupDeniesWithoutOtherTimeRangePermission() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        when(sender.hasPermission("loritime.see.other")).thenReturn(true);
        when(context.storage().getUuid("Lorias_")).thenReturn(Optional.of(PLAYER_ID));
        when(context.playerConverter().getOnlinePlayer(PLAYER_ID))
                .thenReturn(new TrackedLoriTimePlayer(PLAYER_ID, "Lorias_"));

        new LoriTimeCommand(context.plugin(), context.localization()).execute(sender, "Lorias_", "time:3d");

        verify(context.storage(), never()).getTime(eq(PLAYER_ID), eq(TimeScope.GLOBAL), any(TimeRange.class));
        verify(sender, atLeastOnce()).sendMessage(any(TextComponent.class));
    }

    @Test
    void worldScopedOtherLookupRequiresWorldOtherPermission() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        when(sender.hasPermission("loritime.see.world.other")).thenReturn(true);
        when(context.storage().getUuid("Lorias_")).thenReturn(Optional.of(PLAYER_ID));
        when(context.storage().getTime(PLAYER_ID, TimeScope.world("survival", "world"))).thenReturn(OptionalLong.of(45L));
        when(context.playerConverter().getOnlinePlayer(PLAYER_ID))
                .thenReturn(new TrackedLoriTimePlayer(PLAYER_ID, "Lorias_"));

        new LoriTimeCommand(context.plugin(), context.localization()).execute(sender, "world:world", "server:survival", "Lorias_");

        verify(context.storage()).getTime(PLAYER_ID, TimeScope.world("survival", "world"));
        verify(sender).sendMessage(any(TextComponent.class));
    }

    @Test
    void worldScopedOtherLookupDeniesWithoutWorldOtherPermission() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        when(context.storage().getUuid("Lorias_")).thenReturn(Optional.of(PLAYER_ID));
        when(context.playerConverter().getOnlinePlayer(PLAYER_ID))
                .thenReturn(new TrackedLoriTimePlayer(PLAYER_ID, "Lorias_"));

        new LoriTimeCommand(context.plugin(), context.localization()).execute(sender, "world:world", "server:survival", "Lorias_");

        verify(context.storage(), never()).getTime(PLAYER_ID, TimeScope.world("survival", "world"));
        verify(sender).sendMessage(any(TextComponent.class));
    }

    @Test
    void worldScopedLookupDefaultsToStandaloneServerName() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonPlayerSender sender = mock(CommonPlayerSender.class);
        when(sender.hasPermission("loritime.see.world")).thenReturn(true);
        when(sender.getUniqueId()).thenReturn(PLAYER_ID);
        when(context.server().getLocalServerName()).thenReturn(Optional.of("standalone-one"));
        when(context.storage().getTime(PLAYER_ID, TimeScope.world("standalone-one", "world"))).thenReturn(OptionalLong.of(45L));
        when(context.playerConverter().getOnlinePlayer(PLAYER_ID))
                .thenReturn(new TrackedLoriTimePlayer(PLAYER_ID, "Lorias_"));

        new LoriTimeCommand(context.plugin(), context.localization()).execute(sender, "world:world");

        verify(context.storage()).getTime(PLAYER_ID, TimeScope.world("standalone-one", "world"));
        verify(sender).sendMessage(any(TextComponent.class));
    }

    @Test
    void worldScopedLookupDefaultsToProxyCurrentServer() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonPlayerSender sender = mock(CommonPlayerSender.class);
        when(sender.hasPermission("loritime.see.world")).thenReturn(true);
        when(sender.getUniqueId()).thenReturn(PLAYER_ID);
        when(context.server().isProxy()).thenReturn(true);
        when(context.server().getCurrentServer(PLAYER_ID)).thenReturn(Optional.of("minigames"));
        when(context.storage().getUuid("Lorias_")).thenReturn(Optional.of(PLAYER_ID));
        when(context.storage().getTime(PLAYER_ID, TimeScope.world("minigames", "arena"))).thenReturn(OptionalLong.of(45L));
        when(context.playerConverter().getOnlinePlayer(PLAYER_ID))
                .thenReturn(new TrackedLoriTimePlayer(PLAYER_ID, "Lorias_"));

        new LoriTimeCommand(context.plugin(), context.localization()).execute(sender, "Lorias_", "world:arena");

        verify(context.storage()).getTime(PLAYER_ID, TimeScope.world("minigames", "arena"));
        verify(sender).sendMessage(any(TextComponent.class));
    }

    @Test
    void worldScopedLookupWithoutProxyServerIsRejected() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        when(sender.hasPermission("loritime.see.world.other")).thenReturn(true);
        when(context.server().isProxy()).thenReturn(true);
        when(context.server().getCurrentServer(PLAYER_ID)).thenReturn(Optional.empty());
        when(context.storage().getUuid("Lorias_")).thenReturn(Optional.of(PLAYER_ID));
        when(context.playerConverter().getOnlinePlayer(PLAYER_ID))
                .thenReturn(new TrackedLoriTimePlayer(PLAYER_ID, "Lorias_"));

        new LoriTimeCommand(context.plugin(), context.localization()).execute(sender, "Lorias_", "world:arena");

        verify(context.storage(), never()).getTime(any(UUID.class), any(TimeScope.class));
        verify(sender).sendMessage(any(TextComponent.class));
    }

    @Test
    void adminAdjustmentUsesPlayerActorIdentity() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonPlayerSender sender = mock(CommonPlayerSender.class);
        when(sender.getUniqueId()).thenReturn(PLAYER_ID);
        when(sender.getName()).thenReturn("Lorias_");

        modifyCommand(context).modifyOnlineTime(sender, PLAYER_ID, 12L);

        verify(context.storage()).addTime(new ManualTimeAdjustment(PLAYER_ID, 12L,
                TimeEntryReason.MANUAL_ADJUSTMENT, PLAYER_ID, "Lorias_"));
    }

    @Test
    void adminAdjustmentUsesConsoleActorMetadata() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);

        modifyCommand(context).modifyOnlineTime(sender, PLAYER_ID, 12L);

        verify(context.storage()).addTime(new ManualTimeAdjustment(PLAYER_ID, 12L,
                TimeEntryReason.MANUAL_ADJUSTMENT, (UUID) null, "CONSOLE"));
    }

    @Test
    void adminCommandRoutesReload() {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "reload");
        verify(context.plugin()).reload();
    }

    @Test
    void adminCommandRoutesDebug() {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "debug");
        verify(context.plugin().getConfig()).setTemporaryValue("general.debug", true);
    }

    @Test
    void adminCommandRoutesInfo() {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final CommonServer server = mock(CommonServer.class);
        when(context.plugin().getServer()).thenReturn(server);
        when(server.getServerVersion()).thenReturn("server");
        when(server.getPluginVersion()).thenReturn("plugin");
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "info");
        verify(server).getServerVersion();
    }

    @Test
    void adminCommandRoutesUpdate() {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final Updater updater = mock(Updater.class);
        when(context.plugin().getUpdater()).thenReturn(updater);
        when(updater.isUpdateAvailable()).thenReturn(false);
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "update");
        verify(updater).isUpdateAvailable();
    }

    @Test
    void adminTransferPreviewsPlayerServerTransfer() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final AdminStorageMaintenance maintenance = prepareTransferMaintenance(context, sender);
        when(maintenance.previewPlayerTransfer(any(PlayerStorageTransferRequest.class))).thenReturn(transferPreview());
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "transfer", "Lorias_", "server:survival", "to-server:target", "time:3d");

        final ArgumentCaptor<PlayerStorageTransferRequest> requestCaptor =
                ArgumentCaptor.forClass(PlayerStorageTransferRequest.class);
        verify(maintenance).previewPlayerTransfer(requestCaptor.capture());
        verify(maintenance, never()).applyPlayerTransfer(any(), any());
        assertEquals(PLAYER_ID, requestCaptor.getValue().playerUuid(), "Expected resolved player UUID");
        assertEquals(Optional.of("Lorias_"), requestCaptor.getValue().playerName(), "Expected selected player name");
        assertEquals(StorageMaintenanceScope.server("survival"), requestCaptor.getValue().mapping().source(),
                "Expected source server");
        assertEquals(StorageMaintenanceScope.server("target"), requestCaptor.getValue().mapping().target(),
                "Expected target server");
        assertTrue(requestCaptor.getValue().timeRange().isPresent(), "Expected parsed time range");
        assertEquals(Optional.of("3d"), requestCaptor.getValue().timeRangeInput(), "Expected raw time range");
        verify(sender, atLeastOnce()).sendMessage(any(TextComponent.class));
    }

    @Test
    void adminTransferAppliesQueuedWorldTransferAfterConfirm() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final AdminStorageMaintenance maintenance = prepareTransferMaintenance(context, sender);
        final StorageMaintenancePreview preview = transferPreview();
        when(maintenance.previewPlayerTransfer(any(PlayerStorageTransferRequest.class))).thenReturn(preview);
        when(maintenance.applyPlayerTransfer(any(PlayerStorageTransferRequest.class), eq(preview.confirmation())))
                .thenReturn(new StorageMaintenanceResult(StorageMaintenanceOperation.WORLD_TRANSFER, 2L, 1L, 1L));
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "transfer", "Lorias_", "server:survival", "world:old", "to-server:target",
                "to-world:new");
        command.execute(sender, "confirm");

        final ArgumentCaptor<PlayerStorageTransferRequest> requestCaptor =
                ArgumentCaptor.forClass(PlayerStorageTransferRequest.class);
        verify(maintenance).applyPlayerTransfer(requestCaptor.capture(), eq(preview.confirmation()));
        assertEquals(StorageMaintenanceScope.world("survival", "old"), requestCaptor.getValue().mapping().source(),
                "Expected source world");
        assertEquals(StorageMaintenanceScope.world("target", "new"), requestCaptor.getValue().mapping().target(),
                "Expected target world");
        verify(sender, atLeastOnce()).sendMessage(any(TextComponent.class));
    }

    @Test
    void adminTransferPreviewsAllPlayerServerTransferWhenPlayerOmitted() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final AdminStorageMaintenance maintenance = prepareTransferMaintenance(context, sender);
        when(maintenance.previewTransfer(any(StorageTransferRequest.class))).thenReturn(transferPreview());
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "transfer", "server:survival", "to-server:target");

        final ArgumentCaptor<StorageTransferRequest> requestCaptor =
                ArgumentCaptor.forClass(StorageTransferRequest.class);
        verify(maintenance).previewTransfer(requestCaptor.capture());
        verify(maintenance, never()).previewPlayerTransfer(any(PlayerStorageTransferRequest.class));
        assertEquals(StorageMaintenanceOperation.SERVER_TRANSFER, requestCaptor.getValue().operation(),
                "Expected full server transfer");
        assertEquals(StorageMaintenanceScope.server("survival"), requestCaptor.getValue().mappings().get(0).source(),
                "Expected source server");
        assertEquals(StorageMaintenanceScope.server("target"), requestCaptor.getValue().mappings().get(0).target(),
                "Expected target server");
    }

    @Test
    void adminTransferAppliesQueuedAllPlayerWorldTransferAfterConfirm() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final AdminStorageMaintenance maintenance = prepareTransferMaintenance(context, sender);
        final StorageMaintenancePreview preview = transferPreview();
        when(maintenance.previewTransfer(any(StorageTransferRequest.class))).thenReturn(preview);
        when(maintenance.applyTransfer(any(StorageTransferRequest.class), eq(preview.confirmation())))
                .thenReturn(new StorageMaintenanceResult(StorageMaintenanceOperation.WORLD_TRANSFER, 4L, 2L, 2L));
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "transfer", "server:survival", "world:old", "to-world:new");
        command.execute(sender, "confirm");

        final ArgumentCaptor<StorageTransferRequest> requestCaptor =
                ArgumentCaptor.forClass(StorageTransferRequest.class);
        verify(maintenance).applyTransfer(requestCaptor.capture(), eq(preview.confirmation()));
        assertEquals(StorageMaintenanceOperation.WORLD_TRANSFER, requestCaptor.getValue().operation(),
                "Expected full world transfer");
        assertEquals(StorageMaintenanceScope.world("survival", "old"), requestCaptor.getValue().mappings().get(0).source(),
                "Expected source world");
        assertEquals(StorageMaintenanceScope.world("survival", "new"), requestCaptor.getValue().mappings().get(0).target(),
                "Expected target world");
    }

    @Test
    void adminTransferRejectsAllPlayerTimeRange() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final AdminStorageMaintenance maintenance = prepareTransferMaintenance(context, sender);
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "transfer", "server:survival", "to-server:target", "time:3d");

        verify(maintenance, never()).previewTransfer(any());
        verify(maintenance, never()).previewPlayerTransfer(any());
        verify(sender).sendMessage(any(TextComponent.class));
    }

    @Test
    void adminTransferDefaultsWorldSourceToLocalServer() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonPlayerSender sender = mock(CommonPlayerSender.class);
        final AdminStorageMaintenance maintenance = prepareTransferMaintenance(context, sender);
        when(sender.getUniqueId()).thenReturn(PLAYER_ID);
        when(context.server().getLocalServerName()).thenReturn(Optional.of("survival"));
        when(maintenance.previewPlayerTransfer(any(PlayerStorageTransferRequest.class))).thenReturn(transferPreview());
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "transfer", "Lorias_", "world:old", "to-world:new");

        final ArgumentCaptor<PlayerStorageTransferRequest> requestCaptor =
                ArgumentCaptor.forClass(PlayerStorageTransferRequest.class);
        verify(maintenance).previewPlayerTransfer(requestCaptor.capture());
        assertEquals(StorageMaintenanceScope.world("survival", "old"), requestCaptor.getValue().mapping().source(),
                "Expected local source world");
        assertEquals(StorageMaintenanceScope.world("survival", "new"), requestCaptor.getValue().mapping().target(),
                "Expected local target world");
    }

    @Test
    void adminTransferDefaultsWorldSourceToProxyCurrentServer() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonPlayerSender sender = mock(CommonPlayerSender.class);
        final AdminStorageMaintenance maintenance = prepareTransferMaintenance(context, sender);
        when(sender.getUniqueId()).thenReturn(PLAYER_ID);
        when(context.server().isProxy()).thenReturn(true);
        when(context.server().getCurrentServer(PLAYER_ID)).thenReturn(Optional.of("minigames"));
        when(maintenance.previewPlayerTransfer(any(PlayerStorageTransferRequest.class))).thenReturn(transferPreview());
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "transfer", "Lorias_", "world:old", "to-world:new");

        final ArgumentCaptor<PlayerStorageTransferRequest> requestCaptor =
                ArgumentCaptor.forClass(PlayerStorageTransferRequest.class);
        verify(maintenance).previewPlayerTransfer(requestCaptor.capture());
        assertEquals(StorageMaintenanceScope.world("minigames", "old"), requestCaptor.getValue().mapping().source(),
                "Expected current proxy source world");
        assertEquals(StorageMaintenanceScope.world("minigames", "new"), requestCaptor.getValue().mapping().target(),
                "Expected current proxy target world");
    }

    @Test
    void adminTransferRejectsUnresolvedProxyWorldDefault() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final AdminStorageMaintenance maintenance = prepareTransferMaintenance(context, sender);
        when(context.server().isProxy()).thenReturn(true);
        when(context.server().getCurrentServer(PLAYER_ID)).thenReturn(Optional.empty());
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "transfer", "Lorias_", "world:old", "to-world:new");

        verify(maintenance, never()).previewPlayerTransfer(any());
        verify(sender).sendMessage(any(TextComponent.class));
    }

    @Test
    void adminConfirmRejectsExpiredPendingTransfer() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final AtomicReference<Runnable> timeout = new AtomicReference<>();
        when(context.plugin().getScheduler().runAsyncOnceLater(eq(15L), any())).thenAnswer(invocation -> {
            timeout.set(invocation.getArgument(1));
            return mock(PluginTask.class);
        });
        final AdminStorageMaintenance maintenance = prepareTransferMaintenance(context, sender);
        when(maintenance.previewPlayerTransfer(any(PlayerStorageTransferRequest.class))).thenReturn(transferPreview());
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "transfer", "Lorias_", "server:survival", "to-server:target");
        timeout.get().run();
        command.execute(sender, "confirm");

        verify(maintenance, never()).applyPlayerTransfer(any(), any());
        verify(sender, atLeastOnce()).sendMessage(any(TextComponent.class));
    }

    @Test
    void adminConfirmRejectsWithoutPendingAction() {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "confirm");

        verify(sender).sendMessage(any(TextComponent.class));
    }

    @Test
    void adminTransferRejectsUnknownPlayerBeforePreview() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final AdminStorageMaintenance maintenance = mock(AdminStorageMaintenance.class);
        when(sender.hasPermission("loritime.admin")).thenReturn(true);
        when(sender.getName()).thenReturn("Console");
        when(context.plugin().ownsCanonicalStorage()).thenReturn(true);
        when(context.plugin().getAdminStorageMaintenance()).thenReturn(Optional.of(maintenance));
        when(context.storage().getUuid("Missing")).thenReturn(Optional.empty());
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "transfer", "Missing", "server:survival", "to-server:target");

        verify(maintenance, never()).previewPlayerTransfer(any());
        verify(sender).sendMessage(any(TextComponent.class));
    }

    @Test
    void adminTransferRejectsUnsupportedRuntimeAndStorage() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        when(sender.hasPermission("loritime.admin")).thenReturn(true);
        when(context.plugin().ownsCanonicalStorage()).thenReturn(false);
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "transfer", "Lorias_", "server:survival", "to-server:target");

        verify(context.storage(), never()).getUuid(anyString());

        when(context.plugin().ownsCanonicalStorage()).thenReturn(true);
        when(context.plugin().getAdminStorageMaintenance()).thenReturn(Optional.empty());
        command.execute(sender, "transfer", "Lorias_", "server:survival", "to-server:target");

        verify(context.storage(), never()).getUuid(anyString());
        verify(sender, atLeastOnce()).sendMessage(any(TextComponent.class));
    }

    @Test
    void adminDeleteHistoryPreviewsPlayerServerDeleteWithTimeRange() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final AdminStorageMaintenance maintenance = prepareTransferMaintenance(context, sender);
        when(maintenance.previewDelete(any(StorageDeleteRequest.class))).thenReturn(deletePreview());
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "deleteHistory", "Lorias_", "server:survival", "time:3d");

        final ArgumentCaptor<StorageDeleteRequest> requestCaptor =
                ArgumentCaptor.forClass(StorageDeleteRequest.class);
        verify(maintenance).previewDelete(requestCaptor.capture());
        verify(maintenance, never()).applyDelete(any(), any());
        assertEquals(StorageMaintenanceScope.server("survival"), requestCaptor.getValue().scope(),
                "Expected server delete scope");
        assertEquals(Optional.of(PLAYER_ID), requestCaptor.getValue().playerUuid(), "Expected resolved player UUID");
        assertEquals(Optional.of("Lorias_"), requestCaptor.getValue().playerName(), "Expected selected player name");
        assertTrue(requestCaptor.getValue().timeRange().isPresent(), "Expected parsed time range");
        assertEquals(Optional.of("3d"), requestCaptor.getValue().timeRangeInput(), "Expected raw time range");
        verify(sender, atLeastOnce()).sendMessage(any(TextComponent.class));
    }

    @Test
    void adminDeleteHistoryAppliesQueuedAllPlayerWorldDeleteAfterConfirm() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final AdminStorageMaintenance maintenance = prepareTransferMaintenance(context, sender);
        final StorageMaintenancePreview preview = deletePreview();
        when(maintenance.previewDelete(any(StorageDeleteRequest.class))).thenReturn(preview);
        when(maintenance.applyDelete(any(StorageDeleteRequest.class), eq(preview.confirmation())))
                .thenReturn(new StorageMaintenanceResult(StorageMaintenanceOperation.WORLD_DELETE, 2L, 1L, 1L));
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "deleteHistory", "server:survival", "world:old", "time:3d");
        command.execute(sender, "confirm");

        final ArgumentCaptor<StorageDeleteRequest> requestCaptor =
                ArgumentCaptor.forClass(StorageDeleteRequest.class);
        verify(maintenance).applyDelete(requestCaptor.capture(), eq(preview.confirmation()));
        assertEquals(StorageMaintenanceScope.world("survival", "old"), requestCaptor.getValue().scope(),
                "Expected world delete scope");
        assertTrue(requestCaptor.getValue().playerUuid().isEmpty(), "Expected all-player delete");
        assertTrue(requestCaptor.getValue().timeRange().isPresent(), "Expected all-player time range to be allowed");
        verify(sender, atLeastOnce()).sendMessage(any(TextComponent.class));
    }

    @Test
    void adminDeleteHistoryDefaultsWorldScopeToExecutorCurrentServer() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonPlayerSender sender = mock(CommonPlayerSender.class);
        final AdminStorageMaintenance maintenance = prepareTransferMaintenance(context, sender);
        when(sender.getUniqueId()).thenReturn(PLAYER_ID);
        when(context.server().isProxy()).thenReturn(true);
        when(context.server().getCurrentServer(PLAYER_ID)).thenReturn(Optional.of("minigames"));
        when(maintenance.previewDelete(any(StorageDeleteRequest.class))).thenReturn(deletePreview());
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "deleteHistory", "world:old", "time:3d");

        final ArgumentCaptor<StorageDeleteRequest> requestCaptor =
                ArgumentCaptor.forClass(StorageDeleteRequest.class);
        verify(maintenance).previewDelete(requestCaptor.capture());
        assertEquals(StorageMaintenanceScope.world("minigames", "old"), requestCaptor.getValue().scope(),
                "Expected deleteHistory world scope to use executor current server");
    }

    @Test
    void adminDeleteHistoryRejectsUnknownPlayerBeforePreview() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final AdminStorageMaintenance maintenance = mock(AdminStorageMaintenance.class);
        when(sender.hasPermission("loritime.admin")).thenReturn(true);
        when(sender.getName()).thenReturn("Console");
        when(context.plugin().ownsCanonicalStorage()).thenReturn(true);
        when(context.plugin().getAdminStorageMaintenance()).thenReturn(Optional.of(maintenance));
        when(context.storage().getUuid("Missing")).thenReturn(Optional.empty());
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "deleteHistory", "Missing", "server:survival");

        verify(maintenance, never()).previewDelete(any());
        verify(sender).sendMessage(any(TextComponent.class));
    }

    @Test
    void adminDeleteHistoryRejectsUnsupportedRuntimeAndStorage() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        when(sender.hasPermission("loritime.admin")).thenReturn(true);
        when(context.plugin().ownsCanonicalStorage()).thenReturn(false);
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "deleteHistory", "Lorias_", "server:survival");

        verify(context.storage(), never()).getUuid(anyString());

        when(context.plugin().ownsCanonicalStorage()).thenReturn(true);
        when(context.plugin().getAdminStorageMaintenance()).thenReturn(Optional.empty());
        command.execute(sender, "deleteHistory", "Lorias_", "server:survival");

        verify(context.storage(), never()).getUuid(anyString());
        verify(sender, atLeastOnce()).sendMessage(any(TextComponent.class));
    }

    @Test
    @SuppressWarnings("PMD.CloseResource")
    void adminStorageTransferPreviewsAndQueuesConfirmation() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final AdminStorageMaintenance source = prepareStorageTypeTransfer(context, sender, "mysql");
        final AdminStorageMaintenance target = mock(AdminStorageMaintenance.class);
        final DataStorageManager.StorageTransferTarget transferTarget = storageTransferTarget(target);
        when(context.plugin().getDataStorageManager().createStorageTransferTarget("sqlite")).thenReturn(transferTarget);
        when(source.previewStorageTransferTo(target)).thenReturn(storageTransferPreview(false));
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "storage", "transfer", "sqlite");

        verify(context.plugin().getAccumulator()).flushOnlineTimeCache();
        verify(source).previewStorageTransferTo(target);
        verify(source, never()).applyStorageTransferTo(any(), any());
        verify(transferTarget.databaseStorage()).shutdown();
        verify(sender, atLeastOnce()).sendMessage(any(TextComponent.class));
    }

    @Test
    @SuppressWarnings("PMD.CloseResource")
    void adminStorageTransferConfirmRecreatesTargetAndAppliesOriginalConfirmation() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final AdminStorageMaintenance source = prepareStorageTypeTransfer(context, sender, "mysql");
        final AdminStorageMaintenance previewTarget = mock(AdminStorageMaintenance.class);
        final AdminStorageMaintenance confirmTarget = mock(AdminStorageMaintenance.class);
        final DataStorageManager.StorageTransferTarget previewTransferTarget = storageTransferTarget(previewTarget);
        final DataStorageManager.StorageTransferTarget confirmTransferTarget = storageTransferTarget(confirmTarget);
        when(context.plugin().getDataStorageManager().createStorageTransferTarget("sqlite"))
                .thenReturn(previewTransferTarget, confirmTransferTarget);
        final StorageMaintenancePreview preview = storageTransferPreview(false);
        when(source.previewStorageTransferTo(previewTarget)).thenReturn(preview);
        when(source.applyStorageTransferTo(confirmTarget, preview.confirmation()))
                .thenReturn(new StorageMaintenanceResult(StorageMaintenanceOperation.STORAGE_TYPE_TRANSFER, 2L, 1L, 1L));
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "storage", "transfer", "sqlite");
        command.execute(sender, "confirm");

        verify(context.plugin().getAccumulator(), times(2)).flushOnlineTimeCache();
        verify(source).applyStorageTransferTo(confirmTarget, preview.confirmation());
        verify(previewTransferTarget.databaseStorage()).shutdown();
        verify(confirmTransferTarget.databaseStorage()).shutdown();
        verify(sender, atLeastOnce()).sendMessage(any(TextComponent.class));
    }

    @Test
    void adminStorageTransferRejectsInvalidTargetsBeforeTargetCreation() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        prepareStorageTypeTransfer(context, sender, "mysql");
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "storage", "transfer", "postgres");
        command.execute(sender, "storage", "transfer", "mysql");
        command.execute(sender, "storage", "transfer", "mariadb");

        verify(context.plugin().getDataStorageManager(), never()).createStorageTransferTarget(anyString());
        verify(context.plugin().getAccumulator(), never()).flushOnlineTimeCache();
        verify(sender, atLeast(3)).sendMessage(any(TextComponent.class));
    }

    @Test
    @SuppressWarnings("PMD.CloseResource")
    void adminStorageTransferRejectsNonEmptyTargetWithoutConfirmation() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final AdminStorageMaintenance source = prepareStorageTypeTransfer(context, sender, "mysql");
        final AdminStorageMaintenance target = mock(AdminStorageMaintenance.class);
        final DataStorageManager.StorageTransferTarget transferTarget = storageTransferTarget(target);
        when(context.plugin().getDataStorageManager().createStorageTransferTarget("sqlite")).thenReturn(transferTarget);
        when(source.previewStorageTransferTo(target)).thenReturn(storageTransferPreview(true));
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        command.execute(sender, "storage", "transfer", "sqlite");
        command.execute(sender, "confirm");

        verify(source, never()).applyStorageTransferTo(any(), any());
        verify(sender, atLeastOnce()).sendMessage(any(TextComponent.class));
    }

    @Test
    void adminStorageCompletesTransferTargets() {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        when(sender.hasPermission("loritime.admin")).thenReturn(true);
        final LoriTimeAdminCommand command = prepareAdminCommand(context, sender);

        assertEquals(List.of("transfer"), command.handleTabComplete(sender, "storage", ""),
                "storage subcommand should complete transfer");
        assertEquals(List.of("sqlite", "mysql", "mariadb"),
                command.handleTabComplete(sender, "storage", "transfer", ""),
                "storage transfer should complete supported target storage types");
    }

    @Test
    void adminDeleteHistoryRequiresAdminPermission() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        when(sender.hasPermission("loritime.admin")).thenReturn(false);

        new LoriTimeAdminCommand(context.plugin(), context.localization())
                .execute(sender, "deleteHistory", "Lorias_", "server:survival");

        verify(context.storage(), never()).getUuid(anyString());
        verify(sender).sendMessage(any(TextComponent.class));
    }

    @Test
    void adminTransferRequiresAdminPermission() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        when(sender.hasPermission("loritime.admin")).thenReturn(false);

        new LoriTimeAdminCommand(context.plugin(), context.localization())
                .execute(sender, "transfer", "Lorias_", "server:survival", "to-server:target");

        verify(context.storage(), never()).getUuid(anyString());
        verify(sender).sendMessage(any(TextComponent.class));
    }

    @Test
    void modifyCommandRoutesAddMutation() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final TimeParser parser = mock(TimeParser.class);
        prepareMutation(context, sender);
        when(context.storage().getTime(PLAYER_ID, TimeScope.GLOBAL)).thenReturn(OptionalLong.of(5L));
        when(parser.parseToSeconds("12")).thenReturn(OptionalLong.of(12L));

        new LoriTimeModifyCommand(context.plugin(), context.localization(), parser).execute(sender, "add", "Lorias_", "12");

        verify(context.storage()).addTime(new ManualTimeAdjustment(PLAYER_ID, 12L,
                TimeEntryReason.MANUAL_ADJUSTMENT, (UUID) null, "CONSOLE"));
    }

    @Test
    void modifyCommandRoutesSetMutation() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final TimeParser parser = mock(TimeParser.class);
        prepareMutation(context, sender);
        when(context.storage().getTime(PLAYER_ID, TimeScope.GLOBAL)).thenReturn(OptionalLong.of(5L));
        when(parser.parseToSeconds("20")).thenReturn(OptionalLong.of(20L));

        new LoriTimeModifyCommand(context.plugin(), context.localization(), parser).execute(sender, "set", "Lorias_", "20");

        verify(context.storage()).addTime(new ManualTimeAdjustment(PLAYER_ID, 15L,
                TimeEntryReason.MANUAL_ADJUSTMENT, (UUID) null, "CONSOLE"));
    }

    @Test
    void modifyCommandRoutesResetMutation() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        prepareMutation(context, sender);
        when(context.storage().getTime(PLAYER_ID, TimeScope.GLOBAL)).thenReturn(OptionalLong.of(5L));

        modifyCommand(context).execute(sender, "reset", "Lorias_");

        verify(context.storage()).addTime(new ManualTimeAdjustment(PLAYER_ID, -5L,
                TimeEntryReason.MANUAL_ADJUSTMENT, (UUID) null, "CONSOLE"));
    }

    @Test
    void modifyCommandRoutesServerScopedAddMutation() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final TimeParser parser = mock(TimeParser.class);
        prepareMutation(context, sender);
        when(context.storage().getTime(PLAYER_ID, TimeScope.server("survival"))).thenReturn(OptionalLong.of(5L));
        when(parser.parseToSeconds("12")).thenReturn(OptionalLong.of(12L));

        new LoriTimeModifyCommand(context.plugin(), context.localization(), parser)
                .execute(sender, "add", "Lorias_", "12", "server:survival");

        verify(context.storage()).addTime(new ManualTimeAdjustment(PLAYER_ID, 12L,
                TimeEntryReason.MANUAL_ADJUSTMENT, (UUID) null, "CONSOLE", TimeScope.server("survival")));
    }

    @Test
    void modifyCommandRoutesWorldScopedSetMutation() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final TimeParser parser = mock(TimeParser.class);
        prepareMutation(context, sender);
        when(context.storage().getTime(PLAYER_ID, TimeScope.world("survival", "world"))).thenReturn(OptionalLong.of(5L));
        when(parser.parseToSeconds("20")).thenReturn(OptionalLong.of(20L));

        new LoriTimeModifyCommand(context.plugin(), context.localization(), parser)
                .execute(sender, "set", "Lorias_", "20", "world:world", "server:survival");

        verify(context.storage()).addTime(new ManualTimeAdjustment(PLAYER_ID, 15L,
                TimeEntryReason.MANUAL_ADJUSTMENT, (UUID) null, "CONSOLE", TimeScope.world("survival", "world")));
    }

    @Test
    void modifyCommandRoutesServerScopedResetMutation() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        prepareMutation(context, sender);
        when(context.storage().getTime(PLAYER_ID, TimeScope.server("survival"))).thenReturn(OptionalLong.of(5L));

        modifyCommand(context).execute(sender, "reset", "Lorias_", "server:survival");

        verify(context.storage()).addTime(new ManualTimeAdjustment(PLAYER_ID, -5L,
                TimeEntryReason.MANUAL_ADJUSTMENT, (UUID) null, "CONSOLE", TimeScope.server("survival")));
    }

    @Test
    void modifyCommandRoutesBackendWorldOnlyAddMutation() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonPlayerSender sender = mock(CommonPlayerSender.class);
        final TimeParser parser = mock(TimeParser.class);
        prepareMutation(context, sender);
        when(sender.getUniqueId()).thenReturn(PLAYER_ID);
        when(sender.getName()).thenReturn("Lorias_");
        when(context.server().getLocalServerName()).thenReturn(Optional.of("standalone-one"));
        when(context.storage().getTime(PLAYER_ID, TimeScope.world("standalone-one", "arena"))).thenReturn(OptionalLong.of(5L));
        when(parser.parseToSeconds("12")).thenReturn(OptionalLong.of(12L));

        new LoriTimeModifyCommand(context.plugin(), context.localization(), parser)
                .execute(sender, "add", "Lorias_", "12", "world:arena");

        verify(context.storage()).addTime(new ManualTimeAdjustment(PLAYER_ID, 12L,
                TimeEntryReason.MANUAL_ADJUSTMENT, PLAYER_ID, "Lorias_", TimeScope.world("standalone-one", "arena")));
    }

    @Test
    void modifyCommandRoutesProxyWorldOnlyResetMutation() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonPlayerSender sender = mock(CommonPlayerSender.class);
        prepareMutation(context, sender);
        when(sender.getUniqueId()).thenReturn(PLAYER_ID);
        when(sender.getName()).thenReturn("Lorias_");
        when(context.server().isProxy()).thenReturn(true);
        when(context.server().getCurrentServer(PLAYER_ID)).thenReturn(Optional.of("minigames"));
        when(context.storage().getTime(PLAYER_ID, TimeScope.world("minigames", "arena"))).thenReturn(OptionalLong.of(5L));

        modifyCommand(context).execute(sender, "reset", "Lorias_", "world:arena");

        verify(context.storage()).addTime(new ManualTimeAdjustment(PLAYER_ID, -5L,
                TimeEntryReason.MANUAL_ADJUSTMENT, PLAYER_ID, "Lorias_", TimeScope.world("minigames", "arena")));
    }

    @Test
    void modifyCommandRejectsUnresolvedProxyWorldOnlyScope() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final TimeParser parser = mock(TimeParser.class);
        prepareMutation(context, sender);
        when(context.server().isProxy()).thenReturn(true);
        when(context.server().getCurrentServer(PLAYER_ID)).thenReturn(Optional.empty());

        new LoriTimeModifyCommand(context.plugin(), context.localization(), parser)
                .execute(sender, "add", "Lorias_", "12", "world:arena");

        verify(context.storage(), never()).getTime(any(UUID.class), any(TimeScope.class));
        verify(context.storage(), never()).addTime(any(ManualTimeAdjustment.class));
    }

    @Test
    void modifyCommandRejectsLegacyPositionalScopeSyntax() throws StorageException {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final TimeParser parser = mock(TimeParser.class);
        prepareMutation(context, sender);

        new LoriTimeModifyCommand(context.plugin(), context.localization(), parser)
                .execute(sender, "add", "Lorias_", "12", "server", "survival");
        modifyCommand(context).execute(sender, "reset", "Lorias_", "world", "survival", "world");

        verify(context.storage(), never()).getTime(any(UUID.class), any(TimeScope.class));
        verify(context.storage(), never()).addTime(any(ManualTimeAdjustment.class));
    }

    @Test
    void modifyCommandRoutesDeleteUser() throws Exception {
        final CommandContext context = new CommandContext();
        final CommonConsoleSender sender = mock(CommonConsoleSender.class);
        final CommonServer server = mock(CommonServer.class);
        prepareMutation(context, sender);
        when(context.plugin().getServer()).thenReturn(server);
        when(server.getPlayer(PLAYER_ID)).thenReturn(Optional.empty());

        modifyCommand(context).execute(sender, "deleteUser", "Lorias_", "confirm");

        verify(context.storage()).deletePlayer(PLAYER_ID);
    }

    private void prepareMutation(final CommandContext context, final CommonSender sender) throws StorageException {
        when(sender.hasPermission("loritime.admin")).thenReturn(true);
        when(context.storage().getUuid("Lorias_")).thenReturn(Optional.of(PLAYER_ID));
        when(context.playerConverter().getOnlinePlayer(PLAYER_ID)).thenReturn(new TrackedLoriTimePlayer(PLAYER_ID, "Lorias_"));
    }

    private AdminStorageMaintenance prepareTransferMaintenance(final CommandContext context, final CommonSender sender)
            throws StorageException {
        final AdminStorageMaintenance maintenance = mock(AdminStorageMaintenance.class);
        when(sender.hasPermission("loritime.admin")).thenReturn(true);
        when(context.plugin().ownsCanonicalStorage()).thenReturn(true);
        when(context.plugin().getAdminStorageMaintenance()).thenReturn(Optional.of(maintenance));
        when(context.storage().getUuid("Lorias_")).thenReturn(Optional.of(PLAYER_ID));
        return maintenance;
    }

    private AdminStorageMaintenance prepareStorageTypeTransfer(final CommandContext context,
                                                               final CommonSender sender,
                                                               final String sourceMethod) {
        final AdminStorageMaintenance maintenance = mock(AdminStorageMaintenance.class);
        final DataStorageManager dataStorageManager = mock(DataStorageManager.class);
        final TimeAccumulator accumulator = mock(TimeAccumulator.class);
        when(sender.hasPermission("loritime.admin")).thenReturn(true);
        when(context.plugin().ownsCanonicalStorage()).thenReturn(true);
        when(context.plugin().getAdminStorageMaintenance()).thenReturn(Optional.of(maintenance));
        when(context.plugin().getDataStorageManager()).thenReturn(dataStorageManager);
        when(context.plugin().getAccumulator()).thenReturn(accumulator);
        when(context.plugin().getConfig().getString("storageMethod", "sqlite")).thenReturn(sourceMethod);
        return maintenance;
    }

    private DataStorageManager.StorageTransferTarget storageTransferTarget(final AdminStorageMaintenance maintenance)
            throws StorageException {
        final DatabaseStorage databaseStorage = mock(DatabaseStorage.class);
        doNothing().when(databaseStorage).shutdown();
        return new DataStorageManager.StorageTransferTarget(databaseStorage, maintenance);
    }

    private StorageMaintenancePreview transferPreview() {
        return new StorageMaintenancePreview(StorageMaintenanceOperation.SERVER_TRANSFER,
                List.of(new StorageTransferMapping(StorageMaintenanceScope.server("survival"),
                        StorageMaintenanceScope.server("target"))),
                null, 2L, 1L, 1L, true, List.of("target/world"), true, Optional.of(PLAYER_ID),
                Optional.of("Lorias_"), Optional.empty(), Optional.empty(), "fingerprint");
    }

    private StorageMaintenancePreview deletePreview() {
        return new StorageMaintenancePreview(StorageMaintenanceOperation.SERVER_DELETE, List.of(),
                StorageMaintenanceScope.server("survival"), 2L, 1L, 1L, false, List.of(), true,
                Optional.of(PLAYER_ID), Optional.of("Lorias_"), Optional.empty(), Optional.empty(), "delete");
    }

    private StorageMaintenancePreview storageTransferPreview(final boolean targetDataExists) {
        return new StorageMaintenancePreview(StorageMaintenanceOperation.STORAGE_TYPE_TRANSFER,
                List.of(new StorageTransferMapping(StorageMaintenanceScope.storage(),
                        StorageMaintenanceScope.storage())),
                null, 2L, 1L, 1L, targetDataExists, List.of(), true, Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), "storage-transfer");
    }

    private LoriTimeModifyCommand modifyCommand(final CommandContext context) {
        return new LoriTimeModifyCommand(context.plugin(), context.localization(), mock(TimeParser.class));
    }

    private LoriTimeAdminCommand prepareAdminCommand(final CommandContext context, final CommonSender sender) {
        when(sender.hasPermission("loritime.admin")).thenReturn(true);
        when(sender.getName()).thenReturn("Console");
        return new LoriTimeAdminCommand(context.plugin(), context.localization());
    }

    private record CommandContext(LoriTimePlugin plugin, UnifiedStorage storage, Localization localization,
                                  LoriTimePlayerConverter playerConverter, CommonServer server) {

        private CommandContext() {
            this(mock(LoriTimePlugin.class), mock(UnifiedStorage.class), mock(Localization.class),
                    mock(LoriTimePlayerConverter.class), mock(CommonServer.class));
            final PluginScheduler scheduler = mock(PluginScheduler.class);
            when(plugin().getLoggerFactory()).thenReturn(new LoggerFactory(Logger.getLogger("test")));
            when(plugin().getStorage()).thenReturn(storage());
            when(plugin().getLocalization()).thenReturn(localization());
            when(plugin().getPlayerConverter()).thenReturn(playerConverter());
            when(plugin().getScheduler()).thenReturn(scheduler);
            when(plugin().getServer()).thenReturn(server());
            when(plugin().getParser()).thenReturn(parser());
            when(server().isProxy()).thenReturn(false);
            when(server().getLocalServerName()).thenReturn(Optional.of("default"));
            when(plugin().getConfig()).thenReturn(mock(com.jannik_kuehn.common.config.Configuration.class));
            when(plugin().getDataStorageManager()).thenReturn(mock(DataStorageManager.class));
            when(plugin().getConfig().getBoolean(anyString())).thenReturn(false);
            when(plugin().getConfig().getInt("general.debugAutoDisableTime", 30)).thenReturn(-1);
            when(localization().getRawMessage(anyString())).thenReturn("[time]");
            when(localization().formatTextComponent(anyString())).thenReturn(Component.text("message"));
            doAnswer(invocation -> {
                invocation.<Runnable>getArgument(0).run();
                return mock(PluginTask.class);
            }).when(scheduler).runAsyncOnce(any());
        }

        private CommandContext(final LoriTimePlugin plugin, final UnifiedStorage storage,
                               final Localization localization, final LoriTimePlayerConverter playerConverter) {
            this(plugin, storage, localization, playerConverter, mock(CommonServer.class));
        }
    }
}
