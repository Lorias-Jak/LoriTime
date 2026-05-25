package com.jannik_kuehn.common.command.completion;

import com.jannik_kuehn.common.command.core.CommandCompletions;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache-only server and world suggestions for synchronous command completion.
 */
public class ScopeSuggestionCache {

    /**
     * Known servers.
     */
    private final Set<String> servers = ConcurrentHashMap.newKeySet();

    /**
     * Known worlds.
     */
    private final Set<String> worlds = ConcurrentHashMap.newKeySet();

    /**
     * Creates an empty suggestion cache.
     */
    public ScopeSuggestionCache() {
        // Empty
    }

    /**
     * Remembers an observed server/world pair.
     *
     * @param server server name
     * @param world world name
     */
    public void remember(final String server, final String world) {
        if (server != null && !server.isBlank()) {
            servers.add(server);
        }
        if (world != null && !world.isBlank()) {
            worlds.add(world);
        }
    }

    /**
     * Replaces cached storage-backed names.
     *
     * @param serverNames known server names
     * @param worldNames known world names
     */
    public void replaceStoredNames(final Set<String> serverNames, final Set<String> worldNames) {
        servers.clear();
        worlds.clear();
        if (serverNames != null) {
            servers.addAll(serverNames);
        }
        if (worldNames != null) {
            worlds.addAll(worldNames);
        }
    }

    /**
     * Suggests server names.
     *
     * @param liveServers live runtime servers
     * @param prefix current value prefix
     * @return matching names
     */
    public List<String> suggestServers(final List<String> liveServers, final String prefix) {
        final Set<String> values = new LinkedHashSet<>(servers);
        if (liveServers != null) {
            values.addAll(liveServers);
        }
        return CommandCompletions.startsWith(new ArrayList<>(values), prefix);
    }

    /**
     * Suggests world names.
     *
     * @param liveWorlds live runtime worlds
     * @param prefix current value prefix
     * @return matching names
     */
    public List<String> suggestWorlds(final List<String> liveWorlds, final String prefix) {
        final Set<String> values = new LinkedHashSet<>(worlds);
        if (liveWorlds != null) {
            values.addAll(liveWorlds);
        }
        return CommandCompletions.startsWith(new ArrayList<>(values), prefix);
    }
}
