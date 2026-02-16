# Interface Design System — Ad Marketplace

> Living patterns file. Base design rules are in [DESIGN_GUIDELINES.md](../DESIGN_GUIDELINES.md).
> This file captures patterns discovered during development that extend the base guidelines.

---

## Direction

**Feel:** Confident like a banking app, light like a chat. Financial trust meets messaging speed.

**Reference:** TON Wallet (clean numbers, semantic color, zero noise).

**Anti-reference:** ADMINOTEKA (overloaded cards, dark backgrounds, too many data points).

**Constraint:** @telegram-tools/ui-kit provides all base components. All colors via Telegram CSS variables (`--color-*`). No hardcoded hex/rgb. Theme auto-syncs with Telegram.

---

## Signature: Deal Timeline

Two complementary timeline views:

**DealTimeline** (vertical, detailed) — `src/features/deals/components/DealTimeline.tsx`
- Shows all 17 individual statuses on the deal detail page
- Current state: accent-colored node with label
- Completed states: muted checkmark nodes
- Future states: dimmed, smaller nodes
- Connects to role-specific action buttons below

**MiniTimeline** (horizontal, compact) — `src/features/deals/components/MiniTimeline.tsx`
- 3 macro-stages: Agreement → Payment → Publication
- Fits inside deal list cards for at-a-glance progress
- Completed: CheckCircleIcon (accent), Active: PulsingDot (accent), Pending: dimmed circle
- Uses `deal-macro-stage.ts` mapper (17 → 3+1 via `satisfies Record<DealStatus, MacroStage>`)

### VerifiedBadge

`src/shared/ui/icons/verified-badge.tsx` — Telegram brand verified checkmark. Hardcoded `#3390EC` (Telegram Blue). Props: `size` (default 16), `className`.

---

## Established Patterns

### Full-Screen Page Layout (onboarding, empty states)

Prevents UI Kit Button `flex-grow: 1` from stretching in flex containers:

```
<div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh', padding: '0 24px' }}>
  <div style={{ flex: 1, display: 'flex', ... }}>
    {/* Content area — centered, takes remaining space */}
  </div>
  <div style={{ flexShrink: 0, paddingBottom: '32px', paddingTop: '16px' }}>
    <motion.div {...pressScale}>
      <Button text="..." type="primary" />
    </motion.div>
  </div>
</div>
```

**Critical:** Never place `<Button>` directly in a flex column with `minHeight: 100vh` — its `flex-grow: 1` will stretch it to fill remaining space.

### Onboarding Shell (sticky CTA + safe area)

`OnboardingShell` is the canonical wrapper for all onboarding screens:

- Top action row (language / skip)
- Flexible content area
- Sticky footer with primary action

Layout contract:

- `maxWidth: var(--am-onboarding-max-width)`
- Horizontal padding via `var(--am-onboarding-page-padding)`
- Footer bottom spacing via `calc(var(--am-onboarding-footer-padding-bottom) + var(--am-safe-area-bottom))`
- Height tied to stable Telegram viewport token (`--am-viewport-stable-height`)

This keeps CTA placement consistent across mobile and desktop and prevents iOS home-indicator overlap.

### Icon Container (onboarding, feature highlights)

Emoji in a rounded-square container instead of bare emoji:

```
width: 80px, height: 80px, borderRadius: 24px
backgroundColor: var(--color-background-base)
emoji: fontSize 40px, lineHeight 1
```

Provides visual weight without custom illustrations. Consistent across welcome, tour, and empty states.

### Selection Cards (role picker, multi-select)

Custom `<button>` with proper Telegram-native styling:

- Surface: `background-base`, 1px `border-separator`
- Selected: accent border + `Icon name="check" color="accent"` (animated via AnimatePresence)
- Emoji in nested 48x48 rounded-square (`background-secondary`)
- Typography: `body medium` title + `caption1 secondary` hint
- `pressScale` on wrapper, `WebkitTapHighlightColor: transparent`

### Step-Based Tour (carousel replacement)

State-driven slides instead of scroll-snap:

- `AnimatePresence mode="wait"` with x-axis slide variants (enter: x:40, exit: x:-40)
- Pill-dot indicator: active dot `width: 24px`, inactive `8px` — Motion `animate={{ width }}`
- Dot color via CSS `transition` (not Motion `animate`) to avoid jsdom warnings
- Navigation: "Next/Finish" (primary button) + always-visible "Skip tutorial" in header
- Last slide: primary button text is role-aware (owner vs advertiser/both)
- Tour is soft-gated: Next is always available; completion status is shown as hint text

### Text Alignment

UI Kit `<Text>` does not inherit `textAlign` from parent. Always pass `align="center"` prop explicitly when centering is needed.

### Channel Detail Page (catalog)

Stats displayed as compact icon-stat grid (not GroupItem list):

- 3-column grid: Subscribers | Avg. reach | Engagement
- Reach rate shown as percentage below avg reach (`36% reach`)
- Hero CPM calculated from min price / avg reach, shown in pricing section
- Language badge and channel age shown near username
- Owner sees Edit button, non-owner sees Create deal CTA
- Private channels show "Join channel" link instead of "Open channel in Telegram"

