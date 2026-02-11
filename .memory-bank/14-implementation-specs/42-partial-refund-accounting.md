# Partial Refund Accounting

## Overview

When a deal is cancelled/disputed AFTER content was partially delivered, funds are split between advertiser (refund) and owner (partial payout) based on time elapsed.

## New State: PARTIALLY_REFUNDED

Added to the deal state machine as a terminal state:

```
DISPUTED -> PARTIALLY_REFUNDED : Operator resolves with partial refund
```

## Time-Based Split Rules

| Time since publication | Owner share | Advertiser refund | Rationale |
|----------------------|-------------|-------------------|-----------|
| < 1 hour | 10% | 90% | Minimal exposure |
| 1-6 hours | 25% | 75% | Some exposure |
| 6-12 hours | 50% | 50% | Significant exposure |
| 12-24 hours | 75% | 25% | Near-full delivery |
| 24+ hours | 100% | 0% | Full delivery (-> COMPLETED_RELEASED) |

## Commission Calculation

Commission is calculated **only on the owner's share**, not the full deal amount.

```
deal_amount = 1000 TON
owner_share_percent = 50%  (6-12 hours)
commission_rate = 10%

owner_gross = 1000 * 50% = 500 TON
commission = 500 * 10% = 50 TON
owner_net = 500 - 50 = 450 TON
advertiser_refund = 1000 - 500 = 500 TON
```

## Ledger Entries

### Example: 50/50 split on 1000 TON deal, 10% commission

```
tx_ref: {partial_refund_tx_ref}

-- Release owner's share
DEBIT  ESCROW:{deal_id}                 1000 TON    -- full escrow debit

-- Advertiser refund
CREDIT EXTERNAL_TON                      500 TON    -- 50% refund

-- Owner payout (minus commission)
CREDIT OWNER_PENDING:{owner_id}          450 TON    -- 50% - commission

-- Commission on owner's share only
CREDIT COMMISSION:{deal_id}               50 TON    -- 10% of owner's 500 TON
```

**Invariant**: 1000 = 500 + 450 + 50

## Rounding Rules

All amounts in nanoTON (long integer arithmetic).

```java
long ownerGrossNano = dealAmountNano * ownerShareBp / 10_000L;       // floor
long advertiserRefundNano = dealAmountNano - ownerGrossNano;          // exact remainder
long commissionNano = ownerGrossNano * commissionRateBp / 10_000L;   // floor
long ownerNetNano = ownerGrossNano - commissionNano;                  // exact
```

**Dust rule**: All rounding dust goes to the advertiser's refund (largest remainder).

## Gas Fee Handling

Two outbound TX required:
1. Refund TX: subwallet -> advertiser (500 - gas_fee_1)
2. Payout TX: subwallet -> owner (450 - gas_fee_2)
3. Commission sweep: subwallet -> treasury (50 - gas_fee_3)

Gas fees deducted from respective amounts. Fee adjustment entries created after TX confirmation (see spec 38).

## Operator Workflow

1. Operator opens dispute review
2. System shows time-based split suggestion based on `published_at`
3. Operator can accept suggested split or override with custom percentages
4. Operator confirms -> system creates ledger entries + executes TX

### API

```
POST /api/v1/admin/disputes/{disputeId}/resolve
{
  "outcome": "PARTIAL_REFUND",
  "ownerShareBp": 5000,          // 50% (can be overridden)
  "reason": "Post deleted after 8 hours"
}
```

## deals Table Changes

```sql
-- Track partial refund details
-- Stored in deal_events.payload JSONB:
{
  "outcome": "PARTIAL_REFUND",
  "ownerShareBp": 5000,
  "ownerGrossNano": 500000000000,
  "ownerNetNano": 450000000000,
  "commissionNano": 50000000000,
  "advertiserRefundNano": 500000000000,
  "timeSincePublicationHours": 8.5
}
```

## Related Documents

- [Deal State Machine](../06-deal-state-machine.md)
- [Commission Rounding & Sweep](./25-commission-rounding-sweep.md)
- [Payout Wallet Architecture](./40-payout-wallet-architecture.md)
- [Network Fee Accounting](./38-network-fee-accounting.md)
- [Dispute Auto-Resolution](./16-dispute-auto-resolution.md)
