package com.jannik_kuehn.loritimepaper.util;

import com.jannik_kuehn.common.api.common.CommonConsoleSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

/**
 * Paper console sender adapter.
 */
public class PaperSender implements CommonConsoleSender {
    private final CommandSender source;

    /**
     * Creates a sender adapter for a Bukkit command sender.
     *
     * @param source Bukkit command sender
     */
    public PaperSender(final CommandSender source) {
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
     * Checks a Bukkit sender permission.
     *
     * @param permission permission node
     * @return true when the sender has the permission
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
