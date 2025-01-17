package com.jannik_kuehn.loritimepaper.util;

import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.common.CommonServer;
import com.jannik_kuehn.loritimepaper.LoriTimePaper;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("PMD.CommentRequired")
public class PaperServer implements CommonServer {
    private final LoriTimePaper loriTimePaper;

    private final String version;

    private final Server server;

    private String serverMode;

    public PaperServer(final LoriTimePaper loriTimePaper, final String version) {
        this.loriTimePaper = loriTimePaper;
        this.server = Bukkit.getServer();
        this.version = version;
    }

    @Override
    public Optional<CommonSender> getPlayer(final UUID uniqueId) {
        final Optional<Player> player = Optional.ofNullable(server.getPlayer(uniqueId));
        return Optional.ofNullable(player.map(PaperPlayer::new).orElse(null));
    }

    @Override
    public Optional<CommonSender> getPlayer(final String name) {
        final Optional<Player> player = Optional.ofNullable(server.getPlayer(name));
        return Optional.ofNullable(player.map(PaperPlayer::new).orElse(null));
    }

    @Override
    public CommonSender[] getOnlinePlayers() {
        return server.getOnlinePlayers().stream()
                .map(PaperPlayer::new)
                .toList().toArray(new PaperPlayer[0]);
    }

    @Override
    public boolean dispatchCommand(final CommonSender consoleSender, final String command) {
        final CommandSender commandSource;
        if (consoleSender.isConsole()) {
            commandSource = server.getConsoleSender();
        } else {
            final Player player = server.getPlayer(consoleSender.getName());
            if (server.getPlayer(consoleSender.getName()) != null) {
                commandSource = player;
            } else {
                return false;
            }
        }
        if (commandSource == null) {
            return false;
        }
        server.dispatchCommand(commandSource, command);
        return true;
    }

    @Override
    public String getServerVersion() {
        return server.getVersion();
    }

    @Override
    public boolean isProxy() {
        return false;
    }

    @Override
    public String getServerMode() {
        return serverMode;
    }

    @Override
    public void setServerMode(final String serverMode) {
        this.serverMode = serverMode;
    }

    @Override
    public void kickPlayer(final LoriTimePlayer loriTimePlayer, final TextComponent message) {
        final Optional<UUID> optionalUUID = Optional.ofNullable(loriTimePlayer.getUniqueId());
        if (optionalUUID.isEmpty()) {
            return;
        }
        final Player player = Bukkit.getServer().getPlayer(optionalUUID.get());
        if (player == null) {
            return;
        }
        loriTimePaper.getPlugin().getScheduler().scheduleSync(() -> kickPlayer(player, message));
    }

    @Override
    public void sendMessageToConsole(final TextComponent message) {
        server.sendMessage(message);
    }

    @Override
    public String getPluginVersion() {
        return version;
    }

    @Override
    public java.util.logging.Logger getJavaLogger() {
        return null;
    }

    @Override
    public Logger getSl4jLogger() {
        return loriTimePaper.getSLF4JLogger();
    }

    private void kickPlayer(final Player player, final TextComponent message) {
        player.kick(message);
    }
}
