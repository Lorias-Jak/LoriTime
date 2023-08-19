package com.jannik_kuehn.loritime.api;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation independent representation of a Minecraft player. The UUID ist the only needed identifier, but may be
 * extended with the actual players name, if available. As representation of the player the more user-friendly variant
 * is chosen, meaning that the players name will be used as long as available, but if unavailable the uuid.
 */
public final class PlayerData {

    private final UUID uuid;
    private final String name;
    private final String representation;

    /**
     * Create a player data object, using the name as representation as long as available or else the uuid.
     *
     * @param uuid the players UUID
     * @param name the players name if known
     */
    public PlayerData(UUID uuid, String name) {
        this.uuid = Objects.requireNonNull(uuid);
        this.name = name;
        this.representation = null != name ? name : uuid.toString();
    }

    /**
     * Get the players UUID.
     *
     * @return players uuid
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Get the players name if available.
     *
     * @return players name
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Get a user-friendly representation of this player. This is the player name if available and otherwise the uuid.
     *
     * @return players name if available, else players uuid
     */
    public String getRepresentation() {
        return representation;
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "uuid=" + uuid +
                ", name=" + name +
                '}';
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        PlayerData that = (PlayerData) other;
        return getUuid().equals(that.getUuid());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getUuid());
    }
}
