package com.jannik_kuehn.common.api;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.storage.ManualTimeAdjustment;
import com.jannik_kuehn.common.api.storage.TimeEntryReason;
import com.jannik_kuehn.common.exception.StorageException;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Stable public facade for third-party LoriTime integrations.
 */
public final class LoriTimeService {

    /**
     * Actor name used for API adjustments without an explicit actor.
     */
    public static final String API_ACTOR = "API";

    /**
     * The backing LoriTime plugin.
     */
    private final LoriTimePlugin plugin;

    /**
     * Creates a new public API facade.
     *
     * @param plugin the backing LoriTime plugin.
     */
    /* default */ LoriTimeService(final LoriTimePlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Looks up a stored UUID by player name.
     *
     * @param playerName the player name.
     * @return the UUID, if LoriTime knows the player.
     */
    public Optional<UUID> findUuid(final String playerName) {
        Objects.requireNonNull(playerName, "playerName");
        try {
            return plugin.getStorage().getUuid(playerName);
        } catch (final StorageException ex) {
            throw new LoriTimeApiException("Could not look up UUID for player name " + playerName, ex);
        }
    }

    /**
     * Looks up the latest stored player name for a UUID.
     *
     * @param uniqueId the player UUID.
     * @return the player name, if LoriTime knows the player.
     */
    public Optional<String> findName(final UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        try {
            return plugin.getStorage().getName(uniqueId);
        } catch (final StorageException ex) {
            throw new LoriTimeApiException("Could not look up player name for UUID " + uniqueId, ex);
        }
    }

    /**
     * Returns the current total online time for a player.
     *
     * @param uniqueId the player UUID.
     * @return the total online time, if LoriTime has stored time for the player.
     */
    public Optional<Duration> getOnlineTime(final UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        try {
            final OptionalLong seconds = plugin.getStorage().getTime(uniqueId);
            return seconds.isPresent() ? Optional.of(Duration.ofSeconds(seconds.getAsLong())) : Optional.empty();
        } catch (final StorageException ex) {
            throw new LoriTimeApiException("Could not query online time for UUID " + uniqueId, ex);
        }
    }

    /**
     * Adds a signed manual time adjustment using the stable API actor.
     *
     * @param uniqueId the player UUID.
     * @param amount   the signed amount to add or remove.
     */
    public void addTime(final UUID uniqueId, final Duration amount) {
        addTime(uniqueId, amount, null, API_ACTOR);
    }

    /**
     * Adds a signed manual time adjustment with actor metadata.
     *
     * @param uniqueId  the player UUID.
     * @param amount    the signed amount to add or remove.
     * @param actorUuid the actor UUID, or null for system actors.
     * @param actorName the actor display name.
     */
    public void addTime(final UUID uniqueId, final Duration amount, final UUID actorUuid, final String actorName) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(actorName, "actorName");
        final long seconds = seconds(amount);
        try {
            plugin.getStorage().addTime(new ManualTimeAdjustment(uniqueId, seconds,
                    TimeEntryReason.MANUAL_ADJUSTMENT, actorUuid, actorName));
        } catch (final StorageException ex) {
            throw new LoriTimeApiException("Could not add time adjustment for UUID " + uniqueId, ex);
        }
    }

    private long seconds(final Duration amount) {
        Objects.requireNonNull(amount, "amount");
        if (amount.getNano() != 0) {
            throw new IllegalArgumentException("amount must be precise to whole seconds");
        }
        return amount.getSeconds();
    }
}
