# Ad Marketplace — Memory Bank Index

> Telegram Mini App advertising marketplace connecting channel owners and advertisers with TON escrow payments.

## Navigation

### Product & Domain

| File | Description |
|------|-------------|
| [01-product-overview.md](./01-product-overview.md) | Product vision, business model, MVP scope |
| [02-actors-and-personas.md](./02-actors-and-personas.md) | 4 actors, roles, RBAC model |
| [13-glossary.md](./13-glossary.md) | Domain terms: Deal, Escrow, Ledger Entry, nanoTON, etc. |

### Feature Specifications

| File | Description |
|------|-------------|
| [03-feature-specs/01-channel-marketplace.md](./03-feature-specs/01-channel-marketplace.md) | Channel listings, search, filters, statistics |
| [03-feature-specs/02-deal-lifecycle.md](./03-feature-specs/02-deal-lifecycle.md) | Full deal lifecycle from offer to completion |
| [03-feature-specs/03-creative-workflow.md](./03-feature-specs/03-creative-workflow.md) | Creative approval: brief, draft, review, publish |
| [03-feature-specs/04-escrow-payments.md](./03-feature-specs/04-escrow-payments.md) | TON escrow: deposit, hold, release/refund |
| [03-feature-specs/05-delivery-verification.md](./03-feature-specs/05-delivery-verification.md) | Auto-posting + verification (24h retention) |
| [03-feature-specs/06-dispute-resolution.md](./03-feature-specs/06-dispute-resolution.md) | Disputes: auto-rules + operator escalation |
| [03-feature-specs/07-team-management.md](./03-feature-specs/07-team-management.md) | Channel team: OWNER/MANAGER, JSONB rights |
| [03-feature-specs/08-notifications.md](./03-feature-specs/08-notifications.md) | Notification pipeline via Kafka and Bot |
| [03-feature-specs/09-channel-statistics.md](./03-feature-specs/09-channel-statistics.md) | Channel stats collection, verification, freshness |

### Architecture

| File | Description |
|------|-------------|
| [04-architecture/01-system-landscape.md](./04-architecture/01-system-landscape.md) | C4 L1: system + external dependencies |
| [04-architecture/02-containers.md](./04-architecture/02-containers.md) | C4 L2: 8 containers (incl. Nginx) and interactions |
| [04-architecture/03-backend-api-components.md](./04-architecture/03-backend-api-components.md) | C4 L3: 20+ Backend API services |
| [04-architecture/04-workers.md](./04-architecture/04-workers.md) | 7 async workers, callback pattern, DLQ strategy |
| [04-architecture/05-data-stores.md](./04-architecture/05-data-stores.md) | PostgreSQL 18: 20 base tables (14 primary + 6 support), partitioning |
| [04-architecture/06-kafka-topology.md](./04-architecture/06-kafka-topology.md) | 8 Kafka topics, consumer groups |
| [04-architecture/07-redis-usage.md](./04-architecture/07-redis-usage.md) | Balance cache, distributed locks, canary config, update dedup, heartbeats |

### Deal State Machine

| File | Description |
|------|-------------|
| [06-deal-state-machine.md](./06-deal-state-machine.md) | Full state machine spec + Mermaid diagram |

### Patterns & Decisions

| File | Description |
|------|-------------|
| [05-patterns-and-decisions/01-event-sourcing.md](./05-patterns-and-decisions/01-event-sourcing.md) | Event Sourcing for financial operations |
| [05-patterns-and-decisions/02-cqrs.md](./05-patterns-and-decisions/02-cqrs.md) | CQRS: ledger_entries, account_balances, Redis |
| [05-patterns-and-decisions/03-transactional-outbox.md](./05-patterns-and-decisions/03-transactional-outbox.md) | Outbox pattern for Kafka |
| [05-patterns-and-decisions/04-state-machine.md](./05-patterns-and-decisions/04-state-machine.md) | State machine for deal lifecycle |
| [05-patterns-and-decisions/05-double-entry-ledger.md](./05-patterns-and-decisions/05-double-entry-ledger.md) | Double-entry: debit = credit |
| [05-patterns-and-decisions/06-ddd-bounded-contexts.md](./05-patterns-and-decisions/06-ddd-bounded-contexts.md) | Bounded contexts and aggregates |
| [05-patterns-and-decisions/07-idempotency-strategy.md](./05-patterns-and-decisions/07-idempotency-strategy.md) | Idempotency for financial operations |
| [05-patterns-and-decisions/08-abac.md](./05-patterns-and-decisions/08-abac.md) | ABAC: contextual roles, no fixed user types |

