package com.jannik_kuehn.loritimevelocity.util;

import com.jannik_kuehn.common.platform.CommonConsoleSender;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Velocity console sender adapter.
 */
public class VelocitySender implements CommonConsoleSender {

    /**
     * The Velocity command source.
     */
    private final CommandSource source;

    /**
     * Creates a sender adapter for a Velocity command source.
     *
     * @param source Velocity command source
     */
    public VelocitySender(final CommandSource source) {
        this.source = source;
    }

    /**
     * Returns the console display name.
     *
     * @return console name
     */
    @Override
    public String getName() {
        return "CONSOLE";
    }

    /**
     * Checks a Velocity command source permission.
     *
     * @param permission permission node
     * @return true when the source has the permission
     */
    @Override
    public boolean hasPermission(final String permission) {
        return source.hasPermission(permission);
    }

    /**
     * Sends a legacy-formatted text message.
     *
     * @param message plain text message
     */
    @Override
    public void sendMessage(final String message) {
        source.sendMessage(LegacyComponentSerializer.legacy('&').deserialize(message));
    }

    /**
     * Sends an Adventure component message.
     *
     * @param message component message
     */
    @Override
    public void sendMessage(final Component message) {
        source.sendMessage(message);
    }

}
