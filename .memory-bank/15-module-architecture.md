# Module Architecture

> Modular monolith with a clear path to microservices. 18 Gradle subprojects organized by bounded context.

## Project Structure

```
advert-market/
├── platform-bom/                       # BOM: all dependency versions
├── advert-market-shared/               # Shared kernel (value objects, events, exceptions)
├── advert-market-db/                   # jOOQ codegen + Liquibase migrations
│
├── advert-market-identity-api/         # Identity: DTOs, port interfaces (5 ports, 5 DTOs)
├── advert-market-identity/             # Identity: auth (JWT+HMAC), user profile, rate limiting
│
├── advert-market-financial-api/        # Financial: commands, ports, events
├── advert-market-financial/            # Financial: ledger, escrow, TON, reconciliation
│
├── advert-market-marketplace-api/      # Marketplace: DTOs
├── advert-market-marketplace/          # Marketplace: channels, search, pricing
│
├── advert-market-deal-api/             # Deal: DTOs, state types, events
├── advert-market-deal/                 # Deal: state machine, workflow, disputes
│
├── advert-market-delivery-api/         # Delivery: commands/results
├── advert-market-delivery/             # Delivery: post scheduler, verifier (Kafka)
│
├── advert-market-communication-api/    # Communication: notification DTOs
├── advert-market-communication/        # Communication: bot, outbox poller
│
├── advert-market-app/                  # Spring Boot assembly (single fat JAR)
├── advert-market-frontend/             # React 19 + Vite 7 + TypeScript SPA
└── advert-market-integration-tests/    # Testcontainers, ArchUnit, E2E
```

## API/Impl Split

Each bounded context = 2 modules:

- **`-api`**: records (DTOs), interfaces (ports), events. Depends only on `shared`.
- **`-impl`**: implementation. Depends on its own `-api`, `shared`, `db`.

This split enables:
1. Compile-time dependency control — impl modules never leak
2. Clean microservice extraction — `-api` becomes a published Maven artifact
3. Testability — mock ports without touching implementation

## Current Module Status (HEAD)

| Module | Status | Notes |
|---|---|---|
| `advert-market-identity` | Implemented | Auth/login/logout, profile APIs, onboarding |
| `advert-market-marketplace` | Implemented | Channel catalog/search, verification, pricing, team management |
| `advert-market-communication` | Partially implemented | Webhook + bot routing + shared outbox integration points |
| `advert-market-deal` | Implemented | Deal controllers, transition service, workflow engine, timeout scheduler, dispute hooks |
| `advert-market-financial` | Implemented | Wallet API, escrow/ledger services, TON workers (deposit/payout/refund), reconciliation/commission flows |
| `advert-market-delivery` | Partially implemented | Delivery event listener and integration hooks are present; full scheduler/verifier surface is still in progress |

## Dependency Graph (impl → api)

| Module (impl) | Depends on -api |
|---|---|
| identity | identity-api |
| financial | financial-api |
| marketplace | marketplace-api, identity-api |
| deal | deal-api, financial-api, identity-api, marketplace-api |
| delivery | delivery-api |
| communication | communication-api |

### Critical Isolation Rules

- **Financial isolation** — `financial-api` and `financial` do NOT depend on deal/marketplace/delivery/communication. Only `deal` calls `EscrowPort` (from `financial-api`) synchronously.
- **Async communication** — `delivery` and `communication` have NO compile dependencies on `deal`. Communication only through Kafka events (schemas in `shared`).

## Shared Kernel (`advert-market-shared`)

53+ classes providing cross-cutting concerns, domain primitives, and infrastructure contracts.

```
com.advertmarket.shared/
├── error/       ErrorCode (enum, 45+ codes with HTTP status), ErrorCodes (@Fenum constants)
├── exception/   DomainException, EntityNotFoundException, InvalidStateTransitionException,
│                InsufficientBalanceException
├── json/        JsonFacade (wrapper over ObjectMapper), JsonException
├── event/       DomainEvent (marker interface), EventEnvelope (record), EventTypes (constants),
│                TopicNames, ConsumerGroups, EventTypeRegistry,
│                EventEnvelopeSerializer, EventEnvelopeDeserializer, EventDeserializationException
├── model/       Money, DealId, UserId, ChannelId, AccountId (typed ID records),
│                TonAddress, TxHash, SubwalletId (TON primitives),
│                DealStatus, AccountType, EntryType, ActorType (enums),
│                UserBlockCheckPort (interface)
├── financial/   CommissionCalculator (integer arithmetic, basis points), CommissionResult
├── lock/        DistributedLockPort (interface), RedisDistributedLock (SET NX + Lua unlock)
├── outbox/      OutboxEntry, OutboxStatus, OutboxProperties, OutboxPoller (@Scheduled),
│                OutboxPublisher (interface), OutboxRepository (interface)
├── audit/       AuditEntry, AuditEvent, AuditActorType, AuditPort (interface)
├── metric/      MetricsFacade (Micrometer wrapper), MetricNames (40+ @Fenum constants)
├── security/    PrincipalAuthentication (interface), SecurityContextUtil
├── pagination/  CursorPage (record), CursorCodec (Base64 URL-safe)
├── deploy/      DeployProperties, CanaryProperties, UserBucket (SHA-256 stable bucketing)
├── i18n/        LocalizationService (Spring MessageSource, default ru)
├── util/        IdempotencyKey (record)
└── FenumGroup   Checker Framework @Fenum group constants (TopicName, ErrorCode, MetricName, etc.)
```

