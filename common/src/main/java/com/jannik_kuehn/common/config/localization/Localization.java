package com.jannik_kuehn.common.config.localization;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves raw localized messages loaded from versioned language files.
 */
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
public class Localization {
    private static final String PREFIX_KEY = "prefix";

    private final MiniMessage miniMessage;

    private final WrappedLogger log;

    private final LocalizationLoader loader;

    private String configuredDefaultTag;

    private final Map<String, Map<String, String>> messagesByTag;

    private final Map<String, Map<String, List<String>>> listsByTag;

    private final Set<String> knownTags;

    private final Set<String> failedTags;

    private final Set<String> incompleteTags;

    private HealthState state;

    /**
     * Creates a localization resolver and immediately loads language files.
     *
     * @param log                  logger
     * @param dataFolder           plugin data folder
     * @param configuredDefaultTag configured default language tag for non-player contexts
     */
    public Localization(final WrappedLogger log, final File dataFolder, final String configuredDefaultTag) {
        this.miniMessage = MiniMessage.builder().build();
        this.log = Objects.requireNonNull(log, "log");
        this.loader = new LocalizationLoader(this.log, Objects.requireNonNull(dataFolder, "dataFolder"));
        this.configuredDefaultTag = LocalizationTags.normalize(configuredDefaultTag);
        this.messagesByTag = new ConcurrentHashMap<>();
        this.listsByTag = new ConcurrentHashMap<>();
        this.knownTags = ConcurrentHashMap.newKeySet();
        this.failedTags = ConcurrentHashMap.newKeySet();
        this.incompleteTags = ConcurrentHashMap.newKeySet();
        this.state = HealthState.FAILED;
        knownTags.add(this.configuredDefaultTag);
        knownTags.add(LocalizationTags.HARD_FALLBACK_TAG);
        reload();
    }

    /**
     * Reloads localization files.
     */
    public final void reload() {
        this.messagesByTag.clear();
        this.listsByTag.clear();
        this.failedTags.clear();
        this.incompleteTags.clear();
        for (final String tag : Set.copyOf(knownTags)) {
            loadTag(tag);
        }
        validateCompleteness();
        this.state = resolveHealthState();
    }

    /**
     * Reloads localization files with a new configured default language.
     *
     * @param configuredDefaultTag configured default language tag
     */
    public final void reload(final String configuredDefaultTag) {
        this.configuredDefaultTag = LocalizationTags.normalize(configuredDefaultTag);
        knownTags.add(this.configuredDefaultTag);
        knownTags.add(LocalizationTags.HARD_FALLBACK_TAG);
        reload();
    }

    /**
     * Returns the last computed localization health state.
     *
     * @return current localization health state
     */
    public HealthState healthState() {
        return state;
    }

    /**
     * Resolves a raw, unformatted message in non-player context.
     *
     * @param key message key
     * @return stripped message
     */
    public String getRawMessageWithoutFormats(final String key) {
        return miniMessage.stripTags(resolveFromCandidates(nonPlayerCandidateTags(), key));
    }

    /**
     * Resolves a raw message in non-player context.
     *
     * @param key message key
     * @return raw MiniMessage string
     */
    public String getRawMessage(final String key) {
        return resolveFromCandidates(nonPlayerCandidateTags(), key);
    }

    /**
     * Resolves a raw message for a caller-provided language tag.
     *
     * @param languageTag language tag
     * @param key         message key
     * @return raw MiniMessage string
     */
    public String getRawMessage(final String languageTag, final String key) {
        return resolveFromCandidates(playerCandidateTags(languageTag), key);
    }

    /**
     * Resolves a list message value in non-player context.
     *
     * @param key message key
     * @return configured list or empty list
     */
    public List<String> getStringList(final String key) {
        return resolveListFromCandidates(nonPlayerCandidateTags(), key);
    }

    /**
     * Resolves a raw, unformatted message for a caller-provided language tag.
     *
     * @param languageTag tag such as {@code de-de} or {@code de-de-dark}
     * @param key         message key
     * @return stripped message
     */
    public String getRawMessageWithoutFormats(final String languageTag, final String key) {
        return miniMessage.stripTags(resolveFromCandidates(playerCandidateTags(languageTag), key));
    }

    /**
     * Resolves a localized message as an Adventure component in non-player context.
     *
     * @param key message key
     * @return rendered component
     */
    public Component getFormattedMessage(final String key) {
        final String rawMessage = resolveFromCandidates(nonPlayerCandidateTags(), key);
        return deserialize(rawMessage, Map.of());
    }

    /**
     * Resolves a localized message with MiniMessage replacements in non-player context.
     *
     * @param key          message key
     * @param replacements placeholder replacement map
     * @return rendered component
     */
    public Component getFormattedMessage(final String key, final Map<String, String> replacements) {
        final String rawMessage = resolveFromCandidates(nonPlayerCandidateTags(), key);
        return deserialize(rawMessage, replacements);
    }

