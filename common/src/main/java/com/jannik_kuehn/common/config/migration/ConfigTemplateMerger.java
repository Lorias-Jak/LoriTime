package com.jannik_kuehn.common.config.migration;

import com.jannik_kuehn.common.config.StructuredConfigurationDocument;

import java.util.Map;

/**
 * Merges migrated user values onto the latest bundled configuration template.
 */
public final class ConfigTemplateMerger {
    /**
     * Creates a template merger.
     */
    public ConfigTemplateMerger() {
    }

    /**
     * Merges template defaults with user values for known template paths.
     *
     * @param template latest template document
     * @param user     migrated user document
     * @return merged document
     */
    public StructuredConfigurationDocument merge(final StructuredConfigurationDocument template,
                                                 final StructuredConfigurationDocument user) {
        final StructuredConfigurationDocument result = new StructuredConfigurationDocument(template.asMap());
        overlayTemplatePaths("", result, user.flatten(), template.asMap());
        final Object schemaVersion = user.get(ConfigSchema.VERSION_PATH);
        if (schemaVersion != null) {
            result.set(ConfigSchema.VERSION_PATH, schemaVersion);
        }
        preserveUnknownUserPaths(result, user);
        return result;
    }

    private void preserveUnknownUserPaths(final StructuredConfigurationDocument target,
                                          final StructuredConfigurationDocument user) {
        final Map<String, Object> targetValues = target.flatten();
        for (final Map.Entry<String, Object> entry : user.flatten().entrySet()) {
            if (!targetValues.containsKey(entry.getKey())) {
                target.set(entry.getKey(), entry.getValue());
            }
        }
    }

    private void overlayTemplatePaths(final String prefix, final StructuredConfigurationDocument target,
                                      final Map<String, Object> userValues, final Map<String, Object> templateMap) {
        for (final Map.Entry<String, Object> entry : templateMap.entrySet()) {
            final String path = prefix.isBlank() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked") final Map<String, Object> child = (Map<String, Object>) map;
                overlayTemplatePaths(path, target, userValues, child);
            } else {
                final Object userValue = userValues.get(path);
                if (userValue != null) {
                    target.set(path, userValue);
                }
            }
        }
    }
}
