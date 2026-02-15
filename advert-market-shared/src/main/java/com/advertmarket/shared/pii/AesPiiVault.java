package com.advertmarket.shared.pii;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * AES-256-GCM implementation of {@link PiiVaultPort}.
 *
 * <p>Ciphertext format: {@code base64(12-byte IV || ciphertext || 16-byte GCM tag)}.
 * Each encryption uses a unique random IV.
 *
 * <p>NOT {@code @Component} — wired via SharedInfrastructureConfig.
 */
public final class AesPiiVault implements PiiVaultPort {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int KEY_LENGTH_BITS = 256;

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Creates a vault with the given master key.
     *
     * @param masterKey 256-bit (32-byte) AES key
     */
    public AesPiiVault(byte @NonNull [] masterKey) {
        final int keyLengthBytes = KEY_LENGTH_BITS / Byte.SIZE;
        if (masterKey.length != keyLengthBytes) {
            throw new IllegalArgumentException(
                    "Master key must be 256 bits (32 bytes), got: " + masterKey.length);
        }
        this.keySpec = new SecretKeySpec(masterKey, "AES");
    }

    @Override
    public @NonNull String encrypt(@NonNull String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            var cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));

            byte[] ciphertextBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[IV_LENGTH + ciphertextBytes.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(ciphertextBytes, 0, combined, IV_LENGTH, ciphertextBytes.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException ex) {
            throw new PiiEncryptionException("Failed to encrypt PII data", ex);
        }
    }

    @Override
    public @NonNull String decrypt(@NonNull String ciphertext) {
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            if (combined.length < IV_LENGTH) {
                throw new PiiEncryptionException("Ciphertext too short",
                        new IllegalArgumentException("Length: " + combined.length));
            }

            var cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec,
                    new GCMParameterSpec(TAG_LENGTH_BITS, combined, 0, IV_LENGTH));

            byte[] plainBytes = cipher.doFinal(combined, IV_LENGTH, combined.length - IV_LENGTH);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new PiiEncryptionException("Failed to decrypt PII data", ex);
        }
    }

    /**
     * Generates a random 256-bit AES key.
     *
     * @return 32-byte key suitable for use with this vault
     */
    public static byte[] generateKey() {
        try {
            var keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(KEY_LENGTH_BITS, new SecureRandom());
            return keyGen.generateKey().getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new PiiEncryptionException("Failed to generate AES key", ex);
        }
    }
}
