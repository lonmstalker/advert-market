 2. # Identity — Agent Instructions

Auth flow implementation: JWT, Telegram initData, user management.

## Rules
- Infrastructure behind ports (UserRepository, LoginRateLimiterPort)
- Repositories use generated jOOQ classes (Tables.USERS, not DSL.table("users"))
- Non-@Component beans (TelegramInitDataValidator, JwtAuthenticationFilter) wired in app's IdentityConfig
- @RequiredArgsConstructor for all services and adapters
- @Fenum ErrorCodes for DomainException, MetricNames for MetricsFacade
