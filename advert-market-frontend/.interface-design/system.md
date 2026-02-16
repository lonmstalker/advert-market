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

The product's unique element. A vertical timeline showing deal progress through 16 states.

- Current state: accent-colored node with label
- Completed states: muted checkmark nodes
- Future states: dimmed, smaller nodes
- Connects to role-specific action buttons below the timeline

This appears on every deal detail page. It's the first thing users look at.

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