### Catalog Page (channel list)

- CategoryChipRow with horizontal scroll, sorted alphabetically with "All topics" first
- Search with 300ms debounce
- Infinite scroll via IntersectionObserver
- Filters in bottom Sheet
- Empty state with "Reset filters" CTA
- Channel cards: custom ChannelCatalogCard (not GroupItem) with avatar, title, stats row, price badge

### Wallet Section (finance)

**BalanceCard** — unified card combining TON Connect button and balance display:
- Card surface: `background-base`, `border-separator`, `borderRadius: 14`, `overflow: hidden`
- Contained gradient backdrop (80px height, role-aware: success/accent)
- Header row: label (`caption1 bold secondary`) + `<TonConnectButton />` (or Spinner)
- Balance: `largeTitle bold tabular-nums` centered
- Fiat: `subheadline2 tertiary tabular-nums` centered
- Cascade animation: card fadeIn → balance slideUp delay 0.1s → fiat opacity delay 0.25s

**MetricRow** — horizontal 2-cell stat container (same pattern as ChannelDetailStats):
- Flex container: `background-base`, `border-separator`, `borderRadius: 14`, `overflow: hidden`
- 2 equal cells (`flex: 1`, `textAlign: center`, `padding: 14px 12px`)
- 1px vertical divider (`alignSelf: stretch`, `background: border-separator`)
- Value: `title3 bold tabular-nums`, Label: `caption1 secondary`

**TransactionListItem** — 40px icon circle, 20px SVG, `callout bold` amount, haptic on tap

**TransactionDetailPage** — hero contained in card (same surface as BalanceCard), 56px icon, blockchain addresses via `Text caption2` with monospace font, haptic on address copy

### Finance Shell v2 (wallet parity)

- Shared page shell class: `.am-finance-page`
  - max width: `--am-finance-page-max-width`
  - horizontal spacing: `--am-finance-page-padding`
  - bottom spacing includes tab bar + safe area
- Shared card class: `.am-finance-card`
  - surface: `--am-card-surface`
  - border: `--am-card-border`
  - shadow: `--am-card-shadow`
  - radius: `18px`
- Shared stack rhythm: `.am-finance-stack` with consistent vertical gaps.
- Applied to `/wallet`, `/wallet/history`, `/wallet/history/:txId`, including skeleton states.

### Floating Bottom Tabs

- Replaced edge-to-edge flat bar with floating capsule:
  - `--am-tabbar-bg`, `--am-tabbar-border`
  - 28px radius, subtle blur, detached bottom offset
- Active tab uses shape + color (not color only):
  - `--am-tabbar-active-bg`
  - `--am-tabbar-active-color`
- Layout contract updated:
  - content bottom padding tied to `--am-bottom-tabs-height + safe area + offset`.

---

## Design Tokens Mapping (app.css)

### Spacing Tokens

| Token | Value | Tailwind Utility |
|-------|-------|-----------------|
| `--am-space-4` | 4px | `p-am-4`, `gap-am-4` |
| `--am-space-8` | 8px | `p-am-8`, `gap-am-8` |
| `--am-space-12` | 12px | `p-am-12`, `gap-am-12` |
| `--am-space-16` | 16px | `p-am-16`, `gap-am-16` |
| `--am-space-20` | 20px | `p-am-20`, `gap-am-20` |
| `--am-space-24` | 24px | `p-am-24`, `gap-am-24` |

### Border Radius Tokens

| Token | Value | Tailwind Utility | Usage |
|-------|-------|-----------------|-------|
| `--am-radius-control` | 14px | `rounded-control` | Buttons, inputs |
| `--am-radius-row` | 14px | `rounded-row` | List rows |
| `--am-radius-card` | 18px | `rounded-card` | Cards, surfaces |
| `--am-radius-hero` | 22px | `rounded-hero` | Hero sections |
| `--am-radius-chip` | 999px | `rounded-chip` | Pills, chips |

### Color System Bridge

| Category | CSS Variable | Source |
|----------|-------------|--------|
| **Accent** | `--color-accent-primary` | `--tg-theme-button-color` |
| **Foreground** | `--color-foreground-primary` | `--tg-theme-text-color` |
| | `--color-foreground-secondary` | `--tg-theme-hint-color` |
| | `--color-foreground-tertiary` | `--tg-theme-hint-color` |
| **Background** | `--color-background-base` | `--tg-theme-bg-color` |
| | `--color-background-secondary` | `--tg-theme-secondary-bg-color` |
| **State** | `--color-state-success` | `#34c759` (static) |
| | `--color-state-destructive` | `#ff3b30` (static) |
| | `--color-state-warning` | `#ff9500` (static) |
| **Static** | `--color-static-white` | `#ffffff` (never inverts) |
| | `--color-static-black` | `#000000` (never inverts) |
| **Link** | `--color-link` | `--tg-theme-link-color` |
| **Border** | `--color-border-separator` | `rgba(0,0,0,0.08)` |

### App Surface Tokens