    /**
     * Resolves a localized message as an Adventure component for a caller-provided language tag.
     *
     * @param languageTag tag such as {@code de-de} or {@code de-de-dark}
     * @param key         message key
     * @return rendered component
     */
    public Component getFormattedMessage(final String languageTag, final String key) {
        return getFormattedMessage(languageTag, key, Map.of());
    }

    /**
     * Resolves a localized message with MiniMessage replacements for a caller-provided language tag.
     *
     * @param languageTag  tag such as {@code de-de} or {@code de-de-dark}
     * @param key          message key
     * @param replacements placeholder replacement map
     * @return rendered component
     */
    public Component getFormattedMessage(final String languageTag, final String key,
                                         final Map<String, String> replacements) {
        final String rawMessage = resolveFromCandidates(playerCandidateTags(languageTag), key);
        return deserialize(rawMessage, replacements);
    }

    /**
     * Resolves a prefixed localized message in non-player context.
     *
     * @param key message key
     * @return prefixed rendered component
     */
    public Component getPrefixedMessage(final String key) {
        return getPrefixComponent(nonPlayerCandidateTags()).append(getFormattedMessage(key));
    }

    /**
     * Resolves a prefixed localized message with replacements in non-player context.
     *
     * @param key          message key
     * @param replacements placeholder replacement map
     * @return prefixed rendered component
     */
    public Component getPrefixedMessage(final String key, final Map<String, String> replacements) {
        return getPrefixComponent(nonPlayerCandidateTags()).append(getFormattedMessage(key, replacements));
    }

    /**
     * Resolves a prefixed localized message for a caller-provided language tag.
     *
     * @param languageTag tag such as {@code de-de} or {@code de-de-dark}
     * @param key         message key
     * @return prefixed rendered component
     */
    public Component getPrefixedMessage(final String languageTag, final String key) {
        return getPrefixedMessage(languageTag, key, Map.of());
    }

    /**
     * Resolves a prefixed localized message with replacements for a caller-provided language tag.
     *
     * @param languageTag  tag such as {@code de-de} or {@code de-de-dark}
     * @param key          message key
     * @param replacements placeholder replacement map
     * @return prefixed rendered component
     */
    public Component getPrefixedMessage(final String languageTag, final String key,
                                        final Map<String, String> replacements) {
        return getPrefixComponent(playerCandidateTags(languageTag))
                .append(getFormattedMessage(languageTag, key, replacements));
    }

    /**
     * Prefixes an existing component in non-player context.
     *
     * @param message message body
     * @return prefixed message
     */
    public Component getPrefixedMessage(final Component message) {
        return getPrefixComponent(nonPlayerCandidateTags()).append(message);
    }

    /**
     * Prefixes an existing component for a caller-provided language tag.
     *
     * @param languageTag tag such as {@code de-de} or {@code de-de-dark}
     * @param message     message body
     * @return prefixed message
     */
    public Component getPrefixedMessage(final String languageTag, final Component message) {
        return getPrefixComponent(playerCandidateTags(languageTag)).append(message);
    }

    /**
     * Parses an arbitrary MiniMessage string in non-player context.
     *
     * @param message raw MiniMessage input
     * @return rendered component
     */
    public Component getMessageFromString(final String message) {
        return miniMessage.deserialize(message);
    }

    /**
     * Prefixes an arbitrary MiniMessage string in non-player context.
     *
     * @param message raw MiniMessage input
     * @return prefixed rendered component
     */
    public Component getPrefixedMessageFromString(final String message) {
        return getPrefixedMessage(getMessageFromString(message));
    }

    /**
     * Prefixes an arbitrary MiniMessage string for a caller-provided language tag.
     *
     * @param languageTag tag such as {@code de-de} or {@code de-de-dark}
     * @param message     raw MiniMessage input
     * @return prefixed rendered component
     */
    public Component getPrefixedMessageFromString(final String languageTag, final String message) {
        return getPrefixedMessage(languageTag, getMessageFromString(message));
    }

    /**
     * Formats an arbitrary message with the configured default prefix.
     *
     * @param message raw MiniMessage input
     * @return rendered component
     */
    public Component formatTextComponent(final String message) {
        return getPrefixedMessageFromString(message);
    }

    /**
     * Formats an arbitrary message without prefix.
     *
     * @param message raw MiniMessage input
     * @return rendered component
     */
    public Component formatTextComponentWithoutPrefix(final String message) {
        return getMessageFromString(message);
    }

    private Set<String> playerCandidateTags(final String requestedTag) {
        final String normalizedRequested = LocalizationTags.normalize(requestedTag);
        ensureLoaded(normalizedRequested);
        final Set<String> candidates = new LinkedHashSet<>();
        candidates.add(normalizedRequested);
        candidates.add(LocalizationTags.languageOnly(normalizedRequested));
        candidates.add(configuredDefaultTag);
        candidates.add(LocalizationTags.languageOnly(configuredDefaultTag));
        candidates.add(LocalizationTags.HARD_FALLBACK_TAG);
        return candidates;
    }

