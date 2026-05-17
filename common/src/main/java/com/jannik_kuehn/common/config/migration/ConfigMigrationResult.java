package com.jannik_kuehn.common.config.migration;

import com.jannik_kuehn.common.config.StructuredConfigurationDocument;

/**
 * Result of applying configuration migrations.
 *
 * @param document migrated document
 * @param changed  true when migration changed the document
 */
public record ConfigMigrationResult(StructuredConfigurationDocument document, boolean changed) {
}
