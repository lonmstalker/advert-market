# Inter-Module Interaction Architecture

## Module Dependency Graph

```
platform-bom (BOM)
    │
shared (value objects, events, exceptions)
    │
    ├── identity-api ─── identity (impl)
    ├── financial-api ── financial (impl)
    ├── marketplace-api ─ marketplace (impl)
    ├── deal-api ──────── deal (impl)
    ├── delivery-api ──── delivery (impl)
    └── communication-api ─ communication (impl)
                               │
                            app (assembly)
```

### API Module Dependencies

Все `-api` модули зависят **только** от `shared`:

| API Module | Dependencies |
|------------|-------------|
| identity-api | shared |
| financial-api | shared |
| marketplace-api | shared |
| deal-api | shared |
| delivery-api | shared |
| communication-api | shared |

### Impl Module Dependencies

| Impl Module | API Deps | Other Deps |
|-------------|----------|------------|
| identity | identity-api | shared, db |
| financial | financial-api | shared, db |
| marketplace | marketplace-api, identity-api | shared, db |
| deal | deal-api, financial-api, identity-api, marketplace-api | shared, db |
| delivery | delivery-api | shared, db |
| communication | communication-api | shared, db |

**Правила:**
- Impl модули **не зависят** друг от друга — только через API
- `financial` изолирован: никаких зависимостей на deal/marketplace/delivery/communication
- `deal` — единственный модуль, зависящий от 3 API (financial, identity, marketplace)
- `delivery` и `communication` полностью изолированы; взаимодействие через Kafka

---

## Port Catalog

### Exported Ports (из API модулей)

| Port | Module | Methods | Consumers |
|------|--------|---------|-----------|
| `NotificationPort` | communication-api | `send(NotificationRequest)` | deal, financial |

> Остальные Port-интерфейсы будут добавлены по мере реализации бизнес-логики:
> `EscrowPort` (financial-api), `UserPort` (identity-api), `ChannelPort` (marketplace-api), `DealPort` (deal-api), `DeliveryPort` (delivery-api).

### Internal Ports (внутри impl модулей)

| Port | Module | Purpose |
|------|--------|---------|
| `UpdateDeduplicationPort` | communication | Webhook update дедупликация (Redis) |
| `RateLimiterPort` | communication | Rate limiting Telegram API |
| `UserStatePort` | communication | Bot state machine (Redis) |
| `UserBlockPort` | communication | Block/unblock users |

---

## Synchronous Interactions (Port Calls)

Sync вызовы через DI — impl модуль реализует Port из API:

```
deal-impl ──→ EscrowPort (financial-api) ──→ financial-impl
deal-impl ──→ UserPort (identity-api) ──→ identity-impl
deal-impl ──→ ChannelPort (marketplace-api) ──→ marketplace-impl
deal-impl ──→ NotificationPort (communication-api) ──→ communication-impl
```

Spring собирает зависимости в `app` модуле, где все impl на classpath.

---

## Asynchronous Interactions (Kafka)

### Topics

| Topic | Partition Key | Producer | Consumer |
|-------|-------------|----------|----------|
| deal.events | deal_id | deal | delivery, communication, financial |
| escrow.commands | deal_id | deal | financial |
| escrow.confirmations | deal_id | financial | deal |
| delivery.commands | deal_id | deal | delivery |
| delivery.results | deal_id | delivery | deal |
| notifications.outbox | recipient_id | deal, financial | communication |
| reconciliation.triggers | — | scheduler | financial |
| deal.deadlines | deal_id | deal | deal |

### Consumer Groups

| Group | Topic | Module |
|-------|-------|--------|
| cg-deposit-watcher | escrow.commands | financial |
| cg-payout-executor | escrow.commands | financial |
| cg-refund-executor | escrow.commands | financial |
| cg-deal-timeout | deal.deadlines | deal |
| cg-post-scheduler | delivery.commands | delivery |
| cg-delivery-verifier | delivery.commands | delivery |
| cg-reconciliation | reconciliation.triggers | financial |
| cg-bot-notifier | notifications.outbox | communication |

