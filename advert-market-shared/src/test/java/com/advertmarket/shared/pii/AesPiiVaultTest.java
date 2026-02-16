package com.advertmarket.shared.pii;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.KeyGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AesPiiVault — AES-256-GCM encryption")
class AesPiiVaultTest {

    private final byte[] masterKey = generateAesKey();
    private final AesPiiVault vault = new AesPiiVault(masterKey);

    @Test
    @DisplayName("encrypt then decrypt returns original plaintext")
    void encryptDecrypt_roundtrip() {
        String plaintext = "abandon ability able about above absent";
        String ciphertext = vault.encrypt(plaintext);

        assertThat(vault.decrypt(ciphertext)).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("encrypt produces different ciphertext for same plaintext (unique IV)")
    void encrypt_uniqueIvs() {
        String plaintext = "same text twice";
        String ct1 = vault.encrypt(plaintext);
        String ct2 = vault.encrypt(plaintext);

        assertThat(ct1).isNotEqualTo(ct2);
        assertThat(vault.decrypt(ct1)).isEqualTo(plaintext);
        assertThat(vault.decrypt(ct2)).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("decrypt throws on tampered ciphertext")
    void decrypt_tamperedCiphertext() {
        String ciphertext = vault.encrypt("secret");
        byte[] raw = Base64.getDecoder().decode(ciphertext);
        raw[raw.length - 1] ^= 0xFF;
        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> vault.decrypt(tampered))
                .isInstanceOf(PiiEncryptionException.class);
    }

    @Test
    @DisplayName("decrypt with wrong key throws exception")
    void decrypt_wrongKey() {
        String ciphertext = vault.encrypt("secret data");
        var wrongVault = new AesPiiVault(generateAesKey());

        assertThatThrownBy(() -> wrongVault.decrypt(ciphertext))
                .isInstanceOf(PiiEncryptionException.class);
    }

    @Test
    @DisplayName("resolve unwraps ENC(...) and decrypts")
    void resolve_encWrapped() {
        String plaintext = "my mnemonic words";
        String encrypted = vault.encrypt(plaintext);
        String wrapped = "ENC(" + encrypted + ")";

        assertThat(vault.resolve(wrapped)).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("resolve returns plaintext unchanged when not ENC-wrapped")
    void resolve_plaintext() {
        String plaintext = "not encrypted at all";

        assertThat(vault.resolve(plaintext)).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("encrypt handles empty string")
    void encryptDecrypt_emptyString() {
        String ciphertext = vault.encrypt("");
        assertThat(vault.decrypt(ciphertext)).isEmpty();
    }

    @Test
    @DisplayName("generateKey produces valid 256-bit key")
    void generateKey_valid() {
        byte[] key = AesPiiVault.generateKey();
        assertThat(key).hasSize(32);
    }

    private static byte[] generateAesKey() {
        try {
            var keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256, new SecureRandom());
            return keyGen.generateKey().getEncoded();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Cannot generate AES key", ex);
        }
    }
}
