package com.jannik_kuehn.common.module.afk;

import com.jannik_kuehn.common.api.LoriTimePlayer;

/**
 * Internal AFK transition decision before side effects are executed.
 *
 * @param player player affected by the transition
 * @param type transition type
 * @param timeToRemove AFK time to remove in seconds
 */
public record AfkTransition(LoriTimePlayer player, AfkTransitionType type, long timeToRemove) {
}
