package com.jannik_kuehn.common.api.storage;

/**
 * Result of applying a storage maintenance operation.
 *
 * @param operation           operation kind
 * @param affectedSessions    affected session rows
 * @param affectedAdjustments affected adjustment rows
 * @param affectedPlayers     affected player count
 */
public record StorageMaintenanceResult(StorageMaintenanceOperation operation,
                                       long affectedSessions,
                                       long affectedAdjustments,
                                       long affectedPlayers) {
}
