# PRD: TON SDK Integration Hardening (av4.2)

## Introduction

Code audit of the TON SDK integration (`advert-market-financial`) revealed 16 issues: 3 CRITICAL (race condition, NPE, secret exposure), 5 HIGH (overflow, validation), 4 MEDIUM (error handling, config), and 4 test coverage gaps. This PRD addresses all findings through TDD — each fix starts with a failing test.

The TON SDK integration is the financial backbone: deposit address generation, blockchain polling, escrow confirmation. Bugs here can cause double-spend, lost funds, or leaked secrets.

## Goals

- Eliminate all CRITICAL and HIGH bugs before proceeding to av4.3 (Escrow) and av4.4 (Payout)
- Achieve >90% branch coverage on TON-related code
- Add integration tests for concurrent deposit processing
- Harden error handling for untrusted blockchain data

## User Stories

### US-001: Fix race condition in updateConfirmed (CRITICAL)

**Description:** As a platform operator, I need deposit confirmation to be atomic so that double-spend is impossible.

**Root cause:** `JooqTonTransactionRepository.updateConfirmed()` does NOT use CAS (Compare-And-Swap) via `version` column, unlike `updateStatus()`. Two concurrent DepositWatcher instances can both confirm the same deposit, leading to duplicate `DepositConfirmedEvent` and double escrow credit.

**Acceptance Criteria:**
- [ ] RED: Test that concurrent `updateConfirmed()` calls — only one succeeds (second returns false)
- [ ] GREEN: Add `expectedVersion` parameter to `updateConfirmed()`, add `.and(TON_TRANSACTIONS.VERSION.eq(expectedVersion))` to WHERE clause
- [ ] Pass `record.getVersion()` from `DepositWatcher.confirmDeposit()` into `updateConfirmed()`
- [ ] If `updateConfirmed()` returns false, log WARN and skip event publication
- [ ] All existing DepositWatcher tests pass
- [ ] New test: `shouldNotConfirmSameDepositTwice_concurrentCAS`

---

### US-002: Filter null txHash from TON Center response (CRITICAL)

**Description:** As a developer, I need blockchain data to be validated at the boundary so that null txHash never reaches domain logic.

**Root cause:** `TonCenterBlockchainAdapter.getTransactions()` maps `txId.getHash()` which can be null. `TonTransactionInfo.txHash` is `@NonNull` — contract violation causes NPE downstream.

**Acceptance Criteria:**
- [ ] RED: Test that response with `txId.hash = null` is filtered out (not returned)
- [ ] GREEN: Add `.filter(tx -> tx.getTransactionId() != null && tx.getTransactionId().getHash() != null)` before mapping
- [ ] RED: Test that response with `inMsg = null` is filtered out
- [ ] GREEN: Add `.filter(tx -> tx.getInMsg() != null)` before mapping
- [ ] Log WARN for each filtered transaction with available context
- [ ] All existing TonCenterBlockchainAdapterTest tests pass

---

### US-003: Eliminate mnemonic exposure in stack traces (CRITICAL)

**Description:** As a security engineer, I need the platform wallet mnemonic to never appear in logs, stack traces, or error messages.

**Root cause:** `TonWalletService.deriveKeyPair(String mnemonic)` — if `Mnemonic.toKeyPair()` throws, the exception's stack trace includes the mnemonic parameter value in debugging tools.

**Acceptance Criteria:**
- [ ] RED: Test that `deriveKeyPair` failure exception message does NOT contain mnemonic words
- [ ] GREEN: Catch exception, throw new `IllegalStateException("Failed to derive key pair from mnemonic")` without chaining original exception
- [ ] Verify `TonConfig.tonWalletService()` does NOT log decrypted mnemonic
- [ ] Add `@SuppressWarnings` comment ONLY if static analysis flags the intentional exception swallow, with rationale "security: prevent mnemonic leak"

---

### US-004: Add overflow guard for subwalletId long-to-int cast (HIGH)

