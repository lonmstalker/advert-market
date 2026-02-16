# Wallet Parity Audit (Telegram-Native Unification)

Date: 2026-02-16  
Scope: onboarding, catalog, deals, wallet, profile, locale-currency

## Registry

| ID | Page | Element | Current | Wallet target | Token/Component fix | Exception? | Justification |
|---|---|---|---|---|---|---|---|
| WPA-001 | Global | Page background | Mixed per-page backgrounds and gradients | One atmospheric layer + same base surface in light/dark | `--am-page-background`, `--am-page-atmosphere`, `.am-page` | No | Removes perceived fragmentation between tabs/pages |
| WPA-002 | Global | Cards | Multiple ad-hoc card shapes and shadows | Unified capsule card geometry | `AppSurfaceCard`, `.am-surface-card`, `--am-radius-card` | No | One visual hierarchy for financial and non-financial pages |
| WPA-003 | Global | Rows/controls | Different row fills/borders per page | Unified row/control contract | `.am-surface-row`, `--am-row-surface`, `--am-control-surface` | No | Keeps list density and interaction model Telegram-native |
| WPA-004 | Onboarding locale | First impression | Locale step lacked service logo | Locale-first step with same logo as welcome | `OnboardingLogo` on locale stage (`data-testid=onboarding-locale-logo`) | No | Brand continuity + explicit trust cue before settings |
| WPA-005 | Onboarding flow | Locale -> welcome language | Potential stale language rollback from server mismatch | Immediate persisted language in UI before next steps | `LocaleCurrencyEditor` mutation reconciliation (`onSuccess`) | No | Ensures tutorial/welcome locale stays deterministic |
| WPA-006 | Catalog | Layout shell | Independent page paddings and sticky header contrast | Same shell and toolbar contract | `AppPageShell`, `.am-toolbar`, `.am-chip-row` | No | Reduces visual drift vs wallet tab |
| WPA-007 | Catalog/Deals list items | Card style | Custom row cards with local borders/radius | Shared card primitives | `ChannelCatalogCard` + `DealListItem` migrated to `AppSurfaceCard` | No | Same tactile density as wallet transaction rows |
| WPA-008 | Deals list | Header + filters | Separate top header style | Shared section header and segmented container style | `AppSectionHeader`, `.am-surface-row` | No | Aligns action hierarchy with wallet/header rhythm |
| WPA-009 | Profile main | Root shell | Profile had its own padding/background model | Same page shell as tabs | `AppPageShell` (`data-testid=profile-page-shell`) | No | Consistent tab-to-tab transitions |
| WPA-010 | Profile hero | Hero card shape | Standalone profile block with custom styling | Hero within shared card depth system | `ProfileHero` wrapped in `AppSurfaceCard` + `.am-profile-hero` | Yes (minor) | Accent gradient retained for identity emphasis |
| WPA-011 | Locale & currency profile page | Standalone plain layout | Different settings page style | Shared shell + section header + card container | `AppPageShell`, `AppSectionHeader`, `AppSurfaceCard` | No | Keeps settings pages visually consistent |
| WPA-012 | Bottom tabs | Mixed inline nav styling | One floating capsule tabbar system | `.am-bottom-tabs`, tokenized tab colors/borders | No | Unified navigation surface across all tabs |
| WPA-013 | Wallet main/history/transaction | Screen anchors for visual CI | No stable visual anchors | Stable test IDs for parity baselines | `data-testid` on wallet shells | No | Reliable visual regression checks |
| WPA-014 | Deal detail/create, notifications, register channel | Secondary pages had divergent surfaces | Shared shell and surface model | `AppPageShell`, `AppSurfaceCard`, `.am-surface-row` | Yes (minor) | Kept product-specific content blocks, normalized container depth |

## Critical Deviations Addressed

1. Background inconsistency between tabs and subpages.
2. Non-uniform card/row shapes and border contrast.
3. Locale-first onboarding without brand anchor.
4. Locale mutation edge-case causing language fallback in onboarding chain.
5. Missing deterministic visual baseline for light/dark CI.

## Before/After References

- Before captures: `advert-market-frontend/reports/design-fix/2026-02-15/before/*`
- After captures: `advert-market-frontend/reports/design-fix/2026-02-15/after/*`
- New capture flow: `advert-market-frontend/e2e/design-fix-capture.spec.ts`
- New CI visual baseline: `advert-market-frontend/e2e/design-parity.spec.ts`

## Exception Policy Log

1. `ProfileHero` accent overlay kept (`src/pages/profile/components/ProfileHero.tsx`)  
Reason: profile identity needs stronger visual anchor than regular list cards.
2. Transaction/deal semantic status chips remain semantically tinted.  
Reason: status readability and risk signaling cannot be reduced to neutral surfaces.
3. Wallet hero gradient remains stronger than regular cards.  
Reason: financial amount hierarchy; this is the primary information layer.
