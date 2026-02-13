# Testing Strategy

## Overview

Многоуровневая стратегия тестирования для Java 25 + Spring Boot 4.0.2. Охватывает unit, integration, contract и end-to-end уровни с фокусом на финансовую точность, state machine корректность и Kafka consumer идемпотентность.

---

## Test Stack

| Tool | Purpose |
|------|---------|
| JUnit 5 | Test framework |
| AssertJ | Fluent assertions |
| Mockito | Unit test mocks |
| Testcontainers | PostgreSQL, Redis, Kafka для integration tests |
| Spring Boot Test | `@SpringBootTest`, `@DataJpaTest`, `@WebMvcTest` |
| Awaitility | Async assertion (Kafka consumers, scheduled tasks) |
| ArchUnit | Архитектурные constraints |
| WireMock | HTTP mock (TON Center API, Telegram Bot API) |

### Gradle

```groovy
dependencies {
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'org.testcontainers:kafka'
    testImplementation 'com.redis:testcontainers-redis:2.2.0'
    testImplementation 'org.awaitility:awaitility:4.2.0'
    testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'
    testImplementation 'org.wiremock:wiremock-standalone:3.9.0'
}
```

---

## Test Categories

### 1. Unit Tests

**Scope**: Чистая бизнес-логика без Spring context.

| Target | Examples | Coverage Goal |
|--------|----------|---------------|
| Commission calculation | Tiered rate lookup, rounding, overflow guard | 100% branches |
| State machine transitions | Valid/invalid transitions, actor checks | All 17 states x transitions |
| Amount validation | Exact, overpayment, underpayment, multiple deposits | All cases |
| Cursor codec | Encode/decode, malformed input | Edge cases |
| ABAC permission checks | Owner/admin/advertiser/operator decisions | All actor-resource pairs |

**Naming convention**: `{Method}_{Scenario}_{ExpectedResult}`

### 2. Integration Tests (Testcontainers)

**Scope**: Spring context + real DB/Redis/Kafka.

#### Testcontainers Setup

```java
@SpringBootTest
@Testcontainers
abstract class IntegrationTestBase {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18")
            .withDatabaseName("advertmarket_test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.7.0"));

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:8")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }
}
```

#### Integration Test Matrix

| Test Suite | What It Validates |
|-----------|-------------------|
| Deal Lifecycle | Full deal flow: DRAFT -> COMPLETED_RELEASED (happy path) |
| Escrow Funding | Deposit detection -> ledger entry creation -> balance update |
| Payout Flow | DELIVERY_VERIFIED -> payout TX -> COMPLETED_RELEASED |
| Refund Flow | Funded deal cancellation -> refund TX -> REFUNDED |
| Dispute Flow | Dispute open -> evidence -> resolution (both outcomes) |
| Outbox Polling | Outbox write -> Kafka publish -> delivery |
| Auth Flow | initData HMAC -> JWT issue -> token refresh |
| Redis Locks | Concurrent lock acquisition, TTL expiry, re-entry |

### 3. State Machine Test Matrix

**Critical coverage**: 17 states, ~30 valid transitions, ~200+ invalid transitions.

#### Valid Transitions (must succeed)

| # | From | To | Actor | Assertion |
|---|------|----|-------|-----------|
| 1 | DRAFT | OFFER_PENDING | Advertiser | Event emitted, notification sent |
| 2 | DRAFT | CANCELLED | Advertiser | Terminal state |
| 3 | OFFER_PENDING | NEGOTIATING | Owner | Counter-offer stored |
| 4 | OFFER_PENDING | ACCEPTED | Owner | Deposit address generated |
| 5 | OFFER_PENDING | CANCELLED | Owner/Advertiser | Notification to other party |
| 6 | OFFER_PENDING | EXPIRED | System | Timeout worker triggered |
| 7 | NEGOTIATING | ACCEPTED | Either | Terms finalized |
| 8 | NEGOTIATING | CANCELLED | Either | Notification sent |
| 9 | NEGOTIATING | EXPIRED | System | Timeout |
| 10 | ACCEPTED | AWAITING_PAYMENT | System | Address ready |
| 11 | ACCEPTED | CANCELLED | Either | Pre-payment cancel |
| 12 | AWAITING_PAYMENT | FUNDED | System | Ledger entries created |
| 13 | AWAITING_PAYMENT | CANCELLED | Advertiser | No refund needed |
| 14 | AWAITING_PAYMENT | EXPIRED | System | No refund needed |
| 15 | FUNDED | CREATIVE_SUBMITTED | Owner | Draft stored |
| 16 | FUNDED | CANCELLED | Both | Refund triggered |
| 17 | FUNDED | EXPIRED | System | Refund triggered |
| 18 | CREATIVE_SUBMITTED | CREATIVE_APPROVED | Advertiser | Publishing enabled |
| 19 | CREATIVE_SUBMITTED | FUNDED | Advertiser | Revision requested |
| 20 | CREATIVE_SUBMITTED | DISPUTED | Advertiser | Escrow frozen |
| 21 | CREATIVE_APPROVED | SCHEDULED | Owner | Schedule stored |
| 22 | CREATIVE_APPROVED | PUBLISHED | Owner | Post sent |
| 23 | SCHEDULED | PUBLISHED | System | Auto-publish |
| 24 | PUBLISHED | DELIVERY_VERIFYING | System | 24h timer started |
| 25 | DELIVERY_VERIFYING | COMPLETED_RELEASED | System | Payout + commission |
| 26 | DELIVERY_VERIFYING | DISPUTED | System | Post deleted/edited |
| 27 | DISPUTED | COMPLETED_RELEASED | Operator | Release escrow |
| 28 | DISPUTED | REFUNDED | Operator | Refund escrow |

