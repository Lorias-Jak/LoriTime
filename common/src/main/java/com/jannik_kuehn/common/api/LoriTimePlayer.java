package com.jannik_kuehn.common.api;

import java.util.UUID;

/**
 * Stable public identity contract for a LoriTime player.
 */
public interface LoriTimePlayer {

    /**
     * Gets the UUID of the player.
     *
     * @return the UUID of the player.
     */
    UUID getUniqueId();

    /**
     * Gets the latest known name of the player.
     *
     * @return the latest known name of the player.
     */
    String getName();
}
