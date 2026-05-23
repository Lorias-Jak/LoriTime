package com.jannik_kuehn.loritimevelocity.util;

import com.jannik_kuehn.common.api.common.CommonConsoleSender;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

@SuppressWarnings("PMD.CommentRequired")
public class VelocitySender implements CommonConsoleSender {
    private final CommandSource source;

    public VelocitySender(final CommandSource source) {
        this.source = source;
    }

    @Override
    public String getName() {
        return "CONSOLE";
    }

    @Override
    public boolean hasPermission(final String permission) {
        return source.hasPermission(permission);
    }

    @Override
    public void sendMessage(final String message) {
        source.sendMessage(LegacyComponentSerializer.legacy('&').deserialize(message));
    }

    @Override
    public void sendMessage(final TextComponent message) {
        source.sendMessage(message);
    }

}
