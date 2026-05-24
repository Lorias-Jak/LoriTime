package com.jannik_kuehn.common.api.storage;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.exception.StorageException;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Unified storage decorator that keeps active sessions in memory while persisting session rows.
 */
@SuppressWarnings("PMD.TooManyMethods")
public class AccumulatingTimeStorage implements UnifiedStorage, TimeAccumulator {

    /**
     * Logger for accumulator operations.
     */
    private final WrappedLogger log;

    /**
     * Backing storage that owns persistence.
     */
    private final UnifiedStorage storage;

    /**
     * Active persisted sessions keyed by player UUID.
     */
    private final ConcurrentMap<UUID, PersistedPlayerSession> onlineSessions = new ConcurrentHashMap<>();

    /**
     * Creates a new accumulating storage wrapper.
     *
     * @param log the logger.
     * @param timeStorage the backing storage.
     */
    public AccumulatingTimeStorage(final WrappedLogger log, final UnifiedStorage timeStorage) {
        this.log = log;
        this.storage = Objects.requireNonNull(timeStorage);
    }

    @Override
    public Optional<UUID> getUuid(final String playerName) throws StorageException {
        return storage.getUuid(playerName);
    }

    @Override
    public Optional<String> getName(final UUID uniqueId) throws StorageException {
        return storage.getName(uniqueId);
    }

    @Override
    public void setPlayerName(final UUID uniqueId, final String name) throws StorageException {
        storage.setPlayerName(uniqueId, name);
    }

    @Override
    public void setPlayerNames(final Map<UUID, String> entries) throws StorageException {
        storage.setPlayerNames(entries);
    }

    @Override
    public HashSet<String> getNameEntries() throws StorageException {
        return new HashSet<>(storage.getNameEntries());
    }

    @Override
    public List<RecentPlayerIdentity> getRecentPlayerIdentities(final long recentDays) throws StorageException {
        return storage.getRecentPlayerIdentities(recentDays);
    }

    @Override
    public OptionalLong getTime(final UUID uniqueId) throws StorageException {
        final PersistedPlayerSession activeSession = onlineSessions.get(uniqueId);
        final PlayerSessionContext context = activeSession == null ? null : activeSession.context();
        if (context != null) {
            final long accumulatedTime = (System.currentTimeMillis() - context.startedAtMs()) / 1000L;
            final long storedTime = storage.getTime(uniqueId).orElse(0);
            return OptionalLong.of(accumulatedTime + storedTime);
        } else {
            return storage.getTime(uniqueId);
        }
    }

    @Override
    public void addTime(final UUID uuid, final long additionalTime, final TimeEntryReason reason) throws StorageException {
        storage.addTime(uuid, additionalTime, reason);
    }

    @Override
    public void addTime(final ManualTimeAdjustment adjustment) throws StorageException {
        storage.addTime(adjustment);
    }

    @Override
    public void addTimes(final Map<UUID, Long> additionalTimes, final TimeEntryReason reason) throws StorageException {
        storage.addTimes(new HashMap<>(additionalTimes), reason);
    }

    @Override
    public void addAdjustments(final List<ManualTimeAdjustment> adjustments) throws StorageException {
        storage.addAdjustments(adjustments);
    }

    @Override
    public Map<String, ?> getAllTimeEntries() throws StorageException {
        return storage.getAllTimeEntries();
    }

    @Override
    public long startSession(final PlayerSessionContext context, final TimeEntryReason reason) throws StorageException {
        return storage.startSession(context, reason);
    }

    @Override
    public void updateSession(final long sessionId, final long stoppedAtMs, final TimeEntryReason reason) throws StorageException {
        storage.updateSession(sessionId, stoppedAtMs, reason);
    }

    @Override
    public void updateSessionWorld(final long sessionId, final String server, final String world) throws StorageException {
        storage.updateSessionWorld(sessionId, server, world);
    }

    @Override
    public void deletePlayer(final UUID uniqueId) throws StorageException, SQLException {
        onlineSessions.remove(uniqueId);
        storage.deletePlayer(uniqueId);
    }

    @Override
    public int deleteInactiveHistory(final long inactiveDays) throws StorageException {
        return storage.deleteInactiveHistory(inactiveDays);
    }

    @Override
    public void persistSession(final PlayerSessionChunk session) throws StorageException {
        storage.persistSession(session);
    }

