# Parent Instructions
- Parent: `/Users/nikitakocnev/.codex/AGENTS.md`
- This file contains only module-local deltas.

# Integration Tests — Agent Instructions

## Architecture

### SharedContainers Singleton Pattern

`SharedContainers` starts PostgreSQL and Redis once in a `static {}` block.
All test classes share these two containers for the entire JVM lifecycle.
NEVER create `@Container` fields — always go through `SharedContainers`.

- PG: `SharedContainers.POSTGRES` (paradedb/paradedb:latest)
- Redis: `SharedContainers.REDIS` (redis:8.4-alpine)

### Test Categories

| Category | DB Init | Redis Init | Spring Context |
|----------|---------|------------|----------------|
| PG-only | `DatabaseSupport.ensureMigrated()` + `dsl()` | — | No |
| Redis-only | — | `RedisSupport.redisTemplate()` | No |
| PG+Redis non-Spring | Both above | Both above | No |
| Spring Boot HTTP | `DatabaseSupport.ensureMigrated()` in `@BeforeAll` | Via `ContainerProperties.registerAll()` | Yes |

### Support Classes

| Class | Purpose |
|-------|---------|
| `SharedContainers` | Singleton PG + Redis containers |
| `DatabaseSupport` | Thread-safe Liquibase migration, DSLContext factory, table cleanup |
| `RedisSupport` | Singleton LettuceConnectionFactory, StringRedisTemplate, flushAll |
| `TestDataFactory` | Static helpers for common test data (users, channels, pricing, JWT) |
| `ContainerProperties` | `@DynamicPropertySource` delegation for Spring Boot tests |
| `TestExceptionHandler` | Shared `@RestControllerAdvice` with ProblemDetail error handling |
| `MarketplaceTestConfig` | Shared Spring config: DSLContext, JWT, security, metrics, i18n |

### Table Cleanup Order (FK dependencies)

`PRICING_RULE_POST_TYPES` -> `CHANNEL_PRICING_RULES` -> `CHANNEL_CATEGORIES` -> `CHANNEL_MEMBERSHIPS` -> `CHANNELS` -> `NOTIFICATION_OUTBOX` -> `USERS`

## Test Data Factory

- `TestDataFactory.upsertUser(dsl, userId)` — insert or update user
- `TestDataFactory.insertChannelWithOwner(dsl, channelId, ownerId)` — channel + membership
- `TestDataFactory.insertPricingRule(dsl, channelId, name, postType, priceNano, sortOrder)` — pricing rule
- `TestDataFactory.jwt(jwtTokenProvider, userId)` — generate JWT token for tests

## Patterns

### New PG-only Test

```java
@DisplayName("MyRepo — PostgreSQL integration")
class MyRepoIntegrationTest {

    private static DSLContext dsl;

    @BeforeAll
    static void initDatabase() {
        DatabaseSupport.ensureMigrated();
        dsl = DatabaseSupport.dsl();
    }

    @BeforeEach
    void setUp() {
        DatabaseSupport.cleanAllTables(dsl);
    }
}
```

### New Spring Boot HTTP Test

```java
@SpringBootTest(
        classes = MyHttpTest.TestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("My HTTP — end-to-end integration")
class MyHttpTest {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        ContainerProperties.registerAll(r);
    }

    @BeforeAll
    static void initDatabase() {
        DatabaseSupport.ensureMigrated();
    }

    @Configuration
    @EnableAutoConfiguration
    @Import(MarketplaceTestConfig.class)
    static class TestConfig {
        // only domain-specific beans here
    }
}
```

### MarketplaceTestConfig Usage

Use `@Import(MarketplaceTestConfig.class)` when your test needs:
- DSLContext, JWT auth, SecurityFilterChain
- MetricsFacade, JsonFacade, LocalizationService
- TokenBlacklist, LoginRateLimiter, UserBlockCheck
- TestExceptionHandler (ProblemDetail error handling)

Override SecurityFilterChain in your own TestConfig only if you need different auth rules (e.g., `permitAll` for login endpoint). Add `spring.main.allow-bean-definition-overriding=true` in that case.

## Package Structure

```
integration/
  support/           — SharedContainers, DatabaseSupport, RedisSupport, TestDataFactory, ContainerProperties, TestExceptionHandler
  identity/          — User repository, auth workflow, token blacklist, rate limiter tests
  marketplace/       — Channel, pricing, search, team tests
    config/          — MarketplaceTestConfig
  shared/            — Outbox, distributed lock tests
  deploy/            — Canary router, update deduplicator tests
```

## Rules

- `@DisplayName` required on all test classes and `@Test` methods (English)
- Use `bd` for task tracking
- NEVER use `@Container` or `@Testcontainers` — always `SharedContainers`
- Cleanup in `@BeforeEach`, not `@AfterEach` — ensures clean state for each test
- Redis cleanup: `RedisSupport.flushAll()` before each test when using Redis
