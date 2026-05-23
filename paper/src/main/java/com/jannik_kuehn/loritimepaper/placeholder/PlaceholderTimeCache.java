package com.jannik_kuehn.loritimepaper.placeholder;

import java.util.OptionalLong;
import java.util.UUID;

/**
 * Non-blocking time cache used by synchronous placeholder rendering.
 */
public interface PlaceholderTimeCache {

    /**
     * Reads cached time for a player without blocking on storage.
     *
     * @param uniqueId player UUID
     * @return cached time, if present
     */
    OptionalLong getCachedTime(UUID uniqueId);

    /**
     * Requests an asynchronous cache refresh.
     *
     * @param uniqueId player UUID
     */
    void requestRefresh(UUID uniqueId);
}