    @Override
    public void startAccumulating(final UUID uuid, final String name, final String server, final String world, final long when)
            throws StorageException {
        final PlayerSessionContext context = new PlayerSessionContext(uuid, name, server, world, when);
        final long sessionId = storage.startSession(context, TimeEntryReason.PLAYER_JOIN);
        final PersistedPlayerSession previous = onlineSessions.put(uuid, new PersistedPlayerSession(sessionId, context));
        if (previous != null) {
            storage.updateSession(previous.sessionId(), when, switchReason(previous.context(), context));
        }
    }

    @Override
    public void stopAccumulatingAndSaveOnlineTime(final UUID uuid, final long when, final TimeEntryReason reason)
            throws StorageException {
        final PersistedPlayerSession session = onlineSessions.remove(uuid);
        if (session != null) {
            storage.updateSession(session.sessionId(), when, reason);
        }
    }

    @Override
    public void switchContext(final UUID uuid, final String name, final String server, final String world, final long when)
            throws StorageException {
        final PlayerSessionContext next = new PlayerSessionContext(uuid, name, server, world, when);
        final PersistedPlayerSession current = onlineSessions.get(uuid);
        if (current != null
                && current.context().server().equals(server)
                && current.context().world().equals(world)) {
            return;
        }
        final long sessionId = storage.startSession(next, TimeEntryReason.PLAYER_JOIN);
        final PersistedPlayerSession previous = onlineSessions.put(uuid, new PersistedPlayerSession(sessionId, next));
        if (previous != null) {
            storage.updateSession(previous.sessionId(), when, switchReason(previous.context(), next));
        }
    }

    @Override
    public void updateWorldContext(final UUID uuid, final String world, final long observedAtMs) throws StorageException {
        final PersistedPlayerSession current = onlineSessions.get(uuid);
        if (current == null || current.context().world().equals(world) || observedAtMs < current.context().startedAtMs()) {
            return;
        }
        final PlayerSessionContext previous = current.context();
        final PlayerSessionContext updated = new PlayerSessionContext(previous.uuid(), previous.name(),
                previous.server(), world, previous.startedAtMs());
        if (onlineSessions.replace(uuid, current, new PersistedPlayerSession(current.sessionId(), updated))) {
            storage.updateSessionWorld(current.sessionId(), updated.server(), updated.world());
        }
    }

    @Override
    public void switchWorldContext(final UUID uuid, final String world, final long observedAtMs) throws StorageException {
        final PersistedPlayerSession current = onlineSessions.get(uuid);
        if (current == null || current.context().world().equals(world) || observedAtMs < current.context().startedAtMs()) {
            return;
        }
        final PlayerSessionContext previous = current.context();
        final PlayerSessionContext next = new PlayerSessionContext(previous.uuid(), previous.name(),
                previous.server(), world, observedAtMs);
        final long sessionId = storage.startSession(next, TimeEntryReason.PLAYER_JOIN);
        if (onlineSessions.replace(uuid, current, new PersistedPlayerSession(sessionId, next))) {
            storage.updateSession(current.sessionId(), observedAtMs, TimeEntryReason.WORLD_SWITCH);
        }
    }

    @Override
    public void flushOnlineTimeCache() throws StorageException {
        if (onlineSessions.isEmpty()) {
            return;
        }
        log.debug("Flushing online time cache");
        final long now = System.currentTimeMillis();
        for (final Map.Entry<UUID, PersistedPlayerSession> entry : onlineSessions.entrySet()) {
            final UUID uuid = entry.getKey();
            final PersistedPlayerSession current = entry.getValue();
            if (current != null && onlineSessions.replace(uuid, current, current)) {
                storage.updateSession(current.sessionId(), now, TimeEntryReason.AUTO_FLUSH);
            }
        }
    }

    @Override
    @SuppressWarnings("PMD.UseTryWithResources")
    public void close() throws StorageException {
        try {
            if (!onlineSessions.isEmpty()) {
                final long now = System.currentTimeMillis();
                for (final UUID uuid : new HashSet<>(onlineSessions.keySet())) {
                    final PersistedPlayerSession session = onlineSessions.remove(uuid);
                    if (session != null) {
                        storage.updateSession(session.sessionId(), now, TimeEntryReason.SHUTDOWN_FLUSH);
                    }
                }
            }
        } finally {
            this.storage.close();
        }
    }

    private TimeEntryReason switchReason(final PlayerSessionContext previous, final PlayerSessionContext next) {
        if (!previous.server().equals(next.server())) {
            return TimeEntryReason.SERVER_SWITCH;
        }
        return TimeEntryReason.WORLD_SWITCH;
    }
}
