package com.jannik_kuehn.common.config.migration;

import com.jannik_kuehn.common.config.StructuredConfigurationDocument;

import java.util.Locale;

/**
 * Localization-specific config schema migrations.
 */
final class ConfigSchemaLocalizationMigrations {
    /**
     * Localization path for irreversible transfer warnings.
     */
    private static final String TRANSFER_WARNING_PATH = "messages.message.command.loritimeadmin.transfer.warning";

    /**
     * Localization prefix for deleteHistory messages.
     */
    private static final String DELETE_HISTORY_PREFIX = "messages.message.command.loritimeadmin.deleteHistory.";

    /**
     * Localization prefix for statistics command messages.
     */
    private static final String STATISTICS_PREFIX = "messages.message.command.stats.";

    private ConfigSchemaLocalizationMigrations() {
    }

    /* default */
    static ConfigMigration transferWarning() {
        return ConfigMigration.from(1)
                .operation(ConfigSchemaLocalizationMigrations::addTransferWarningMessage)
                .operation(ConfigSchemaLocalizationMigrations::addDeleteHistoryMessages)
                .build();
    }

    /* default */
    static ConfigMigration statisticsMessages() {
        return ConfigMigration.from(2)
                .operation(ConfigSchemaLocalizationMigrations::addStatisticsMessages)
                .build();
    }

    private static void addTransferWarningMessage(final StructuredConfigurationDocument document) {
        if (document.contains(TRANSFER_WARNING_PATH)) {
            return;
        }

        if (localeTag(document).startsWith("de")) {
            document.set(TRANSFER_WARNING_PATH, "<#FF3232>WARNUNG: Dieser Transfer schreibt gespeicherte "
                    + "Verlaufsdaten um und kann von LoriTime nicht rueckgaengig gemacht werden. Erstelle und pruefe "
                    + "ein Backup, bevor du /lta confirm ausfuehrst.");
            return;
        }

        document.set(TRANSFER_WARNING_PATH, "<#FF3232>WARNING: This transfer rewrites stored history and cannot "
                + "be reverted by LoriTime. Create and verify a backup before running /lta confirm.");
    }

    private static String localeTag(final StructuredConfigurationDocument document) {
        final Object locale = document.get("locale");
        return locale instanceof final String value ? value.toLowerCase(Locale.ROOT) : "";
    }

    private static void addDeleteHistoryMessages(final StructuredConfigurationDocument document) {
        final boolean german = localeTag(document).startsWith("de");
        putIfMissing(document, "usage", german
                ? "<#A4A4A4>Benutze <red>/loritimeadmin deleteHistory [player] server:<source> "
                  + "[world:<source>] [time:<range>] <#A4A4A4>um das Loeschen von Verlaufsdaten vorzubereiten. "
                  + "Ohne Spieler werden alle Spieler beruecksichtigt."
                : "<#A4A4A4>Use <red>/loritimeadmin deleteHistory [player] server:<source> "
                  + "[world:<source>] [time:<range>] <#A4A4A4>to preview deleting scoped history. "
                  + "Omit player to delete matching history for all players.");
        putIfMissing(document, "preview", german
                ? "<#A4A4A4>DeleteHistory-Vorschau fuer <#50CBAB>[player]<#A4A4A4>: Scope "
                  + "<#50CBAB>[scope]<#A4A4A4>, Zeitraum <#50CBAB>[range]<#A4A4A4>, "
                  + "<#50CBAB>[sessions] <#A4A4A4>Sitzungen, <#50CBAB>[adjustments] <#A4A4A4>Anpassungen, "
                  + "<#50CBAB>[players] <#A4A4A4>Spieler. Nutze <red>/lta confirm <#A4A4A4>innerhalb "
                  + "von 15 Sekunden zum Anwenden."
                : "<#A4A4A4>DeleteHistory preview for <#50CBAB>[player]<#A4A4A4>: scope "
                  + "<#50CBAB>[scope]<#A4A4A4>, range <#50CBAB>[range]<#A4A4A4>, "
                  + "<#50CBAB>[sessions] <#A4A4A4>sessions, <#50CBAB>[adjustments] <#A4A4A4>adjustments, "
                  + "<#50CBAB>[players] <#A4A4A4>players. Use <red>/lta confirm <#A4A4A4>within "
                  + "15 seconds to apply.");
        putIfMissing(document, "warning", german
                ? "<#FF3232>WARNUNG: Dieses Loeschen entfernt gespeicherte Verlaufsdaten und kann von LoriTime "
                  + "nicht rueckgaengig gemacht werden. Erstelle und pruefe ein Backup, bevor du /lta confirm ausfuehrst."
                : "<#FF3232>WARNING: This delete removes stored history and cannot be reverted by LoriTime. "
                  + "Create and verify a backup before running /lta confirm.");
        putIfMissing(document, "success", german
                ? "<#1AFA29>Verlaufsdaten geloescht: <#50CBAB>[sessions] <#1AFA29>Sitzungen, "
                  + "<#50CBAB>[adjustments] <#1AFA29>Anpassungen, <#50CBAB>[players] <#1AFA29>Spieler."
                : "<#1AFA29>History deleted: <#50CBAB>[sessions] <#1AFA29>sessions, "
                  + "<#50CBAB>[adjustments] <#1AFA29>adjustments, <#50CBAB>[players] <#1AFA29>players.");
        putIfMissing(document, "failure", german
                ? "<#FF3232>Loeschen der Verlaufsdaten fehlgeschlagen."
                : "<#FF3232>Storage history delete failed.");
    }

