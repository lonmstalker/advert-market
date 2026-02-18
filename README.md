# advert-market

Telegram Mini App marketplace for advertising in Telegram channels with TON escrow payments.

![Java 25](https://img.shields.io/badge/Java-25-orange)
![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4.0-green)
![CI](https://github.com/lonmstalker/advert-market/actions/workflows/ci.yml/badge.svg)
![License](https://img.shields.io/badge/License-TBD-lightgrey)

## About

Advert Market connects Telegram channel owners with advertisers through a secure marketplace.
Channel owners list their channels with automatically refreshed audience stats and pricing; advertisers browse the catalog,
create deals, and pay through TON blockchain escrow. The platform verifies ad delivery automatically,
handles dispute resolution, and releases funds only after confirmation — protecting both parties.

Key capabilities: channel catalog with full-text search, deal lifecycle management, TON escrow payments,
automated delivery verification, dispute resolution, double-entry financial ledger.

## Demo

> **Bot**: [@adv_markt_bot](https://t.me/adv_markt_bot)

## Tech Stack

| Layer | Technologies |
|-------|-------------|
| **Backend** | Java 25, Spring Boot 4, Spring Security, jOOQ, Liquibase |
| **Frontend** | React 19, TypeScript, Vite 7, TMA.js SDK |
| **Data** | PostgreSQL 18 (ParadeDB), Redis 8.4, Apache Kafka 4.1 (KRaft) |
| **Blockchain** | TON (ton4j), TON Connect |
| **Messaging** | Telegram Bot API (java-telegram-bot-api) |
| **Quality** | Checkstyle, SpotBugs, Checker Framework, Lombok, MapStruct |
| **Testing** | JUnit 5, Testcontainers, AssertJ, ArchUnit |
| **Observability** | Micrometer, Prometheus |

Exact versions are managed in `platform-bom` and root `build.gradle`.

## Architecture

Modular monolith with 6 bounded contexts, each split into API and implementation modules:

| Context | Responsibility |
|---------|---------------|
| **Identity** | Users, Telegram auth, JWT tokens |
| **Marketplace** | Channel catalog, pricing rules, full-text search |
| **Deal** | Deal lifecycle, state machine, negotiation |
| **Financial** | Double-entry ledger, escrow, commissions |
| **Delivery** | Ad delivery verification, screenshots |
| **Communication** | Telegram notifications, templates |

Key patterns: ABAC authorization, Double-Entry Ledger (nanoTON), Transactional Outbox,
CQRS, Event Sourcing for financial operations, State Machine for deals.

<details>
<summary>Module tree (18 Gradle subprojects)</summary>

```
advert-market/
├── advert-market-shared/              # Common DTOs, exceptions, utilities
├── advert-market-db/                  # jOOQ generated classes, Liquibase migrations
├── advert-market-identity-api/        # Identity port interfaces
├── advert-market-identity/            # Identity implementation
├── advert-market-marketplace-api/     # Marketplace port interfaces
├── advert-market-marketplace/         # Marketplace implementation
├── advert-market-deal-api/            # Deal port interfaces
├── advert-market-deal/                # Deal implementation
├── advert-market-financial-api/       # Financial port interfaces
├── advert-market-financial/           # Financial implementation
├── advert-market-delivery-api/        # Delivery port interfaces
├── advert-market-delivery/            # Delivery implementation
├── advert-market-communication-api/   # Communication port interfaces
├── advert-market-communication/       # Communication implementation
├── advert-market-app/                 # Spring Boot application, composition root
├── advert-market-frontend/            # React SPA (Telegram Mini App)
├── advert-market-integration-tests/   # Cross-module integration tests
└── platform-bom/                      # Dependency version management
```
</details>

See [Architecture docs (C4)](.memory-bank/04-architecture/) and [Patterns & Decisions](.memory-bank/05-patterns-and-decisions/).

## Data Model

20 base tables across 6 bounded contexts (14 primary operational + 6 support/lookup).

Primary operational tables include:

- **Identity**: `users`, `pii_store`
- **Marketplace**: `channels`, `channel_memberships`, `channel_pricing_rules`
- **Deal**: `deals`, `deal_events`
- **Financial**: `ledger_entries`, `account_balances`, `ton_transactions`, `audit_log`
- **Delivery**: `posting_checks`, `dispute_evidence`
- **Infrastructure**: `notification_outbox`

See the full ER model and complete table inventory in
`.memory-bank/04-architecture/05-data-stores.md`.

Migrations: [`advert-market-db/src/main/resources/db/changelog/changes/`](advert-market-db/src/main/resources/db/changelog/changes/).
Schema docs: [.memory-bank/04-architecture/05-data-stores.md](.memory-bank/04-architecture/05-data-stores.md).

## Quick Start

### Prerequisites

- Java 25 (with `--enable-preview`)
- Node.js 24 LTS
- Docker & Docker Compose

### Run

```bash
# Start infrastructure
docker compose up -d

# Create Kafka topics
bash scripts/create-kafka-topics.sh

# Build & run backend
./gradlew build
./gradlew :advert-market-app:bootRun

# Frontend (separate terminal)
cd advert-market-frontend
npm install
npm run dev
```

The backend starts on `http://localhost:8080`, frontend on `http://localhost:5173`.

## Testing

```bash
# Backend (requires Docker for Testcontainers)
./gradlew test

# Frontend
cd advert-market-frontend
npm test
npm run lint
npm run typecheck
```

## Deployment

Blue-green deployment via Docker Compose + nginx reverse proxy.

### Required Environment Variables

| Variable | Description |
|----------|-------------|
| `DB_PASSWORD` | PostgreSQL password for app + migrations |
| `JWT_SECRET` | JWT signing secret |
| `INTERNAL_API_KEY` | Shared key for internal worker endpoints |
| `CANARY_ADMIN_TOKEN` | Token for `/internal/v1/canary` |
| `TELEGRAM_BOT_TOKEN` | Telegram Bot API token |
| `TELEGRAM_BOT_USERNAME` | Bot username (without `@`) |
| `TELEGRAM_WEBHOOK_URL` | Public HTTPS webhook URL |
| `TELEGRAM_WEBHOOK_SECRET` | Telegram webhook secret token |
| `TELEGRAM_WEBAPP_URL` | Public Mini App URL |
| `TON_API_KEY` | TON Center API key |
| `TON_WALLET_MNEMONIC` | Platform wallet mnemonic |
| `PII_ENCRYPTION_KEY` | Base64-encoded 32-byte key for encrypted fields |
| `APP_MARKETPLACE_CHANNEL_BOT_USER_ID` | Bot user id (numeric) |
| `CREATIVES_STORAGE_ACCESS_KEY` | S3/MinIO access key |
| `CREATIVES_STORAGE_SECRET_KEY` | S3/MinIO secret key |

Optional (runtime defaults are defined in `deploy/docker-compose.prod.yml` and `application.yml`):

- `TON_NETWORK`
- `APP_TELEGRAM_WELCOME_CUSTOM_EMOJI_ID`
- `CREATIVES_STORAGE_ENABLED`
- `CREATIVES_STORAGE_BUCKET`
- `CREATIVES_STORAGE_REGION`
- `CREATIVES_STORAGE_PUBLIC_BASE_URL`
- `CREATIVES_STORAGE_KEY_PREFIX`
- `APP_IMAGE`

See [deploy/RUNBOOK.md](deploy/RUNBOOK.md) and [deploy/README.md](deploy/README.md) for the canonical deployment contract and full variable matrix.

## API Documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI spec: `http://localhost:8080/v3/api-docs`
- OpenAPI YAML: `http://localhost:8080/v3/api-docs.yaml`
- Generate frontend types from spec: `cd advert-market-frontend && npm run api:types`

## Configuration Properties

Generated Spring configuration properties docs are the canonical source for app-level property names:

- `docs/properties/advert-market-app.md`
- `docs/properties/advert-market-communication.md`
- `docs/properties/advert-market-deal.md`
- `docs/properties/advert-market-financial.md`
- `docs/properties/advert-market-identity.md`
- `docs/properties/advert-market-marketplace.md`
- `docs/properties/advert-market-shared.md`

Regenerate after changing `@ConfigurationProperties` records:

```bash
./gradlew generatePropertyDocs
```

## Known Limitations

- **Single bot instance** — no webhook sharding (MVP)
- **TON testnet only** — mainnet requires security audit
- **All workers in one JVM** — separation planned for scaled version
- **ParadeDB required** — full-text search uses ParadeDB extensions, not vanilla PostgreSQL
- **No PII Vault** — planned for production compliance
- **No horizontal scaling** — single Kafka consumer group per topic

## Documentation

Detailed documentation lives in `.memory-bank/`:

| Topic | Path |
|-------|------|
| Product overview & requirements | [01-product-overview.md](.memory-bank/01-product-overview.md) |
| Actors & personas | [02-actors-and-personas.md](.memory-bank/02-actors-and-personas.md) |
| Feature specifications | [03-feature-specs/](.memory-bank/03-feature-specs/) |
| Architecture (C4 diagrams) | [04-architecture/](.memory-bank/04-architecture/) |
| Patterns & decisions (ADR) | [05-patterns-and-decisions/](.memory-bank/05-patterns-and-decisions/) |
| Deal state machine | [06-deal-state-machine.md](.memory-bank/06-deal-state-machine.md) |
| Financial system | [07-financial-system/](.memory-bank/07-financial-system/) |
| Security & compliance | [10-security-and-compliance.md](.memory-bank/10-security-and-compliance.md) |
| Implementation specs (45 specs) | [14-implementation-specs/](.memory-bank/14-implementation-specs/) |
| Frontend pages (28 routes) | [15-frontend-pages/](.memory-bank/15-frontend-pages/) |
| Java conventions | [16-java-conventions.md](.memory-bank/16-java-conventions.md) |
| TypeScript conventions | [17-typescript-conventions.md](.memory-bank/17-typescript-conventions.md) |
| Deployment runbook | [deploy/RUNBOOK.md](deploy/RUNBOOK.md) |

## License

TBD
