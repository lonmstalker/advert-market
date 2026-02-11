## Summary

<!-- Describe your changes -->

## Type of change

- [ ] Bug fix
- [ ] New feature
- [ ] Refactoring
- [ ] Documentation
- [ ] Infrastructure / CI

## Checklist

- [ ] Tests added/updated
- [ ] No breaking changes to public API modules

## Financial PR Checklist

> Complete this section if the PR touches `advert-market-financial`, `advert-market-financial-api`, or ledger/escrow logic.

- [ ] Amounts use `long` nanoTON
- [ ] `commission + payout == amount`
- [ ] Distributed lock + fencing token
- [ ] Idempotency key
- [ ] Audit log entry
- [ ] `SUM(debits) == SUM(credits)` per tx_ref
- [ ] No UPDATE/DELETE on ledger_entries
- [ ] Integration test with Testcontainers
- [ ] `@AfterEach` double-entry invariant check