    private Set<String> nonPlayerCandidateTags() {
        ensureLoaded(configuredDefaultTag);
        ensureLoaded(LocalizationTags.HARD_FALLBACK_TAG);
        final Set<String> candidates = new LinkedHashSet<>();
        candidates.add(configuredDefaultTag);
        candidates.add(LocalizationTags.languageOnly(configuredDefaultTag));
        candidates.add(LocalizationTags.HARD_FALLBACK_TAG);
        return candidates;
    }

    private String resolveFromCandidates(final Set<String> candidates, final String key) {
        for (final String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            final Map<String, String> localeMessages = messagesByTag.get(candidate);
            if (localeMessages != null && localeMessages.containsKey(key)) {
                return localeMessages.get(key);
            }
        }
        return "No message to the key: '" + key + "' found";
    }

    private List<String> resolveListFromCandidates(final Set<String> candidates, final String key) {
        for (final String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            final Map<String, List<String>> localeLists = listsByTag.get(candidate);
            if (localeLists != null && localeLists.containsKey(key)) {
                return localeLists.get(key);
            }
        }
        return List.of();
    }

    private void ensureLoaded(final String tag) {
        if (tag == null || tag.isBlank() || messagesByTag.containsKey(tag) || failedTags.contains(tag)) {
            return;
        }
        loadTag(tag);
        state = resolveHealthState();
    }

    private void loadTag(final String tag) {
        final LocalizationLoader.LocaleData data = loader.load(tag);
        if (data == null) {
            failedTags.add(LocalizationTags.normalize(tag));
            return;
        }
        knownTags.add(data.tag());
        messagesByTag.put(data.tag(), data.messages());
        listsByTag.put(data.tag(), data.lists());
    }

    private void validateCompleteness() {
        final Map<String, String> fallbackMessages = messagesByTag.get(LocalizationTags.HARD_FALLBACK_TAG);
        final Map<String, List<String>> fallbackLists = listsByTag.get(LocalizationTags.HARD_FALLBACK_TAG);
        if (fallbackMessages == null || fallbackLists == null) {
            return;
        }
        for (final String tag : Set.copyOf(knownTags)) {
            if (LocalizationTags.HARD_FALLBACK_TAG.equals(tag) || !messagesByTag.containsKey(tag)) {
                continue;
            }
            final Set<String> missingMessages = missingKeys(fallbackMessages.keySet(), messagesByTag.get(tag).keySet());
            final Set<String> missingLists = missingKeys(fallbackLists.keySet(), listsByTag.getOrDefault(tag, Map.of()).keySet());
            if (!missingMessages.isEmpty() || !missingLists.isEmpty()) {
                incompleteTags.add(tag);
                log.warn("Localization file for '" + tag + "' is incomplete. Missing message keys: "
                        + missingMessages + "; missing list keys: " + missingLists
                        + ". Missing values will fall back to '" + LocalizationTags.HARD_FALLBACK_TAG + "'.");
            }
        }
    }

    private Set<String> missingKeys(final Set<String> requiredKeys, final Set<String> actualKeys) {
        final Set<String> missing = new TreeSet<>(requiredKeys);
        missing.removeAll(actualKeys);
        return missing;
    }

    private HealthState resolveHealthState() {
        if (messagesByTag.isEmpty()) {
            return HealthState.FAILED;
        }
        final boolean hasHardFallback = messagesByTag.containsKey(LocalizationTags.HARD_FALLBACK_TAG);
        final boolean hasConfiguredDefault = messagesByTag.containsKey(configuredDefaultTag)
                || messagesByTag.containsKey(LocalizationTags.languageOnly(configuredDefaultTag));
        return hasHardFallback && hasConfiguredDefault && failedTags.isEmpty() && incompleteTags.isEmpty()
                ? HealthState.READY : HealthState.DEGRADED;
    }

    private Component getPrefixComponent(final Set<String> candidates) {
        final String prefix = resolveFromCandidates(candidates, PREFIX_KEY);
        return miniMessage.deserialize(prefix);
    }

    private Component deserialize(final String rawMessage, final Map<String, String> replacements) {
        final Map<String, String> resolvedReplacements = replacements == null ? Map.of() : replacements;
        if (resolvedReplacements.isEmpty()) {
            return miniMessage.deserialize(rawMessage);
        }
        final TagResolver resolver = TagResolver.resolver(resolvedReplacements.entrySet().stream()
                .map(entry -> Placeholder.parsed(entry.getKey(), entry.getValue()))
                .toList());
        return miniMessage.deserialize(rawMessage, resolver);
    }

    /**
     * Health indicator for runtime localization usage.
     */
    public enum HealthState {
        /**
         * Localization data is fully loaded and complete.
         */
        READY,
        /**
         * Localization loaded but with missing or invalid locale resources.
         */
        DEGRADED,
        /**
         * Localization could not load any usable locale resources.
         */
        FAILED
    }
}
