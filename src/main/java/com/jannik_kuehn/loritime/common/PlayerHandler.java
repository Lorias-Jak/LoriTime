package com.jannik_kuehn.loritime.common;

import com.jannik_kuehn.loritime.api.LoriTimePlayer;
import com.jannik_kuehn.loritime.common.module.afk.AfkStatusProvider;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

public class PlayerHandler {
    private final LoriTimePlugin loriTimePlugin;
    private final ArrayList<LoriTimePlayer> playerList;
    private AfkStatusProvider afkStatusProvider;

    public PlayerHandler(final LoriTimePlugin loriTimePlugin, AfkStatusProvider afkStatusProvider) {
        this.loriTimePlugin = loriTimePlugin;
        this.afkStatusProvider = afkStatusProvider;
        this.playerList = new ArrayList<>();
    }
    //ToDo afkStatusProvider und den AFKHandling hier einzelnd integrieren? 

    public Optional<LoriTimePlayer> getPlayer(UUID uuid) {
        for (LoriTimePlayer player : playerList) {
            if (player.getUuid().equals(uuid)) {
                return Optional.of(player);
            }
        }
        return Optional.empty();
    }

    public Optional<LoriTimePlayer> getPlayer(String name) {
        for (LoriTimePlayer player : playerList) {
            if (player.getName().equals(name)) {
                return Optional.of(player);
            }
        }
        return Optional.empty();
    }

    public boolean contains(LoriTimePlayer player) {
        return playerList.contains(player);
    }

    public void registerPlayer(LoriTimePlayer player) {
        if (playerList.contains(player)) {
            return;
        }
        playerList.add(player);
    }

    public void unregisterPlayer(LoriTimePlayer player) {
        // ToDo Checken ob das hier vernünftig funktioniert!
        if (!playerList.contains(player)) {
            loriTimePlugin.getLogger().severe("Cant unregister player " + player.getName() + " because he is not registered!");
            return;
        }
        playerList.remove(player);
        afkStatusProvider.playerLeft(player);
    }

    public AfkStatusProvider getAfkStatusProvider() {
        return afkStatusProvider;
    }

    public void setAfkStatusProvider(AfkStatusProvider afkStatusProvider) {
        this.afkStatusProvider = afkStatusProvider;
    }

    public void reloadPlayerHandler() {
        afkStatusProvider.reloadConfigValues();
    }
}