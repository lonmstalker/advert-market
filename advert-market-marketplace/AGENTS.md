# Marketplace — Agent Instructions

Channel management, pricing, team membership, and full-text search implementation.

## Structure

| Area | Key Classes |
|------|------------|
| Services | `ChannelService`, `ChannelRegistrationService`, `ChannelVerificationService`, `PricingRuleService`, `TeamService` |
| Repositories | `JooqChannelRepository`, `JooqCategoryRepository`, `JooqPricingRuleRepository`, `JooqTeamMembershipRepository` |
| Search | `ParadeDbChannelSearch` (BM25 full-text) |
| Controllers | `ChannelController`, `ReferenceDataController`, `PricingRuleController`, `TeamController` |
| Mappers | `ChannelRecordMapper`, `PricingRuleRecordMapper` (MapStruct) |
| Adapters | `ChannelAuthorizationAdapter`, `ChannelLifecycleAdapter` |

## Rules

- Infrastructure behind port interfaces from `advert-market-marketplace-api`
- Repositories use generated jOOQ classes (`Tables.CHANNELS`, not `DSL.table("channels")`)
- Non-`@Component` beans wired in app's `*Config` classes
- `@RequiredArgsConstructor` for all services and adapters
- `@Fenum` for error codes and metric names
- ParadeDB `rank_bm25()` for search scoring — ALWAYS include `LIMIT`
- MapStruct mappers for record ↔ DTO conversion
