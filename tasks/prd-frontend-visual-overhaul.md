# PRD: Frontend Visual Overhaul

## Introduction

Frontend looks broken: text is nearly invisible, dark mode doesn't work outside Telegram, catalog cards overflow, deal status badges are unreadable, and the overall visual quality is far below the reference (Telegram Wallet). The root cause is CSS variables from Telegram SDK not being set outside Telegram context, combined with layout/contrast issues that persist regardless of context.

The interface follows "Spatial Native 2026" concept: multi-layered glass (Glassmorphism 3.0), atmospheric lighting, haptic choreography, and Deep Gunmetal OLED-safe dark theme. The app must feel like a premium, native part of Telegram, not a WebView website.

Reference: Telegram Wallet (13 screenshots in `скрины кошелька/`) — clean typography hierarchy, clear contrast in both light/dark, crisp cards with subtle shadows, readable financial data.

## Goals

- Fix text contrast so all content is readable in both light and dark themes
- Provide theme fallback outside Telegram (dev, E2E, web preview)
- Fix all text overflow/truncation issues on catalog and deal cards
- Bring visual quality to Telegram Wallet level: clean hierarchy, readable financial data, polished cards
- Audit and enforce DESIGN_GUIDELINES rules: glass blur budget, pressScale, haptics, tabular-nums
- Establish Playwright visual regression tests to prevent future degradation
- Pass DESIGN_GUIDELINES.md pre-release checklist (section 20) on every screen

## External Analysis Review

An external Staff/Principal-level analysis provided 6 artifacts (Design Guidelines, Tailwind Config, Icons, SVG Assets, UX Rules, Implementation Strategy). Below is the disposition:

### Accepted (valid insights integrated into this PRD)
1. **CSS variable fallbacks in `:root`** — correct diagnosis of the core problem
2. **OLED-safe Deep Gunmetal** — no pure `#000000`, use `#1C1C1E` base
3. **Glass blur budget** — `blur(12px)` for cards, `blur(20px)` only for persistent chrome (tabs, headers)
4. **Haptic choreography** — haptic fires before animation; selectionChanged for tabs, impactOccurred('light') for cards, impactOccurred('medium') for CTAs
5. **Rule of 4-3-1** — max 4 data points/card, 3 typography levels/viewport, 1 primary CTA/screen
6. **Staggered reveals** — cascade appearance with 50-60ms delay between items
7. **Escrow safety** — DialogModal required for all destructive/financial actions, NEVER 1-click
8. **No dead ends** — every empty state and error screen must have a CTA returning user to action
9. **Safe area dynamic padding** — use Telegram viewport CSS vars with env() fallback
10. **Softened accent in dark** — accent color may need slight adjustment for dark theme readability

### Rejected (conflicts with existing project conventions)
| # | Proposed | Why Rejected | Correct Approach |
|---|----------|-------------|-----------------|
| 1 | `tailwind.config.ts` | Project uses Tailwind CSS v4 with CSS-first config | All tokens in `@theme {}` blocks in `.css` files |
| 2 | `cn()` utility | Forbidden by convention | Use `joinClasses()` |
| 3 | `framer-motion` import | Wrong package | Use `import { motion } from 'motion/react'` |
| 4 | `useHapticFeedback` from SDK | Not the project pattern | Use `useHaptic()` from `@/shared/hooks/use-haptic` |
| 5 | New `SpatialPage` component | Duplicates existing | Use existing `AppPageShell` |
| 6 | New `GlassCard` component | Duplicates existing | Use existing `AppSurfaceCard` |
| 7 | `[theme-mode='dark']` selector | Not how theme works | Use `prefers-color-scheme: dark` for fallback; Telegram SDK sets `--tg-theme-*` vars directly |
| 8 | `proportional-nums` for hero | Violates DESIGN_GUIDELINES | `tabular-nums` for ALL numeric displays — prevents layout shift on updates |
| 9 | `#0F161C` as app background | Conflicts with current token | Deep Gunmetal is `#1C1C1E` per DESIGN_GUIDELINES |
| 10 | `#007AFF` as accent | Wrong brand color | Telegram Blue is `#3390EC` |
| 11 | Optimistic updates for escrow/financial | **EXPLICITLY FORBIDDEN** by DESIGN_GUIDELINES sec.10 | `networkMode: 'online'`, `staleTime: 0`, NEVER optimistic for deposit/release/refund |
| 12 | New `deal-status-mapper.ts` | Already exists | `deal-macro-stage.ts` in `src/features/deals/lib/` |
| 13 | New `useSpatialInteraction` hook | Duplicates existing | Use `useHaptic()` + `pressScale` from `animations.ts` |

