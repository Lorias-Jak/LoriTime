package com.jannik_kuehn.common.command.config;

import com.jannik_kuehn.common.config.Configuration;

import java.util.List;
import java.util.Locale;

/**
 * Resolves command names and aliases from commands.yml.
 */
public class CommandAliasConfig {

    /**
     * Default root section for command profiles.
     */
    private static final String PROFILES_PATH = "profiles";

    /**
     * Command configuration file.
     */
    private final Configuration commandConfiguration;

    /**
     * Creates a command alias configuration reader.
     *
     * @param configuration loaded commands.yml configuration
     */
    public CommandAliasConfig(final Configuration configuration) {
        this.commandConfiguration = configuration;
    }

    /**
     * Resolves command metadata for a profile and command node.
     *
     * @param profile profile selector
     * @param node command node
     * @param defaultName fallback command name
     * @param defaultAliases fallback aliases
     * @return resolved command metadata
     */
    public CommandAlias resolve(final CommandProfile profile, final CommandNode node,
                                final String defaultName, final List<String> defaultAliases) {
        final String path = PROFILES_PATH + "." + profile.path() + "." + node.configKey();
        final String configuredName = commandConfiguration.getString(path + ".name", defaultName);
        final List<String> configuredAliases = commandConfiguration.getArrayList(path + ".aliases").stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(alias -> !alias.isBlank())
                .toList();
        final List<String> aliases = configuredAliases.isEmpty() ? List.copyOf(defaultAliases) : configuredAliases;
        return new CommandAlias(configuredName, aliases);
    }

    /**
     * Command profile sections supported by commands.yml.
     */
    public enum CommandProfile {
        /**
         * Proxy command aliases.
         */
        PROXY("proxy"),

        /**
         * Backend storage-owner aliases.
         */
        BACKEND_CANONICAL("backend.canonical"),

        /**
         * Backend slave aliases.
         */
        BACKEND_SLAVE("backend.slave");

        /**
         * Dot-path section.
         */
        private final String commandProfilePath;

        CommandProfile(final String configPath) {
            this.commandProfilePath = configPath;
        }

        /**
         * Gets the dot-path section.
         *
         * @return config path
         */
        public String path() {
            return commandProfilePath;
        }
    }

    /**
     * Command nodes configurable in commands.yml.
     */
    public enum CommandNode {
        /**
         * Canonical admin command.
         */
        ADMIN,

        /**
         * Canonical mutation command.
         */
        MODIFY,

        /**
         * Player time lookup command.
         */
        TIME,

        /**
         * Top time command.
         */
        TOP,

        /** Network statistics command. */
        STATS,

        /**
         * AFK command.
         */
        AFK;

        /**
         * Gets the commands.yml node key.
         *
         * @return config key
         */
        public String configKey() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
