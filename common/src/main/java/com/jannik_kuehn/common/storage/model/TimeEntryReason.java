package com.jannik_kuehn.common.storage.model;

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
     * Time was adjusted by AFK handling.
     */
    AFK_ADJUSTMENT,

    /**
     * Player moved to another server context.
     */
    SERVER_SWITCH,

    /**
     * Player moved to another world context on the same server.
     */
    WORLD_SWITCH,

    /**
     * When a player joins the Server
     */
    PLAYER_JOIN,

    /**
     * Player left and current session was persisted.
     */
    PLAYER_LEAVE,

    /**
     * Player session was stopped because the player became AFK.
     */
    PLAYER_AFK,

    /**
     * Player session was stopped because the player was kicked for being AFK.
     */
    PLAYER_AFK_KICK,

    /**
     * Online cache flush persisted partial session time.
     */
    AUTO_FLUSH,

    /**
     * Plugin shutdown persisted remaining online cache.
     */
    SHUTDOWN_FLUSH
}

