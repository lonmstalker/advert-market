# Architecture: Backend API Components (C4 Level 3)

## Overview

The Backend API container (Java 25 + Spring Boot 4) contains 20+ services organized into four concern groups: Deal Domain, Financial Core, Marketplace, and Identity/Infrastructure.

## Component Map

```mermaid
flowchart TB
    subgraph "Deal Domain"
        DC[Deal Controller<br/>REST /api/v1/deals]
        DTS[Deal Transition Service<br/>State machine, actor-checked]
        DWE[Deal Workflow Engine<br/>Lifecycle orchestration]
        DIS[Dispute Service<br/>Auto rules + escalation]
    end

    subgraph "Financial Core"
        LS[Ledger Service<br/>Double-entry, append-only]
        BP[Balance Projection<br/>CQRS read model]
        ES[Escrow Service<br/>Deposit, hold, release, refund]
        CS[Commission Service<br/>10% calculation]
        RS[Reconciliation Service<br/>3-way reconciliation]
        TPG[TON Payment Gateway<br/>Blockchain abstraction]
        CP[Confirmation Policy<br/>Tiered confirmations]
    end

    subgraph "Identity (Implemented)"
        AS[Auth Service<br/>HMAC-SHA256 + JWT + anti-replay]
        US[User Service<br/>Profile, onboarding, soft delete]
        RL[Login Rate Limiter<br/>Redis Lua, IP-based]
        TB[Token Blacklist<br/>Redis, fail-closed]
        AZ[Authorization Service<br/>isOperator check]
    end

    subgraph "Identity (Planned)"
        ABAC[ABAC Service<br/>Full attribute-based authz]
        CTS[Channel Team Service<br/>Team management]
        PII[PII Vault<br/>AES-256-GCM encryption]
    end

    subgraph "Infrastructure"
        OP[Outbox Publisher<br/>TX outbox → Kafka]
        WCC[Worker Callback Controller<br/>POST /internal/v1/worker-events]
    end

    DC --> DTS --> DWE
    DC --> AZ
    DWE --> ES --> LS
    DWE --> LS
    ES --> TPG
    ES --> CP
    LS --> CS
    BP -->|reads| LS
    WCC --> DTS
    AS --> RL
    AS --> TB
```

## Deal Domain Services

### Deal Controller

| Attribute | Value |
|-----------|-------|
| **Type** | Component (REST Controller) |
| **Endpoints** | `/api/v1/deals` — CRUD, transitions, offers |
| **Depends on** | Deal Transition Service, Auth Service |

### Deal Transition Service

| Attribute | Value |
|-----------|-------|
| **Type** | Service |
| **Tags** | `#idempotent` |
| **Role** | State machine transitions: actor-checked, idempotent, event-emitting |
| **Stores to** | `deals` (status update), `deal_events` (append transition event) |
| **Called by** | Deal Controller, Worker Callback Controller |

### Deal Workflow Engine

| Attribute | Value |
|-----------|-------|
| **Type** | Service |
| **Role** | Lifecycle orchestration: deadlines, side-effects, timeout scheduling |
| **Depends on** | Escrow Service, Ledger Service |

### Dispute Service

| Attribute | Value |
|-----------|-------|
| **Type** | Service |
| **Role** | Hybrid dispute resolution: auto rules engine + operator escalation |
| **Stores to** | `dispute_evidence` (append evidence) |
| **Depends on** | Deal Transition Service, Deal Workflow Engine, Ledger Service |

## Financial Core Services

### Ledger Service

| Attribute | Value |
|-----------|-------|
| **Type** | Service |
| **Tags** | `#financial`, `#cqrs-write`, `#immutable`, `#critical` |
| **Role** | Double-entry append-only ledger — source of truth for all money movement |
| **Stores to** | `ledger_entries` (double-entry record), `audit_log` (audit trail) |
| **Depends on** | Commission Service |

### Balance Projection

| Attribute | Value |
|-----------|-------|
| **Type** | Service |
| **Tags** | `#financial`, `#cqrs-read` |
| **Role** | CQRS read model — materialized from ledger events, cached in Redis |
| **Reads** | `ledger_entries` |
| **Stores to** | `account_balances` (materialized view), Redis Balance Cache |

### Escrow Service

| Attribute | Value |
|-----------|-------|
| **Type** | Service |
| **Tags** | `#financial`, `#custodial`, `#idempotent` |
| **Role** | Deposit address generation, funding confirmation, hold, release, refund |
| **Depends on** | TON Payment Gateway, Confirmation Policy, Ledger Service |
| **Uses** | Redis Distributed Locks (escrow deduplication) |

### Commission Service

| Attribute | Value |
|-----------|-------|
| **Type** | Service |
| **Tags** | `#financial` |
| **Role** | Platform commission calculation (10% default, segment-configurable) |

### Reconciliation Service

| Attribute | Value |
|-----------|-------|
| **Type** | Service |
| **Tags** | `#financial`, `#audit` |
| **Role** | Ledger vs blockchain vs deal aggregate reconciliation |
| **Reads** | `ledger_entries`, `ton_transactions`, `deals` |

### TON Payment Gateway

| Attribute | Value |
|-----------|-------|
| **Type** | Service |
| **Tags** | `#financial`, `#blockchain` |
| **Role** | TON blockchain abstraction: address gen, tx submit, confirmation tracking |
| **Stores to** | `ton_transactions` |
| **External** | TON Center API |

### Confirmation Policy