| Token | Purpose | Default |
|-------|---------|---------|
| `--am-app-background` | Root background | `--color-background-secondary` |
| `--am-card-surface` | Card background | `--color-background-base` |
| `--am-card-border` | Card border | `--color-border-separator` |
| `--am-card-shadow` | Card shadow | Multi-layer box shadow |
| `--am-input-bg` | Input background | `--color-background-base` |
| `--am-tabbar-bg` | Tab bar background | `--am-card-surface` |

### Soft Badge Backgrounds

| Token | Usage |
|-------|-------|
| `--am-soft-accent-bg` | Accent status badges |
| `--am-soft-warning-bg` | Warning/pending badges |
| `--am-soft-success-bg` | Success/completed badges |
| `--am-soft-destructive-bg` | Error/destructive badges |
| `--am-soft-secondary-bg` | Neutral/cancelled badges |

### Shadow System

| Token | Usage |
|-------|-------|
| `--am-shadow-sm` | Subtle elevation |
| `--am-shadow-card` | Standard card shadow |
| `--am-shadow-elevated` | High-elevation elements (modals, popovers) |

### Motion Tokens

| Token | Value | Usage |
|-------|-------|-------|
| `--am-motion-fast` | 120ms | Micro-interactions |
| `--am-motion-base` | 180ms | Standard transitions |
| `--am-motion-slow` | 240ms | Page transitions |
| `--am-motion-ease-out` | `cubic-bezier(0.22, 1, 0.36, 1)` | Smooth deceleration |

---

## Component Patterns Catalog

### Layout Components

| Component | File | Purpose |
|-----------|------|---------|
| `AppPageShell` | `shared/ui/components/app-page-shell.tsx` | Page wrapper with safe areas, tab padding, atmosphere |
| `FixedBottomBar` | `shared/ui/components/fixed-bottom-bar.tsx` | Fixed CTA area above tabs |
| `BackButtonHandler` | `shared/ui/components/back-button-handler.tsx` | TMA SDK back button integration |
| `BottomTabs` | `shared/ui/components/bottom-tabs.tsx` | 4-tab navigation (Catalog/Deals/Wallet/Profile) |
| `OnboardingShell` | `features/onboarding/components/onboarding-shell.tsx` | Onboarding page wrapper with sticky footer |

### Data Display Components

| Component | File | Purpose |
|-----------|------|---------|
| `AppSurfaceCard` | `shared/ui/components/app-surface-card.tsx` | Glass surface card container |
| `AppSectionHeader` | `shared/ui/components/app-section-header.tsx` | Section title with optional action |
| `AppListRow` | `shared/ui/components/app-list-row.tsx` | Custom list row with chevron |
| `FormattedPrice` | `shared/ui/components/formatted-price.tsx` | TON + fiat price display with tabular-nums |
| `ChannelAvatar` | `shared/ui/components/channel-avatar.tsx` | Letter avatar with hue-based color |
| `EmptyState` | `shared/ui/components/empty-state.tsx` | Empty list placeholder with icon + CTA |
| `EndOfList` | `shared/ui/components/end-of-list.tsx` | Infinite scroll end marker |

### Interactive Components

| Component | File | Purpose |
|-----------|------|---------|
| `Tappable` | `shared/ui/components/tappable.tsx` | Generic tappable wrapper with motion |
| `Chip` | `shared/ui/components/chip.tsx` | Selection chip/pill |
| `FilterButton` | `shared/ui/components/filter-button.tsx` | Filter trigger with active count badge |
| `SegmentControl` | `shared/ui/components/segment-control.tsx` | iOS-style segmented control |
| `SearchInput` | `shared/ui/components/search-input.tsx` | Search field with haptic |
| `Popover` | `shared/ui/components/popover.tsx` | Info tooltip with arrow |

### Animation Presets

| Preset | File | Usage |
|--------|------|-------|
| `pressScale` | `shared/ui/animations.ts` | All interactive elements (spring: 400/17) |
| `fadeIn` | `shared/ui/animations.ts` | Loading → content transitions |
| `slideUp` | `shared/ui/animations.ts` | Card appearance |
| `scaleIn` | `shared/ui/animations.ts` | Empty states, modals |
| `slideFromRight` | `shared/ui/animations.ts` | Push navigation |
| `slideFromBottom` | `shared/ui/animations.ts` | Sheet appearance |
| `staggerChildren` + `listItem` | `shared/ui/animations.ts` | Animated lists (max 8-10 items) |
| `shimmer` | `shared/ui/animations.ts` | Skeleton loading |
| `pulse` | `shared/ui/animations.ts` | Active process indicator |
| `toast` | `shared/ui/animations.ts` | Toast show/hide |

---

## Audit Status

Last audit: 2026-02-17. See [DESIGN_AUDIT.md](../DESIGN_AUDIT.md) for full violation report.

- P0 items: 8 (blur values, missing haptic on key cards, popover shadow)
- P1 items: 22 (inline styles, missing pressScale on controls, raw HTML)
- P2 items: 17 (polish: secondary interactions, minor consistency)
