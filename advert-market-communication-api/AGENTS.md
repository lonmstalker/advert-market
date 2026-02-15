# Communication API — Agent Instructions

Pure API module: ports and models for Telegram channel operations and notifications.

## Contents

- **Ports** (2): `TelegramChannelPort`, `NotificationPort`
- **Models** (5): `ChatInfo`, `ChatMemberInfo`, `ChatMemberStatus`, `NotificationEvent`, `NotificationButton`
- **Notification** (2): `NotificationRequest`, `NotificationType`

## Rules

- DTOs are Java records with Jakarta Validation annotations
- Port interfaces use `@NonNull`/`@Nullable` on all parameters and return types
- No Spring/infrastructure dependencies (compileOnly only)
- `@Fenum` for string constants
