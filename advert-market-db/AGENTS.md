# Database — Agent Instructions

jOOQ code generation and Liquibase database migrations.

## Structure

- `src/main/resources/db/changelog/` — Liquibase master changelog + SQL migrations (001–013)
- `src/main/resources/db/jooq-codegen/schema.sql` — DDL for jOOQ code generation (DDL-based, no live DB)
- `src/main/generated/` — generated jOOQ classes (gitignored)

## Rules

- NEVER edit generated jOOQ code — it is regenerated on build
- New migrations: sequential numbering (e.g., `014-*.sql`)
- After schema changes: update `schema.sql` AND add Liquibase migration, then `./gradlew generateJooq`
- jOOQ codegen: DDL-based, PostgreSQL dialect, package `com.advertmarket.db.generated`
- Partitioned tables excluded from codegen: `.*_p_\\d{4}_\\d{2}`
