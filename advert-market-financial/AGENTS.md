# Financial — Agent Instructions

Double-entry ledger for nanoTON accounting. Escrow, payouts, and reconciliation pending.

## Structure

| Area | Key Classes |
|------|------------|
| Services | `LedgerService` (double-entry posting, idempotency, deadlock prevention) |
| Repositories | `JooqLedgerRepository`, `JooqAccountBalanceRepository` |
| Cache | `RedisBalanceCache` (fail-open, TTL-based) |
| Config | `LedgerProperties` (@ConfigurationProperties record) |

## Patterns

- **Idempotency**: `idempotency_key` column with unique constraint — duplicate posts return existing entry
- **Deadlock prevention**: accounts locked in deterministic order (lower ID first) via `SELECT ... FOR UPDATE`
- **Fail-open cache**: Redis cache misses fall through to DB; cache errors logged, not propagated
- **Overflow protection**: `Math.addExact` for balance arithmetic — throws on overflow
- **Partitioned tables**: `ledger_entries` range-partitioned by `created_at`
- **Metrics**: Micrometer counters/timers via `MetricsFacade` with @Fenum names

## Rules

- Infrastructure behind port interfaces from `advert-market-financial-api`
- Repositories use generated jOOQ classes (`LEDGER_ENTRIES`, `ACCOUNT_BALANCES`)
- All amounts in nanoTON (`long`, never floating point)
- `@RequiredArgsConstructor` for all classes
- `@Fenum` for error codes, metric names, event types
- `@NonNull/@Nullable` on all public APIs

## Not Yet Implemented

- Escrow flows (av4.2)
- Payouts (av4.3)
- Refunds (av4.4)
- Reconciliation (4fr.2)
- TON blockchain integration (av4.2)

## References

- [Financial System Spec](../.memory-bank/07-financial-system/)
- [Financial API](../advert-market-financial-api/AGENTS.md)
