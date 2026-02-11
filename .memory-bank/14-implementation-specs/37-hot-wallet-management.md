# Hot Wallet Management

## Overview

Platform treasury wallet monitoring, balance alerts, and replenishment procedures. Ensures payout/refund operations always have sufficient funds for gas fees.

## Architecture

Platform uses a **single mnemonic** generating subwallets per deal. The "hot wallet" is the master wallet (subwallet_id=0) used for commission sweep destination.

### Wallet Hierarchy

```
Platform Mnemonic (24 words)
  |-- subwallet_id=0  -> PLATFORM_TREASURY wallet (commission recipient)
  |-- subwallet_id=1  -> Deal #1 escrow subwallet
  |-- subwallet_id=2  -> Deal #2 escrow subwallet
  |-- ...
```

## Balance Monitoring

### Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `ton.treasury.balance_nano` | Gauge | Current PLATFORM_TREASURY on-chain balance |
| `ton.subwallets.total_balance_nano` | Gauge | Sum of all active subwallet balances |
| `ton.treasury.low_balance` | Gauge | 1 if below threshold, 0 otherwise |

### Poll Schedule

```yaml
ton:
  monitoring:
    treasury-poll-interval: 60s
    subwallet-poll-interval: 300s
    low-balance-threshold-nano: 1000000000  # 1 TON
```

### Alerts

| Condition | Severity | Action |
|-----------|----------|--------|
| Treasury balance < 1 TON | WARNING | Notify operator via Telegram |
| Treasury balance < 0.1 TON | CRITICAL | Block new payouts, notify operator |
| Any subwallet with deal in terminal state and balance > 0 | HIGH | Trigger sweep/refund |

## Replenishment

Manual process for MVP:

1. Operator receives low-balance alert
2. Operator sends TON to PLATFORM_TREASURY address from external wallet
3. System detects incoming TX via monitoring
4. Balance metric updates automatically

### Future: Auto-Replenishment

Roadmap item: auto-sweep from exchange account when balance drops below threshold.

## Security

- Mnemonic stored as `TON_WALLET_MNEMONIC` env var (MVP)
- Mnemonic NEVER logged, NEVER in config files
- Roadmap: AWS KMS / HashiCorp Vault envelope encryption

## Related Documents

- [TON SDK Integration](./01-ton-sdk-integration.md)
- [Network Fee Accounting](./38-network-fee-accounting.md)
- [Payout Wallet Architecture](./40-payout-wallet-architecture.md)
- [Account Types](../07-financial-system/05-account-types.md)
