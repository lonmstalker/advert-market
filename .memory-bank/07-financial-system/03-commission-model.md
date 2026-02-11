# Financial System: Commission Model

## Overview

The platform earns revenue through a commission on each completed deal. Commission is deducted from the escrow amount when funds are released to the channel owner.

## Default Rate

| Parameter | Value |
|-----------|-------|
| **Default commission** | 10% |
| **Calculation base** | Deal `amount_nano` (full escrow amount) |
| **Deduction point** | On escrow release (after delivery verification) |
| **Configurability** | Segment-configurable (future) |

## Commission Calculation

Performed by the **Commission Service** when called by the Ledger Service during escrow release.

```
deal_amount = 1000 TON = 1,000,000,000,000 nanoTON

commission = deal_amount * 10 / 100
           = 100,000,000,000 nanoTON (100 TON)

owner_payout = deal_amount - commission
             = 900,000,000,000 nanoTON (900 TON)
```

### Integer Arithmetic

All calculations use integer arithmetic on nanoTON to avoid floating-point precision issues:

```
commission_nano = amount_nano * commission_rate_bps / 10000
```

Where `commission_rate_bps` is in basis points (1000 bps = 10%).

## Ledger Entries on Release

When commission is deducted (3 entries, 1 tx_ref):

| tx_ref | account_id | debit_nano | credit_nano | entry_type |
|--------|-----------|------------|-------------|------------|
| uuid-1 | `ESCROW:{deal_id}` | 1,000,000,000,000 | 0 | ESCROW_RELEASE |
| uuid-1 | `COMMISSION:{deal_id}` | 0 | 100,000,000,000 | PLATFORM_COMMISSION |
| uuid-1 | `OWNER_PENDING:{owner_id}` | 0 | 900,000,000,000 | OWNER_PAYOUT |

**Invariant**: SUM(debit_nano) = SUM(credit_nano) per tx_ref

For tiered commission rates and rounding details, see [Commission Rounding & Sweep](../14-implementation-specs/25-commission-rounding-sweep.md).

## Commission Account Flow

```mermaid
flowchart LR
    ESC[ESCROW:deal_id] -->|Release| COM[COMMISSION:deal_id]
    COM -->|Sweep| TREAS[PLATFORM_TREASURY]
```

1. **On deal release**: Commission credited to `COMMISSION:{deal_id}` (per-deal tracking)
2. **Periodic sweep**: Commission accounts swept to `PLATFORM_TREASURY` (aggregation)

## Future: Segment Configuration

The Commission Service supports configurable rates per segment (post-MVP):

| Segment | Rate | Condition |
|---------|------|-----------|
| Default | 10% | All deals |
| Premium channels | 8% | Channels with >100K subscribers |
| Bulk advertisers | 7% | Advertisers with >50 deals/month |
| New user promo | 5% | First 3 deals for new advertisers |

## Commission on Refund

When a deal is refunded (dispute/cancellation), **no commission is charged**. The full escrow amount is returned to the advertiser.

## Related Documents

- [Ledger Design](./01-ledger-design.md) — entry structure
- [Escrow Flow](./02-escrow-flow.md) — when commission is applied
- [Account Types](./05-account-types.md) — COMMISSION and PLATFORM_TREASURY accounts
- [Product Overview](../01-product-overview.md) — business model