**Description:** As a developer, I need subwalletId to be safely stored so that overflow doesn't create address collisions.

**Root cause:** `EscrowService.generateDepositAddress()` casts `long subwalletId` to `int` without bounds check. At >2B deals, overflow produces negative values and potential address collisions.

**Acceptance Criteria:**
- [ ] RED: Test that `subwalletId > Integer.MAX_VALUE` throws `IllegalStateException`
- [ ] GREEN: Add guard clause before cast in `EscrowService.generateDepositAddress()`
- [ ] Verify `DepositAddressInfo.subwalletId()` return type is `long`
- [ ] Verify DB column `deals.subwallet_id` is `INTEGER` and document future migration to `BIGINT` in `.memory-bank`

---

### US-005: Validate positive amount in DepositWatcher matching (HIGH)

**Description:** As a platform operator, I need deposits with zero or negative amounts to be rejected so that invalid blockchain data doesn't create false confirmations.

**Root cause:** `DepositWatcher.findMatchingTx()` filters `tx.amountNano() >= expectedAmount` but doesn't guard against `amountNano <= 0` from malformed TON Center responses.

**Acceptance Criteria:**
- [ ] RED: Test that TX with `amountNano = 0` is NOT matched
- [ ] RED: Test that TX with negative `amountNano` is NOT matched
- [ ] GREEN: Add `tx.amountNano() > 0` to filter predicate
- [ ] All existing DepositWatcher tests pass

---

### US-006: Fix seqno long-to-int overflow (HIGH)

**Description:** As a developer, I need seqno to be stored and transmitted as `long` to prevent overflow on high-activity wallets.

**Root cause:** `TonBlockchainPort.getSeqno()` returns `int`, but TON seqno is natively `long`. Cast overflow at >2B sends.

**Acceptance Criteria:**
- [ ] Change `TonBlockchainPort.getSeqno()` return type from `int` to `long`
- [ ] Update `TonCenterBlockchainAdapter.getSeqno()` — remove cast
- [ ] Update `TonWalletService.submitTransaction()` — use `long seqno`
- [ ] Update `ton_transactions.seqno` column type if needed (check current DDL)
- [ ] All existing tests updated and passing

---

### US-007: Fix seqno persistence for first-time deposit detection (HIGH)

**Description:** As a developer, I need the first-detected block seqno to be persisted so that confirmation counting starts correctly.

**Root cause:** `DepositWatcher.resolveConfirmedBlocks()` returns `-1` when `record.seqno = null`, triggering a no-op `updateStatus("PENDING")`. The TX's block seqno is never saved, so the deposit loops indefinitely without progressing.

**Acceptance Criteria:**
- [ ] RED: Test that first poll of a deposit with `seqno = null` saves the TX block seqno to the record
- [ ] GREEN: Extract block seqno from `TonTransactionInfo.lt` or add `blockSeqno` field to `TonTransactionInfo`
- [ ] Add `updateSeqno(id, seqno, expectedVersion)` method to `JooqTonTransactionRepository`
- [ ] After saving seqno, re-calculate confirmations in same cycle
- [ ] All existing DepositWatcher tests pass

---

### US-008: Handle duplicate TX in getTransactions matching (HIGH)

**Description:** As a developer, I need deposit matching to pick the correct transaction when TON Center returns multiple TXs to the same address.

**Root cause:** `DepositWatcher.findMatchingTx()` uses `.findFirst()` — order depends on TON Center API response ordering, which may not be deterministic.

**Acceptance Criteria:**
- [ ] RED: Test that when multiple TXs match amount, the most recent one (by `lt`) is selected
- [ ] GREEN: Replace `.findFirst()` with `.max(Comparator.comparingLong(TonTransactionInfo::lt))`
- [ ] RED: Test that already-confirmed txHashes (existing in DB) are excluded from matching
- [ ] GREEN: Filter out txHashes already in `ton_transactions` before matching
- [ ] All existing DepositWatcher tests pass

