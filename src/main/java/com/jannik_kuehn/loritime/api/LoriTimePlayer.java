package com.jannik_kuehn.loritime.api;

import java.util.UUID;

public class LoriTimePlayer {

    private final UUID uuid;
    private final String name;
    private boolean afkStatus;

    public LoriTimePlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;

        this.afkStatus = false;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public boolean isAfk() {
        return afkStatus;
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
        return uuid.equals(targetPlayer.uuid);
    }
}