### Financial System

| File | Description |
|------|-------------|
| [07-financial-system/01-ledger-design.md](./07-financial-system/01-ledger-design.md) | ledger_entries structure, example records |
| [07-financial-system/02-escrow-flow.md](./07-financial-system/02-escrow-flow.md) | Escrow mechanics: address to payout |
| [07-financial-system/03-commission-model.md](./07-financial-system/03-commission-model.md) | 10% commission, configurability |
| [07-financial-system/04-reconciliation.md](./07-financial-system/04-reconciliation.md) | Three-way reconciliation: ledger vs TON vs deals |
| [07-financial-system/05-account-types.md](./07-financial-system/05-account-types.md) | PLATFORM_TREASURY, ESCROW, OWNER_PENDING, EXTERNAL_TON, NETWORK_FEES, OVERPAYMENT, LATE_DEPOSIT |
| [07-financial-system/06-confirmation-policy.md](./07-financial-system/06-confirmation-policy.md) | Tiered policy: <=100 TON->1, <=1000->3, >1000->5+review |
| [07-financial-system/07-entry-type-catalog.md](./07-financial-system/07-entry-type-catalog.md) | Complete entry_type enumeration for ledger_entries |
| [07-financial-system/08-network-fees-account.md](./07-financial-system/08-network-fees-account.md) | NETWORK_FEES expense account for TON gas fees |

### Module Architecture & Conventions

| File | Description |
|------|-------------|
| [15-module-architecture.md](./15-module-architecture.md) | 18 Gradle subprojects, dependency graph, service mapping |
| [16-java-conventions.md](./16-java-conventions.md) | Java 25 conventions: testing, performance, quality, payment guarantees |
| [17-typescript-conventions.md](./17-typescript-conventions.md) | TypeScript/React conventions: testing, performance, financial UI |

### Frontend Pages

| File | Description |
|------|-------------|
| [15-frontend-pages/01-onboarding.md](./15-frontend-pages/01-onboarding.md) | Onboarding: welcome, interest, tour (3 screens) |
| [15-frontend-pages/02-catalog.md](./15-frontend-pages/02-catalog.md) | Catalog: channel list, filters, channel detail, create deal |
| [15-frontend-pages/03-deals.md](./15-frontend-pages/03-deals.md) | Deals: list, detail, negotiate, brief, creative, review, schedule, payment, dispute |
| [15-frontend-pages/04-wallet.md](./15-frontend-pages/04-wallet.md) | Wallet: summary, withdraw (owner only), history, transaction detail |
| [15-frontend-pages/05-profile.md](./15-frontend-pages/05-profile.md) | Profile: settings, channel registration, management, team |
| [15-frontend-pages/06-shared-components.md](./15-frontend-pages/06-shared-components.md) | Shared: sheets, modals, toasts, skeletons, empty states, navigation, routing |
| [15-frontend-pages/07-ton-connect-integration.md](./15-frontend-pages/07-ton-connect-integration.md) | TON Connect lifecycle, hooks, error mapping, deposit/withdraw flows |

### Infrastructure & Operations

| File | Description |
|------|-------------|
| [08-tech-stack.md](./08-tech-stack.md) | Technology stack and rationale |
| [09-deployment.md](./09-deployment.md) | MVP (single VPS) to Scaled (sharding, clusters) |
| [10-security-and-compliance.md](./10-security-and-compliance.md) | Auth, PII, immutability, custodial risk |
| [11-api-contracts.md](./11-api-contracts.md) | REST API surface, internal API, conventions |
| [12-development-roadmap.md](./12-development-roadmap.md) | 5 phases: Foundation to Trust & Scale |

