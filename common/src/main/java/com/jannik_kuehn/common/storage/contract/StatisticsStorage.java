package com.jannik_kuehn.common.storage.contract;

import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.api.storage.TimeScope;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.model.AfkPeriod;
import com.jannik_kuehn.common.storage.model.AfkPeriodEndReason;
import com.jannik_kuehn.common.storage.model.StatisticsRequest;
import com.jannik_kuehn.common.storage.model.StatisticsSnapshot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Focused canonical statistics and AFK-history storage capability. */
@SuppressWarnings("PMD.CommentRequired")
public interface StatisticsStorage {
    void openAfkPeriod(UUID playerId, String playerName, String server, String world, Instant startedAt)
            throws StorageException;

    void closeAfkPeriod(UUID playerId, Instant endedAt, AfkPeriodEndReason reason) throws StorageException;

    int recoverOpenAfkPeriods(Instant endedAt) throws StorageException;

    List<AfkPeriod> getAfkPeriods(TimeRange range, TimeScope scope) throws StorageException;

    StatisticsSnapshot getStatistics(StatisticsRequest request) throws StorageException;
}
