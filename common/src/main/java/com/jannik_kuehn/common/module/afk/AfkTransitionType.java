package com.jannik_kuehn.common.module.afk;

/**
 * AFK transition outcome.
 */
public enum AfkTransitionType {
    /**
     * Player entered AFK state without immediate kick enforcement.
     */
    START,

    /**
     * Player resumed from AFK state.
     */
    RESUME,

    /**
     * Player was kicked by AFK enforcement.
     */
    KICK
}
