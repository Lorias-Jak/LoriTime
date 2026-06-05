package com.jannik_kuehn.common.storage.model;

import java.util.List;
import java.util.Objects;

/**
 * Preview of a maintenance operation.
 *
 * @param operation            operation kind
 * @param mappings             transfer mappings, if any
 * @param deleteScope          delete scope, if any
 * @param affectedSessions     affected session rows
 * @param affectedAdjustments  affected adjustment rows
 * @param affectedPlayers      affected player count
 * @param targetDataExists     true when target storage/scope already contains data
 * @param targetCollisions     target collision labels
 * @param confirmationRequired true when execution requires explicit confirmation
 * @param fingerprint          operation fingerprint for confirmation
 */
public record StorageMaintenancePreview(StorageMaintenanceOperation operation,
                                        List<StorageTransferMapping> mappings,
                                        StorageMaintenanceScope deleteScope,
                                        long affectedSessions,
                                        long affectedAdjustments,
                                        long affectedPlayers,
                                        boolean targetDataExists,
                                        List<String> targetCollisions,
                                        boolean confirmationRequired,
                                        String fingerprint) {

    public StorageMaintenancePreview {
        Objects.requireNonNull(operation, "operation");
        mappings = mappings == null ? List.of() : List.copyOf(mappings);
        targetCollisions = targetCollisions == null ? List.of() : List.copyOf(targetCollisions);
        Objects.requireNonNull(fingerprint, "fingerprint");
    }

    /**
     * Returns a confirmation for this preview.
     *
     * @return confirmation token
     */
    public StorageMaintenanceConfirmation confirmation() {
        return new StorageMaintenanceConfirmation(fingerprint);
    }
}
