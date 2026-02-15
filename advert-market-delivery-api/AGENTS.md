# Delivery API — Agent Instructions

Pure API module: commands, result events, and ports for post publishing and delivery verification.

## Contents

- **Commands** (2): `PublishPostCommand`, `VerifyDeliveryCommand`
- **Result events** (3): `DeliveryVerifiedEvent`, `DeliveryFailedEvent`, `PublicationResultEvent`
- **Models** (2): `CreativeDraft`, `InlineButton`
- **Enums** (1): `DeliveryFailureReason`
- **Ports** (1): `DeliveryEventPort`

## Rules

- DTOs are Java records with Jakarta Validation annotations
- Port interfaces use `@NonNull`/`@Nullable` on all parameters and return types
- No Spring/infrastructure dependencies (compileOnly only)
- `@Fenum` for string constants
