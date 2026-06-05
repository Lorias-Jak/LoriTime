package com.jannik_kuehn.common.storage.model;

import com.jannik_kuehn.common.api.storage.TimeScope;

import java.util.Optional;
import java.util.UUID;

/**
 * Signed manual time adjustment with audit actor context.
 *
 * @param playerUuid    adjusted player UUID
 * @param amountSeconds signed adjustment amount in seconds
 * @param reason        adjustment reason
 * @param actorUuid     optional actor UUID
 * @param actorName     actor display name
 * @param scope         adjustment scope
 */
public record ManualTimeAdjustment(UUID playerUuid, long amountSeconds, TimeEntryReason reason,
                                   Optional<UUID> actorUuid, String actorName, TimeScope scope) {

    /**
     * Compact constructor.
     *
     * @param playerUuid    adjusted player UUID
     * @param amountSeconds signed adjustment amount in seconds
     * @param reason        adjustment reason
     * @param actorUuid     optional actor UUID
     * @param actorName     actor display name
     * @param scope         adjustment scope
     */
    public ManualTimeAdjustment {
        scope = scope == null ? TimeScope.GLOBAL : scope;
    }

    /**
     * Creates a player actor adjustment.
     *
     * @param playerUuid    adjusted player UUID
     * @param amountSeconds signed adjustment amount in seconds
     * @param reason        adjustment reason
     * @param actorUuid     actor UUID
     * @param actorName     actor name
     */
    public ManualTimeAdjustment(final UUID playerUuid, final long amountSeconds, final TimeEntryReason reason,
                                final UUID actorUuid, final String actorName) {
        this(playerUuid, amountSeconds, reason, Optional.ofNullable(actorUuid), actorName, TimeScope.GLOBAL);
    }

    /**
     * Creates a player actor adjustment.
     *
     * @param playerUuid    adjusted player UUID
     * @param amountSeconds signed adjustment amount in seconds
     * @param reason        adjustment reason
     * @param actorUuid     actor UUID
     * @param actorName     actor name
     * @param scope         adjustment scope
     */
    public ManualTimeAdjustment(final UUID playerUuid, final long amountSeconds, final TimeEntryReason reason,
                                final UUID actorUuid, final String actorName, final TimeScope scope) {
        this(playerUuid, amountSeconds, reason, Optional.ofNullable(actorUuid), actorName, scope);
    }

    /**
     * Creates a system actor adjustment.
     *
     * @param playerUuid    adjusted player UUID
     * @param amountSeconds signed adjustment amount in seconds
     * @param reason        adjustment reason
     * @param actorName     actor name
     */
    public ManualTimeAdjustment(final UUID playerUuid, final long amountSeconds, final TimeEntryReason reason,
                                final String actorName) {
        this(playerUuid, amountSeconds, reason, Optional.empty(), actorName, TimeScope.GLOBAL);
    }

    /**
     * Creates a system actor adjustment.
     *
     * @param playerUuid    adjusted player UUID
     * @param amountSeconds signed adjustment amount in seconds
     * @param reason        adjustment reason
     * @param actorName     actor name
     * @param scope         adjustment scope
     */
    public ManualTimeAdjustment(final UUID playerUuid, final long amountSeconds, final TimeEntryReason reason,
                                final String actorName, final TimeScope scope) {
        this(playerUuid, amountSeconds, reason, Optional.empty(), actorName, scope);
    }
}
