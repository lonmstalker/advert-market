# Marketplace API — Agent Instructions

Pure API module: DTOs, port interfaces, enums for channels, pricing, teams, and categories.

## Contents

- **Ports** (7): `ChannelRepository`, `ChannelSearchPort`, `ChannelAuthorizationPort`, `ChannelLifecyclePort`, `PricingRuleRepository`, `TeamMembershipRepository`, `CategoryRepository`
- **DTOs** (19): channel CRUD, search criteria, pricing rules, team management
- **Enums** (3): `PostType`, `ChannelMembershipRole`, `ChannelRight`

## Rules

- DTOs are Java records with Jakarta Validation annotations
- Port interfaces use `@NonNull`/`@Nullable` on all parameters and return types
- No Spring/infrastructure dependencies (compileOnly only)
- `@Fenum` for string constants (error codes via `ErrorCodes`)
- `ChannelRight` enum for ABAC permission checks
