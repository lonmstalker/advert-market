# Integration Tests — Agent Instructions

## Containers

Always use `SharedContainers` singleton — NEVER `@Container` or `@Testcontainers`.

- PG: `SharedContainers.POSTGRES` (paradedb/paradedb:latest)
- Redis: `SharedContainers.REDIS` (redis:8.4-alpine)

## Database

- Migration: `DatabaseSupport.ensureMigrated()` in `@BeforeAll`
- DSLContext: `DatabaseSupport.dsl()` (non-Spring) or `@Autowired DSLContext` (Spring)
- Cleanup: `DatabaseSupport.cleanAllTables(dsl)` / `cleanMarketplaceTables(dsl)` / `cleanUserTables(dsl)`

## Redis

- Template: `RedisSupport.redisTemplate()`
- Cleanup: `RedisSupport.flushAll()`

## Test Data

- Users: `TestDataFactory.upsertUser(dsl, userId)`
- Channels: `TestDataFactory.insertChannelWithOwner(dsl, channelId, ownerId)`
- Pricing: `TestDataFactory.insertPricingRule(dsl, channelId, name, postType, priceNano, sortOrder)`
- JWT: `TestDataFactory.jwt(jwtTokenProvider, userId)`

## Spring Boot HTTP Tests

- `@Import(MarketplaceTestConfig.class)` for shared infrastructure
- `@DynamicPropertySource` -> `ContainerProperties.registerAll(registry)`
- Error handling via shared `TestExceptionHandler` (included in MarketplaceTestConfig)
- WebTestClient, ProblemDetail assertions

## Rules

- `@DisplayName` required on all classes and `@Test` methods
- Use `bd` for task tracking
