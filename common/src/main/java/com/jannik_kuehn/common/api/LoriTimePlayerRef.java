package com.jannik_kuehn.common.api;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable public player reference for LoriTime API calls.
 *
 * @param uniqueId the player UUID.
 * @param name     the latest known player name.
 */
public record LoriTimePlayerRef(UUID uniqueId, String name) implements LoriTimePlayer {

    /**
     * Creates a player reference.
     *
     * @param uniqueId the player UUID.
     * @param name     the latest known player name.
     */
    public LoriTimePlayerRef {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    @Override
    public UUID getUniqueId() {
        return uniqueId;
    }

    @Override
    public String getName() {
        return name;
    }
}
