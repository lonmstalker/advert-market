# Module Architecture

> Modular monolith with a clear path to microservices. 18 Gradle subprojects organized by bounded context.

## Project Structure

```
advert-market/
├── platform-bom/                       # BOM: all dependency versions
├── advert-market-shared/               # Shared kernel (value objects, events, exceptions)
├── advert-market-db/                   # jOOQ codegen + Liquibase migrations
│
├── advert-market-identity-api/         # Identity: DTOs, interfaces
├── advert-market-identity/             # Identity: auth, ABAC, PII, team
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

```
com.advertmarket.shared/
├── event/       EventEnvelope (record), DomainEvent (sealed), EventType (enum)
├── model/       Money, DealId, UserId, ChannelId, AccountId (typed ID records), DealStatus (enum)
├── exception/   DomainException, EntityNotFoundException, InvalidStateTransitionException
├── pagination/  CursorPage (record), CursorCodec
└── util/        IdempotencyKey (record)
```

Zero external dependencies — pure Java library.

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
| Auth Service, ABAC Service, PII Vault | identity |
| Channel Team Service | identity |
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

All 36 specs from `14-implementation-specs/` are covered:

| Spec | Module |
|---|---|
| 01-ton-sdk, 08-ton-center-api, 15-confirmation-policy | financial |
| 02-telegram-bot, 22-notification-templates | communication |
| 03-auth-flow, 07-pii-encryption, 13-abac | identity |
| 04-kafka-schemas, 18-kafka-consumer-errors | shared (schemas), each consumer module |
| 05-ddl-migrations | db |
| 06-project-scaffold | root + app |
| 09-redis-locks | shared (util) + financial |
| 10-worker-callbacks | delivery |
| 11-outbox-poller | communication |
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
| 27-rate-limiting | app (filter) |
| 28-external-api-resilience | financial, delivery |
| 29-channel-search | marketplace |
| 30-payout-execution-flow | financial + delivery |
| 31-overpayment-underpayment | financial |
| 32-worker-monitoring-dlq | app (cross-cutting) |
| 33-partition-automation | db |
| 35-deal-workflow-engine | deal |
| 36-logging-strategy | app (cross-cutting) |
