package com.jannik_kuehn.common.command.profile;

import com.jannik_kuehn.common.api.storage.StorageMode;
import com.jannik_kuehn.common.command.config.CommandAliasConfig;

/**
 * Code-defined command registration profiles.
 */
public enum RuntimeCommandProfile {
    /**
     * Proxy runtime profile.
     */
    PROXY(CommandAliasConfig.CommandProfile.PROXY),

    /**
     * Backend storage-owner profile.
     */
    BACKEND_CANONICAL(CommandAliasConfig.CommandProfile.BACKEND_CANONICAL),

    /**
     * Backend slave profile.
     */
    BACKEND_SLAVE(CommandAliasConfig.CommandProfile.BACKEND_SLAVE);

    /**
     * Alias config profile.
     */
    private final CommandAliasConfig.CommandProfile aliasConfigProfile;

    RuntimeCommandProfile(final CommandAliasConfig.CommandProfile aliasProfile) {
        this.aliasConfigProfile = aliasProfile;
    }

    /**
     * Selects a runtime command profile.
     *
     * @param proxy true for proxy platforms
     * @param storageMode configured storage mode
     * @return command profile
     */
    public static RuntimeCommandProfile select(final boolean proxy, final String storageMode) {
        if (proxy) {
            return PROXY;
        }
        if (StorageMode.SLAVE.configValue().equalsIgnoreCase(storageMode)) {
            return BACKEND_SLAVE;
        }
        return BACKEND_CANONICAL;
    }

    /**
     * Gets the alias config profile.
     *
     * @return alias profile
     */
    public CommandAliasConfig.CommandProfile aliases() {
        return aliasConfigProfile;
    }
}
