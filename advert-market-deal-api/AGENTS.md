# Parent Instructions
- Parent: `/Users/nikitakocnev/.codex/AGENTS.md`
- This file contains only module-local deltas.

# Deal API — Agent Instructions

Pure API module: DTOs, ports, and events for the deal state machine.

## Contents

| Area | Classes |
|------|---------|
| DTOs | `CreateDealCommand`, `DealDto`, `DealDetailDto`, `DealEventDto`, `DealRecord`, `DealEventRecord`, `DealListCriteria`, `DealTransitionCommand`, `DealTransitionResult` (sealed) |
| Events | `DealStateChangedEvent`, `DeadlineSetEvent`, `DeadlineAction` |
| Ports | `DealPort`, `DealRepository`, `DealEventRepository`, `DealAuthorizationPort` |

## Rules

- DTOs are Java records with Jakarta Validation annotations
- Port interfaces use `@NonNull`/`@Nullable` on all parameters and return types
- No Spring/infrastructure dependencies (compileOnly only)
- `@Fenum` for string constants
- `DealTransitionResult` is a sealed interface with `Success` and `AlreadyInTargetState` permits
