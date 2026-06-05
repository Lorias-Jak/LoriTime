package com.jannik_kuehn.common.platform;

import com.jannik_kuehn.common.api.LoriTimePlayer;

/**
 * Platform-neutral online player sender.
 */
public interface CommonPlayerSender extends CommonSender, LoriTimePlayer {

    /**
     * Returns whether the player is currently online on this platform instance.
     *
     * @return true when the player is online
     */
    boolean isOnline();
}
