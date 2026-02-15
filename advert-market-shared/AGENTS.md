# Shared — Agent Instructions

Cross-cutting domain primitives, value objects, events, and infrastructure ports shared across all modules.

## Package Structure

| Package | Purpose |
|---------|---------|
| `model/` | Value objects: `Money`, `UserId`, `ChannelId`, `DealId`, `AccountId`, `TonAddress`, enums (`ActorType`, `AccountType`, `EntryType`, `DealStatus`) |
| `event/` | `EventEnvelope`, `DomainEvent`, `EventTypeRegistry`, Kafka constants (`TopicNames`, `ConsumerGroups`, `EventTypes`), serializers |
| `exception/` | `DomainException` (base), `EntityNotFoundException`, `InsufficientBalanceException`, `InvalidStateTransitionException` |
| `outbox/` | Transactional Outbox: `OutboxEntry`, `OutboxRepository`, `OutboxPublisher`, `OutboxPoller` |
| `audit/` | `AuditEvent`, `AuditPort`, `AuditEntry`, `AuditActorType` |
| `json/` | `JsonFacade` (wraps ObjectMapper), `JsonException` |
| `lock/` | `DistributedLockPort`, `RedisDistributedLock` |
| `metric/` | `MetricsFacade`, `MetricNames` (45+ metrics with @Fenum) |
| `security/` | `PrincipalAuthentication`, `SecurityContextUtil` |
| `pagination/` | `CursorPage`, `CursorCodec` |
| `financial/` | `CommissionCalculator`, `CommissionResult` |
| `deploy/` | `UserBucket`, `CanaryProperties`, `DeployProperties` |
| `i18n/` | `LocalizationService` |
| `error/` | `ErrorCode` (109 codes with HTTP status and i18n key) |
| `logging/` | `MdcKeys` |
| `util/` | `IdempotencyKey` |

## Key Concepts

- **Money** — immutable record, stores nanoTON as `long` (1 TON = 1_000_000_000 nanoTON). NEVER use float/double
- **EventEnvelope** — wraps DomainEvent with metadata (eventId, type, timestamp, correlationId)
- **Transactional Outbox** — events persisted in DB transaction, polled and published to Kafka asynchronously
- **@Fenum** — Checker Framework annotation for type-safe string constants via `FenumGroup`
- **CursorPage** — cursor-based pagination for all list endpoints

## Rules

- NEVER depend on Spring Boot (only `spring-context` compileOnly for annotations)
- All value objects are immutable Java records
- Money amounts: `long` nanoTON — NEVER float, double, or BigDecimal
- `JsonFacade` wraps `IOException` → `JsonException` — NEVER use raw ObjectMapper
- `DomainException` carries `@Fenum(FenumGroup.ERROR_CODE) String errorCode`
- `MetricNames` constants are `@Fenum(FenumGroup.METRIC_NAME)` — NEVER inline metric strings
- `TopicNames`, `ConsumerGroups`, `EventTypes` use @Fenum — NEVER hardcode Kafka strings
