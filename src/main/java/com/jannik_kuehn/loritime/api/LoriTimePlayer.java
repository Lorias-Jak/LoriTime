package com.jannik_kuehn.loritime.api;

import java.util.UUID;

public class LoriTimePlayer {

    private final CommonSender commonSender;
    private boolean afkStatus;

    public LoriTimePlayer(CommonSender commonSender) {
        this.commonSender = commonSender;
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

    public CommonSender getCommon() {
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