    private static void addStatisticsMessages(final StructuredConfigurationDocument document) {
        addStatisticsMessage(document, "commandUsage",
                "<#A4A4A4>Use <red>/ltstats [users|sessions|usage|top|afk|retention] "
                        + "[time:<range>|calendar:<period>] "
                        + "[server:<server>] [world:<world>].",
                "<#A4A4A4>Benutze <red>/ltstats [users|sessions|usage|top|afk|retention] "
                        + "[time:<Zeitraum>|calendar:<Zeitraum>] "
                        + "[server:<Server>] [world:<Welt>].");
        addStatisticsMessage(document, "unsupported",
                "<#FF3232>Statistics are not supported by the active storage.",
                "<#FF3232>Statistiken werden vom aktiven Speicher nicht unterstützt.");
        addStatisticsMessage(document, "error",
                "<#FF3232>Statistics could not be calculated.",
                "<#FF3232>Statistiken konnten nicht berechnet werden.");
        addStatisticsMessage(document, "overview",
                "<#A4A4A4><newline>Network statistics — <#50CBAB>[range] <#A4A4A4>([scope])<newline>"
                        + "Unique users: <#50CBAB>[unique]<#A4A4A4> | New users: <#50CBAB>[new]<newline>"
                        + "<#A4A4A4>Sessions: <#50CBAB>[sessions]<#A4A4A4> | Playtime: <#50CBAB>[playtime]"
                        + "<newline><#A4A4A4>Median: <#50CBAB>[median]<#A4A4A4> | Longest: <#50CBAB>[longest]"
                        + "<#A4A4A4> | Sessions/user: <#50CBAB>[peruser]<newline><#A4A4A4>Bounces: "
                        + "<#50CBAB>[bounces] ([bounce])<#A4A4A4> | Peak: <#50CBAB>[peak]<newline><#A4A4A4>"
                        + "AFK players: <#50CBAB>[afkplayers]<#A4A4A4> | AFK kicks: <#50CBAB>[afkkicks]",
                "<#A4A4A4><newline>Netzwerkstatistik — <#50CBAB>[range] <#A4A4A4>([scope])<newline>"
                        + "Eindeutige Nutzer: <#50CBAB>[unique]<#A4A4A4> | Neue Nutzer: <#50CBAB>[new]<newline>"
                        + "<#A4A4A4>Sitzungen: <#50CBAB>[sessions]<#A4A4A4> | Spielzeit: <#50CBAB>[playtime]"
                        + "<newline><#A4A4A4>Median: <#50CBAB>[median]<#A4A4A4> | Längste: <#50CBAB>[longest]"
                        + "<#A4A4A4> | Sitzungen/Nutzer: <#50CBAB>[peruser]<newline><#A4A4A4>Bounces: "
                        + "<#50CBAB>[bounces] ([bounce])<#A4A4A4> | Maximum: <#50CBAB>[peak]<newline>"
                        + "<#A4A4A4>AFK-Spieler: <#50CBAB>[afkplayers]<#A4A4A4> | AFK-Kicks: "
                        + "<#50CBAB>[afkkicks]");
        addStatisticsMessage(document, "users",
                "<#A4A4A4>Users — [range] ([scope])<newline>Unique: <#50CBAB>[unique]<#A4A4A4> | New: "
                        + "<#50CBAB>[new]<#A4A4A4> | 7-day retention: <#50CBAB>[retention]",
                "<#A4A4A4>Nutzer — [range] ([scope])<newline>Eindeutig: <#50CBAB>[unique]<#A4A4A4> | Neu: "
                        + "<#50CBAB>[new]<#A4A4A4> | 7-Tage-Retention: <#50CBAB>[retention]");
        addStatisticsMessage(document, "sessions",
                "<#A4A4A4>Sessions — [range] ([scope])<newline>Total: <#50CBAB>[sessions]<#A4A4A4> | "
                        + "Median: <#50CBAB>[median]<#A4A4A4> | Longest: <#50CBAB>[longest]<newline>"
                        + "<#A4A4A4>Per user: <#50CBAB>[peruser]<#A4A4A4> | Bounces: <#50CBAB>[bounces] "
                        + "([bounce])<#A4A4A4> | Peak: <#50CBAB>[peak]",
                "<#A4A4A4>Sitzungen — [range] ([scope])<newline>Gesamt: <#50CBAB>[sessions]<#A4A4A4> | "
                        + "Median: <#50CBAB>[median]<#A4A4A4> | Längste: <#50CBAB>[longest]<newline>"
                        + "<#A4A4A4>Pro Nutzer: <#50CBAB>[peruser]<#A4A4A4> | Bounces: <#50CBAB>[bounces] "
                        + "([bounce])<#A4A4A4> | Maximum: <#50CBAB>[peak]");
        addStatisticsMessage(document, "usage",
                "<#A4A4A4>Usage — [range] ([scope])<newline>[usage]",
                "<#A4A4A4>Nutzung — [range] ([scope])<newline>[usage]");
        addStatisticsMessage(document, "top",
                "<#A4A4A4>Top playtime — [range] ([scope])<newline>[top]",
                "<#A4A4A4>Meiste Spielzeit — [range] ([scope])<newline>[top]");
        addStatisticsMessage(document, "afk",
                "<#A4A4A4>AFK — [range] ([scope])<newline>Players: <#50CBAB>[afkplayers]<#A4A4A4> | "
                        + "Periods: <#50CBAB>[afkperiods]<#A4A4A4> | Duration: <#50CBAB>[afktime]<#A4A4A4> | "
                        + "Kicks: <#50CBAB>[afkkicks]",
                "<#A4A4A4>AFK — [range] ([scope])<newline>Spieler: <#50CBAB>[afkplayers]<#A4A4A4> | "
                        + "Perioden: <#50CBAB>[afkperiods]<#A4A4A4> | Dauer: <#50CBAB>[afktime]<#A4A4A4> | "
                        + "Kicks: <#50CBAB>[afkkicks]");
        addStatisticsMessage(document, "retention",
                "<#A4A4A4>Retention — [range] ([scope])<newline>New users: <#50CBAB>[new]<#A4A4A4> | "
                        + "Matured 7-day retention: <#50CBAB>[retention]",
                "<#A4A4A4>Retention — [range] ([scope])<newline>Neue Nutzer: <#50CBAB>[new]<#A4A4A4> | "
                        + "Reife 7-Tage-Retention: <#50CBAB>[retention]");
    }

    private static void addStatisticsMessage(final StructuredConfigurationDocument document,
                                             final String suffix,
                                             final String english,
                                             final String german) {
        final String locale = localeTag(document);
        final String value;
        if (locale.startsWith("de")) {
            value = german;
        } else {
            value = english;
        }
        final String path = STATISTICS_PREFIX + suffix;
        if (!document.contains(path)) {
            document.set(path, value);
        }
    }

    private static void putIfMissing(final StructuredConfigurationDocument document,
                                     final String suffix,
                                     final String value) {
        final String path = DELETE_HISTORY_PREFIX + suffix;
        if (!document.contains(path)) {
            document.set(path, value);
        }
    }
}
