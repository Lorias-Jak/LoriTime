package com.jannik_kuehn.common.command.core;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.platform.CommonPlayerSender;
import com.jannik_kuehn.common.platform.CommonSender;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared player-name completion source.
 */
public final class PlayerNameCompletions {

    private PlayerNameCompletions() {
    }

    /**
     * Suggests known player names from memory only.
     *
     * @param plugin LoriTime plugin
     * @param prefix current prefix
     * @return matching player names
     */
    public static List<String> suggest(final LoriTimePlugin plugin, final String prefix) {
        if (plugin.getRecentPlayerSuggestionCache() != null) {
            return plugin.getRecentPlayerSuggestionCache().suggest(plugin.getServer(), prefix);
        }
        final Set<String> names = new LinkedHashSet<>(plugin.getKnownPlayerNames());
        for (final CommonPlayerSender player : plugin.getServer().getOnlinePlayers()) {
            names.add(player.getName());
        }
        return CommandCompletions.startsWith(new ArrayList<>(names), prefix);
    }

    /**
     * Suggests visible online player names.
     *
     * @param plugin LoriTime plugin
     * @param prefix current prefix
     * @return matching online player names
     */
    public static List<String> online(final LoriTimePlugin plugin, final String prefix) {
        final List<String> names = new ArrayList<>();
        for (final CommonSender player : plugin.getServer().getOnlinePlayers()) {
            names.add(player.getName());
        }
        return CommandCompletions.startsWith(names, prefix);
    }
}
