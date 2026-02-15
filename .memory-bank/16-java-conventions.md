# Java Conventions

> Mandatory reference for all Java code in advert-market. Java 25 + Spring Boot 4.0.2.

## 1. Testing

### Test Layers

| Layer | Tool | Coverage Target |
|---|---|---|
| Domain (commission, state machine, ABAC) | JUnit 5 + AssertJ | 100% branches |
| Service (Ledger, Escrow) | Testcontainers (PG+Kafka+Redis) | 90% line |
| Kafka consumers | Testcontainers Kafka | All consumer groups |
| REST controllers | `@WebMvcTest` + MockMvc | All endpoints |
| Cross-module flows | Full `@SpringBootTest` | Happy path + critical failures |

### Financial Testing (No Mocking)

The following services MUST be tested with Testcontainers only — mocking is FORBIDDEN:
- `LedgerService`, `EscrowService`, `CommissionService`
- `BalanceProjection`, `ReconciliationService`, `TonPaymentGateway`

Enforced by ArchUnit rule.

### State Machine Testing

100% coverage required. Parameterized test for EVERY `(from, to, actor)` pair.

Naming convention:
```
transition_{FROM}_{TO}_{by_ACTOR}_{shouldSucceed|shouldReject}
```

### Property-Based Testing

`CommissionService.calculate()`: 10,000+ random inputs.
Invariants:
- `commission + payout == amount`
- Both values >= 0
- No overflow

### Double-Entry Invariant

`@AfterEach` in every financial integration test:
```java
assertThat(sumDebits()).isEqualTo(sumCredits());
```

### Testcontainers Setup

Single `IntegrationTestBase` with `static` containers. `@BeforeEach` truncates tables — never restart containers.

### ArchUnit Rules (8 Mandatory)

| ID | Rule |
|---|---|
| ARCH-01 | Controllers must NOT access repositories directly |
| ARCH-02 | Financial module must NOT depend on deal/marketplace |
| ARCH-03 | Events and ledger entries must have no setters |
| ARCH-04 | Financial packages must NOT use float/double/BigDecimal for amounts |
| ARCH-05 | Cross-context communication only through port interfaces |
| ARCH-06 | `@Transactional` must NOT be on controllers |
| ARCH-07 | Kafka listeners must NOT call `KafkaTemplate.send()` synchronously |
| ARCH-08 | Request handler methods must NOT have more than 2 `@RequestParam` parameters (use request params object) |

## 2. Performance

### Virtual Threads

Default executor via `spring.threads.virtual.enabled: true`.

**FORBIDDEN**: `synchronized` blocks in service classes — use `ReentrantLock` only. Enforced by ArchUnit.

### jOOQ

- Batch INSERT for 2+ records
- `SELECT *` is FORBIDDEN — always list columns explicitly
- Every query MUST have `.limit()`
- HikariCP: max 20, min 5, timeout 5s

### Pagination

ONLY keyset cursor pagination. OFFSET is FORBIDDEN, except for `sort=RELEVANCE` + full-text `query` with ParadeDB score,
where correct keyset pagination is not possible yet (temporary workaround documented in
`14-implementation-specs/29-channel-search-impl.md`).

### Redis

- `executePipelined()` for 2+ keys
- JSON serialization (not Java serialization)
- Pool: max 20, min 5

### Kafka

- `acks=all`, `enable.idempotence=true`
- `batch.size=16384`, `linger.ms=5`
- Manual offset commit

### N+1 Prevention

Individual queries in loops are FORBIDDEN. Use JOINs or batch fetches.

### GC Configuration

```
-XX:+UseZGC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom -Xmx512m -Xms512m
```

## 3. Code Quality

### Records and Sealed Types

**Records mandatory for**: DTOs, Kafka events, value objects, search criteria.

**Sealed interfaces mandatory for**: deal states, worker events, transition results.

### Null Safety

- `@Nullable` on nullable params/returns
- `Optional` only as return type (never as field or parameter)
- `fetchOptional()` instead of `fetchOne()` in jOOQ

### Error Handling

- Sealed `Result` type for state machine transitions
- Domain exceptions with `ErrorCode` enum
- `@ControllerAdvice` + RFC 7807 Problem Details
- `catch(Exception)` is FORBIDDEN in business logic

### Immutability

- All DTOs are records
- Collections: `List.of()`, `Map.of()`, `Set.of()`
- `ledger_entries` table: only `insert()` — no `update()` or `delete()`

### Structured Logging

- JSON format (Logback + Logstash encoder)
- MDC fields: `correlation_id`, `user_id`, `deal_id`
- Parameterized messages only (`log.info("Deal {} transitioned to {}", dealId, status)`)
- PII logging is FORBIDDEN

### Validation

- **Bean Validation** on DTOs: format constraints (`@NotNull`, `@Size`, `@Pattern`)
- **Domain Validation** in services: business rules (balance sufficient, state allows transition)

### Configuration

- `@ConfigurationProperties` with records
- `@Value("${...}")` is forbidden for application configuration binding
- Secrets only through environment variables (never in config files)

## 4. Payment Guarantees

### Core Invariant

```
SUM(debits) == SUM(credits)
```

Per `tx_ref` and globally. Reconciliation runs every 15 minutes.

### Amount Representation

- ONLY `long` nanoTON (1 TON = 1,000,000,000 nanoTON)
- **ABSOLUTE PROHIBITION**: `float`, `double`, `BigDecimal` for monetary amounts
- Overflow protection: use `BigInteger` for intermediate calculations only

### Idempotency

| Operation | Mechanism |
|---|---|
| REST mutations | `Idempotency-Key` header |
| State machine | `WHERE status = :expected AND version = :expected` |
| Financial ops | Redis `SET NX` TTL 24h |
| Kafka consumers | PK constraint + state guard + Redis lock |

### Distributed Locks

Redis `SET NX EX` + fencing token. Mandatory for all financial side-effects.

Lock key pattern: `lock:{operation}:{entityId}`

### Audit Trail

Every financial operation writes to `audit_log`:
- Fields: `actor`, `operation`, `tx_ref`, `payload` (JSONB)
- Append-only, 7 years retention

### Reconciliation (3-Way)

| Check | Severity |
|---|---|
| `SUM(debits) == SUM(credits)` | CRITICAL alert |
| Ledger vs TON blockchain totals | CRITICAL |
| Escrow balances vs deal amounts | WARNING |
| Per-deal: `0 <= escrow balance <= deal.amount` | CRITICAL |

### Financial PR Checklist

Every PR touching financial code must verify:

- [ ] Amounts use `long` nanoTON
- [ ] `commission + payout == amount`
- [ ] Distributed lock + fencing token
- [ ] Idempotency key
- [ ] Audit log entry
- [ ] `SUM(debits) == SUM(credits)` per tx_ref
- [ ] No UPDATE/DELETE on ledger_entries
- [ ] Integration test with Testcontainers
- [ ] `@AfterEach` double-entry invariant check
