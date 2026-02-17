# Parent Instructions
- Parent: `/Users/nikitakocnev/.codex/AGENTS.md`
- This file contains only module-local deltas.

# Deal — Agent Instructions

Deal state machine, CAS-based transitions, deadline management, and lifecycle orchestration.

## Structure

| Area | Key Classes |
|------|------------|
| Services | `DealService`, `DealTransitionService` (non-@Component, wired via DealConfig) |
| Repositories | `JooqDealRepository`, `JooqDealEventRepository` |
| Controller | `DealController` (package-private) |
| Adapters | `DealAuthorizationAdapter` (ABAC checks via jOOQ) |
| Config | `DealConfig` (wires DealTransitionService) |

## Rules

- Infrastructure behind port interfaces from `advert-market-deal-api`
- Repositories use generated jOOQ classes (`DEALS`, `DEAL_EVENTS`)
- `DealTransitionService` is NOT a `@Component` — wired via `DealConfig`
- `@RequiredArgsConstructor` for all services and adapters
- `@Fenum` for error codes and event types
- CAS via `version` column — optimistic locking on transitions
- Cursor pagination uses `CursorCodec` with composite `{ts, id}` cursor
- `@PathVariable("name")` / `@RequestParam(value = "name")` explicit — no reliance on `-parameters` flag
- If an endpoint has >2 query params, use a request params object (single controller parameter) instead of multiple `@RequestParam`
- Outbox events published via `OutboxRepository` in same transaction as state change
