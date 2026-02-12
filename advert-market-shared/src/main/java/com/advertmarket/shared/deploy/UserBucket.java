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

    private static final int BYTE_MASK = 0xFF;
    private static final int BUCKET_COUNT = 100;

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
            byte[] hash = digest.digest(
                    input.getBytes(StandardCharsets.UTF_8));
            int value = 0;
            for (int i = 0; i < Integer.BYTES; i++) {
                value = (value << Byte.SIZE)
                        | (hash[i] & BYTE_MASK);
            }
            return Math.abs(value % BUCKET_COUNT);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(
                    "SHA-256 not available", e);
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
    public static boolean isCanary(long userKey, String salt,
            int canaryPercent) {
        if (canaryPercent <= 0) {
            return false;
        }
        if (canaryPercent >= BUCKET_COUNT) {
            return true;
        }
        return compute(userKey, salt) < canaryPercent;
    }
}