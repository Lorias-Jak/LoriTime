package com.jannik_kuehn.common.storage.contract;

import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.model.StorageDeleteRequest;
import com.jannik_kuehn.common.storage.model.StorageMaintenanceConfirmation;
import com.jannik_kuehn.common.storage.model.StorageMaintenancePreview;
import com.jannik_kuehn.common.storage.model.StorageMaintenanceResult;
import com.jannik_kuehn.common.storage.model.StorageTransferRequest;

/**
 * Optional admin-only maintenance contract for bulk storage operations.
 */
public interface AdminStorageMaintenance {

    /**
     * Previews a full storage-to-storage transfer into a target maintenance backend.
     *
     * @param target target maintenance backend
     * @return operation preview
     * @throws StorageException if the preview fails or the target is unsupported
     */
    default StorageMaintenancePreview previewStorageTransferTo(final AdminStorageMaintenance target)
            throws StorageException {
        throw new StorageException("Storage-type transfer is not supported by this storage");
    }

    /**
     * Applies a full storage-to-storage transfer into a target maintenance backend.
     *
     * @param target       target maintenance backend
     * @param confirmation confirmation from the preview
     * @return operation result
     * @throws StorageException if the transfer fails or the target is unsupported
     */
    default StorageMaintenanceResult applyStorageTransferTo(final AdminStorageMaintenance target,
                                                            final StorageMaintenanceConfirmation confirmation)
            throws StorageException {
        throw new StorageException("Storage-type transfer is not supported by this storage");
    }

    /**
     * Previews a transfer operation without mutating storage.
     *
     * @param request transfer request
     * @return operation preview
     * @throws StorageException if the preview fails
     */
    StorageMaintenancePreview previewTransfer(StorageTransferRequest request) throws StorageException;

    /**
     * Applies a transfer operation after preview confirmation.
     *
     * @param request      transfer request
     * @param confirmation confirmation from the preview
     * @return operation result
     * @throws StorageException if the transfer fails
     */
    StorageMaintenanceResult applyTransfer(StorageTransferRequest request,
                                           StorageMaintenanceConfirmation confirmation) throws StorageException;

    /**
     * Previews a scoped delete operation without mutating storage.
     *
     * @param request delete request
     * @return operation preview
     * @throws StorageException if the preview fails
     */
    StorageMaintenancePreview previewDelete(StorageDeleteRequest request) throws StorageException;

    /**
     * Applies a scoped delete operation after preview confirmation.
     *
     * @param request      delete request
     * @param confirmation confirmation from the preview
     * @return operation result
     * @throws StorageException if the delete fails
     */
    StorageMaintenanceResult applyDelete(StorageDeleteRequest request,
                                         StorageMaintenanceConfirmation confirmation) throws StorageException;
}
