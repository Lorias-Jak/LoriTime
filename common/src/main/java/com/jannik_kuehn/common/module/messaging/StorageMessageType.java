package com.jannik_kuehn.common.module.messaging;

/**
 * Storage plugin message operation.
 */
public enum StorageMessageType {
    /**
     * Slave requests the current total from the master.
     */
    GET("get"),

    /**
     * Master sends the current total to a slave.
     */
    SEND("send"),

    /**
     * Slave requests a manual time write on the master.
     */
    ADD("add"),

    /**
     * Legacy completed remote session payload.
     */
    SESSION("session"),

    /**
     * Slave reports the currently observed world context.
     */
    WORLD("world"),

    /**
     * Slave reports an observed world switch.
     */
    WORLD_SWITCH("world_switch");

    /**
     * Wire payload value.
     */
    private final String payloadValue;

    StorageMessageType(final String payloadValue) {
        this.payloadValue = payloadValue;
    }

    /**
     * Returns the wire payload value.
     *
     * @return the wire value
     */
    public String wireValue() {
        return payloadValue;
    }
}
