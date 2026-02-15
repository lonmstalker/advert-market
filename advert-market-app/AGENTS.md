# App — Agent Instructions

Spring Boot application module: configuration, wiring, HTTP filters, Kafka listeners, global error handling. NO business logic here.

## Package Structure

| Package | Purpose |
|---------|---------|
| `config/` | Spring configuration: Jackson, Security, OpenAPI, CORS, Kafka (client, topics, serialization, consumers, producers), SharedInfrastructure, Identity, Deploy, ABAC stubs |
| `error/` | `GlobalExceptionHandler` (RFC 9457 ProblemDetail), `SecurityExceptionHandler` |
| `filter/` | `CorrelationIdFilter`, `InternalApiKeyFilter`, `InternalServiceAuthentication` |
| `listener/` | Kafka: `FinancialEventListener`, `DeliveryEventListener`, `ReconciliationResultListener`, `KafkaMdcInterceptor` |
| `outbox/` | `JooqOutboxRepository`, `KafkaOutboxPublisher`, `OutboxConfig` |
| `internal/` | `WorkerCallbackController`, `WorkerCallbackResponse` — internal API for worker callbacks |
| `admin/` | `CanaryAdminController` — canary rollout management |

## Key Integrations

- **Security**: JWT + Telegram initData HMAC-SHA256, ABAC authorization
- **Kafka**: KRaft mode, EventEnvelope serialization, MDC propagation via interceptor
- **Outbox**: jOOQ-based repository + Kafka publisher, polled by OutboxPoller
- **Canary**: feature flag routing via DeployProperties + UserBucket
- **Internal API**: key-authenticated endpoints for TON worker callbacks
- **OpenAPI**: springdoc-openapi 3.0.1, code-first

## Rules

- NO business logic — only configuration, wiring, and cross-cutting concerns
- `@ConfigurationProperties` records — NEVER `@Value`
- Error handling: RFC 9457 ProblemDetail via `GlobalExceptionHandler`
- `*Config` classes wire non-`@Component` beans from other modules (e.g., `IdentityConfig`)
- Kafka listeners delegate to domain services — no processing logic in listeners
- `@PropertyGroupDoc` / `@PropertyDoc` on all `@ConfigurationProperties` records