---

## Transactional Outbox

### Schema

```sql
CREATE TABLE notification_outbox (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    deal_id         UUID,
    idempotency_key UUID NOT NULL UNIQUE,
    topic           TEXT NOT NULL,
    partition_key   TEXT NOT NULL,
    payload         JSONB NOT NULL,
    status          TEXT NOT NULL DEFAULT 'PENDING',
    retry_count     INT NOT NULL DEFAULT 0,
    version         INT NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at    TIMESTAMPTZ
);
```

### Flow

```
Business Tx: INSERT deal row + INSERT outbox row (same tx)
     ↓
Outbox Poller (500ms): SELECT ... WHERE status='PENDING' FOR UPDATE SKIP LOCKED
     ↓
Kafka Producer: send to topic
     ↓
On success: UPDATE status='DELIVERED', processed_at=now()
On failure: UPDATE retry_count++, exponential backoff (1s, 2s, 4s), max 3 retries
```

### Produced Topics

- `deal.events` — deal state changes
- `escrow.commands` — financial commands
- `delivery.commands` — delivery operations
- `notifications.outbox` — user notifications
- `deal.deadlines` — timeout events

---

## Key Interaction Scenarios

### 1. Deal Funded (deposit confirmed)

```
TON blockchain → financial (deposit watcher)
  → Kafka: escrow.confirmations (DEPOSIT_CONFIRMED)
    → deal (consumer): transition deal to FUNDED
      → Outbox: deal.events (DEAL_FUNDED)
        → communication: send notification to advertiser
        → delivery: schedule post publication
```

### 2. Creative Approved

```
Advertiser approves creative in Mini App
  → deal: transition to CREATIVE_APPROVED
    → Outbox: deal.events (CREATIVE_APPROVED)
      → communication: notify channel owner
      → delivery: unlock publication slot
```

### 3. Delivery Verified

```
delivery (scheduler checks post)
  → Kafka: delivery.results (DELIVERY_VERIFIED)
    → deal: transition to DELIVERED
      → Outbox: escrow.commands (EXECUTE_PAYOUT)
        → financial: release funds to channel owner
      → Outbox: notifications.outbox
        → communication: notify both parties
```

### 4. Deal Expired (timeout)

```
Outbox: deal.deadlines (DEADLINE_SET, scheduled)
  → deal (consumer): check if deadline passed
    → transition to EXPIRED
      → Outbox: escrow.commands (EXECUTE_REFUND)
        → financial: refund to advertiser
      → Outbox: notifications.outbox
        → communication: notify both parties
```

---

## Adapter Package Structure

Каждый impl модуль организован по слоям:

```
com.advertmarket.<module>/
├── api/           # Реализации Port интерфейсов
├── internal/      # Внутренняя логика
│   ├── config/    # @Configuration, @ConfigurationProperties
│   ├── adapter/   # Kafka listeners, external API clients
│   ├── service/   # Domain services
│   └── repo/      # jOOQ repositories
└── package-info.java
```

### Адаптеры по модулям

| Module | Inbound Adapters | Outbound Adapters |
|--------|-----------------|-------------------|
| identity | REST (auth endpoints) | PostgreSQL, JWT |
| financial | Kafka consumer (escrow.commands) | TON API, PostgreSQL, Kafka producer |
| marketplace | REST (channel CRUD) | PostgreSQL |
| deal | REST (deal CRUD), Kafka consumer (escrow.confirmations, delivery.results, deal.deadlines) | PostgreSQL, Outbox → Kafka |
| delivery | Kafka consumer (delivery.commands) | Telegram API (post verification), PostgreSQL, Kafka producer |
| communication | Telegram webhook, Kafka consumer (notifications.outbox) | Telegram Bot API, Redis, PostgreSQL |
