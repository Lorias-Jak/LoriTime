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
import java.util.concurrent.CompletableFuture;

/**
 * Stable public facade for third-party LoriTime integrations.
 */
@SuppressWarnings("PMD.TooManyMethods")
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
     * @return future for the UUID, if LoriTime knows the player.
     */
    public CompletableFuture<Optional<UUID>> findUuid(final String playerName) {
        Objects.requireNonNull(playerName, "playerName");
        return supplyAsync("Could not look up UUID for player name " + playerName,
                () -> plugin.getStorage().getUuid(playerName));
    }

    /**
     * Looks up the latest stored player name for a UUID.
     *
     * @param uniqueId the player UUID.
     * @return future for the player name, if LoriTime knows the player.
     */
    public CompletableFuture<Optional<String>> findName(final UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        return supplyAsync("Could not look up player name for UUID " + uniqueId,
                () -> plugin.getStorage().getName(uniqueId));
    }

    /**
     * Returns the current total online time for a player.
     *
     * @param uniqueId the player UUID.
     * @return future for the total online time, if LoriTime has stored time for the player.
     */
    public CompletableFuture<Optional<Duration>> getOnlineTime(final UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        return supplyAsync("Could not query online time for UUID " + uniqueId, () -> {
            final OptionalLong seconds = plugin.getStorage().getTime(uniqueId);
            return seconds.isPresent() ? Optional.of(Duration.ofSeconds(seconds.getAsLong())) : Optional.empty();
        });
    }

    /**
     * Returns the current total online time for a player.
     *
     * @param player the player identity.
     * @return future for the total online time, if LoriTime has stored time for the player.
     */
    public CompletableFuture<Optional<Duration>> getOnlineTime(final LoriTimePlayer player) {
        return getOnlineTime(validate(player, "player").getUniqueId());
    }

    /**
     * Adds a signed manual time adjustment using the stable API actor.
     *
     * @param uniqueId the player UUID.
     * @param amount   the signed amount to add or remove.
     */
    public CompletableFuture<Void> addTime(final UUID uniqueId, final Duration amount) {
        return addTime(uniqueId, amount, null, API_ACTOR);
    }

    /**
     * Adds a signed manual time adjustment using the stable API actor.
     *
     * @param player the player identity.
     * @param amount the signed amount to add or remove.
     */
    public CompletableFuture<Void> addTime(final LoriTimePlayer player, final Duration amount) {
        return addTime(validate(player, "player").getUniqueId(), amount);
    }

    /**
     * Adds a signed manual time adjustment with actor metadata.
     *
     * @param uniqueId  the player UUID.
     * @param amount    the signed amount to add or remove.
     * @param actorUuid the actor UUID, or null for system actors.
     * @param actorName the actor display name.
     */
    public CompletableFuture<Void> addTime(final UUID uniqueId, final Duration amount, final UUID actorUuid,
                                           final String actorName) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(actorName, "actorName");
        if (actorName.isBlank()) {
            throw new IllegalArgumentException("actorName must not be blank");
        }
        final long seconds = seconds(amount);
        return runAsync("Could not add time adjustment for UUID " + uniqueId, () ->
            plugin.getStorage().addTime(new ManualTimeAdjustment(uniqueId, seconds,
                    TimeEntryReason.MANUAL_ADJUSTMENT, actorUuid, actorName)));
    }

    /**
     * Adds a signed manual time adjustment with actor player metadata.
     *
     * @param player the target player identity.
     * @param amount the signed amount to add or remove.
     * @param actor  the actor player identity.
     */
    public CompletableFuture<Void> addTime(final LoriTimePlayer player, final Duration amount,
                                           final LoriTimePlayer actor) {
        final LoriTimePlayer validPlayer = validate(player, "player");
        final LoriTimePlayer validActor = validate(actor, "actor");
        return addTime(validPlayer.getUniqueId(), amount, validActor.getUniqueId(), validActor.getName());
    }

    private CompletableFuture<Void> runAsync(final String failureMessage, final StorageRunnable action) {
        return supplyAsync(failureMessage, () -> {
            action.run();
            return null;
        });
    }

    private <T> CompletableFuture<T> supplyAsync(final String failureMessage, final StorageSupplier<T> supplier) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        plugin.getScheduler().runAsyncOnce(() -> {
            try {
                future.complete(supplier.get());
            } catch (final StorageException ex) {
                future.completeExceptionally(new LoriTimeApiException(failureMessage, ex));
            }
        });
        return future;
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

    /**
     * Storage value supplier that may fail.
     *
     * @param <T> result type
     */
    @FunctionalInterface
    private interface StorageSupplier<T> {
        /**
         * Gets the value from storage.
         *
         * @return the storage result
         * @throws StorageException if storage access fails
         */
        T get() throws StorageException;
    }

    /**
     * Storage action that may fail.
     */
    @FunctionalInterface
    private interface StorageRunnable {
        /**
         * Runs the storage action.
         *
         * @throws StorageException if storage access fails
         */
        void run() throws StorageException;
    }
}
