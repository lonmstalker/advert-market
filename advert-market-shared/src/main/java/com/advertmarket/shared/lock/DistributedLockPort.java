package com.advertmarket.shared.lock;

import com.advertmarket.shared.exception.DomainException;
import com.advertmarket.shared.exception.ErrorCodes;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for distributed locking backed by an external store (e.g. Redis).
 *
 * <p>Implementations must use ownership tokens (e.g. random UUIDs)
 * to prevent releasing a lock that was re-acquired by another caller
 * after TTL expiry.
 */
public interface DistributedLockPort {

    /**
     * Attempts to acquire a lock for the given key.
     *
     * @param key lock identifier (must not be blank)
     * @param ttl maximum time the lock is held (must be positive)
     * @return an ownership token if the lock was acquired, or empty
     */
    @NonNull Optional<String> tryLock(
            @NonNull String key, @NonNull Duration ttl);

    /**
     * Releases a previously acquired lock.
     *
     * <p>Implementations must verify that the token matches the
     * current holder before releasing (compare-and-delete).
     *
     * @param key lock identifier
     * @param token the ownership token returned by {@link #tryLock}
     */
    void unlock(@NonNull String key, @NonNull String token);

    /**
     * Executes the given action while holding the lock.
     *
     * @param key lock identifier (must not be blank)
     * @param ttl maximum time the lock is held (must be positive)
     * @param action the action to execute
     * @param <T> action result type
     * @return the result of the action
     * @throws DomainException if the lock cannot be acquired
     */
    default <T> T withLock(
            @NonNull String key,
            @NonNull Duration ttl,
            @NonNull Supplier<T> action) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(ttl, "ttl");
        Objects.requireNonNull(action, "action");
        if (key.isBlank()) {
            throw new IllegalArgumentException("Lock key must not be blank");
        }
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException(
                    "Lock TTL must be positive, got: " + ttl);
        }

        String token = tryLock(key, ttl).orElseThrow(() ->
                new DomainException(
                        ErrorCodes.LOCK_ACQUISITION_FAILED,
                        "Failed to acquire lock: " + key));
        try {
            return action.get();
        } finally {
            unlock(key, token);
        }
    }
}
