# Deal API — Agent Instructions

Pure API module: events and ports for the deal state machine and deadline management.

## Contents

- **Events** (2): `DealStateChangedEvent`, `DeadlineSetEvent`
- **Enums** (1): `DeadlineAction`
- **Ports** (1): `DealAuthorizationPort`

## Rules

- DTOs are Java records with Jakarta Validation annotations
- Port interfaces use `@NonNull`/`@Nullable` on all parameters and return types
- No Spring/infrastructure dependencies (compileOnly only)
- `@Fenum` for string constants
