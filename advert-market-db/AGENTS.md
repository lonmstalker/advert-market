# Database — Agent Instructions

jOOQ code generation and Liquibase database migrations.

## Structure

- `src/main/resources/db/changelog/` — Liquibase master changelog + SQL migrations (001–014)
- `src/main/generated/` — generated jOOQ classes (gitignored)

## jOOQ Code Generation

- **Source of truth**: Liquibase migrations (no separate DDL file)
- **Method**: Testcontainers ParadeDB → Liquibase → `org.jooq.meta.postgres.PostgresDatabase` codegen
- **Plugin**: `org.jooq.jooq-codegen-gradle` (official jOOQ Gradle plugin)
- **Task**: `./gradlew :advert-market-db:jooqCodegen`
- **External DB** (CI/prod): pass `-PjooqJdbcUrl=... -PjooqUsername=... -PjooqPassword=...`
- Package: `com.advertmarket.db.generated`
- JSONB columns generate as `org.jooq.JSONB` (not `JSON`)

## Rules

- NEVER edit generated jOOQ code — it is regenerated on build
- New migrations: sequential numbering (e.g., `015-*.sql`)
- After schema changes: add Liquibase migration, then `./gradlew :advert-market-db:jooqCodegen`
- Partitioned tables excluded from codegen: `.*_p_\\d{4}_\\d{2}`
- PostGIS/ParadeDB system tables excluded from codegen