---

### US-009: Add retry tracking for failed deposit processing (MEDIUM)

**Description:** As a platform operator, I need failed deposit processing attempts to be tracked so that permanently broken deposits are escalated, not silently retried forever.

**Root cause:** `DepositWatcher.processOneSafely()` catches exceptions and logs them, but has no retry counter or failure escalation.

**Acceptance Criteria:**
- [ ] RED: Test that after N (configurable, default 5) failed processing attempts, deposit status transitions to `FAILED`
- [ ] GREEN: Add `retry_count INTEGER DEFAULT 0` column to `ton_transactions` (DB migration)
- [ ] Increment `retry_count` on each failed processing attempt
- [ ] When `retry_count >= maxRetries`: update status to `FAILED`, increment `ton.deposit.permanently_failed` metric
- [ ] Add `maxRetries` to `TonProperties.Deposit`

---

### US-010: Normalize wallet address format for seqno queries (MEDIUM)

**Description:** As a developer, I need consistent address formats when querying TON Center API so that seqno lookups always match.

**Root cause:** `TonWalletService.submitTransaction()` uses `wallet.getAddress().toBounceable()` for seqno queries, but the wallet's canonical form may differ from what TON Center expects.

**Acceptance Criteria:**
- [ ] RED: Test that seqno query uses the same address format as the wallet's on-chain address
- [ ] GREEN: Use `wallet.getAddress().toString()` (raw format) consistently
- [ ] Document address format convention in `AGENTS.md` for financial module

---

### US-011: Tune Circuit Breaker for TON Center API (MEDIUM)

**Description:** As a platform operator, I need the circuit breaker to tolerate TON Center's rate limiting (429) without unnecessarily blocking deposit monitoring.

**Root cause:** Default config opens circuit after 50% failure rate on 20 calls (min 10). TON Center 429s during burst polling can trigger this prematurely.

**Acceptance Criteria:**
- [ ] Separate 429 (rate limit) from 5xx (server error) in circuit breaker recording
- [ ] 429 responses should trigger backoff but NOT count as circuit breaker failures
- [ ] Update `TonCenterBlockchainAdapter` to throw distinct exception types for 429 vs 5xx
- [ ] Configure circuit breaker to ignore `TonRateLimitException`
- [ ] Add test for 429 handling

---

### US-012: Add integration tests for concurrent deposit processing

**Description:** As a developer, I need integration tests that verify the entire deposit flow under concurrent load to catch race conditions that unit tests miss.

**Acceptance Criteria:**
- [ ] Integration test: two threads call `pollDeposits()` simultaneously — verify only one confirms the deposit
- [ ] Integration test: `FOR UPDATE SKIP LOCKED` prevents double-processing of `findPendingDeposits()`
- [ ] Integration test: `updateConfirmed()` CAS prevents double event publication
- [ ] Tests use `SharedContainers` (PostgreSQL + Redis), NOT `@Container`
- [ ] Tests in `advert-market-integration-tests` module

---

### US-013: Add negative tests for TonCenterBlockchainAdapter

**Description:** As a developer, I need the blockchain adapter to handle all malformed responses gracefully, since TON Center data is untrusted external input.

**Acceptance Criteria:**
- [ ] Test: response with `txId = null` — filtered, not NPE
- [ ] Test: response with `txId.hash = null` — filtered, not NPE
- [ ] Test: response with `inMsg = null` — filtered, not NPE
- [ ] Test: response with `amountNano = "invalid_string"` — mapped to 0 or exception
- [ ] Test: empty result list — returns empty list, no exception
- [ ] Test: `ok = false` response — throws appropriate exception

---

### US-014: Add idempotency tests for EscrowService

**Description:** As a developer, I need tests proving that duplicate `confirmDeposit()` / `releaseEscrow()` calls are idempotent, not creating double ledger entries.

