package com.jannik_kuehn.common.command.completion;

import com.jannik_kuehn.common.platform.CommonPlayerSender;
import com.jannik_kuehn.common.platform.CommonServer;
import com.jannik_kuehn.common.storage.model.RecentPlayerIdentity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Cache-only player suggestions for synchronous command completion.
 */
public class RecentPlayerSuggestionCache {

    /**
     * Recent stored identities by UUID.
     */
    private final ConcurrentMap<UUID, RecentPlayerIdentity> identities = new ConcurrentHashMap<>();

    /**
     * Runtime-observed names that may not have a UUID yet.
     */
    private final Set<String> observedNames = ConcurrentHashMap.newKeySet();

    /**
     * Creates a recent player suggestion cache.
     */
    public RecentPlayerSuggestionCache() {
    }

    /**
     * Remembers a runtime-observed player.
     *
     * @param uuid player UUID
     * @param name player name
     */
    public void remember(final UUID uuid, final String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        observedNames.add(name);
        if (uuid != null) {
            identities.put(uuid, new RecentPlayerIdentity(uuid, name, Optional.empty()));
        }
    }

    /**
     * Replaces stored recent identities.
     *
     * @param recentIdentities recent identities
     */
    public void replaceRecentIdentities(final List<RecentPlayerIdentity> recentIdentities) {
        identities.clear();
        if (recentIdentities == null) {
            return;
        }
        for (final RecentPlayerIdentity identity : recentIdentities) {
            if (identity.uuid() != null && identity.name() != null && !identity.name().isBlank()) {
                identities.put(identity.uuid(), identity);
                observedNames.add(identity.name());
            }
        }
    }

    /**
     * Suggests known player names without touching storage.
     *
     * @param server common server
     * @param prefix current completion prefix
     * @return matching names
     */
    public List<String> suggest(final CommonServer server, final String prefix) {
        final Set<String> names = new LinkedHashSet<>();
        identities.values().stream()
                .sorted(Comparator.comparing(identity -> identity.lastSeen().orElse(java.time.Instant.EPOCH), Comparator.reverseOrder()))
                .map(RecentPlayerIdentity::name)
                .forEach(names::add);
        names.addAll(observedNames);
        if (server != null) {
            for (final CommonPlayerSender player : server.getOnlinePlayers()) {
                if (player != null && player.getName() != null && !player.getName().isBlank()) {
                    names.add(player.getName());
                }
            }
        }

        final String normalizedPrefix = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        final List<String> suggestions = new ArrayList<>();
        for (final String name : names) {
            if (name.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix)) {
                suggestions.add(name);
            }
        }
        return suggestions;
    }

    /**
     * Returns known player names.
     *
     * @return immutable snapshot
     */
    public Set<String> names() {
        final Set<String> names = new LinkedHashSet<>(observedNames);
        identities.values().stream()
                .map(RecentPlayerIdentity::name)
                .forEach(names::add);
        return Set.copyOf(names);
    }
}
