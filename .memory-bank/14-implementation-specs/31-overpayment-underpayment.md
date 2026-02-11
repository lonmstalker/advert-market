# Overpayment & Underpayment Handling

## Overview

When an advertiser sends TON to a deposit address, the amount may not exactly match the expected deal amount. This spec defines detection and resolution flows.

## Detection

TON Deposit Watcher compares `ton_transactions.amount_nano` against `deals.amount_nano`:

- **Match**: `|received - expected| <= tolerance` -> proceed to FUNDED
- **Overpayment**: `received > expected + tolerance`
- **Underpayment**: `received < expected - tolerance`

**Tolerance**: configurable, default 1_000_000 nanoTON (0.001 TON) to account for network fees.

## Overpayment Flow

```mermaid
sequenceDiagram
    participant W as Deposit Watcher
    participant API as Backend API
    participant L as Ledger
    participant O as Operator

    W->>API: deposit_confirmed (amount > expected)
    API->>L: Debit EXTERNAL_TON, Credit ESCROW:{deal_id} (expected)
    API->>L: Debit EXTERNAL_TON, Credit OVERPAYMENT:{deal_id} (excess)
    API->>API: Transition deal -> FUNDED
    API->>O: Notify: overpayment detected
```

### Overpayment Resolution

| Option | Action | Who |
|--------|--------|-----|
| Auto-refund excess | Refund Executor sends excess back to source address | System (default) |
| Apply to deal | Credit full amount to ESCROW:{deal_id} | Operator override |
| Manual review | Hold in OVERPAYMENT:{deal_id} until operator decides | For large overages (>10%) |

### Ledger Entries (Auto-Refund Excess)

| Step | Debit | Credit | Amount |
|------|-------|--------|--------|
| Fund deal | EXTERNAL_TON | ESCROW:{deal_id} | expected_amount_nano |
| Park excess | EXTERNAL_TON | OVERPAYMENT:{deal_id} | excess_nano |
| Refund excess | OVERPAYMENT:{deal_id} | EXTERNAL_TON | excess_nano |

## Underpayment Flow

```mermaid
sequenceDiagram
    participant W as Deposit Watcher
    participant API as Backend API
    participant A as Advertiser

    W->>API: deposit_confirmed (amount < expected)
    API->>API: Record partial deposit
    API->>A: Notify: need additional {shortfall} TON
    Note right of API: Deal stays in AWAITING_PAYMENT
```

### Underpayment Resolution

| Option | Action | Who |
|--------|--------|-----|
| Top-up | Advertiser sends remaining amount to same address | Advertiser |
| Adjust deal | Renegotiate deal amount to match received | Both parties agree |
| Cancel + refund | Cancel deal, refund partial deposit | Advertiser or timeout |

### Multiple Deposits

- Deposit Watcher tracks cumulative deposits per deal address
- Each deposit recorded as separate `ton_transactions` record
- Running total: `SUM(amount_nano) FROM ton_transactions WHERE deal_id = ? AND direction = 'IN' AND status = 'CONFIRMED'`
- When cumulative >= expected: transition to FUNDED
- Timeout (24h from first deposit): offer cancel + refund

## Configuration

```yaml
escrow:
  payment:
    tolerance-nano: 1000000          # 0.001 TON
    overpayment-auto-refund: true
    overpayment-manual-review-threshold: 0.10  # 10% over
    underpayment-topup-window: 24h
```

## Notifications

| Event | Recipient | Template |
|-------|-----------|----------|
| Overpayment detected | Advertiser + Operator | "Получен перевод {received} TON вместо {expected} TON. Излишек {excess} TON будет возвращён" |
| Underpayment detected | Advertiser | "Получен частичный перевод {received} из {expected} TON. Доплатите {shortfall} TON" |
| Excess refunded | Advertiser | "Возврат излишка {excess} TON. TX: {tx_hash}" |
| Top-up received | Advertiser | "Дополнительный платёж получен. Сделка переведена в статус FUNDED" |

## Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `escrow.overpayment` | Counter | Overpayment events |
| `escrow.underpayment` | Counter | Underpayment events |
| `escrow.overpayment.amount` | Summary | Excess amounts |
| `escrow.underpayment.shortfall` | Summary | Shortfall amounts |

## Related Documents

- [Escrow Payments](../03-feature-specs/04-escrow-payments.md)
- [Confirmation Policy](../07-financial-system/06-confirmation-policy.md)
- [TON SDK Integration](./01-ton-sdk-integration.md)
- [Payout Execution Flow](./30-payout-execution-flow.md)