# Communication API — Agent Instructions

Pure API module: ports and models for notifications.

## Contents

- **Ports** (1): `NotificationPort`
- **Models** (2): `NotificationEvent`, `NotificationButton`
- **Notification** (2): `NotificationRequest`, `NotificationType`

## Notes

- Telegram channel query port and DTOs are owned by `marketplace-api` to avoid `communication` <-> `marketplace` dependency cycles.

## Rules

- DTOs are Java records with Jakarta Validation annotations (when applicable)
- Port interfaces use `@NonNull`/`@Nullable` on all parameters and return types
- No Spring/infrastructure dependencies (compileOnly only)
- `@Fenum` for string constants
