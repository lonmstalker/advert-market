# Glossary

## Domain Terms

| Term | Definition |
|------|-----------|
| **Deal** | A single advertising agreement between an Advertiser and a Channel Owner. The central aggregate of the system. Tracked by state machine from offer to completion. |
| **Escrow** | Funds held by the platform on behalf of the advertiser until delivery conditions are met. Managed via `ESCROW:{deal_id}` accounts in the ledger. |
| **Creative** | The advertising content (text, images, links) to be published in the Telegram channel. Goes through brief → draft → review → approval workflow. |
| **Brief** | Requirements document submitted by the advertiser describing what the creative should contain. |
| **Listing** | A channel's public profile on the marketplace, including description, pricing, audience stats, and availability. |
| **Delivery** | The act of publishing approved creative to the Telegram channel. Verified by the Delivery Verifier worker. |
| **Retention** | The minimum time (24 hours by default) an ad must remain published in the channel without deletion or modification. |

## Financial Terms

| Term | Definition |
|------|-----------|
| **nanoTON** | The smallest unit of TON cryptocurrency. 1 TON = 10^9 nanoTON. All amounts in the system are stored in nanoTON for precision. |
| **TON** | The Open Network cryptocurrency used for all payments on the platform. |
| **Ledger Entry** | An immutable, append-only record in the double-entry ledger. Every money movement creates exactly two entries: one debit and one credit. |
| **Double-Entry** | Accounting principle where every transaction creates equal debit and credit entries. Ensures `SUM(debits) = SUM(credits)` invariant. |
| **Commission** | Platform fee (10% default) deducted from the deal amount when escrow is released. Credited to `PLATFORM_TREASURY`. |
| **Payout** | TON transfer from the platform to the Channel Owner after escrow release minus commission. |
| **Refund** | TON transfer from the platform back to the Advertiser when a deal is cancelled or dispute is resolved in their favor. |
| **Deposit Address** | A unique TON address generated per deal for the advertiser to send escrow funds to. |
| **Confirmation** | A TON blockchain block containing the deposit transaction. Higher-value deposits require more confirmations (tiered policy). |
| **Reconciliation** | Periodic three-way comparison of ledger entries vs TON blockchain transactions vs deal aggregates to detect discrepancies. |

## Account Types

| Account | Format | Purpose |
|---------|--------|---------|
| **PLATFORM_TREASURY** | `PLATFORM_TREASURY` | Platform's commission revenue account |
| **ESCROW** | `ESCROW:{deal_id}` | Per-deal escrow holding account |
| **OWNER_PENDING** | `OWNER_PENDING:{user_id}` | Channel owner's pending payout balance |
| **COMMISSION** | `COMMISSION:{deal_id}` | Per-deal commission tracking account |
| **EXTERNAL_TON** | `EXTERNAL_TON` | Virtual account representing external TON blockchain |

## Deal States

| State | Description |
|-------|-------------|
| **DRAFT** | Deal created but not yet submitted |
| **OFFER_PENDING** | Offer sent to channel owner, awaiting response |
| **NEGOTIATING** | Parties negotiating terms |
| **ACCEPTED** | Channel owner accepted the deal |
| **AWAITING_PAYMENT** | Waiting for advertiser to deposit TON |
| **FUNDED** | Escrow funded, creative workflow begins |
| **CREATIVE_SUBMITTED** | Creative draft submitted by channel owner |
| **CREATIVE_APPROVED** | Advertiser approved the creative |
| **SCHEDULED** | Publication scheduled |
| **PUBLISHED** | Creative published to channel |
| **DELIVERY_VERIFYING** | Automated verification in progress (24h retention check) |
| **COMPLETED_RELEASED** | Delivery verified, escrow released, payout sent |
| **DISPUTED** | Dispute opened by either party |
| **CANCELLED** | Deal cancelled, escrow refunded (if funded) |
| **REFUNDED** | Escrow refunded after dispute resolution |
| **EXPIRED** | Deal expired due to timeout |

See [Deal State Machine](./06-deal-state-machine.md) for full transition diagram.

## Technical Terms

| Term | Definition |
|------|-----------|
| **CQRS** | Command Query Responsibility Segregation. Write model: `ledger_entries`. Read model: `account_balances` + Redis cache. |
| **Event Sourcing** | Pattern where state is derived from an append-only sequence of events (`deal_events`, `ledger_entries`). |
| **Transactional Outbox** | Pattern where domain events are written to a `notification_outbox` table within the same transaction, then published to Kafka. |
| **Idempotency Key** | Unique identifier ensuring an operation is processed exactly once. Used in financial operations and worker callbacks. |
| **Worker Callback** | Internal REST endpoint (`POST /internal/v1/worker-events`) where async workers report results back to Backend API. |
| **Deposit Watcher** | Worker that polls TON blockchain for incoming deposit transactions and triggers deal state transitions. |
| **Delivery Verifier** | Worker that checks published posts for tampering/deletion over 24h retention period. |
| **Outbox Publisher** | Service that reads from `notification_outbox` table and produces messages to Kafka topics. |
| **PII Vault** | Isolated storage with AES-256-GCM field-level encryption for personally identifiable information. |
| **Confirmation Policy** | Tiered rule set: <=100 TON needs 1 confirmation, <=1000 TON needs 3, >1000 TON needs 5 + operator review. |
| **ABAC** | Attribute-Based Access Control. Permissions evaluated from subject attributes (`user.id`, `is_operator`, `memberships`), resource attributes (`deal.advertiser_id`, `deal.status`), action, and context. No fixed user roles. |
| **Contextual Role** | Actor role derived at runtime from data relationships, not from a column on `users`. E.g., "Advertiser" = `deals.advertiser_id == user.id`. |

## Abbreviations

| Abbreviation | Full Form |
|-------------|-----------|
| **TMA** | Telegram Mini App |
| **DDD** | Domain-Driven Design |
| **CQRS** | Command Query Responsibility Segregation |
| **ABAC** | Attribute-Based Access Control — permissions based on subject/resource/action/context attributes |
| **RBAC** | Role-Based Access Control (used only within `channel_memberships`, not globally) |
| **PII** | Personally Identifiable Information |
| **CDC** | Change Data Capture (Debezium) |
| **GMV** | Gross Merchandise Value |
| **WORM** | Write Once Read Many |
| **SPA** | Single Page Application |

## Related Documents

- [Product Overview](./01-product-overview.md)
- [Deal State Machine](./06-deal-state-machine.md)
- [Financial System — Account Types](./07-financial-system/05-account-types.md)
- [Financial System — Ledger Design](./07-financial-system/01-ledger-design.md)
