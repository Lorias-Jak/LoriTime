package com.jannik_kuehn.common.storage.model;

/** Reason a persisted AFK period ended. */
public enum AfkPeriodEndReason {
    RESUMED,
    KICKED,
    DISCONNECTED,
    SHUTDOWN
}
