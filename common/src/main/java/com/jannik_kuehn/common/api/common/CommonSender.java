package com.jannik_kuehn.common.api.common;

import net.kyori.adventure.text.Component;

/**
 * Platform-neutral sender that can receive messages and expose permissions.
 */
public interface CommonSender {

    /**
     * Returns the display name used for logs and messages.
     *
     * @return sender name
     */
    String getName();

    /**
     * Checks whether the sender has a permission.
     *
     * @param permission permission node
     * @return true when the sender has the permission
     */
    boolean hasPermission(String permission);

    /**
     * Sends a plain text message to the sender.
     *
     * @param message plain text message
     */
    void sendMessage(String message);

    /**
     * Sends an Adventure component message to the sender.
     *
     * @param message component message
     */
    void sendMessage(Component message);
}
