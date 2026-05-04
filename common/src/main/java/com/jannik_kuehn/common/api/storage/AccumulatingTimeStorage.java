package com.jannik_kuehn.common.api.storage;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.exception.StorageException;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("PMD.CommentRequired")
public class AccumulatingTimeStorage implements UnifiedStorage, TimeAccumulator {

    private final WrappedLogger log;

    private final UnifiedStorage storage;

    private final ConcurrentMap<UUID, PlayerSessionContext> onlineSessions = new ConcurrentHashMap<>();

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
    public OptionalLong getTime(final UUID uniqueId) throws StorageException {
        final PlayerSessionContext context = onlineSessions.get(uniqueId);
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
        final PlayerSessionContext present = onlineSessions.computeIfPresent(uuid, (key, value) ->
                new PlayerSessionContext(value.uuid(), value.name(), value.server(), value.world(),
                        value.startedAtMs() - additionalTime * 1000));
        if (null == present) {
            storage.addTime(uuid, additionalTime, reason);
        }
    }

    @Override
    public void addTimes(final Map<UUID, Long> additionalTimes, final TimeEntryReason reason) throws StorageException {
        final Map<UUID, Long> directWrite = new HashMap<>();
        for (final Map.Entry<UUID, Long> entry : additionalTimes.entrySet()) {
            final PlayerSessionContext present = onlineSessions.computeIfPresent(entry.getKey(), (key, value) ->
                    new PlayerSessionContext(value.uuid(), value.name(), value.server(), value.world(),
                            value.startedAtMs() - entry.getValue() * 1000));
            if (null == present) {
                directWrite.put(entry.getKey(), entry.getValue());
            }
        }
        storage.addTimes(directWrite, reason);
    }

    @Override
    public Map<String, ?> getAllTimeEntries() throws StorageException {
        return storage.getAllTimeEntries();
    }

    @Override
    public void removePlayer(final UUID uniqueId) throws StorageException, SQLException {
        onlineSessions.remove(uniqueId);
        storage.removePlayer(uniqueId);
    }

    @Override
    public void persistSession(final PlayerSessionChunk session) throws StorageException {
        storage.persistSession(session);
    }

    @Override
    public void startAccumulating(final UUID uuid, final String name, final String server, final String world, final long when)
            throws StorageException {
        final PlayerSessionContext context = new PlayerSessionContext(uuid, name, server, world, when);
        final PlayerSessionContext previous = onlineSessions.put(uuid, context);
        if (previous != null) {
            storage.persistSession(PlayerSessionChunk.from(previous, when, TimeEntryReason.CONTEXT_SWITCH));
        }
    }

    @Override
    public void stopAccumulatingAndSaveOnlineTime(final UUID uuid, final String server, final String world,
                                                  final long when, final TimeEntryReason reason) throws StorageException {
        final PlayerSessionContext context = onlineSessions.remove(uuid);
        if (context != null) {
            storage.persistSession(PlayerSessionChunk.from(context, when, reason));
        }
    }

    @Override
    public void switchContext(final UUID uuid, final String name, final String server, final String world, final long when)
            throws StorageException {
        final PlayerSessionContext next = new PlayerSessionContext(uuid, name, server, world, when);
        final PlayerSessionContext previous = onlineSessions.put(uuid, next);
        if (previous != null) {
            storage.persistSession(PlayerSessionChunk.from(previous, when, TimeEntryReason.CONTEXT_SWITCH));
        }
    }

    @Override
    public void flushOnlineTimeCache() throws StorageException {
        if (onlineSessions.isEmpty()) {
            return;
        }
        log.debug("Flushing online time cache");
        final long now = System.currentTimeMillis();
        for (final Map.Entry<UUID, PlayerSessionContext> entry : onlineSessions.entrySet()) {
            final UUID uuid = entry.getKey();
            final PlayerSessionContext current = entry.getValue();
            if (current != null) {
                final PlayerSessionContext next = new PlayerSessionContext(current.uuid(), current.name(), current.server(), current.world(), now);
                if (onlineSessions.replace(uuid, current, next)) {
                    storage.persistSession(PlayerSessionChunk.from(current, now, TimeEntryReason.AUTO_FLUSH));
                }
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
                    final PlayerSessionContext context = onlineSessions.remove(uuid);
                    if (context != null) {
                        storage.persistSession(PlayerSessionChunk.from(context, now, TimeEntryReason.SHUTDOWN_FLUSH));
                    }
                }
            }
        } finally {
            this.storage.close();
        }
    }
}
