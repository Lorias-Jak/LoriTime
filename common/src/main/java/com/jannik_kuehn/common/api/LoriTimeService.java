package com.jannik_kuehn.common.api;

import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.api.storage.TimeScope;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Stable public facade for third-party LoriTime integrations.
 */
@SuppressWarnings("PMD.CommentRequired")
public interface LoriTimeService {
    /**
     * Actor name used for API adjustments without an explicit actor.
     */
    String API_ACTOR = "API";

    CompletableFuture<Optional<UUID>> findUuid(String playerName);

    CompletableFuture<Optional<String>> findName(UUID uniqueId);

    CompletableFuture<Optional<Duration>> getOnlineTime(UUID uniqueId);

    CompletableFuture<Optional<Duration>> getOnlineTime(UUID uniqueId, TimeScope scope);

    CompletableFuture<Optional<Duration>> getOnlineTime(UUID uniqueId, TimeScope scope, TimeRange range);

    CompletableFuture<Optional<Duration>> getOnlineTime(LoriTimePlayer player);

    CompletableFuture<Optional<Duration>> getOnlineTime(LoriTimePlayer player, TimeScope scope);

    CompletableFuture<Optional<Duration>> getOnlineTime(LoriTimePlayer player, TimeScope scope, TimeRange range);

    CompletableFuture<Void> addTime(UUID uniqueId, Duration amount);

    CompletableFuture<Void> addTime(UUID uniqueId, Duration amount, TimeScope scope);

    CompletableFuture<Void> addTime(LoriTimePlayer player, Duration amount);

    CompletableFuture<Void> addTime(LoriTimePlayer player, Duration amount, TimeScope scope);

    CompletableFuture<Void> addTime(UUID uniqueId, Duration amount, UUID actorUuid, String actorName);

    CompletableFuture<Void> addTime(UUID uniqueId, Duration amount, UUID actorUuid, String actorName, TimeScope scope);

    CompletableFuture<Void> addTime(LoriTimePlayer player, Duration amount, LoriTimePlayer actor);

    CompletableFuture<Void> addTime(LoriTimePlayer player, Duration amount, LoriTimePlayer actor, TimeScope scope);
}
