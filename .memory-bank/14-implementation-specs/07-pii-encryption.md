# PII Encryption & Key Management

## Overview

Field-level encryption for PII data (TON addresses, phone numbers) using AES-256-GCM. Stored in `pii_store` table with `key_version` for rotation support.

---

## Encryption Algorithm: AES-256-GCM

| Parameter | Value |
|-----------|-------|
| Algorithm | AES/GCM/NoPadding |
| Key size | 256 bits (32 bytes) |
| IV/Nonce size | 12 bytes (96 bits) |
| Tag size | 128 bits (16 bytes) |
| Output format | `IV (12 bytes) + ciphertext + tag (16 bytes)` |

### Why AES-256-GCM?

- Authenticated encryption (integrity + confidentiality)
- Built into Java standard library (`javax.crypto`)
- No external dependencies needed (no Bouncy Castle)
- GCM mode provides tamper detection via authentication tag

---

## Key Management

### MVP: Environment Variable

```yaml
pii:
  encryption:
    key: ${PII_ENCRYPTION_KEY}  # Base64-encoded 32-byte key
    current-version: 1
```

**Key generation**:
```bash
openssl rand -base64 32
```

### Scaled: AWS KMS / HashiCorp Vault

- Envelope encryption: KMS master key wraps data encryption key (DEK)
- DEK cached in memory for performance
- Master key never leaves KMS
- Automatic key rotation via KMS policy

---

## Storage Schema

```sql
-- pii_store table
user_id         BIGINT      -- FK to users
field_name      VARCHAR(50) -- e.g., 'ton_address', 'phone'
encrypted_value BYTEA       -- IV + ciphertext + tag
key_version     INTEGER     -- which key encrypted this value
```

---

## Encryption / Decryption Flow

### Encrypt

1. Load current encryption key by `current-version`
2. Generate random 12-byte IV via `SecureRandom`
3. Initialize `Cipher` with `AES/GCM/NoPadding`, ENCRYPT_MODE
4. Set `GCMParameterSpec(128, iv)`
5. Encrypt plaintext bytes
6. Concatenate: `IV (12) + ciphertext + tag`
7. Store as `BYTEA` in `pii_store` with `key_version`

### Decrypt

1. Read `key_version` from `pii_store` record
2. Load encryption key for that version
3. Extract IV (first 12 bytes) from `encrypted_value`
4. Extract ciphertext+tag (remaining bytes)
5. Initialize `Cipher` with DECRYPT_MODE and `GCMParameterSpec(128, iv)`
6. Decrypt and return plaintext

---

## Key Rotation Procedure

1. Generate new key, assign `key_version = N+1`
2. Update config: `pii.encryption.current-version = N+1`
3. New writes use version N+1
4. Background job: re-encrypt all records with `key_version < N+1`
5. After migration complete: decommission old keys

**Important**: During rotation, both old and new keys must be available.

### Key Registry

```yaml
pii:
  encryption:
    current-version: 2
    keys:
      1: ${PII_KEY_V1}  # Base64
      2: ${PII_KEY_V2}  # Base64
```

---

## PII Vault Service API

Internal service interface:

| Method | Purpose |
|--------|---------|
| `store(userId, fieldName, plaintext)` | Encrypt and store |
| `retrieve(userId, fieldName)` | Decrypt and return |
| `delete(userId, fieldName)` | Remove PII record |
| `reEncrypt(userId, fieldName)` | Re-encrypt with current key version |

---

## Security Considerations

- IV must be unique per encryption (random generation via `SecureRandom`)
- Never reuse IV with same key (GCM catastrophic failure)
- Key must not appear in logs or error messages
- Decrypted values must not be cached in Redis
- Access to PII Vault service via internal API only (no public exposure)

---

## Related Documents

- [Security & Compliance](../10-security-and-compliance.md)
- [Data Stores](../04-architecture/05-data-stores.md)
