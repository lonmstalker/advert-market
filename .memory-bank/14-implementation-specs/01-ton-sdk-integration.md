# TON SDK Integration Specification

## SDK Choice: ton4j

**Decision**: `ton4j` (io.github.neodix42:ton4j) — native Java SDK for TON blockchain.

| Criterion | ton4j | ton-kotlin | tonweb (JS) |
|-----------|-------|-----------|-------------|
| Language | Native Java | Kotlin (JVM-compat) | JavaScript |
| Maintenance | Active, community | TON Foundation | Community |
| Type safety | Full | Full | None |
| Spring Boot | Natural | OK (Kotlin interop) | Awkward |

### Gradle

```groovy
dependencies {
    implementation 'io.github.neodix42:smartcontract:1.3.2'
    implementation 'io.github.neodix42:tonlib:1.3.2'
    implementation 'io.github.neodix42:address:1.3.2'
}
```

> Modules `cell` and `mnemonic` are transitive dependencies. See `platform-bom/build.gradle` for version management.

---

## TON Center API

See [TON Center API Endpoint Catalog](./08-ton-center-api-catalog.md) for endpoints, rate limits, error handling, and retry strategy.

---

## Address Generation

### Bounceable vs Non-Bounceable

| Type | Prefix | Use |
|------|--------|-----|
| Bounceable | `EQ...` | Active smart contracts |
| Non-bounceable | `UQ...` | First deposit to uninitialized wallet |

**Decision**: Generate **non-bounceable** (`UQ...`) deposit addresses for advertisers, since wallet may be uninitialized.

### Strategy: Subwallet ID Derivation

Each deal gets a unique deposit address derived from a single platform mnemonic using different `subwallet_id`:

```
subwallet_id = nextval('deal_subwallet_seq')   -- DB sequence, guaranteed unique
wallet = WalletV4R2(publicKey, subwalletId) -> unique address per deal
```

This avoids managing multiple private keys — one mnemonic generates all addresses.

**IMPORTANT**: Previous design used `Math.abs(dealId.hashCode())` which had two critical flaws:
1. `Math.abs(Integer.MIN_VALUE)` returns negative (Java overflow)
2. Birthday paradox: 32-bit space -> 50% collision at ~65K deals

**Fix (006-financial-fixes.sql)**: DB sequence `deal_subwallet_seq` + `UNIQUE` constraint on `deals.subwallet_id`.

### Key Storage

| Environment | Method |
|-------------|--------|
| MVP | `TON_WALLET_MNEMONIC` env var (24 words) |
| Scaled | AWS KMS / HashiCorp Vault, envelope encryption |

---

## Transaction Building Patterns

### Payout (Platform -> Owner)

1. Look up deal's `subwallet_id`
2. Decrypt owner's TON address from `pii_store` (via PII Vault)
3. Get wallet seqno via `getAddressInformation`
4. Build internal transfer message with WalletV4R2 contract
5. Sign with platform's private key
6. Encode as BOC (Bag of Cells)
7. Submit via `POST /sendBoc`
8. Record in `ton_transactions` (direction=OUT, status=PENDING)
9. Poll for confirmation

### Refund (Platform -> Advertiser)

Same as payout but:
- `toAddress` = original sender from `ton_transactions` where direction=IN
- Comment/memo: `refund:{deal_id}`

### Fee Estimation

Before submitting, call `estimateFee` to verify sufficient balance. Typical transfer fee: ~0.005 TON.

---

## Deposit Watching Algorithm

```
1. Receive WATCH_DEPOSIT command from Kafka (deal_id)
2. Load deal.deposit_address
3. Call GET /getTransactions for that address
4. For each incoming transaction:
   a. Check tx_hash not already in ton_transactions (idempotency)
   b. Insert ton_transactions (direction=IN, status=PENDING, confirmations=0)
5. For pending deposits:
   a. Get current masterchain seqno via getMasterchainInfo
   b. Calculate: confirmations = current_seqno - tx_block_seqno
   c. Update ton_transactions.confirmations
   d. If confirmations >= required (per confirmation policy):
      - Validate amount (exact match, over/underpayment)
      - Send DEPOSIT_CONFIRMED to escrow.confirmations topic
```

### Amount Validation

| Case | Action |
|------|--------|
| exact match | Accept, proceed to FUNDED |
| overpayment | Accept, flag for manual review (refund difference later) |
| underpayment | Reject, notify advertiser |
| multiple deposits to same address | Sum all, then validate |

---

## Configuration

API configuration: see [TON Center API Catalog](./08-ton-center-api-catalog.md).

