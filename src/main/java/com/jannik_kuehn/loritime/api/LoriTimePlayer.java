package com.jannik_kuehn.loritime.api;

import com.jannik_kuehn.loritime.common.LoriTimePlugin;

import java.util.UUID;

public class LoriTimePlayer {

    private final CommonSender commonSender;
    private boolean afkStatus;

    public LoriTimePlayer(final CommonSender commonSender) {
        this.commonSender = commonSender;
    }

    public LoriTimePlayer(final LoriTimePlugin loriTimePlugin, final UUID uuid) {
        this.commonSender = loriTimePlugin.getServer().getPlayer(uuid).orElse(null);
        if (commonSender == null) {
            loriTimePlugin.getLogger().severe("Cant find common Sender with UUID: " + uuid.toString() + " while creating LoriTimePlayer object");
            throw new IllegalArgumentException("Player not found");
        }
    }

    public LoriTimePlayer(final LoriTimePlugin loriTimePlugin, final String playerName) {
        this.commonSender = loriTimePlugin.getServer().getPlayer(playerName).orElse(null);
        if (commonSender == null) {
            loriTimePlugin.getLogger().severe("Cant find common Sender with Name: " + playerName + " while creating LoriTimePlayer object");
            throw new IllegalArgumentException("Player not found");
        }
    }

    public boolean isAfk() {
        return afkStatus;
    }

    public String getName() {
        return commonSender.getName();
    }

    public UUID getUuid() {
        return commonSender.getUniqueId();
    }

    public CommonSender getCommonSender() {
        return commonSender;
    }

    public void setAFk(final boolean afk) {
        this.afkStatus = afk;
    }

    public boolean equals(Object data) {
        if (this == data)
            return true;
        if (data == null || getClass() != data.getClass())
            return false;
        LoriTimePlayer targetPlayer = (LoriTimePlayer) data;
        return getUuid().equals(targetPlayer.getUuid());
    }
}
