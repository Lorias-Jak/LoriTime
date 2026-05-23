package com.jannik_kuehn.loritimepaper.util;

import com.jannik_kuehn.common.api.common.CommonConsoleSender;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

@SuppressWarnings("PMD.CommentRequired")
public class PaperSender implements CommonConsoleSender {
    private final CommandSender source;

    public PaperSender(final CommandSender source) {
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