### Already Implemented (verified in codebase)
- Progressive disclosure 17→3 macro-stages (`deal-macro-stage.ts`)
- Atmosphere gradient (`AppPageShell` with `::before` pseudo-element)
- Animation presets (`animations.ts`: fadeIn, slideUp, scaleIn, pressScale, staggerChildren, shimmer)
- Icon conventions in DESIGN_GUIDELINES (strokeWidth rules)
- VerifiedBadge component with hardcoded `#3390EC`
- TonDiamondIcon component
- PulsingDot component
- Haptic hook (`useHaptic`)

## User Stories

### US-001: Define CSS variable fallbacks for non-Telegram environments
**Description:** As a developer, I want the app to render correctly outside Telegram (dev server, E2E, web preview) so that visual issues are caught during development.

**Acceptance Criteria:**
- [ ] Light theme fallback values in `global.css` `:root`: `--color-foreground-primary: #000000`, `--color-foreground-secondary: #8E8E93`, `--color-foreground-tertiary: #C7C7CC`, `--color-background-base: #FFFFFF`, `--color-background-secondary: #F1F3F4`, `--color-accent-primary: #3390EC`, `--color-state-success: #34C759`, `--color-state-destructive: #FF3B30`, `--color-state-warning: #FF9500`, `--color-border-separator: rgba(0,0,0,0.08)`
- [ ] Dark theme fallback via `@media (prefers-color-scheme: dark)`: `--color-background-base: #1C2C3E`, `--color-background-secondary: #1C1C1E` (Deep Gunmetal OLED), `--color-foreground-primary: #FFFFFF`, `--color-foreground-secondary: #98989F`
- [ ] `VITE_FORCE_THEME` env var: `'dark'` applies `[data-theme='dark']` on `<html>` in `main.tsx`
- [ ] All `--am-*` spatial tokens have fallback values in both light and dark
- [ ] No pure `#000000` used as background (OLED-safe Deep Gunmetal only)
- [ ] SDK `--tg-theme-*` vars override fallbacks when available (CSS specificity)
- [ ] WCAG AA contrast: 4.5:1 for body text, 3:1 for large text
- [ ] Dark mode: cards use subtle borders (`rgba(255,255,255,0.06)`) instead of shadows
- [ ] Dark mode: accent softened if needed for readability
- [ ] Typecheck passes
- [ ] Verify in Playwright: both themes render readable text on all pages

### US-002: Fix catalog channel card layout
**Description:** As a user, I want to read channel names, badges, and prices without overlap so that I can browse the catalog.

**Acceptance Criteria:**
- [ ] Channel name truncates with ellipsis (`flex: 1, min-width: 0, overflow: hidden, text-overflow: ellipsis, white-space: nowrap`)
- [ ] Language badge (RU/EN) is fixed-width, does not shift with name length
- [ ] VerifiedBadge icon inline with name, never wraps to next line
- [ ] Price ("от 5 TON") right-aligned with `flex-shrink: 0`, never overlaps name
- [ ] Subscriber count and @handle on separate line, fully visible, secondary color
- [ ] Max 4 data points per card (avatar + name + subscribers + price)
- [ ] Avatar letter has proper contrast against colored background
- [ ] Category chip row scrolls horizontally without layout shift
- [ ] Summary line uses footnote/secondary color
- [ ] Typecheck passes
- [ ] Verify in Playwright screenshot at 390x844

### US-003: Fix deal list card readability
**Description:** As a user, I want to see deal status clearly on the deals list so I know what needs my attention.

**Acceptance Criteria:**
- [ ] Status badge: `caption1` (12px) minimum, colored pill background, contrasting text
- [ ] Status colors follow `deal-status.ts` config, visible in both themes
- [ ] Deal amount uses `callout` typography, right-aligned, clearly visible
- [ ] Channel name `body/medium`, truncates with ellipsis
- [ ] Date/time `subheadline2/secondary`, right-aligned below amount
- [ ] Channel avatar proper letter contrast
- [ ] Segment control clear active/inactive states
- [ ] Empty state with icon + heading + description + CTA ("Find a channel" → `/catalog`)
- [ ] Typecheck passes
- [ ] Verify in Playwright screenshot

