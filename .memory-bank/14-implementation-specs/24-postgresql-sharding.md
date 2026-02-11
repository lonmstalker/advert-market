# PostgreSQL Sharding & Routing (Scaled)

## Overview

Scaled deployment: 3 PostgreSQL shards for horizontal scaling. This spec is for **future Scaled deployment**, not MVP.

---

## Shard Layout

| Shard | Data | Shard Key |
|-------|------|-----------|
| Shard 0 (Core) | users, channels, channel_memberships | user_id / channel_id |
| Shard 1 (Financial) | ledger_entries, account_balances, ton_transactions | deal_id hash |
| Shard 2 (Deals) | deals, deal_events, disputes, dispute_evidence, posting_checks | deal_id hash |

### Shared Tables (replicated to all shards)

- `notification_outbox` -- local to each shard, polled independently
- `pii_store` -- on Core shard only
- `audit_log` -- on Core shard only

---

## Routing Algorithm

### Consistent Hashing

```
shard_index = Math.abs(shardKey.hashCode()) % SHARD_COUNT
```

For deal-related operations:
```
shard = (deal_id.hashCode() & 0x7FFFFFFF) % 2  // shards 1 or 2
if (shard == 0) shard = 1  // financial
if (shard == 1) shard = 2  // deals
```

Simplified for MVP->Scaled migration:
- Financial queries -> Shard 1
- Deal queries -> Shard 2
- User/channel queries -> Shard 0

---

## ShardedDslContextProvider (jOOQ Abstraction)

Все взаимодействие с БД через **jOOQ**. Абстракция шардирования встроена в провайдер `DSLContext`.

### Interface

```java
public interface ShardedDslContextProvider {
    /** Shard 0: users, channels, channel_memberships, pii_store, audit_log */
    DSLContext core();

    /** Shard 1: ledger_entries, account_balances, ton_transactions, commission_tiers */
    DSLContext financial();

    /** Shard 2: deals, deal_events, disputes, dispute_evidence, posting_checks,
     *  channel_pricing_rules, notification_outbox */
    DSLContext deals();

    /** Route to appropriate shard based on deal_id (financial or deals) */
    DSLContext forDeal(UUID dealId);

    /** Execute in transaction across single shard */
    <T> T transactional(DSLContext ctx, TransactionalCallable<T> callable);
}
```

### MVP Implementation

Все методы возвращают один и тот же `DSLContext` (единая БД):

```java
@Component
public class SingleDatabaseDslContextProvider implements ShardedDslContextProvider {
    private final DSLContext dsl;

    public SingleDatabaseDslContextProvider(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override public DSLContext core() { return dsl; }
    @Override public DSLContext financial() { return dsl; }
    @Override public DSLContext deals() { return dsl; }
    @Override public DSLContext forDeal(UUID dealId) { return dsl; }

    @Override
    public <T> T transactional(DSLContext ctx, TransactionalCallable<T> callable) {
        return ctx.transactionResult(callable);
    }
}
```

### Scaled Implementation

Каждый метод возвращает `DSLContext` подключённый к соответствующему шарду `DataSource`.

### Repository Pattern с ShardedDslContextProvider

Все репозитории получают `DSLContext` через провайдер:

```java
@Repository
public class DealRepository {
    private final ShardedDslContextProvider shards;

    public Optional<DealRecord> findById(UUID id) {
        return shards.deals()
            .selectFrom(DEALS)
            .where(DEALS.ID.eq(id))
            .fetchOptional();
    }
}

@Repository
public class LedgerRepository {
    private final ShardedDslContextProvider shards;

    public void insertEntry(LedgerEntry entry) {
        shards.financial()
            .insertInto(LEDGER_ENTRIES)
            .set(/* ... */)
            .execute();
    }
}
```

---

## Cross-Shard Query Strategy

**Decision**: Avoid cross-shard queries. Design API to minimize joins across shard boundaries.

### Patterns

| Scenario | Strategy |
|----------|----------|
| Deal details + user info | Two queries: deal from Shard 2, user from Shard 0 |
| Ledger + deal info | Two queries: ledger from Shard 1, deal from Shard 2 |
| Reconciliation | Each check queries single shard |
| User's deals list | Query Shard 2 by advertiser_id/owner_id (indexed) |

### Anti-Patterns

- No JOINs across shards
- No distributed transactions across shards
- No foreign keys referencing other shards

---

## Migration Procedure (MVP -> Scaled)

### Phase 1: Preparation

1. Add shard routing interface (returns same DB for all)
2. Update all repositories to use `ShardedDslContextProvider`
3. Verify all queries work with routing abstraction
4. Deploy and test

### Phase 2: Data Split

1. Set up 3 PostgreSQL instances
2. Replicate full database to all 3
3. On each shard: DROP tables not belonging to that shard
4. Update `ShardedDslContextProvider` to route to real shards
5. Test with read traffic

### Phase 3: Cutover

1. Maintenance window (pause financial operations)
2. Final data sync
3. Switch routing to sharded mode
4. Verify reconciliation passes
5. Resume operations

---

## Shard Rebalancing

Not needed for 3-shard layout. If growth requires more shards:
1. Use consistent hashing with virtual nodes
2. Add shard, migrate data for affected hash range
3. Update routing table atomically

---

## Configuration

```yaml
# Scaled deployment
datasource:
  core:
    url: jdbc:postgresql://shard0:5432/advertmarket
    username: ${DB_USER}
    password: ${DB_PASSWORD_SHARD0}
  financial:
    url: jdbc:postgresql://shard1:5432/advertmarket
    username: ${DB_USER}
    password: ${DB_PASSWORD_SHARD1}
  deals:
    url: jdbc:postgresql://shard2:5432/advertmarket
    username: ${DB_USER}
    password: ${DB_PASSWORD_SHARD2}
```

---

## Related Documents

- [Data Stores](../04-architecture/05-data-stores.md)
- [Deployment](../09-deployment.md)
- [DDL Migrations](./05-ddl-migrations.md)