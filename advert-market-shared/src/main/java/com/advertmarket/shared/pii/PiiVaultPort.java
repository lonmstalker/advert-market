package com.advertmarket.shared.pii;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Port for PII encryption/decryption at rest.
 *
 * <p>Current implementation uses AES-256-GCM.
 * Migration path: replace with HashiCorp Vault Transit engine.
 */
public interface PiiVaultPort {

    /**
     * Encrypts plaintext and returns base64-encoded ciphertext.
     *
     * @param plaintext the text to encrypt
     * @return base64-encoded ciphertext (IV + encrypted data + GCM tag)
     */
    @NonNull String encrypt(@NonNull String plaintext);

    /**
     * Decrypts base64-encoded ciphertext.
     *
     * @param ciphertext base64-encoded ciphertext produced by {@link #encrypt}
     * @return the original plaintext
     * @throws PiiEncryptionException if decryption fails
     */
    @NonNull String decrypt(@NonNull String ciphertext);

    /**
     * Resolves an {@code ENC(...)} wrapped value by decrypting;
     * returns plaintext unchanged if not wrapped.
     *
     * @param value either {@code ENC(base64ciphertext)} or plain text
     * @return decrypted or original value
     */
    default @NonNull String resolve(@NonNull String value) {
        final int prefixLength = "ENC(".length();
        if (value.startsWith("ENC(") && value.endsWith(")")) {
            return decrypt(value.substring(prefixLength, value.length() - 1));
        }
        return value;
    }
}