### US-004: Fix deal detail page
**Description:** As a user, I want to see deal details with clear hierarchy so I understand the current state and available actions.

**Acceptance Criteria:**
- [ ] Deal amount `title1 bold` or `title2 bold`, high contrast `foreground-primary`
- [ ] Fiat equivalent `body/secondary` below amount
- [ ] Status section: soft color pill background with readable text
- [ ] Timeline: proper vertical connector lines, consistent spacing
- [ ] Active step: accent color dot with subtle pulse (PulsingDot)
- [ ] Completed steps: check icon; pending steps: muted dot
- [ ] "Ещё N шагов" link accent-colored, properly spaced
- [ ] Metadata chips (date, deadline, timer): consistent chip styling, no overlap
- [ ] Comment text: body weight, 16px padding
- [ ] Action button in FixedBottomBar with safe-area-bottom padding
- [ ] No excessive empty whitespace
- [ ] Typecheck passes
- [ ] Verify in Playwright screenshot

### US-005: Fix wallet page visual quality
**Description:** As a user, I want to see my balance and transactions clearly, matching Telegram Wallet quality.

**Acceptance Criteria:**
- [ ] Balance: `hero` (88px) or `title1 bold`, centered, `foreground-primary`, `tabular-nums`
- [ ] Fiat equivalent: `body/secondary` below balance
- [ ] "В эскроу: N TON" readable with soft accent background badge
- [ ] Action buttons: icon above label, equal width, no truncation, icons size 24 strokeWidth 1.5 accent
- [ ] Transaction amounts: green (`--color-state-success`) for +, red (`--color-state-destructive`) for -, `tabular-nums`
- [ ] Transaction type icons: colored circles (deposit=blue, payout=green, refund=orange, fee=gray)
- [ ] Date group headers: section header styling, proper spacing
- [ ] "Connect Wallet" button prominent, TonConnect brand
- [ ] Crypto/TON segment control clear active state
- [ ] Stats cards readable
- [ ] Typecheck passes
- [ ] Verify in Playwright screenshot (ref: скрины кошелька/ #1, #5, #9, #10)

### US-006: Fix profile page
**Description:** As a user, I want to see my profile information clearly.

**Acceptance Criteria:**
- [ ] User name: `title2 bold`, `foreground-primary`
- [ ] @handle: `body/secondary`
- [ ] Role badge: accent soft background + accent text
- [ ] "Участник с...": `caption1/tertiary`
- [ ] Avatar circle: proper border/shadow for depth
- [ ] "МОИ КАНАЛЫ" section header: `footnote/secondary` uppercase
- [ ] Empty state: icon visible, "Нет каналов" `title3 foreground-primary`, description `body/secondary`, "Добавить канал" primary button
- [ ] Settings GroupItems: proper icon colors, chevron
- [ ] Typecheck passes
- [ ] Verify in Playwright screenshot

### US-007: Fix onboarding flow
**Description:** As a user, I want onboarding to feel polished — readable text, clear hierarchy, no wasted space.

**Acceptance Criteria:**
- [ ] Locale step: heading `title2 bold foreground-primary`, description `body/secondary`, no excessive top whitespace
- [ ] Welcome: "Ad Market" `title1 bold foreground-primary`, not blending into gradient
- [ ] Feature cards: consistent card styling with icon + title + description
- [ ] Interest: "Кто вы?" fully visible, not clipped at viewport top
- [ ] Role cards: clear selected/unselected (accent border vs default)
- [ ] Expanded details appear with smooth animation
- [ ] Tour: channel names in mock list visible with truncation
- [ ] Step indicators: proper accent/tertiary contrast
- [ ] "Продолжить" always visible at bottom
- [ ] Typecheck passes
- [ ] Verify in Playwright screenshots per step

### US-008: Fix channel detail page
**Description:** As a user, I want to see channel details and pricing clearly.

**Acceptance Criteria:**
- [ ] Channel name: `title2 bold foreground-primary`
- [ ] @handle: `body/secondary`
- [ ] VerifiedBadge + language badge inline with name
- [ ] Category tags: chip styling with soft color backgrounds
- [ ] Stats cards: numbers `title2 bold`, labels `caption1/secondary`
- [ ] "36% reach" readable in both themes
- [ ] Tab bar: clear active/inactive with accent underline
- [ ] Pricing: "от N TON" `title2 bold`, fiat `body/secondary`
- [ ] Price cards: type icon + name + metrics left, amount + fiat right
- [ ] Share/Edit header buttons: proper icon contrast
- [ ] Typecheck passes
- [ ] Verify in Playwright screenshot

### US-009: Audit glass blur, atmosphere, pressScale, haptics, tabular-nums
**Description:** Systematic cross-cutting audit to enforce DESIGN_GUIDELINES rules.

**Acceptance Criteria:**
- [ ] All `AppSurfaceCard` usages: `backdrop-filter: blur(12px)`, NOT `blur(20px)`
- [ ] Bottom tabs (`.am-bottom-tabs`): `blur(20px)` confirmed
- [ ] Atmosphere gradient: opacity doesn't reduce text contrast below WCAG AA
- [ ] Finance atmosphere: slightly stronger glow, still WCAG compliant
- [ ] All Button/GroupItem/card onClick: `pressScale` animation
- [ ] All card/list onClick: `impactOccurred('light')` via `useHaptic()`
- [ ] All CTA buttons: `impactOccurred('medium')`
- [ ] All tab/segment switches: `selectionChanged()`
- [ ] `font-variant-numeric: tabular-nums` on ALL numeric displays
- [ ] No `blur(20px)` on content cards
- [ ] Every list has empty state with heading + description + CTA (no dead ends)
- [ ] Typecheck passes

### US-010: Fix Eruda debug button positioning
**Description:** Eruda widget overlaps bottom tab bar and interactive elements.

**Acceptance Criteria:**
- [ ] Eruda entry doesn't overlap bottom tabs, FixedBottomBar, or interactive elements
- [ ] CSS override in `global.css` repositions `.eruda-entry` (e.g., top-right below safe area)
- [ ] Hidden in production, hidden in E2E (verify)
- [ ] Typecheck passes

### US-011: Implement Playwright visual regression test suite
**Description:** Automated screenshot tests for all main pages in light + dark to prevent regression.

**Acceptance Criteria:**
- [ ] New file: `e2e/visual-regression.spec.ts`
- [ ] 10 pages: onboarding-locale, onboarding-welcome, onboarding-interest, catalog, channel-detail, channel-prices, deals-list, deal-detail, wallet, profile
- [ ] Each page in light + dark themes (`VITE_FORCE_THEME`)
- [ ] Viewport: 390x844 (iPhone 14 Pro)
- [ ] `toHaveScreenshot()` with `maxDiffPixels` tolerance
- [ ] Verifies key text elements present and visible
- [ ] MSW-mocked flow (`VITE_MOCK_API=true`)
- [ ] Baseline generation: `npx playwright test --update-snapshots`
- [ ] Integrated into `npm run test:e2e`
- [ ] Baselines in `e2e/__screenshots__/`
- [ ] Typecheck passes

## Functional Requirements

- FR-1: CSS variable fallback values in `global.css` `:root` for all `--tg-theme-*` and `--color-*` variables
- FR-2: `VITE_FORCE_THEME` env var for light/dark outside Telegram
- FR-3: Dark mode fallback via `@media (prefers-color-scheme: dark)` + `[data-theme='dark']`
- FR-4: Channel cards: truncate names with ellipsis, fixed badges, right-aligned prices
- FR-5: Deal status badges: `caption1` minimum, colored pill with contrasting text
- FR-6: Wallet balance: `hero` typography, centered, `tabular-nums`
- FR-7: Financial amounts: green income, red expense (DESIGN_GUIDELINES color coding)
- FR-8: Transaction icons: colored circles matching Telegram Wallet reference
- FR-9: `AppPageShell` atmosphere gradient must not reduce text readability below WCAG AA
- FR-10: Glass blur budget: `blur(12px)` cards, `blur(20px)` chrome only
- FR-11: `pressScale` + haptics on all interactive elements
- FR-12: `tabular-nums` on all numeric displays
- FR-13: Every empty state and error screen has CTA (no dead ends)
- FR-14: Dark mode: no pure `#000000`, cards use borders not shadows
- FR-15: Playwright visual regression: 10 pages x 2 themes = 20 baselines
- FR-16: All pages pass DESIGN_GUIDELINES section 20 pre-release checklist

## Non-Goals

- No new features or pages — purely visual fixes and polish
- No backend changes
- No changes to routing, data fetching, or business logic
- No redesign of information architecture or page structure
- No custom component library — continue using `@telegram-tools/ui-kit`
- No new wrapper components (SpatialPage, GlassCard) — use existing `AppPageShell`, `AppSurfaceCard`
- No `tailwind.config.ts` — all Tailwind config stays CSS-first
- No Storybook updates (separate task)
- No accessibility audit beyond contrast ratios (separate task)
- **No optimistic updates for financial operations** — FORBIDDEN per DESIGN_GUIDELINES

## Design Considerations

### Reference: Telegram Wallet
13 screenshots in `скрины кошелька/` showing:
- **Light theme**: White background, dark text, subtle gray cards with soft shadows, gradient accents
- **Dark theme**: Deep dark `#1C1C1E` background, white text, subtle border cards, colored icons
- **Balance**: Large centered number, fiat equivalent below, change indicator (green/red)
- **Action buttons**: Row of 4 icons with labels below, equal spacing
- **Transaction list**: Colored circle icon + type + name on left, amount + time on right, grouped by date
- **Detail view**: Large amount in green/black, status card with label-value pairs
- **Skeleton loading**: White/gray rectangles matching content shape, animation shimmer
- **Tab bar**: 4 tabs with icons + labels, active tab highlighted with brand color

### Spatial Native 2026 Principles (validated)
1. **Atmospheric Glass**: Every page has radial glow via `AppPageShell::before`, cards use `blur(12px)`
2. **OLED-Safe Dark**: Deep Gunmetal `#1C1C1E`, no pure black, elevation via borders not shadows
3. **Press Physics**: `pressScale` (spring stiffness 400, damping 17) on all interactive elements
4. **Staggered Reveals**: Elements appear with 50-60ms cascade delay
5. **Haptic Choreography**: Vibration fires before animation: selectionChanged → light → medium → success/error
6. **Data Density Control**: 4-3-1 rule enforced at component level
7. **Progressive Disclosure**: 17 deal statuses → 3 macro-stages (existing `deal-macro-stage.ts`)
8. **Escrow Safety**: DialogModal for all destructive/financial; no optimistic updates for financial ops

### Key patterns to adopt from reference
1. **Text hierarchy**: Large bold numbers → medium labels → small secondary
2. **Color discipline**: Primary text = max contrast, secondary = muted, tertiary = hints only
3. **Card clarity**: Clean borders/shadows, atmosphere gradient doesn't create visual noise
4. **Financial prominence**: Balance is hero, amounts in lists are clearly colored green/red
5. **Consistent spacing**: Even padding, aligned elements, no orphaned floating content

## Technical Considerations

- Theme fallback: pure CSS (`:root` defaults + `@media (prefers-color-scheme: dark)`) — zero JS runtime cost
- `global.css` is single source of truth for fallback values
- Tailwind CSS v4: tokens in `@theme {}` blocks in `.css` files (NO `tailwind.config.ts`)
- Conditional classes: `joinClasses()` (NOT `cn()` or `clsx`)
- Motion: `import { motion } from 'motion/react'` (NOT `framer-motion`)
- Haptics: `useHaptic()` from `@/shared/hooks/use-haptic` (NOT `useHapticFeedback` from SDK)
- Existing components to reuse: `AppPageShell`, `AppSurfaceCard`, `AppListRow`, `AppSectionHeader`, `FixedBottomBar`, `Chip`, `SegmentControl`, `EmptyState`, `FormattedPrice`, `ChannelAvatar`
- Catalog card fix: likely CSS-only (flex layout, text-overflow, min-width: 0)
- Playwright tests: `page.emulateMedia({ colorScheme })` for theme, `toHaveScreenshot()` for baseline
- E2E mode already disables Eruda (`MODE=e2e`)

## Success Metrics

- All text readable at arm's length on 390px viewport
- WCAG AA contrast (4.5:1 body, 3:1 large) for all text in both themes
- Zero text overflow/clipping on catalog and deal cards
- Dark mode matches Deep Gunmetal OLED spec
- 20/20 Playwright screenshot tests pass (10 pages x 2 themes)
- DESIGN_GUIDELINES section 20 checklist: all items pass on all screens
- `pressScale` + haptic on 100% of interactive elements
- `tabular-nums` on 100% of numeric displays

## Open Questions

- Should atmosphere gradient opacity be reduced globally, or only on pages with header text overlay?
- Eruda: reposition to top-right, or hide completely in dev and only show via console command?
- Visual regression baselines: generate from fallback theme (reproducible in CI) or from Telegram context?