### Implementation Specs

| File | Description |
|------|-------------|
| [14-implementation-specs/01-ton-sdk-integration.md](./14-implementation-specs/01-ton-sdk-integration.md) | TON SDK Integration Specification |
| [14-implementation-specs/02-telegram-bot-framework.md](./14-implementation-specs/02-telegram-bot-framework.md) | Telegram Bot Framework & Webhook Setup |
| [14-implementation-specs/03-auth-flow.md](./14-implementation-specs/03-auth-flow.md) | Auth Flow: initData HMAC + Session |
| [14-implementation-specs/04-kafka-event-schemas.md](./14-implementation-specs/04-kafka-event-schemas.md) | Kafka Event Schemas for All 8 Topics |
| [14-implementation-specs/05-ddl-migrations.md](./14-implementation-specs/05-ddl-migrations.md) | Complete DDL Migration Scripts |
| [14-implementation-specs/06-project-scaffold.md](./14-implementation-specs/06-project-scaffold.md) | Project Scaffold & Docker Compose |
| [14-implementation-specs/07-pii-encryption.md](./14-implementation-specs/07-pii-encryption.md) | PII Encryption & Key Management |
| [14-implementation-specs/08-ton-center-api-catalog.md](./14-implementation-specs/08-ton-center-api-catalog.md) | TON Center API Endpoint Catalog |
| [14-implementation-specs/09-redis-distributed-locks.md](./14-implementation-specs/09-redis-distributed-locks.md) | Redis Distributed Lock Implementation |
| [14-implementation-specs/10-worker-callback-schemas.md](./14-implementation-specs/10-worker-callback-schemas.md) | Worker Callback Payload Schemas (All 6 Types) |
| [14-implementation-specs/11-outbox-poller.md](./14-implementation-specs/11-outbox-poller.md) | Outbox Poller Implementation |
| [14-implementation-specs/12-error-code-catalog.md](./14-implementation-specs/12-error-code-catalog.md) | Error Code Catalog |
| [14-implementation-specs/13-abac-spring-security.md](./14-implementation-specs/13-abac-spring-security.md) | ABAC Policy Rules — Spring Security Mapping |
| [14-implementation-specs/14-reconciliation-sql.md](./14-implementation-specs/14-reconciliation-sql.md) | Reconciliation SQL Queries |
| [14-implementation-specs/15-confirmation-policy.md](./14-implementation-specs/15-confirmation-policy.md) | TON Block Height Tracking & Confirmation Policy |
| [14-implementation-specs/16-dispute-auto-resolution.md](./14-implementation-specs/16-dispute-auto-resolution.md) | Dispute Auto-Resolution Rules Engine |
| [14-implementation-specs/17-delivery-verifier-telegram-api.md](./14-implementation-specs/17-delivery-verifier-telegram-api.md) | Delivery Verifier — Telegram API Details |
| [14-implementation-specs/18-kafka-consumer-error-handling.md](./14-implementation-specs/18-kafka-consumer-error-handling.md) | Kafka Consumer Error Handling Strategy |
| [14-implementation-specs/19-deal-timeout-scheduler.md](./14-implementation-specs/19-deal-timeout-scheduler.md) | Deal Timeout Scheduler Implementation |
| [14-implementation-specs/20-creative-jsonb-schemas.md](./14-implementation-specs/20-creative-jsonb-schemas.md) | Creative Brief & Draft JSONB Schemas |
| [14-implementation-specs/21-metrics-slos-monitoring.md](./14-implementation-specs/21-metrics-slos-monitoring.md) | Metrics, SLOs & Monitoring Definitions |
| [14-implementation-specs/22-notification-templates-i18n.md](./14-implementation-specs/22-notification-templates-i18n.md) | Notification Templates & i18n |
| [14-implementation-specs/23-deployment-runbook.md](./14-implementation-specs/23-deployment-runbook.md) | Deployment Runbook & Troubleshooting Guide |
| [14-implementation-specs/24-postgresql-sharding.md](./14-implementation-specs/24-postgresql-sharding.md) | PostgreSQL Sharding & Routing (Scaled) |
| [14-implementation-specs/25-commission-rounding-sweep.md](./14-implementation-specs/25-commission-rounding-sweep.md) | Commission, Pricing Rules & Sweep Mechanism |
| [14-implementation-specs/26-testing-strategy.md](./14-implementation-specs/26-testing-strategy.md) | Testing Strategy (Testcontainers, state machine matrix, financial accuracy) |
| [14-implementation-specs/27-rate-limiting-strategy.md](./14-implementation-specs/27-rate-limiting-strategy.md) | Rate Limiting Strategy (API, auth, business, outbound) |
| [14-implementation-specs/28-external-api-resilience.md](./14-implementation-specs/28-external-api-resilience.md) | External API Resilience (Circuit Breakers, Fallbacks) |
| [14-implementation-specs/29-channel-search-impl.md](./14-implementation-specs/29-channel-search-impl.md) | Channel Search Implementation (jOOQ, indexes, cursor pagination) |
| [14-implementation-specs/30-payout-execution-flow.md](./14-implementation-specs/30-payout-execution-flow.md) | Payout Execution Flow (release, commission, TON payout) |
| [14-implementation-specs/31-overpayment-underpayment.md](./14-implementation-specs/31-overpayment-underpayment.md) | Overpayment & Underpayment Handling |
| [14-implementation-specs/32-worker-monitoring-dlq.md](./14-implementation-specs/32-worker-monitoring-dlq.md) | Worker Monitoring, Health Checks & DLQ Strategy |
| [14-implementation-specs/33-partition-automation.md](./14-implementation-specs/33-partition-automation.md) | Partition Auto-Creation & Archival |
| [14-implementation-specs/34-inter-module-interaction.md](./14-implementation-specs/34-inter-module-interaction.md) | Module dependency rules and allowed interaction patterns |
| [14-implementation-specs/35-deal-workflow-engine.md](./14-implementation-specs/35-deal-workflow-engine.md) | Deal Workflow Engine (post-transition side-effects) |
| [14-implementation-specs/36-logging-strategy.md](./14-implementation-specs/36-logging-strategy.md) | Structured Logging, Redaction & Retention |
| [14-implementation-specs/37-hot-wallet-management.md](./14-implementation-specs/37-hot-wallet-management.md) | Treasury wallet monitoring, balance alerts, replenishment |
| [14-implementation-specs/38-network-fee-accounting.md](./14-implementation-specs/38-network-fee-accounting.md) | NETWORK_FEES account, gas fee ledger entries, reconciliation |
| [14-implementation-specs/39-late-deposit-handling.md](./14-implementation-specs/39-late-deposit-handling.md) | Auto-refund for deposits to expired/cancelled deals |
| [14-implementation-specs/40-payout-wallet-architecture.md](./14-implementation-specs/40-payout-wallet-architecture.md) | Subwallet -> recipient direct payout, commission sweep |
| [14-implementation-specs/41-unclaimed-payouts.md](./14-implementation-specs/41-unclaimed-payouts.md) | Owner without TON address: notifications, 30-day timeout |
| [14-implementation-specs/42-partial-refund-accounting.md](./14-implementation-specs/42-partial-refund-accounting.md) | Time-based split, PARTIALLY_REFUNDED state, commission rules |
| [14-implementation-specs/43-seqno-management.md](./14-implementation-specs/43-seqno-management.md) | TON wallet seqno handling, nonce locking, recovery strategy |
| [14-implementation-specs/44-post-scheduler-impl.md](./14-implementation-specs/44-post-scheduler-impl.md) | Post Scheduler worker: publish flow, idempotency, retries |
| [14-implementation-specs/45-channel-statistics-collector.md](./14-implementation-specs/45-channel-statistics-collector.md) | Channel statistics refresh and freshness policy |

---

*Generated from C4 architecture model. 97 files total.*
