# NETWORK_FEES Account

## Overview

Dedicated expense account tracking all TON blockchain gas fees incurred by the platform. Part of the double-entry ledger system.

## Account Definition

| Attribute | Value |
|-----------|-------|
| **Format** | `NETWORK_FEES` |
| **Cardinality** | Singleton |
| **Type** | Expense (contra to PLATFORM_TREASURY) |
| **Normal balance** | Credit (increases with credits) |
| **Purpose** | Tracks cumulative gas fees for all outbound TON transactions |

## How It Works

Every outbound TON transaction (payout, refund, commission sweep) incurs a gas fee (~0.005 TON). The fee is:

1. **Actual** value recorded after on-chain confirmation (single-phase recording)
2. See [Network Fee Accounting](../14-implementation-specs/38-network-fee-accounting.md) for recording flow

## Fee Sources

| Operation | Who pays gas | Ledger |
|-----------|-------------|--------|
| **Payout to owner** | Platform (from commission) | DEBIT PLATFORM_TREASURY, CREDIT NETWORK_FEES |
| **Refund to advertiser** | Advertiser (deducted from refund) | CREDIT NETWORK_FEES (part of refund tx_ref) |
| **Commission sweep** | Platform (from treasury) | DEBIT PLATFORM_TREASURY, CREDIT NETWORK_FEES |
| **Late deposit refund** | Advertiser (deducted from refund) | CREDIT NETWORK_FEES (part of refund tx_ref) |

## Balance Interpretation

| NETWORK_FEES balance | Meaning |
|---------------------|---------|
| 0.05 TON | Platform has spent 0.05 TON on gas fees total |
| Growing fast | High transaction volume or TON network congestion |

## Reconciliation

See [Reconciliation SQL Check 7](../14-implementation-specs/14-reconciliation-sql.md) for NETWORK_FEES consistency check.

## Impact on Platform Revenue

Effective platform revenue = PLATFORM_TREASURY balance - NETWORK_FEES balance

```sql
SELECT
    (SELECT balance_nano FROM account_balances WHERE account_id = 'PLATFORM_TREASURY') -
    (SELECT balance_nano FROM account_balances WHERE account_id = 'NETWORK_FEES')
    AS net_platform_revenue_nano;
```

## Related Documents

- [Account Types](./05-account-types.md)
- [Ledger Design](./01-ledger-design.md)
- [Network Fee Accounting](../14-implementation-specs/38-network-fee-accounting.md)
- [Entry Type Catalog](./07-entry-type-catalog.md)
