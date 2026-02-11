# Payout Wallet Architecture

## Overview

Defines how funds flow from deal subwallets to recipients. **Decision**: direct payout from subwallet to recipient (no central hot wallet aggregation).

## Architecture: Subwallet -> Recipient Direct

```
Deal Subwallet (subwallet_id=N)
  |-- Payout:    subwallet -> Owner TON address
  |-- Refund:    subwallet -> Advertiser TON address (original sender)
  |-- Commission sweep: subwallet -> PLATFORM_TREASURY wallet (subwallet_id=0)
```

### Rationale

| Approach | Pros | Cons |
|----------|------|------|
| **Direct from subwallet** (chosen) | Fewer TX, lower total gas, simpler | Each subwallet needs enough balance for gas |
| Via hot wallet aggregation | Single source for payouts | Extra TX (sweep -> aggregate -> payout), higher gas, more complex |

## Payout Flow (Happy Path)

Deal: 1000 TON, 10% commission (100 TON), gas ~0.005 TON per TX.

### Step 1: Payout to Owner

```
TX: subwallet -> owner_address (900 TON)
Gas: ~0.005 TON from subwallet balance
Subwallet balance after: 1000 - 900 - 0.005 = 99.995 TON
```

Ledger entries (created AFTER TX confirmation with actual amounts):

```
tx_ref: {payout_tx_ref}
DEBIT  ESCROW:{deal_id}            1000 TON
CREDIT OWNER_PENDING:{owner_id}     900 TON
CREDIT COMMISSION:{deal_id}         100 TON
```

### Step 2: Commission Sweep

```
TX: subwallet -> treasury_wallet (99.99 TON)
Gas: ~0.005 TON from subwallet balance
Subwallet balance after: ~0 (dust)
```

Ledger entries:

```
tx_ref: {sweep_tx_ref}
DEBIT  COMMISSION:{deal_id}         100 TON
CREDIT PLATFORM_TREASURY            100 TON
```

### Step 3: Fee Adjustment (after TX confirmation)

```
tx_ref: {fee_tx_ref_1}
DEBIT  PLATFORM_TREASURY            0.005 TON  (payout gas)
CREDIT NETWORK_FEES                 0.005 TON

tx_ref: {fee_tx_ref_2}
DEBIT  PLATFORM_TREASURY            0.005 TON  (sweep gas)
CREDIT NETWORK_FEES                 0.005 TON
```

### Net Result

| Recipient | Receives | Note |
|-----------|----------|------|
| Owner | 900 TON | Full payout |
| PLATFORM_TREASURY | ~99.99 TON | Commission minus 2x gas |
| NETWORK_FEES | ~0.01 TON | 2x gas fees |
| Subwallet | ~0 | Dust |

## Refund Flow

Deal: 1000 TON, full refund.

```
TX: subwallet -> advertiser_address (999.995 TON)
Gas: ~0.005 TON from subwallet balance
```

Ledger entries:

```
tx_ref: {refund_tx_ref}
DEBIT  ESCROW:{deal_id}            1000 TON
CREDIT EXTERNAL_TON                 999.995 TON
CREDIT NETWORK_FEES                   0.005 TON
```

> **Note**: All entries (including gas fee) are written after TX confirmation with actual amounts. No estimated/adjustment entries needed.

## Ledger Recording Strategy

> **Ledger is append-only**: entries are NEVER updated or deleted. All corrections use additional entries.

### Single-Phase Recording (chosen approach)

Ledger entries are written **after** on-chain TX confirmation, not before:

1. **Submit TX** to blockchain via outbox (outbox record guarantees delivery)
2. **Wait for confirmation** (TxConfirmationWorker polls)
3. **Write ledger entries** with actual amounts including actual gas fee
4. **Update account_balances** projection

### Why Not Two-Phase?

Two-phase (pre-TX + post-TX adjustment) was considered but rejected:
- **Complexity**: requires adjustment/reversal entries for every TX
- **Immutability conflict**: temptation to "update" entries violates append-only
- **Outbox guarantees delivery**: if app crashes after TX submit, outbox replays the confirmation check

### Crash Recovery

| Failure Point | Recovery |
|---------------|----------|
| Crash before TX submit | Outbox record still PENDING, poller retries |
| Crash after TX submit, before confirmation | TxConfirmationWorker detects confirmed TX on-chain, writes ledger |
| Crash after confirmation, before ledger write | Idempotency key prevents duplicates on retry |
| TX fails on-chain | No ledger entries written, outbox marked FAILED, operator alerted |

### Idempotency

All ledger writes use DB-backed idempotency:
1. **`ledger_idempotency_keys` table**: global dedup guard (PRIMARY KEY on idempotency_key)
2. **`idempotency_key` column** on `ledger_entries` (per-partition unique index)
3. Both checked in the same DB transaction as the entry insert

> **Redis is NOT the idempotency store** for financial operations. Redis is used only for caching `account_balances` (read model). All financial idempotency is DB-backed.

## Dust Handling

After all TX from a subwallet, ~0.001 TON may remain (dust).

| Threshold | Action |
|-----------|--------|
| < 1000 nanoTON (0.000001 TON) | Ignore, write off monthly |
| >= 1000 nanoTON | Include in next commission sweep batch |

Monthly dust write-off ledger entry:

```
tx_ref: {dust_writeoff_tx_ref}
DEBIT  DUST_WRITEOFF    {total_dust}
CREDIT NETWORK_FEES     {total_dust}
```

## Related Documents

- [TON SDK Integration](./01-ton-sdk-integration.md)
- [Payout Execution Flow](./30-payout-execution-flow.md)
- [Network Fee Accounting](./38-network-fee-accounting.md)
- [Hot Wallet Management](./37-hot-wallet-management.md)
- [Commission Rounding & Sweep](./25-commission-rounding-sweep.md)
