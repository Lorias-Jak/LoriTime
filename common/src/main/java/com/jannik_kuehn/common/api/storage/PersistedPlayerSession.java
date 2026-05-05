package com.jannik_kuehn.common.api.storage;

/**
 * Active session context and storage row id.
 *
 * @param sessionId database session id
 * @param context   active session context
 */
public record PersistedPlayerSession(long sessionId, PlayerSessionContext context) {
}
