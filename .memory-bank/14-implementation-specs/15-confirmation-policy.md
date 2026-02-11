# TON Block Height Tracking & Confirmation Policy

## Tiered Confirmation Policy

| Amount (TON) | Required Confirmations | Extra |
|-------------|----------------------|-------|
| <= 100 | 1 | -- |
| <= 1000 | 3 | -- |
| > 1000 | 5 | + Operator manual review |

---

## Block Height Query

### Algorithm

1. Get current masterchain seqno via `GET /getMasterchainInfo`
2. For each pending deposit transaction:
   - Calculate: `confirmations = current_seqno - tx_block_seqno`
   - If confirmations >= required: mark deposit as confirmed

### Polling Schedule

| Parameter | Value |
|-----------|-------|
| Poll interval | 10 seconds |
| Max poll duration | 30 minutes (then expire) |
| Confirmation check frequency | Every poll cycle |

---

## Confirmation Counting

```
confirmations = current_masterchain_seqno - transaction_block_seqno
```

- `transaction_block_seqno`: extracted from transaction metadata at detection time
- `current_masterchain_seqno`: queried live from TON Center API
- TON produces blocks every ~5 seconds, so 1 confirmation ~ 5 seconds

---

## Operator Review Flow (> 1000 TON)

### Trigger

When deposit confirmed with 5+ confirmations AND amount > 1000 TON:

1. Send `NOTIFICATION` to operator via `notifications.outbox`
2. Template: `LARGE_DEPOSIT_REVIEW`
3. Deal stays in `AWAITING_OPERATOR_REVIEW` sub-state (tracked in `deal_events`)

### Operator Actions

| Action | Endpoint | Result |
|--------|----------|--------|
| Approve | `POST /api/v1/admin/deposits/{dealId}/approve` | Transition to FUNDED |
| Reject | `POST /api/v1/admin/deposits/{dealId}/reject` | Refund to advertiser |

### SLA

| Metric | Target |
|--------|--------|
| Review response time | < 2 hours |
| Escalation if no response | After 4 hours, re-notify |

---

## Amount Validation

### Exact Match

- Expected: `deal.amount_nano`
- Received: `ton_transaction.amount_nano`
- Match: `received == expected` (exact nanoTON match)

### Overpayment

- `received > expected`
- Action: Accept deposit, fund escrow with `expected` amount
- Flag overpayment for manual review
- Future: auto-refund difference

### Underpayment

- `received < expected`
- Action: Do NOT fund escrow
- Notify advertiser: "Insufficient deposit, expected {expected} TON, received {received} TON"
- Advertiser can send additional deposit to same address

### Multiple Deposits

- Sum all confirmed incoming transactions to the deposit address
- If sum >= expected: accept
- If sum < expected: wait for more

---

## Configuration

```yaml
ton:
  confirmation:
    tiers:
      - max-amount-ton: 100
        confirmations: 1
      - max-amount-ton: 1000
        confirmations: 3
      - max-amount-ton: 999999999
        confirmations: 5
        operator-review: true
    poll-interval: 10s
    max-poll-duration: 30m
```

---

## Related Documents

- [TON SDK Integration](./01-ton-sdk-integration.md)
- [TON Center API Catalog](./08-ton-center-api-catalog.md)
- [Escrow Flow](../07-financial-system/02-escrow-flow.md)
- [Confirmation Policy](../07-financial-system/06-confirmation-policy.md)