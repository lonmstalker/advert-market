# Tech Stack

## Overview

Technology choices are driven by the MVP constraints: fast development, Telegram-native experience, TON blockchain integration, and financial system reliability.

## Frontend

| Technology | Version | Rationale |
|-----------|---------|-----------|
| **React** | 19.2 | Modern component model, large ecosystem, Telegram Mini App SDK support |
| **Vite** | 7 | Fast builds, HMR, Environment API, optimized for SPA deployment |
| **TypeScript** | 5.7 | Type safety for complex deal state management |
| **Telegram Mini App SDK** | Latest | Native WebApp integration: initData, MainButton, BackButton, haptics |

### Deployment

- Static SPA served via **nginx / CDN**
- No SSR needed — all data fetched via REST API
- Telegram WebApp context provides user identity

## Backend

| Technology | Version | Rationale |
|-----------|---------|-----------|
| **Java** | 25 | Virtual threads, records, sealed classes, pattern matching, --enable-preview |
| **Spring Boot** | 4.0.2 | Production-ready framework, large ecosystem, virtual threads support |
| **Spring Security** | — | Auth middleware for Telegram HMAC validation |
| **Spring Kafka** | — | Kafka producer/consumer integration |
| **jOOQ** | — | Type-safe SQL, ShardedDslContextProvider abstraction |
| **Liquibase** | — | Database migration management (YAML changelogs) |

### Why Java 25?

- Virtual threads (Project Loom) for scalable I/O (TON polling, webhook handling)
- Records for immutable domain models (DTOs, events)
- Sealed classes/interfaces for deal state machine
- Pattern matching for cleaner business logic
- `--enable-preview` for latest language features

## Data Stores

| Technology | Version | Rationale |
|-----------|---------|-----------|
| **PostgreSQL** | 18 | ACID transactions, async I/O, partitioning, JSONB, mature ecosystem |
| **Redis** | 8.4 | Sub-millisecond reads for balance cache, distributed locks, canary config |
| **Apache Kafka** | 4.1.1 | Event streaming, KRaft mode (no ZooKeeper), partition ordering |

### Why PostgreSQL?

- ACID transactions for financial double-entry writes
- Table partitioning for time-series data (events, checks)
- JSONB for flexible schemas (creative briefs, rights)
- `FOR UPDATE SKIP LOCKED` for outbox polling
- Mature tooling for backups, replication, monitoring

### Why Kafka over simpler queues?

- Partition-ordered delivery (per `deal_id`) for financial operations
- Consumer groups for independent worker scaling
- Message replay capability for debugging
- Schema Registry for contract versioning

## Blockchain

| Technology | Purpose |
|-----------|---------|
| **TON Center API** | HTTP API for TON blockchain interaction |
| **ton4j** (1.3.2) | Java SDK for address generation, transaction building |

## Build & Code Quality

| Technology | Version | Purpose |
|-----------|---------|---------|
| **Gradle** | 9.3.1 (Groovy DSL) | Build system, 18 subprojects |
| **jOOQ** | 3.20.11 | Type-safe SQL generation |
| **Liquibase** | — | Database migration management |
| **Lombok** | 1.18.40 | Boilerplate reduction (constructors, loggers, builders) |
| **MapStruct** | 1.6.3 | Type-safe DTO mapping |
| **Checker Framework** | 3.53.1 | @Nullable/@NonNull annotations |
| **Checkstyle** | 13.2.0 | Google Java Style (4-space indent) |
| **SpotBugs** | 6.4.8 | Static analysis (MAX effort, MEDIUM reportLevel) |
| **Testcontainers** | 2.0.3 | Integration testing (PostgreSQL, Kafka, Redis) |
| **ArchUnit** | 1.4.1 | Architecture rule enforcement |
| **springdoc-openapi** | 3.0.1 | Code-first OpenAPI spec export |

## Infrastructure

| Technology | Purpose |
|-----------|---------|
| **Docker** (29.2.1) | Containerization for all services |
| **nginx** | Reverse proxy, TLS termination, blue-green switching |
| **Debezium** (Scaled) | CDC for transactional outbox |
| **Confluent Schema Registry** (Scaled) | Avro/JSON Schema versioning |

## Monitoring & Observability

| Technology | Purpose |
|-----------|---------|
| **Micrometer** | Metrics collection (Spring Boot integration) |
| **Prometheus** | Metrics storage and alerting |
| **Grafana** | Dashboards and visualization |
| **Structured logging** | JSON logs for aggregation |

## Security

| Technology | Purpose |
|-----------|---------|
| **Telegram initData HMAC** | User authentication via Telegram WebApp |
| **AES-256-GCM** | Field-level encryption for PII |
| **Redis SET NX EX** | Distributed lock primitive |

## Related Documents

- [Deployment](./09-deployment.md) — how the stack is deployed
- [Backend API Components](./04-architecture/03-backend-api-components.md) — service details
- [Security & Compliance](./10-security-and-compliance.md)
