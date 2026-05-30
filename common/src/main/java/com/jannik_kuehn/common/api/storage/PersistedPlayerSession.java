package com.jannik_kuehn.common.api.storage;

/**
 * Active session context and storage row id.
 *
 * @param sessionId         database session id
 * @param context           active session context
 * @param lastPersistedAtMs timestamp of the last persistence in epoch milliseconds
 */
public record PersistedPlayerSession(long sessionId, PlayerSessionContext context, long lastPersistedAtMs) {
}
