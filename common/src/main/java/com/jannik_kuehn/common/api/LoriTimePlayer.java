package com.jannik_kuehn.common.api;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Objects;
import java.util.UUID;

public class LoriTimePlayer {
    private final UUID uuid;

    private final String name;

    private boolean afkStatus;

    private long lastResumeTime;

    public LoriTimePlayer(final UUID uuid, final String name) {
        this.uuid = uuid;
        this.name = name;
        this.afkStatus = false;
        this.lastResumeTime = System.currentTimeMillis();
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

    public void setLastResumeTime() {
        this.lastResumeTime = System.currentTimeMillis();
    }

    public long getLastResumeTime() {
        return lastResumeTime;
    }

    @Override
    @SuppressFBWarnings("EC_UNRELATED_TYPES")
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final LoriTimePlayer that = (LoriTimePlayer) obj;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, name);
    }
}
