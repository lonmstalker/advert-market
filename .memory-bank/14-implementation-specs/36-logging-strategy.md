# Logging Strategy

## Overview

Structured JSON logging with sensitive data redaction, correlation IDs, and level-based retention. All logs are machine-parseable for aggregation and alerting.

## Logging Framework

- SLF4J + Logback (Spring Boot default)
- JSON encoder: `logstash-logback-encoder` for structured output
- Lombok `@Slf4j` annotation on all classes

## JSON Log Format

```json
{
  "timestamp": "2025-01-15T10:30:00.000Z",
  "level": "INFO",
  "logger": "com.advertmarket.deal.DealTransitionService",
  "message": "Deal state transition",
  "thread": "virtual-thread-42",
  "correlation_id": "a1b2c3d4-...",
  "user_id": 123456789,
  "deal_id": "550e8400-...",
  "trace_id": "abc123...",
  "span_id": "def456..."
}
```

## MDC (Mapped Diagnostic Context)

### Standard MDC Fields

| Field | Source | Lifecycle |
|-------|--------|-----------|
| `correlation_id` | X-Correlation-Id header or UUID.randomUUID() | Per-request |
| `user_id` | JWT claims (after auth) | Per-request |
| `deal_id` | Request path param or event payload | Per-operation |
| `trace_id` | Micrometer Tracing (auto) | Per-request |
| `span_id` | Micrometer Tracing (auto) | Per-span |

### MDC Population

- HTTP requests: Spring `OncePerRequestFilter` sets `correlation_id` and `user_id`
- Kafka consumers: `RecordInterceptor` extracts `correlation_id` from message header
- Scheduled tasks: generate new `correlation_id` at start

## Log Levels

### Category-Specific Levels (Production)

| Logger | Level | Description |
|--------|:-----:|-------------|
| `com.advertmarket` | INFO | Application code |
| `com.advertmarket.financial` | INFO | Financial operations (audit-relevant) |
| `org.springframework` | WARN | Spring framework |
| `org.jooq` | WARN | jOOQ (DEBUG for SQL in dev) |
| `org.apache.kafka` | WARN | Kafka client |
| `io.lettuce` | WARN | Redis client |

## Sensitive Data Redaction

### Never Log

| Data Type | Redaction |
|-----------|-----------|
| TON private keys | Never reaches application code |
| Payout addresses | Masked: `EQ...***...abc` (first 4 + last 3) |
| Bot token | Never log (env variable only) |
| JWT tokens | Masked: `eyJ***` |
| initData | Log only `user_id` and `auth_date` |

Financial amounts: **always log in full** (audit requirement).

### Redaction Implementation

Custom Logback `PatternLayout` with regex-based masking for TON addresses, JWT tokens, webhook secrets.

## Business Event Log Categories

### Financial Events (always INFO)

```
[FINANCIAL] Escrow funded: deal={deal_id}, amount={amount_nano}, tx_hash={tx_hash}
[FINANCIAL] Payout released: deal={deal_id}, owner={owner_id}, amount={amount_nano}
[FINANCIAL] Refund executed: deal={deal_id}, advertiser={advertiser_id}, amount={amount_nano}
[FINANCIAL] Reconciliation: check={check_type}, result={PASS|FAIL}
```

### Deal Events (always INFO)

```
[DEAL] State transition: deal={deal_id}, {from_status} -> {to_status}, actor={actor_id}
[DEAL] Dispute opened: deal={deal_id}, opened_by={user_id}
```

### Security Events (INFO or WARN)

```
[SECURITY] Auth success: user_id={user_id}
[SECURITY] Auth failure: reason={reason}
[SECURITY] ABAC denied: user={user_id}, action={action}, resource={resource}
```

## Retention Policy

| Log Type | Retention | Storage |
|----------|-----------|---------|
| Application logs | 90 days | Local disk -> log aggregator |
| Financial events | Indefinite | Also recorded in `audit_log` table |
| Security events | 90 days | Also in application logs |

## Configuration

```yaml
logging:
  level:
    com.advertmarket: INFO
    org.springframework: WARN
    org.jooq: WARN
    org.apache.kafka: WARN
  file:
    name: /var/log/advertmarket/app.log
    max-size: 100MB
    max-history: 90
    total-size-cap: 10GB
```

## Logback Profiles

- **default/local**: ConsoleAppender with colored pattern
- **prod**: FileAppender with JSON format (logstash-logback-encoder), async appender

## Related Documents

- [Metrics & SLOs](./21-metrics-slos-monitoring.md)
- [Security & Compliance](../10-security-and-compliance.md)
- [Deployment Runbook](./23-deployment-runbook.md)