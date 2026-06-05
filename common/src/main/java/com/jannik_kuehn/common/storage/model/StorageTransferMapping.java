package com.jannik_kuehn.common.storage.model;

import java.util.Objects;

/**
 * Source-to-target mapping for maintenance transfer.
 *
 * @param source source scope
 * @param target target scope
 */
public record StorageTransferMapping(StorageMaintenanceScope source, StorageMaintenanceScope target) {

    public StorageTransferMapping {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        if (source.type() != target.type()) {
            throw new IllegalArgumentException("source and target scope types must match");
        }
    }
}
