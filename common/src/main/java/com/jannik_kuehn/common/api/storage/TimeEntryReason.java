package com.jannik_kuehn.common.api.storage;

/**
 * Reason why an accumulated time chunk is persisted.
 */
public enum TimeEntryReason {

    /**
     * No explicit reason is available.
     */
    UNSPECIFIED,

    /**
     * Time was imported from legacy flat storage.
     */
    LEGACY_IMPORT,

    /**
     * Time was added manually or via command/API.
     */
    MANUAL_ADJUSTMENT,

    /**
     * Player context changed (for example server/world switch).
     */
    CONTEXT_SWITCH,

    /**
     * When a player joins the Server
     */
    PLAYER_JOIN,

    /**
     * Player left and current session was persisted.
     */
    PLAYER_LEAVE,

    /**
     * Online cache flush persisted partial session time.
     */
    AUTO_FLUSH,

    /**
     * Plugin shutdown persisted remaining online cache.
     */
    SHUTDOWN_FLUSH
}