| Attribute | Value |
|-----------|-------|
| **Type** | Service |
| **Tags** | `#financial` |
| **Role** | Tiered confirmation: <=100 TON → 1 conf, <=1000 TON → 3, >1000 TON → 5 + operator review |

## Marketplace Services

### Channel Service

| Attribute | Value |
|-----------|-------|
| **Type** | Service |
| **Module** | marketplace |
| **Role** | Channel CRUD, statistics updates, listing management, pricing rules |
| **Stores to** | `channels` |
| **Depends on** | Identity API (for ownership verification) |

### Search Service

| Attribute | Value |
|-----------|-------|
| **Type** | Service |
| **Module** | marketplace |
| **Role** | Channel search: PostgreSQL full-text search + composite filters (topic, subscriber_count, price range), cursor pagination |
| **Reads** | `channels` |
| **Indexes** | GIN index on `to_tsvector(title || description)`, composite index on `(topic, subscriber_count, price_per_post_nano)` |

## Identity Services (Implemented)

### Auth Service (`AuthServiceImpl`)

| Attribute | Value |
|-----------|-------|
| **Type** | Service |
| **Tags** | `#mvp`, `#implemented` |
| **Module** | identity |
| **Role** | Telegram initData HMAC-SHA256 validation + JWT (HS256, JJWT) issuance + anti-replay window + token revocation |
| **Endpoints** | `POST /api/v1/auth/login`, `POST /api/v1/auth/logout` |
| **Dependencies** | TelegramInitDataValidator, JwtTokenProvider, UserRepository, TokenBlacklistPort, LoginRateLimiterPort, MetricsFacade |
| **Stores to** | `users` (upsert on login) |
| **Metrics** | `auth.login.success`, `auth.logout` |

### User Service (`UserServiceImpl`)

| Attribute | Value |
|-----------|-------|
| **Type** | Service |
| **Tags** | `#mvp`, `#implemented` |
| **Module** | identity |
| **Role** | User profile CRUD: get profile, complete onboarding (interests), soft delete account |
| **Endpoints** | `GET /api/v1/profile`, `PUT /api/v1/profile/onboarding`, `DELETE /api/v1/profile` |
| **Dependencies** | UserRepository, MetricsFacade, AuthService (logout on delete) |
| **Metrics** | `account.deleted` |

### Login Rate Limiter (`RedisLoginRateLimiter`)

| Attribute | Value |
|-----------|-------|
| **Type** | Infrastructure adapter |
| **Tags** | `#implemented` |
| **Module** | identity |
| **Role** | IP-based rate limiting via Redis atomic Lua script (INCR + EXPIRE). Fail-closed on Redis errors |
| **Config** | `app.auth.rate-limiter.maxAttempts`, `app.auth.rate-limiter.windowSeconds` |

### Token Blacklist (`RedisTokenBlacklist`)

| Attribute | Value |
|-----------|-------|
| **Type** | Infrastructure adapter |
| **Tags** | `#implemented` |
| **Module** | identity |
| **Role** | JWT revocation tracking in Redis with TTL matching token expiration. Fail-closed: Redis errors → token treated as blacklisted |

### Authorization Service

| Attribute | Value |
|-----------|-------|
| **Type** | Service |
| **Tags** | `#implemented`, `#minimal` |
| **Module** | identity |
| **Role** | Simple `isOperator()` check via SecurityContextUtil. Used in `@PreAuthorize("@auth.isOperator()")` SpEL |
| **Note** | Will evolve into full ABAC service |

## Identity Services (Planned)

### ABAC Service

| Attribute | Value |
|-----------|-------|
| **Type** | Service |
| **Tags** | `#planned` |
| **Module** | identity |
| **Role** | Attribute-Based Access Control: evaluates permissions from subject + resource + action + context attributes. No fixed user roles — permissions derived from resource relationships |
| **Reads** | `channel_memberships`, `deals` (for ownership check) |
| **See** | [ABAC Pattern](../05-patterns-and-decisions/08-abac.md) |

### Channel Team Service

| Attribute | Value |
|-----------|-------|
| **Type** | Service |
| **Tags** | `#planned` |
| **Module** | identity |
| **Role** | Channel team management: invite, rights delegation, revocation |
| **Stores to** | `channel_memberships` |

### PII Vault

| Attribute | Value |
|-----------|-------|
| **Type** | Service |
| **Tags** | `#planned`, `#pii` |
| **Role** | Isolated PII storage with AES-256-GCM field-level encryption |
| **Stores to** | `pii_store` |

### Outbox Publisher

| Attribute | Value |
|-----------|-------|
| **Type** | Service |
| **Tags** | `#outbox` |
| **Role** | Transactional outbox to Kafka producer (Debezium CDC or direct produce) |
| **Reads** | `notification_outbox` |
| **Produces to** | `deal.events`, `escrow.commands`, `delivery.commands`, `notifications.outbox`, `deal.deadlines` |

### Worker Callback Controller

| Attribute | Value |
|-----------|-------|
| **Type** | Component (REST Controller) |
| **Tags** | `#internal-only`, `#worker-callback` |
| **Endpoint** | `POST /internal/v1/worker-events` |
| **Role** | Receives results from workers: deposit confirmed, payout sent, etc. |
| **Depends on** | Deal Transition Service |

## Related Documents

- [Containers](./02-containers.md) — C4 Level 2
- [Workers](./04-workers.md) — async processors that call back to Backend API
- [Data Stores](./05-data-stores.md) — tables used by services
- [Patterns & Decisions](../05-patterns-and-decisions/) — architectural patterns used
