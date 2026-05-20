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
     * Returns the current total online time for a player.
     *
     * @param player the player identity.
     * @return the total online time, if LoriTime has stored time for the player.
     */
    public Optional<Duration> getOnlineTime(final LoriTimePlayer player) {
        return getOnlineTime(validate(player, "player").getUniqueId());
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
     * Adds a signed manual time adjustment using the stable API actor.
     *
     * @param player the player identity.
     * @param amount the signed amount to add or remove.
     */
    public void addTime(final LoriTimePlayer player, final Duration amount) {
        addTime(validate(player, "player").getUniqueId(), amount);
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
        if (actorName.isBlank()) {
            throw new IllegalArgumentException("actorName must not be blank");
        }
        final long seconds = seconds(amount);
        try {
            plugin.getStorage().addTime(new ManualTimeAdjustment(uniqueId, seconds,
                    TimeEntryReason.MANUAL_ADJUSTMENT, actorUuid, actorName));
        } catch (final StorageException ex) {
            throw new LoriTimeApiException("Could not add time adjustment for UUID " + uniqueId, ex);
        }
    }

    /**
     * Adds a signed manual time adjustment with actor player metadata.
     *
     * @param player the target player identity.
     * @param amount the signed amount to add or remove.
     * @param actor  the actor player identity.
     */
    public void addTime(final LoriTimePlayer player, final Duration amount, final LoriTimePlayer actor) {
        final LoriTimePlayer validPlayer = validate(player, "player");
        final LoriTimePlayer validActor = validate(actor, "actor");
        addTime(validPlayer.getUniqueId(), amount, validActor.getUniqueId(), validActor.getName());
    }

    private LoriTimePlayer validate(final LoriTimePlayer player, final String parameterName) {
        Objects.requireNonNull(player, parameterName);
        Objects.requireNonNull(player.getUniqueId(), parameterName + ".uniqueId");
        final String name = Objects.requireNonNull(player.getName(), parameterName + ".name");
        if (name.isBlank()) {
            throw new IllegalArgumentException(parameterName + ".name must not be blank");
        }
        return player;
    }

    private long seconds(final Duration amount) {
        Objects.requireNonNull(amount, "amount");
        if (amount.getNano() != 0) {
            throw new IllegalArgumentException("amount must be precise to whole seconds");
        }
        return amount.getSeconds();
    }
}
