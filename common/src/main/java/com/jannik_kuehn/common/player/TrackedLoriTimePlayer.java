package com.jannik_kuehn.common.player;

import com.jannik_kuehn.common.api.LoriTimePlayer;

import java.util.Objects;
import java.util.UUID;

/**
 * Internal mutable player state used for LoriTime runtime tracking.
 */
public class TrackedLoriTimePlayer implements LoriTimePlayer {
    /**
     * The UUID of the player.
     */
    private final UUID uuid;

    /**
     * The name of the player.
     */
    private final String name;

    /**
     * The AFK status of the player.
     */
    private boolean afkStatus;

    /**
     * The time when the player was last resumed.
     */
    private long lastResumeTime;

    /**
     * Creates a tracked player.
     *
     * @param uuid the player UUID.
     * @param name the player name.
     */
    public TrackedLoriTimePlayer(final UUID uuid, final String name) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.name = Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        this.afkStatus = false;
        this.lastResumeTime = System.currentTimeMillis();
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * Gets the AFK status of the player.
     *
     * @return true if the player is AFK.
     */
    public boolean isAfk() {
        return afkStatus;
    }

    /**
     * Sets the AFK status of the player.
     *
     * @param afk true if the player is AFK.
     */
    public void setAfk(final boolean afk) {
        this.afkStatus = afk;
    }

    /**
     * Sets the last resume time of the player to the current time.
     */
    public void setLastResumeTime() {
        this.lastResumeTime = System.currentTimeMillis();
    }

    /**
     * Gets the last resume time of the player.
     *
     * @return the last resume time.
     */
    public long getLastResumeTime() {
        return lastResumeTime;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final TrackedLoriTimePlayer that = (TrackedLoriTimePlayer) obj;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