#### Invalid Transition Tests (must throw `InvalidStateTransitionException`)

Parameterized test: для каждого состояния проверить все НЕвалидные переходы.

```java
@ParameterizedTest
@MethodSource("invalidTransitions")
void shouldRejectInvalidTransition(DealStatus from, DealStatus to, ActorType actor) {
    assertThatThrownBy(() -> transitionService.transition(deal, to, actor))
            .isInstanceOf(InvalidStateTransitionException.class);
}
```

#### Actor Authorization Tests

Для каждого перехода проверить: правильный актор — ОК, неправильный актор — `ForbiddenException`.

### 4. Financial Accuracy Tests

**Invariant**: `commission_nano + owner_payout_nano == deal_amount_nano` для ЛЮБЫХ входных данных.

#### Double-Entry Balance Tests

```java
@Test
void ledgerMustAlwaysBalance() {
    // After any deal completion:
    long totalDebits = ledgerRepo.sumByDirection("DEBIT");
    long totalCredits = ledgerRepo.sumByDirection("CREDIT");
    assertThat(totalDebits).isEqualTo(totalCredits);
}
```

#### Commission Rounding Tests

| Amount (nanoTON) | Rate (bp) | Expected Commission | Expected Payout |
|------------------|-----------|--------------------:|----------------:|
| 1_000_000_000 | 1000 | 100_000_000 | 900_000_000 |
| 1_000_000_001 | 1000 | 100_000_000 | 900_000_001 |
| 1 | 1000 | 0 | 1 |
| Long.MAX_VALUE / 10_001 | 5000 | verify no overflow | exact remainder |
| 50_000_000 | 1500 | 7_500_000 | 42_500_000 |

#### Overflow Guard Test

```java
@Test
void commissionCalculationMustNotOverflow() {
    // amount * rate_bp can overflow for large amounts
    // Safe formula: Math.multiplyHigh or BigInteger intermediate
    long largeAmount = 922_000_000_000_000_000L; // ~922 TTON
    int maxRate = 5000;
    assertDoesNotThrow(() -> commissionService.calculate(largeAmount, maxRate));
}
```

### 5. Kafka Consumer Idempotency Tests

| Scenario | Test |
|----------|------|
| Duplicate DEPOSIT_CONFIRMED | Send same tx_hash twice -> only 1 ledger entry |
| Duplicate PAYOUT_COMPLETED | Send same deal_id payout twice -> only 1 payout |
| Out-of-order events | PAYOUT before DEPOSIT -> reject gracefully |
| DLT routing | Poison message -> appears in DLT topic |
| Consumer restart | Kill consumer mid-batch -> no message loss after restart |

### 6. Contract Tests (WireMock)

**TON Center API**:
- Mock `/getTransactions` -> verify deposit detection
- Mock `/sendBoc` -> verify payout submission
- Mock 429 response -> verify rate limit backoff
- Mock 500 response -> verify retry

**Telegram Bot API**:
- Mock `sendMessage` -> verify notification delivery
- Mock 403 (blocked) -> verify user marked unreachable
- Mock 429 -> verify `retry_after` honored

---

## Test Configuration

```yaml
# application-test.yml
spring:
  jpa:
    hibernate:
      ddl-auto: none  # Liquibase manages schema
  liquibase:
    enabled: true

ton:
  api:
    url: http://localhost:${wiremock.server.port}/api/v2/
    key: test-key

telegram:
  bot:
    token: 123456:TEST_TOKEN
```

---

## Test Execution Strategy

| Phase | Scope | Trigger | Target Time |
|-------|-------|---------|-------------|
| Unit | Business logic | Every commit | < 30s |
| Integration | DB + Kafka + Redis | PR merge | < 3 min |
| Contract | External API mocks | PR merge | < 1 min |
| Architecture | ArchUnit rules | Every commit | < 10s |

### ArchUnit Rules

| Rule | Description |
|------|-------------|
| Layer isolation | Controllers must not access repositories directly |
| Immutable events | `deal_events` entity has no setter methods |
| No floats in financial | `*Service` classes must not use `float`/`double`/`BigDecimal` for amounts |
| Naming convention | `*Controller`, `*Service`, `*Repository` suffixes |

---

## CI Pipeline Integration

```
git push -> Unit tests -> Build -> Integration tests -> Contract tests -> Deploy
                                        |
                                    Testcontainers
                                  (PostgreSQL, Kafka, Redis)
```

---

## Related Documents

- [Deal State Machine](../06-deal-state-machine.md)
- [Commission & Rounding](./25-commission-rounding-sweep.md)
- [Kafka Consumer Error Handling](./18-kafka-consumer-error-handling.md)
- [TON SDK Integration](./01-ton-sdk-integration.md)
- [Worker Callback Schemas](./10-worker-callback-schemas.md)
