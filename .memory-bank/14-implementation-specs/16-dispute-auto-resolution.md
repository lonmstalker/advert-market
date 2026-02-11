# Dispute Auto-Resolution Rules Engine

## Overview

Disputes can be auto-resolved by rules engine or escalated to operator. Rules evaluated in priority order; first matching rule determines outcome.

---

## Rule Catalog

### Auto-Refund Rules (favor advertiser)

| # | Condition | Outcome | Priority |
|---|-----------|---------|----------|
| 1 | Post deleted during 24h verification | REFUND_FULL | 10 (highest) |
| 2 | Post content edited (hash mismatch) | REFUND_FULL | 20 |
| 3 | Creative approval timeout (owner didn't publish within deadline) | REFUND_FULL | 30 |
| 4 | Channel became private/restricted | REFUND_FULL | 40 |

### Auto-Payout Rules (favor owner)

| # | Condition | Outcome | Priority |
|---|-----------|---------|----------|
| 5 | 24h verification passed, all checks OK | PAYOUT | 10 |
| 6 | Advertiser opened dispute but no evidence within 48h | PAYOUT | 50 |

### Escalation Rules (require operator)

| # | Condition | Outcome | Priority |
|---|-----------|---------|----------|
| 7 | Both parties submitted conflicting evidence | ESCALATE | 60 |
| 8 | Amount > 1000 TON | ESCALATE | 70 |
| 9 | Post partially edited (minor changes) | ESCALATE | 80 |
| 10 | No matching auto-resolution rule | ESCALATE | 999 (fallback) |

---

## Rule Evaluation Flow

```
1. Dispute opened (by advertiser or owner, or auto-opened by system)
2. Collect evidence:
   - posting_checks results (content hash, deletion status)
   - deal timeline (deadlines, state transitions)
   - TON transactions
3. Evaluate rules in priority order (lowest number = highest priority)
4. First matching rule -> execute outcome
5. If no rule matches -> ESCALATE to operator
```

---

## Evidence Types

| Type | Source | Description |
|------|--------|-------------|
| `POST_CHECK` | Delivery Verifier | Content hash, deletion status |
| `TIMELINE` | Deal events | State transition timestamps |
| `SCREENSHOT` | User submission | Manual evidence (JSONB) |
| `TON_TX` | TON transactions | Payment proof |
| `CHANNEL_STATUS` | Telegram API | Channel accessibility |

### Evidence Requirements per Rule

| Rule | Required Evidence |
|------|-------------------|
| Post deleted | POST_CHECK with status=DELETED |
| Content edited | POST_CHECK with hash mismatch |
| Creative timeout | TIMELINE: deadline_at < now() |
| Channel restricted | CHANNEL_STATUS: not accessible |
| 24h passed OK | POST_CHECK: all checks passed |
| No evidence | TIMELINE: 48h since dispute opened |

---

## Outcomes

| Outcome | Action |
|---------|--------|
| `REFUND_FULL` | Emit EXECUTE_REFUND command, transition deal -> REFUNDED |
| `PAYOUT` | Emit EXECUTE_PAYOUT command, transition deal -> COMPLETED |
| `ESCALATE` | Set dispute status ESCALATED, notify operator |
| `REFUND_PARTIAL` | Future: partial refund (operator-only for now) |

---

## Operator Resolution

When escalated, operator can:

1. **View all evidence** via admin panel
2. **Choose outcome**: REFUND_FULL, PAYOUT, or REFUND_PARTIAL (future)
3. **Add resolution note** (stored in dispute_evidence)
4. **Execute** via `POST /api/v1/deals/{id}/dispute/resolve`

```json
{
  "outcome": "REFUND_FULL",
  "note": "Post was deleted within 6 hours"
}
```

---

## Configuration Approach

Rules are defined in code (not database) for MVP:
- Rule interface with `matches(dispute, evidence)` and `outcome()` methods
- Rules registered in priority-ordered list
- Easy to add new rules without schema changes

For Scaled: consider rules database table for operator-configurable rules.

---

## Conflict Resolution

- Rules evaluated in strict priority order
- First match wins
- Conflicting evidence -> ESCALATE (rule #7)
- Operator decision is final

---

## Related Documents

- [Dispute Resolution Feature](../03-feature-specs/06-dispute-resolution.md)
- [Delivery Verification](../03-feature-specs/05-delivery-verification.md)
- [Deal State Machine](../06-deal-state-machine.md)