package com.jannik_kuehn.loritime.common;

import com.jannik_kuehn.loritime.api.LoriTimePlayer;
import com.jannik_kuehn.loritime.common.module.afk.AfkStatusProvider;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

public class PlayerHandler {
    private final ArrayList<LoriTimePlayer> playerList;

    public PlayerHandler(AfkStatusProvider afkStatusProvider) {
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
        playerList.remove(player);
    }
}