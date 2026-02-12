# Interface Design System — Ad Marketplace

> Telegram Mini App marketplace for channel advertising with TON escrow payments.

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

## Depth Strategy: Borders-only

Telegram Mini Apps live inside a messenger — shadows feel foreign. Structure comes from:

- `--color-border-separator` for list dividers (via Group/GroupItem)
- `--color-background-base` vs `--color-background-secondary` for surface elevation
- No drop shadows. No box-shadow. Ever.

---

## Surfaces

```
Page background:     --color-background-secondary
Card/section:        --color-background-base (via <Group>)
Modal:               --color-background-modal
Overlay:             --color-background-overlay
Input (inset):       --color-background-secondary (slightly recessed)
```

Sidebar: N/A (mobile-only, tab navigation).

---

## Typography

Provided by UI Kit `<Text type="..." weight="...">`. Key mappings:

| Context              | Type          | Weight    |
|----------------------|---------------|-----------|
| Wallet balance       | `hero`        | `bold`    |
| Page heading         | `title1`      | `bold`    |
| Section heading      | `title2`      | `bold`    |
| Card title           | `body`        | `medium`  |
| Card description     | `subheadline1`| `regular` |
| Amounts in lists     | `callout`     | `medium`  |
| Metadata/timestamps  | `subheadline2`| `regular` |
| Group footer         | `footnote`    | `regular` |
| Badges/labels        | `caption1`    | `medium`  |

**Rule:** Max 3 typography levels per viewport.

---

## Spacing

Base unit: **4px**. All spacing is multiples of 4.

| Token           | Value | Use                                |
|-----------------|-------|------------------------------------|
| `--space-xs`    | 4px   | Icon-to-text gap                   |
| `--space-sm`    | 8px   | Intra-component gaps               |
| `--space-md`    | 12px  | Between paired buttons             |
| `--space-base`  | 16px  | Page padding, between sections     |
| `--space-lg`    | 24px  | Empty state CTA gap, major breaks  |
| `--space-xl`    | 32px  | Hero element breathing room        |
| `--space-2xl`   | 40px  | Empty state top/bottom padding     |

---

## Color Semantics

All from Telegram CSS variables — no custom palette.

| Purpose             | Variable                       |
|---------------------|--------------------------------|
| Primary action      | `--color-accent-primary`       |
| Income/profit       | `--color-state-success`        |
| Expense/error       | `--color-state-destructive`    |
| Pending/warning     | `--color-state-warning`        |
| Primary text        | `--color-foreground-primary`   |
| Secondary text      | `--color-foreground-secondary` |
| Hints/placeholder   | `--color-foreground-tertiary`  |
| Links               | `--color-link`                 |

**Financial color rule:**
- Green = money received (success)
- Red = money spent or error (destructive)
- Accent = active/in-progress deal states
- Secondary = neutral/cancelled states

---

## Financial Data

- All amounts in nanoTON (bigint), formatted as `XX.XX TON`
- `font-variant-numeric: tabular-nums` on all amounts
- Balance is the largest element on wallet screen (`hero bold`)
- Positive changes: `+10.00 TON` in success color
- Negative changes: `-5.00 TON` in destructive color
- Thousands separator: space (RU) — `1 250 000`
- Abbreviations in lists: `125K`, `1.2M`

---

## Component Patterns

### List Items (primary pattern)

Everything is a list. GroupItem is the building block.

```
[avatar/icon]  [title + subtitle]  ...  [value/action]  [chevron?]
```

Max 4 data points per item:
- Channel: avatar + name + subscribers + price
- Deal: channel avatar + channel name + post type + status badge
- Transaction: type icon + description + date + amount (colored)

### Deal Status Badge

Exhaustive mapping of 16 states to Text `color` prop:
- New/Active/In-progress: `accent`
- Completed: success (green)
- Disputed: `danger` (red)
- Cancelled/Neutral: `secondary`

### Action Buttons

- One primary CTA per viewport max
- Action pairs: secondary (left) + primary (right), `gap: 12px`
- Quick actions (wallet): circular 56px buttons with caption labels
- All interactive elements wrapped in `pressScale` animation

### Empty States

Every list has one:
- Emoji (48px) + title (title3 bold) + description (body secondary) + CTA button
- Animation: `scaleIn`
- CTA is mandatory (except disputes)

### Modals & Sheets

- Sheet: content-rich (filters, previews) — `slideFromBottom`
- DialogModal: binary decisions — `scaleIn`
- Toast: transient feedback — `toast` animation, 3s duration
- Never nest modals (Sheet -> DialogModal is the only exception)

---

## Animation Presets

All defined in `src/shared/ui/animations.ts`. Key mappings:

| Context              | Preset           |
|----------------------|------------------|
| Page transition      | `slideFromRight` |
| Section appear       | `slideUp`        |
| Empty state          | `scaleIn`        |
| List items           | `staggerChildren` + `listItem` (max 8-10 items) |
| Interactive press    | `pressScale`     |
| Sheet open           | `slideFromBottom` |
| Toast                | `toast`          |
| Skeleton shimmer     | `shimmer`        |
| Loading pulse        | `pulse`          |

**Rules:**
- No animations during scroll
- `AnimatePresence mode="wait"` for loading -> content
- `pressScale` on ALL interactive elements

---

## Loading States

| Level              | Component                          |
|--------------------|------------------------------------|
| App init           | Centered `Spinner` 40px accent     |
| Section loading    | `Group skeleton={{ show: true }}`  |
| Button submitting  | `Button loading`                   |
| Inline loading     | `Spinner` 20px                     |

Skeleton must mirror the shape of future content.

---

## Information Architecture

```
Tab 1: Catalog     → Channel List → Channel Detail → Create Deal
Tab 2: Deals       → Deal List → Deal Detail → [10+ sub-pages]
Tab 3: Wallet      → Summary → Withdraw / History → Transaction Detail
Tab 4: Profile     → Settings → Channels → Manage → Team
```

Navigation: Telegram BackButton via SDK. No custom back arrows.
Deep links: `channel_{id}`, `deal_{id}`, `dispute_{id}`.

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
- Navigation: "Next" (primary button) + "Skip" (text link, `subheadline1 secondary`)
- Last slide: button text changes, Skip disappears

### Text Alignment

UI Kit `<Text>` does not inherit `textAlign` from parent. Always pass `align="center"` prop explicitly when centering is needed.

---

## Checklist (every screen)

- [ ] Max 4 data points per list item
- [ ] Max 3 typography levels in viewport
- [ ] One primary CTA max
- [ ] No hardcoded colors (only `--color-*`)
- [ ] Empty state with CTA for every list
- [ ] Skeleton matching content shape
- [ ] 16px between Group sections
- [ ] `pressScale` on interactive elements
- [ ] Telegram BackButton (not custom)
- [ ] `tabular-nums` on financial data
- [ ] Button in `flexShrink: 0` container (never in stretching flex)
