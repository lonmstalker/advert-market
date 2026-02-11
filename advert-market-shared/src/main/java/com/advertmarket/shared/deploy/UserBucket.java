package com.advertmarket.shared.deploy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Stable bucketing for canary routing.
 * Uses SHA-256 hash of user key to produce a deterministic bucket [0..99].
 * Same user always lands in the same bucket across instances and restarts.
 */
public final class UserBucket {

    private UserBucket() {
    }

    /**
     * Compute a stable bucket in range [0, 100) for the given user key.
     *
     * @param userKey stable identifier (Telegram user_id or chat_id)
     * @param salt    optional salt to redistribute buckets; empty string for no salt
     * @return bucket in [0, 99]
     */
    public static int compute(long userKey, String salt) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var input = userKey + ":" + (salt != null ? salt : "");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            // Use first 4 bytes as unsigned int, mod 100
            int value = ((hash[0] & 0xFF) << 24)
                    | ((hash[1] & 0xFF) << 16)
                    | ((hash[2] & 0xFF) << 8)
                    | (hash[3] & 0xFF);
            return Math.abs(value % 100);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the JVM spec
            throw new AssertionError("SHA-256 not available", e);
        }
    }

    /**
     * Check if a user should be routed to canary.
     *
     * @param userKey        stable Telegram user id
     * @param salt           hash salt
     * @param canaryPercent  canary traffic percentage [0..100]
     * @return true if user should go to canary
     */
    public static boolean isCanary(long userKey, String salt, int canaryPercent) {
        if (canaryPercent <= 0) return false;
        if (canaryPercent >= 100) return true;
        return compute(userKey, salt) < canaryPercent;
    }
}