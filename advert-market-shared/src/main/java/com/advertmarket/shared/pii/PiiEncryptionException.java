package com.advertmarket.shared.pii;

/**
 * Thrown when PII encryption or decryption fails.
 */
public class PiiEncryptionException extends RuntimeException {

    /**
     * Creates a new PII encryption exception.
     *
     * @param message description of the encryption/decryption failure
     * @param cause   underlying cryptographic exception
     */
    public PiiEncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