```yaml
ton:
  wallet:
    mnemonic: ${TON_WALLET_MNEMONIC:}   # 24-word platform wallet mnemonic
  deposit:
    poll-interval: 10s
```

---

## TX Lifecycle & Failure Recovery

### Outbound TX States

```
PENDING -> SUBMITTED -> CONFIRMING -> CONFIRMED
                     -> FAILED -> RETRY_PENDING -> SUBMITTED (retry)
                                -> ABANDONED (max retries)
```

| State | Description |
|-------|-------------|
| `PENDING` | TX built and signed, not yet submitted |
| `SUBMITTED` | `sendBoc` returned OK, waiting for on-chain confirmation |
| `CONFIRMING` | TX seen on-chain, counting confirmations |
| `CONFIRMED` | Required confirmations reached |
| `FAILED` | `sendBoc` failed or TX not seen after timeout |
| `RETRY_PENDING` | Queued for retry |
| `ABANDONED` | Max retries exceeded, operator intervention required |

### Failure Scenarios & Recovery

| Scenario | Detection | Recovery |
|----------|-----------|----------|
| `sendBoc` returns HTTP error | Immediate | Retry with backoff (max 3) |
| `sendBoc` returns OK but TX not on-chain after 5 min | Polling timeout | Re-query `getTransactions` for the source address. If TX missing, rebuild and resubmit. |
| TX on-chain but revert (insufficient balance) | `getTransactions` shows failed TX | Alert operator. Do NOT auto-retry (balance issue). |
| Redis failover during payout lock | Lock lost, duplicate payout risk | Idempotency guard: check `ton_transactions` for existing OUT tx with same `deal_id` before submitting |
| App crash after `sendBoc` but before DB update | TX submitted but status unknown | On startup: reconcile `ton_transactions` with on-chain state for all `SUBMITTED` records |

### Payout/Refund TX Retry Algorithm

```
1. Build TX (sign with platform key)
2. Acquire Redis lock: lock:payout:{deal_id} (TTL 120s)
3. Check ton_transactions for existing OUT tx with same deal_id
   -> If CONFIRMED: skip (idempotent)
   -> If SUBMITTED: poll for confirmation instead of re-sending
4. INSERT ton_transactions (status=PENDING)
5. Call sendBoc
   -> Success: UPDATE status=SUBMITTED
   -> Failure: UPDATE status=FAILED, schedule retry
6. Release lock
7. Poll for confirmation (separate scheduled task)
```

### Confirmation Polling (Outbound TX)

```
For each ton_transactions WHERE direction=OUT AND status=SUBMITTED:
1. Call getTransactions for platform wallet address
2. Match by BOC hash or amount+destination+timestamp
3. If found and confirmations >= 1:
   -> UPDATE status=CONFIRMED, confirmations=N
   -> Emit PAYOUT_COMPLETED / REFUND_COMPLETED event
4. If not found after max_poll_duration (10 min):
   -> UPDATE status=FAILED
   -> Increment retry_count
   -> If retry_count < 3: schedule retry
   -> If retry_count >= 3: UPDATE status=ABANDONED, alert operator
```

### Operator Manual Resolution

Для `ABANDONED` транзакций:

| Action | Endpoint | Description |
|--------|----------|-------------|
| Manual retry | `POST /api/v1/admin/tx/{txHash}/retry` | Rebuild and resubmit TX |
| Manual confirm | `POST /api/v1/admin/tx/{txHash}/confirm` | Mark as confirmed (after manual on-chain verification) |
| Manual refund | `POST /api/v1/admin/tx/{txHash}/refund` | Trigger alternative refund path |

### Configuration

```yaml
ton:
  tx:
    submit-timeout: 120s
    confirmation-poll-interval: 15s
    max-poll-duration: 10m
    max-retries: 3
    retry-backoff: 30s, 60s, 120s
```

### Monitoring

| Metric | Type | Alert |
|--------|------|-------|
| `ton.tx.submitted` | Counter | -- |
| `ton.tx.confirmed` | Counter | -- |
| `ton.tx.failed` | Counter | rate > 3/hour -> WARNING |
| `ton.tx.abandoned` | Counter | any > 0 -> CRITICAL |
| `ton.tx.confirmation.duration` | Timer | p99 > 5min -> WARNING |

---

## Related Documents

- [Escrow Flow](../07-financial-system/02-escrow-flow.md)
- [Confirmation Policy](./15-confirmation-policy.md)
- [Seqno Management](./43-seqno-management.md)
- [Workers -- TON Deposit Watcher](../04-architecture/04-workers.md)
- [External API Resilience](./28-external-api-resilience.md)