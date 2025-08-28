package com.jannik_kuehn.common.api;

import java.util.Objects;
import java.util.UUID;

/**
 * The {@link LoriTimePlayer} class represents a player in the LoriTime system.
 */
public class LoriTimePlayer {
    /**
     * The {@link UUID} of the player.
     */
    private final UUID uuid;

    /**
     * The name of the player.
     */
    private final String name;

    /**
     * The AFK status of the player.
     * {@code true} if the player is AFK, otherwise {@code false}.
     */
    private boolean afkStatus;

    /**
     * The time when the player was last resumed.
     */
    private long lastResumeTime;

    /**
     * Creates a new {@link LoriTimePlayer} instance.
     *
     * @param uuid The {@link UUID} of the player.
     * @param name The name of the player.
     */
    public LoriTimePlayer(final UUID uuid, final String name) {
        this.uuid = uuid;
        this.name = name;
        this.afkStatus = false;
        this.lastResumeTime = System.currentTimeMillis();
    }

    /**
     * Gets the {@link UUID} of the player.
     *
     * @return The {@link UUID} of the player.
     */
    public UUID getUniqueId() {
        return uuid;
    }

    /**
     * Gets the name of the player.
     *
     * @return The name of the player.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the AFK status of the player.
     *
     * @return {@code true} if the player is AFK, otherwise {@code false}.
     */
    public boolean isAfk() {
        return afkStatus;
    }

    /**
     * Sets the AFK status of the player.
     *
     * @param afk {@code true} if the player is AFK, otherwise {@code false}.
     */
    public void setAFk(final boolean afk) {
        this.afkStatus = afk;
    }

    /**
     * Sets the last resume time of the player to the current time.
     * Uses the {@link System#currentTimeMillis()} method to get the current time.
     */
    public void setLastResumeTime() {
        this.lastResumeTime = System.currentTimeMillis();
    }

    /**
     * Gets the last resume time of the player.
     *
     * @return The last resume time of the player.
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
        final LoriTimePlayer that = (LoriTimePlayer) obj;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, name);
    }
}
