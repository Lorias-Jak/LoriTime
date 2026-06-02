package com.jannik_kuehn.common.api.storage;

import java.util.Objects;

/**
 * Request to delete one server or world dataset.
 *
 * @param scope scope to delete
 */
public record StorageDeleteRequest(StorageMaintenanceScope scope) {

    public StorageDeleteRequest {
        Objects.requireNonNull(scope, "scope");
        if (scope.type() == StorageMaintenanceScope.Type.STORAGE) {
            throw new IllegalArgumentException("delete scope must be server or world");
        }
    }
}
