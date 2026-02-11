# Partition Automation

## Overview

Three tables use monthly RANGE partitioning: `ledger_entries`, `deal_events`, `posting_checks`. This spec defines automated partition creation and archival to prevent insert failures and manage storage.

## Partitioned Tables

| Table | Partition Key | Strategy |
|-------|--------------|----------|
| `ledger_entries` | `created_at` | RANGE (monthly) |
| `deal_events` | `created_at` | RANGE (monthly) |
| `posting_checks` | `checked_at` | RANGE (monthly) |

## Partition Naming Convention

```
{table}_y{yyyy}m{mm}
```

Examples: `ledger_entries_y2025m01`, `deal_events_y2025m02`, `posting_checks_y2025m03`

## Auto-Creation Strategy

### Spring @Scheduled Job

Runs daily at 00:00 UTC. Creates partitions for next 2 months if they don't exist.

### Partition Creation SQL

```sql
CREATE TABLE IF NOT EXISTS {table}_y{yyyy}m{mm}
    PARTITION OF {table}
    FOR VALUES FROM ('{yyyy}-{mm}-01 00:00:00+00')
    TO ('{next_yyyy}-{next_mm}-01 00:00:00+00');
```

### Creation Logic

1. For each partitioned table:
   a. Check if partition for current month exists
   b. Check if partition for next month exists
   c. Check if partition for month+2 exists
   d. Create any missing partitions
2. Log each creation: INFO level
3. If creation fails: CRITICAL alert (inserts will fail when current partition fills)

## Alerting

| Alert | Condition | Severity |
|-------|-----------|----------|
| Partition missing (next month) | `partition_next_month_exists == 0` | CRITICAL |
| Partition missing (month+2) | `partition_month_plus_2_exists == 0` | WARNING |
| Partition creation failed | SQL error during CREATE | CRITICAL |

### Health Check Query

```sql
SELECT
    parent.relname AS table_name,
    COUNT(child.relname) AS partition_count,
    MAX(pg_get_expr(child.relpartbound, child.oid)) AS latest_partition_bound
FROM pg_inherits
JOIN pg_class parent ON pg_inherits.inhparent = parent.oid
JOIN pg_class child ON pg_inherits.inhrelid = child.oid
WHERE parent.relname IN ('ledger_entries', 'deal_events', 'posting_checks')
GROUP BY parent.relname;
```

## Archival Strategy

### Detach Old Partitions

```sql
ALTER TABLE {table} DETACH PARTITION {table}_y{yyyy}m{mm};
```

### Retention Periods

| Table | Active Retention | Archive Action |
|-------|:----------------:|:-----------------:|
| `ledger_entries` | Indefinite | Never drop (legal requirement) |
| `deal_events` | 12 months | Cold storage after detach |
| `posting_checks` | 12 months | Drop after 24 months |

### Archival Flow

1. Detach partition older than retention period
2. Export to compressed CSV (`COPY ... TO ... WITH CSV HEADER`)
3. Upload to object storage (future: S3/R2)
4. For `posting_checks`: DROP detached partition after 24 months
5. For `ledger_entries`/`deal_events`: keep detached (legal requirement)

## Configuration

```yaml
partition:
  auto-create:
    enabled: true
    lookahead-months: 2
    schedule: "0 0 0 * * *"  # daily at midnight UTC
  archival:
    enabled: false  # MVP: manual archival
    deal-events-retention-months: 12
    posting-checks-retention-months: 12
    posting-checks-drop-after-months: 24
```

## Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `partition.created` | Counter | Partitions auto-created |
| `partition.creation.failed` | Counter | Failed partition creations |
| `partition.next.exists` | Gauge | 1 if next month partition exists, 0 if missing |
| `partition.count` | Gauge | Total partition count per table |

## MVP vs Scaled

| Aspect | MVP | Scaled |
|--------|-----|--------|
| Auto-creation | @Scheduled in app | pg_partman extension or cron |
| Archival | Manual (disabled) | Automated with object storage |
| Monitoring | Application metrics | pg_partman + external |

## Related Documents

- [Data Stores](../04-architecture/05-data-stores.md)
- [DDL Migrations](./05-ddl-migrations.md)
- [PostgreSQL Sharding](./24-postgresql-sharding.md)