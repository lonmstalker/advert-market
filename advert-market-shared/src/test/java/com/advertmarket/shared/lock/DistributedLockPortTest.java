package com.advertmarket.shared.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.advertmarket.shared.exception.DomainException;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DistributedLockPort — withLock default method")
class DistributedLockPortTest {

    private final DistributedLockPort lock =
            mock(DistributedLockPort.class);

    @Test
    @DisplayName("withLock executes action and releases lock on success")
    void withLock_executesAndUnlocks() {
        when(lock.withLock(any(), any(), any()))
                .thenCallRealMethod();
        when(lock.tryLock(eq("key"), any()))
                .thenReturn(Optional.of("token-123"));

        String result = lock.withLock("key", Duration.ofSeconds(5),
                () -> "done");

        assertThat(result).isEqualTo("done");
        verify(lock).unlock("key", "token-123");
    }

    @Test
    @DisplayName("withLock throws DomainException when lock not acquired")
    void withLock_throwsOnLockFailure() {
        when(lock.withLock(any(), any(), any()))
                .thenCallRealMethod();
        when(lock.tryLock(eq("key"), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> lock.withLock(
                "key", Duration.ofSeconds(5), () -> "x"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Failed to acquire lock");
    }

    @Test
    @DisplayName("withLock releases lock even if action throws")
    void withLock_unlocksOnException() {
        when(lock.withLock(any(), any(), any()))
                .thenCallRealMethod();
        when(lock.tryLock(eq("key"), any()))
                .thenReturn(Optional.of("tok"));

        assertThatThrownBy(() -> lock.withLock(
                "key", Duration.ofSeconds(5),
                () -> {
                    throw new RuntimeException("boom");
                }))
                .isInstanceOf(RuntimeException.class);

        verify(lock).unlock("key", "tok");
    }

    @Test
    @DisplayName("withLock rejects blank key")
    void withLock_rejectsBlankKey() {
        when(lock.withLock(any(), any(), any()))
                .thenCallRealMethod();

        assertThatThrownBy(() -> lock.withLock(
                " ", Duration.ofSeconds(1), () -> "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("withLock rejects non-positive TTL")
    void withLock_rejectsZeroTtl() {
        when(lock.withLock(any(), any(), any()))
                .thenCallRealMethod();

        assertThatThrownBy(() -> lock.withLock(
                "key", Duration.ZERO, () -> "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
