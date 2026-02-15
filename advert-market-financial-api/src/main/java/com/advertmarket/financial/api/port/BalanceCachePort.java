package com.advertmarket.financial.api.port;

import com.advertmarket.shared.model.AccountId;
import java.util.OptionalLong;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for balance caching (Redis or in-memory).
 */
public interface BalanceCachePort {

    /**
     * Returns cached balance for the account.
     *
     * @param accountId account to look up
     * @return cached balance in nanoTON, or empty if not cached
     */
    @NonNull OptionalLong get(@NonNull AccountId accountId);

    /**
     * Stores balance in cache.
     *
     * @param accountId account to cache
     * @param balanceNano balance in nanoTON
     */
    void put(@NonNull AccountId accountId, long balanceNano);

    /**
     * Removes cached balance.
     *
     * @param accountId account to evict
     */
    void evict(@NonNull AccountId accountId);
}
