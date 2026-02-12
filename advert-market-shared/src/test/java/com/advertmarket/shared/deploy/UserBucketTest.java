package com.advertmarket.shared.deploy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("UserBucket — stable bucketing for canary routing")
class UserBucketTest {

    @Test
    @DisplayName("Same input always produces the same bucket")
    void compute_sameInput_alwaysSameBucket() {
        long userId = 123456789L;
        int bucket1 = UserBucket.compute(userId, "salt1");
        int bucket2 = UserBucket.compute(userId, "salt1");
        assertThat(bucket1).isEqualTo(bucket2);
    }

    @Test
    @DisplayName("Bucket is always in range [0, 99]")
    void compute_bucketInRange() {
        for (long userId = 0; userId < 10_000; userId++) {
            int bucket = UserBucket.compute(userId, "");
            assertThat(bucket).isBetween(0, 99);
        }
    }

    @Test
    @DisplayName("Distribution is reasonably uniform across buckets")
    void compute_distributionIsReasonablyUniform() {
        var counts = new HashMap<Integer, Integer>();
        int total = 100_000;
        for (long userId = 0; userId < total; userId++) {
            int bucket = UserBucket.compute(userId, "test-salt");
            counts.merge(bucket, 1, Integer::sum);
        }

        // Each bucket should get ~1000 items (1%). Allow +/-30% variance.
        double expected = total / 100.0;
        for (int bucket = 0; bucket < 100; bucket++) {
            int count = counts.getOrDefault(bucket, 0);
            assertThat((double) count)
                    .as("Bucket %d count", bucket)
                    .isBetween(expected * 0.7, expected * 1.3);
        }
    }

    @Test
    @DisplayName("Different salt redistributes buckets for most users")
    void compute_differentSalt_differentBucket() {
        int changed = 0;
        for (long userId = 0; userId < 1000; userId++) {
            int b1 = UserBucket.compute(userId, "salt-a");
            int b2 = UserBucket.compute(userId, "salt-b");
            if (b1 != b2) {
                changed++;
            }
        }
        assertThat(changed).isGreaterThan(900);
    }

    @Test
    @DisplayName("isCanary returns false for 0%")
    void isCanary_zeroPercent_alwaysFalse() {
        for (long userId = 0; userId < 1000; userId++) {
            assertThat(UserBucket.isCanary(userId, "", 0)).isFalse();
        }
    }

    @Test
    @DisplayName("isCanary returns true for 100%")
    void isCanary_hundredPercent_alwaysTrue() {
        for (long userId = 0; userId < 1000; userId++) {
            assertThat(UserBucket.isCanary(userId, "", 100)).isTrue();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 25, 50})
    @DisplayName("Canary percentage matches approximate ratio")
    void isCanary_percentMatchesApproximateRatio(int percent) {
        int canaryCount = 0;
        int total = 100_000;
        for (long userId = 0; userId < total; userId++) {
            if (UserBucket.isCanary(userId, "test", percent)) {
                canaryCount++;
            }
        }

        double actualPercent = (canaryCount * 100.0) / total;
        assertThat(actualPercent)
                .as("Expected ~%d%% canary, got %.1f%%", percent, actualPercent)
                .isBetween(percent - 2.0, percent + 2.0);
    }

    @Test
    @DisplayName("Canary status is sticky per user")
    void isCanary_stickyPerUser_doesNotFlap() {
        long userId = 999999L;
        boolean first = UserBucket.isCanary(userId, "stable", 50);
        for (int i = 0; i < 100; i++) {
            assertThat(UserBucket.isCanary(userId, "stable", 50))
                    .as("User canary status must be stable")
                    .isEqualTo(first);
        }
    }

    @Test
    @DisplayName("Negative percent always returns false")
    void isCanary_negativePercent_alwaysFalse() {
        assertThat(UserBucket.isCanary(123L, "", -1)).isFalse();
    }

    @Test
    @DisplayName("Over 100% always returns true")
    void isCanary_overHundredPercent_alwaysTrue() {
        assertThat(UserBucket.isCanary(123L, "", 101)).isTrue();
    }

    @Test
    @DisplayName("Null salt treated as empty string")
    void compute_nullSalt_treatedAsEmpty() {
        int b1 = UserBucket.compute(123L, null);
        int b2 = UserBucket.compute(123L, "");
        assertThat(b1).isEqualTo(b2);
    }
}
