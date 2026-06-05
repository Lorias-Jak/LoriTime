package com.jannik_kuehn.common.storage.model;

import java.util.List;
import java.util.Objects;

/**
 * Request to transfer one or more datasets.
 *
 * @param operation transfer operation
 * @param mappings  source-to-target mappings
 */
public record StorageTransferRequest(StorageMaintenanceOperation operation, List<StorageTransferMapping> mappings) {

    public StorageTransferRequest {
        Objects.requireNonNull(operation, "operation");
        mappings = List.copyOf(Objects.requireNonNull(mappings, "mappings"));
        if (mappings.isEmpty()) {
            throw new IllegalArgumentException("mappings must not be empty");
        }
        if (operation != StorageMaintenanceOperation.STORAGE_TYPE_TRANSFER
                && operation != StorageMaintenanceOperation.SERVER_TRANSFER
                && operation != StorageMaintenanceOperation.WORLD_TRANSFER) {
            throw new IllegalArgumentException("operation must be a transfer operation");
        }
    }

    /**
     * Creates a storage-type transfer request.
     *
     * @return storage-type transfer request
     */
    public static StorageTransferRequest storageTypeTransfer() {
        return new StorageTransferRequest(StorageMaintenanceOperation.STORAGE_TYPE_TRANSFER,
                List.of(new StorageTransferMapping(StorageMaintenanceScope.storage(), StorageMaintenanceScope.storage())));
    }

    /**
     * Creates a server transfer request.
     *
     * @param mappings server mappings
     * @return server transfer request
     */
    public static StorageTransferRequest serverTransfer(final List<StorageTransferMapping> mappings) {
        return new StorageTransferRequest(StorageMaintenanceOperation.SERVER_TRANSFER, mappings);
    }

    /**
     * Creates a world transfer request.
     *
     * @param mappings world mappings
     * @return world transfer request
     */
    public static StorageTransferRequest worldTransfer(final List<StorageTransferMapping> mappings) {
        return new StorageTransferRequest(StorageMaintenanceOperation.WORLD_TRANSFER, mappings);
    }
}
