# Parent Instructions
- Parent: `/Users/nikitakocnev/.codex/AGENTS.md`
- This file contains only module-local deltas.

# Marketplace API — Agent Instructions

Pure API module: DTOs, port interfaces, enums for channels, pricing, teams, categories, and Telegram channel verification.

## Contents

- **Ports** (8): `ChannelRepository`, `ChannelSearchPort`, `ChannelAuthorizationPort`, `ChannelLifecyclePort`, `PricingRuleRepository`, `TeamMembershipRepository`, `CategoryRepository`, `TelegramChannelPort`
- **DTOs** (22): channel CRUD, search criteria, pricing rules, team management, plus Telegram query DTOs (`dto/telegram/*`)
- **Enums** (3): `PostType`, `ChannelMembershipRole`, `ChannelRight`

## Rules

- DTOs are Java records with Jakarta Validation annotations
- Port interfaces use `@NonNull`/`@Nullable` on all parameters and return types
- No Spring/infrastructure dependencies (compileOnly only)
- `@Fenum` for string constants (error codes via `ErrorCodes`)
- `ChannelRight` enum for ABAC permission checks
