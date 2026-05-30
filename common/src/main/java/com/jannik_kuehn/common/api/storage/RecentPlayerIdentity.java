package com.jannik_kuehn.common.api.storage;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Stored player identity used for recent-player suggestions.
 *
 * @param uuid player UUID
 * @param name latest known player name
 * @param lastSeen latest stored observation timestamp
 */
public record RecentPlayerIdentity(UUID uuid, String name, Optional<Instant> lastSeen) {

    /**
     * Creates an immutable recent player identity.
     *
     * @param uuid player UUID
     * @param name latest known player name
     * @param lastSeen latest stored observation timestamp
     */
    public RecentPlayerIdentity {
        lastSeen = lastSeen == null ? Optional.empty() : lastSeen;
    }
}
