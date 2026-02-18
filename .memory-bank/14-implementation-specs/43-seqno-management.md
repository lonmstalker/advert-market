# Seqno Management for WalletV4R2 Subwallets

## Overview

WalletV4R2 requires a strictly increasing `seqno` (sequence number) for each outbound message. Sending a message with an incorrect seqno causes TX rejection. This spec defines how to manage seqno when multiple outbound TXs are required from the same subwallet (e.g., payout + commission sweep).

## Problem

A single deal subwallet may need to send **multiple outbound TXs** in sequence:

| Scenario | TX Count | From Same Subwallet |
|----------|----------|---------------------|
| Happy path release | 2 | Payout to owner + commission sweep |
| Refund | 1 | Refund to advertiser |
| Partial refund | 3 | Refund + owner payout + commission sweep |
| Late deposit refund | 1 | Auto-refund to advertiser |
| Overpayment refund | 1 | Excess refund to advertiser |

WalletV4R2 enforces:
- Each outbound message must include `seqno = current_wallet_seqno`
- After TX confirmed, wallet's seqno increments by 1
- Sending with wrong seqno → TX silently dropped or rejected

## Solution: Sequential TX Queue per Subwallet

### Architecture

```
PayoutExecutor
  └─ SubwalletTxQueue (per subwallet_id)
       ├─ TX #1: Payout to owner    (seqno = N)   → wait confirm → seqno = N+1
       ├─ TX #2: Commission sweep    (seqno = N+1) → wait confirm → seqno = N+2
       └─ (done)
```

### Seqno Retrieval

Before building any TX, retrieve the current seqno from the blockchain:

```
GET /getAddressInformation?address={subwallet_address}
Response: { "result": { "seqno": N, ... } }
```

**Cache policy**: Do NOT cache seqno. Always fetch fresh from chain before each TX submission, because:
1. Another TX may have incremented seqno
2. Network forks may reset state
3. Cache invalidation is more complex than a single API call

### TX Submission Algorithm

```
submitSubwalletTxBatch(subwalletId, txList):
  1. Acquire Redis lock: lock:subwallet-tx:{subwalletId} (TTL 300s)
  2. For each TX in txList (ordered):
     a. Fetch current seqno: GET /getAddressInformation
     b. Build & sign TX with seqno
     c. Submit via POST /sendBoc
     d. Record in ton_transactions (status=SUBMITTED, seqno=N)
     e. Wait for confirmation (poll getTransactions, max 5 min)
        - If CONFIRMED: continue to next TX
        - If TIMEOUT/FAILED: abort remaining TXs, mark as FAILED
  3. Release lock
  4. Return results
```

### Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Batch vs sequential submit | Sequential with confirmation wait | WalletV4R2 rejects wrong seqno; must wait for each TX confirmation |
| Seqno source | Always on-chain | Avoids stale cache, minimal extra latency (~100ms) |
| Lock scope | Per subwallet | Different subwallets are independent |
| Lock TTL | 300s | Covers worst case: 2-3 TXs × (submit + 60s confirmation poll) |

### Alternative Considered: Internal Message (rejected)

WalletV4R2 supports up to 4 internal messages in a single external message. This would allow combining payout + sweep in one TX with one seqno:

```
External message (seqno=N):
  ├─ Internal: send 900 TON to owner
  └─ Internal: send 100 TON to treasury
```

**Rejected because**:
1. Atomic failure: if one internal message fails, all fail (e.g., owner address invalid → commission sweep also fails)
2. Gas accounting: single fee for combined TX makes it harder to attribute gas to payout vs sweep
3. Complexity: building multi-message BOC requires advanced cell serialization
4. Reconciliation: one on-chain TX maps to multiple ledger operations (harder to match)

**Revisit when**: transaction volume exceeds 1000 deals/day and gas savings justify the complexity.

## Database Schema

### ton_transactions Extension

```sql
-- Added to ton_transactions for seqno tracking
ALTER TABLE ton_transactions ADD COLUMN seqno INTEGER;
```

This enables:
- Debugging failed TXs (which seqno was used?)
- Reconciliation (verify sequential seqno per subwallet)
- Detecting seqno gaps (missed/stuck TXs)

## Failure Scenarios

### Scenario 1: Payout confirmed, app crash before sweep

```
TX #1 (payout): CONFIRMED, seqno=5
--- crash ---
TX #2 (sweep): never submitted
```

**Recovery**:
1. On startup, scan `ton_transactions` for subwallets with incomplete TX batches
2. Detect: payout CONFIRMED but no commission sweep for same deal
3. Resume from TX #2: fetch fresh seqno (now 6), build sweep TX, submit

### Scenario 2: TX submitted, confirmation timeout

```
TX #1 (payout): SUBMITTED, seqno=5, no confirmation after 5 min
```

**Recovery**:
1. Check on-chain: `getTransactions` for the subwallet address
2. If TX found on-chain → mark CONFIRMED, continue
3. If TX NOT found → seqno may have been consumed or not:
   a. Fetch current seqno from chain
   b. If seqno still 5 → TX was never accepted, resubmit with seqno=5
   c. If seqno is 6 → TX was accepted but we missed confirmation, mark CONFIRMED

### Scenario 2a: seqno advanced but tx hash recovery is ambiguous

`sendBoc` may fail at transport level while the chain still accepts the message.
In this case, seqno can advance without a direct response hash.

Recovery policy:
1. Fetch recent wallet transactions.
2. Extract outgoing transfers from `out_msgs`.
3. Match by `(destination_address, amount_nano)` for the attempted transfer.
4. If a unique match is found -> persist recovered `tx_hash`.
5. If no match is found -> keep submission in reconciliation-required state (no optimistic completion event).

This guard prevents false-positive `CONFIRMED` statuses caused by attaching an unrelated hash.

### Scenario 3: Concurrent lock expiry

```
Instance A: holds lock, submitting TX #1 (slow network)
Lock TTL expires
Instance B: acquires lock, submits TX #1 with same seqno
```

**Prevention**:
- Redis lock TTL = 300s (generous)
- Lock extension: holder extends TTL every 60s while processing
- Idempotency: `ton_transactions` checked before submission (if SUBMITTED/CONFIRMED exists, skip)

### Scenario 4: seqno desync after failed TX

```
TX with seqno=5 failed (insufficient balance)
Next TX should still use seqno=5 (seqno didn't increment)
```

**Handling**: Always fetch fresh seqno from chain. Failed TXs don't increment on-chain seqno.

## Monitoring

| Metric | Type | Alert |
|--------|------|-------|
| `ton.tx.seqno_fetch.duration` | Timer | p99 > 2s → WARNING |
| `ton.tx.seqno_mismatch` | Counter | any > 0 → CRITICAL |
| `ton.tx.batch.incomplete` | Gauge | > 0 for > 30 min → HIGH |
| `ton.tx.lock.timeout` | Counter | rate > 1/hour → WARNING |

## Configuration

```yaml
ton:
  tx:
    seqno:
      fetch-timeout: 5s
    batch:
      lock-ttl: 300s
      lock-extension-interval: 60s
      max-batch-size: 4
      inter-tx-confirmation-timeout: 5m
```

## Related Documents

- [TON SDK Integration](./01-ton-sdk-integration.md) — TX lifecycle and retry
- [Payout Execution Flow](./30-payout-execution-flow.md) — payout pipeline
- [Payout Wallet Architecture](./40-payout-wallet-architecture.md) — subwallet direct payout
- [Redis Distributed Locks](./09-redis-distributed-locks.md) — lock patterns
- [Commission Rounding & Sweep](./25-commission-rounding-sweep.md) — sweep after payout