Dependencies: Jackson, Guava, Commons Lang3, Checker Framework, Lombok, Spring Data Redis (Lettuce), Spring Framework, Micrometer.

## Database Module (`advert-market-db`)

Single module containing:
- All Liquibase migration scripts (changelog per bounded context)
- jOOQ codegen configuration
- Generated jOOQ classes used by all `-impl` modules

All `-impl` modules depend on `db` for generated jOOQ types. This ensures a single source of truth for the database schema.

## Assembly Module (`advert-market-app`)

The only module with the Spring Boot Gradle plugin. Responsibilities:
- `@SpringBootApplication` entry point
- Global configuration: `JooqConfig`, `KafkaConfig`, `SecurityFilterChain`, `RedisConfig`
- Fat JAR packaging
- No business logic

## Frontend Module (`advert-market-frontend`)

Independent module with `com.github.node-gradle.node` plugin:
- React 19 + Vite 7 + TypeScript
- Builds separately from backend
- Deployed to CDN/nginx (not inside fat JAR)
- See [17-typescript-conventions.md](./17-typescript-conventions.md) for conventions

## Integration Tests Module (`advert-market-integration-tests`)

Depends on all `-impl` modules. Contains:
- Testcontainers-based integration tests (PostgreSQL, Kafka, Redis)
- ArchUnit rules (cross-module dependency enforcement)
- E2E flow tests (deal lifecycle, escrow flow)

## Path to Microservices

When splitting:

1. `-api` module is published as Maven artifact
2. Sync calls replaced by REST/gRPC clients implementing the same port interfaces
3. Each context gets its own `app` module
4. Business logic does not change — only wiring

### Extraction Order (recommended)

1. `communication` — fewest dependencies, Kafka-only input
2. `delivery` — Kafka-only input, isolated verification logic
3. `financial` — strict isolation already enforced
4. `marketplace` — search may benefit from dedicated scaling
5. `deal` — most complex, extract last

## Service-to-Module Mapping

All 20+ services from C4 Level 3 mapped to modules:

| C4 Service | Module |
|---|---|
| Auth Service (implemented), Login Rate Limiter (implemented) | identity |
| User Profile Service (implemented) | identity |
| ABAC Service (implemented), Channel Team Service (implemented), PII Vault (planned) | identity + marketplace |
| Ledger Service, Escrow Service, Commission Service | financial |
| Balance Projection, Reconciliation Service | financial |
| TON Payment Gateway, Confirmation Policy | financial |
| Channel Service, Search Service | marketplace |
| Deal Transition Service, Deal Workflow Engine | deal |
| Dispute Service | deal |
| Post Scheduler, Delivery Verifier | delivery |
| Bot Notifier, Outbox Publisher | communication |
| Webhook Handler, Bot Command Router, Canary Router | communication (Telegram Bot) |

## Implementation Spec Coverage

All 45 specs from `14-implementation-specs/` are covered:

| Spec | Module |
|---|---|
| 01-ton-sdk, 08-ton-center-api, 15-confirmation-policy | financial |
| 02-telegram-bot, 22-notification-templates | communication |
| 03-auth-flow | identity (implemented) |
| 07-pii-encryption | identity (planned, not yet implemented) |
| 13-abac | identity + marketplace (implemented in current channel/team authorization flow) |
| 04-kafka-schemas, 18-kafka-consumer-errors | shared (schemas), each consumer module |
| 05-ddl-migrations | db |
| 06-project-scaffold | root + app |
| 09-redis-locks | shared (lock/) + financial |
| 10-worker-callbacks | delivery |
| 11-outbox-poller | shared (outbox/) — OutboxPoller, ports; implementations in communication |
| 12-error-codes | shared |
| 14-reconciliation-sql | financial |
| 16-dispute-auto-resolution | deal |
| 17-delivery-verifier | delivery |
| 19-deal-timeout-scheduler | deal |
| 20-creative-jsonb | deal |
| 21-metrics-slos | app (cross-cutting) |
| 23-deployment-runbook | app |
| 24-postgresql-sharding | db |
| 25-commission-rounding | financial |
| 26-testing-strategy | integration-tests |
| 27-rate-limiting | identity (LoginRateLimiterPort + RedisLoginRateLimiter) |
| 28-external-api-resilience | financial, delivery |
| 29-channel-search | marketplace |
| 30-payout-execution-flow | financial + delivery |
| 31-overpayment-underpayment | financial |
| 32-worker-monitoring-dlq | app (cross-cutting) |
| 33-partition-automation | db |
| 34-inter-module-interaction | shared + all `-impl` modules (dependency rules) |
| 35-deal-workflow-engine | deal |
| 36-logging-strategy | app (cross-cutting) |
| 37-hot-wallet-management | financial |
| 38-network-fee-accounting | financial |
| 39-late-deposit-handling | financial + deal |
| 40-payout-wallet-architecture | financial |
| 41-unclaimed-payouts | financial + communication |
| 42-partial-refund-accounting | financial + deal |
| 43-seqno-management | financial |
| 44-post-scheduler-impl | delivery |
| 45-channel-statistics-collector | marketplace |
