package com.jannik_kuehn.common.api.storage;

import java.util.Objects;

/**
 * Confirmation token produced by a maintenance preview.
 *
 * @param fingerprint operation fingerprint
 */
public record StorageMaintenanceConfirmation(String fingerprint) {

    public StorageMaintenanceConfirmation {
        Objects.requireNonNull(fingerprint, "fingerprint");
        if (fingerprint.isBlank()) {
            throw new IllegalArgumentException("fingerprint must not be blank");
        }
    }
}