**Acceptance Criteria:**
- [ ] Test: `confirmDeposit()` called twice with same `txHash` — `ledgerPort.transfer()` called only once
- [ ] Test: `releaseEscrow()` called twice with same `dealId` — `ledgerPort.transfer()` called only once
- [ ] Test: `refundEscrow()` called twice with same `dealId` — `ledgerPort.transfer()` called only once
- [ ] Idempotency enforced via `idempotency_key` in `ledger_entries`

## Functional Requirements

- FR-1: `JooqTonTransactionRepository.updateConfirmed()` MUST use CAS via `version` column
- FR-2: `TonCenterBlockchainAdapter.getTransactions()` MUST filter transactions with null `txHash` or null `inMsg`
- FR-3: `TonWalletService` MUST NOT expose mnemonic in any log output or exception message
- FR-4: `EscrowService` MUST validate `subwalletId <= Integer.MAX_VALUE` before storing
- FR-5: `DepositWatcher` MUST reject transactions with `amountNano <= 0`
- FR-6: `TonBlockchainPort.getSeqno()` MUST return `long`, not `int`
- FR-7: `DepositWatcher` MUST persist block seqno on first TX detection for correct confirmation counting
- FR-8: `DepositWatcher` MUST select the most recent matching TX by logical time (`lt`) when multiple match
- FR-9: `DepositWatcher` MUST track retry count and escalate to FAILED after max retries
- FR-10: Circuit breaker MUST NOT count HTTP 429 as failures
- FR-11: All fixes MUST follow TDD (RED test first, then GREEN implementation)

## Non-Goals

- No new features — this is hardening/bugfix only
- No payout/refund execution (that's av4.4)
- No escrow lifecycle changes (that's av4.3)
- No wallet REST API changes (that's av4.6)
- No reconciliation (that's 4fr.2)
- No hot wallet management (that's av4.7)

## Technical Considerations

- **Database migration**: US-009 requires `ALTER TABLE ton_transactions ADD COLUMN retry_count INTEGER DEFAULT 0`
- **API breaking change**: US-006 changes `TonBlockchainPort.getSeqno()` return type from `int` to `long`. All callers must be updated.
- **Circuit breaker**: US-011 requires a custom `RecordExceptionPredicate` in Resilience4j config
- **Integration tests**: US-012 goes in `advert-market-integration-tests` module, uses `SharedContainers` singleton
- **ton4j 1.3.2**: WalletV4R2 API for address format normalization

## Success Metrics

- 0 CRITICAL bugs in TON financial code
- 0 HIGH bugs in TON financial code
- Branch coverage >90% for `ton/` package
- Integration tests prove no double-spend under concurrent load
- Circuit breaker does not open on 429 rate limiting

## Implementation Order

Story dependencies require this execution sequence:

```
Phase 1 (CRITICAL — do first):
  US-001 (CAS fix)  ←  US-012 depends on this
  US-002 (null txHash filter)
  US-003 (mnemonic security)

Phase 2 (HIGH — unblocks av4.3/av4.4):
  US-006 (seqno long)  ←  US-007 depends on this
  US-007 (seqno persistence)
  US-004 (subwalletId overflow)
  US-005 (amount validation)
  US-008 (duplicate TX matching)

Phase 3 (MEDIUM + Tests):
  US-009 (retry tracking) — needs DB migration
  US-010 (address format)
  US-011 (circuit breaker tuning)
  US-012 (integration tests) — depends on US-001
  US-013 (negative adapter tests)
  US-014 (idempotency tests)
```

## Open Questions

1. Should `ton_transactions.seqno` be migrated from `INTEGER` to `BIGINT`? (US-006 changes Java type but DB may stay `INTEGER` if overflow is impractical for wallet seqno)
2. What should `maxRetries` default be for DepositWatcher? (proposed: 5, configurable via `app.ton.deposit.max-retries`)
3. Should permanently FAILED deposits trigger a Telegram notification to the operator? (deferred to communication module)
