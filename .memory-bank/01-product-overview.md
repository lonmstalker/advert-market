# Product Overview

## Vision

Ad Marketplace is a **Telegram Mini App** that connects Telegram channel owners with advertisers through a transparent, escrow-secured advertising marketplace. The platform eliminates trust issues in channel advertising by holding funds in TON escrow until delivery is verified.

## Problem Statement

Telegram channel advertising suffers from:

1. **No trust guarantees** — advertisers risk paying for ads that never get published or get deleted early
2. **No standardized workflow** — deal negotiation, creative approval, and payment happen across fragmented channels
3. **No delivery verification** — no automated way to confirm an ad was published and retained for the agreed duration
4. **No dispute resolution** — disagreements between parties have no neutral arbiter

## Solution

A Telegram-native marketplace that provides:

- **Channel discovery** — searchable catalog of channels with verified statistics
- **Structured deal flow** — standardized lifecycle from offer to completion
- **Creative workflow** — built-in brief, draft, and approval process
- **TON escrow** — funds held on-platform until delivery is confirmed
- **Automated verification** — bot checks that ads are published and retained (24h minimum)
- **Dispute resolution** — hybrid auto-rules + human operator escalation

## Business Model

| Revenue Stream | Mechanism |
|---------------|-----------|
| **Platform commission** | 10% of deal amount (configurable per segment) deducted on escrow release |
| **Future: Premium listings** | Promoted channel placement in marketplace |
| **Future: Analytics** | Advanced channel analytics for advertisers |

### Unit Economics

```
Deal amount:         1,000 TON
Platform commission: 100 TON (10%)
Channel owner payout: 900 TON
```

All amounts are stored and processed in **nanoTON** (1 TON = 10^9 nanoTON) for precision.

## MVP Scope

### In Scope (MVP)

- Advertiser and Channel Owner flows via Telegram Mini App
- Channel listing and discovery
- Deal lifecycle with state machine
- Creative brief and approval workflow
- TON escrow: deposit, hold, release, refund
- Automated delivery verification (24h retention check)
- Basic dispute resolution with operator escalation
- Channel team management (OWNER/MANAGER roles)
- Telegram Bot notifications

### Out of Scope (Post-MVP)

- Multi-currency support (only TON in MVP)
- Advanced analytics dashboard
- API for third-party integrations
- Bulk deal management
- Automated pricing recommendations

## Key Metrics

| Metric | Description |
|--------|-------------|
| **GMV** | Gross Merchandise Value — total deal volume |
| **Take rate** | Commission revenue / GMV |
| **Deal completion rate** | Completed deals / Total deals |
| **Dispute rate** | Disputed deals / Total deals |
| **Time to fund** | Average time from deal creation to escrow funding |
| **Verification pass rate** | Deals passing delivery verification / Total verified |

## Related Documents

- [Actors and Personas](./02-actors-and-personas.md)
- [Deal State Machine](./06-deal-state-machine.md)
- [Tech Stack](./08-tech-stack.md)
- [Development Roadmap](./12-development-roadmap.md)
