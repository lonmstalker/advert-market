# Design Guidelines — 2026 Edition

> Living design system for advert-market Telegram Mini App.
> Bound to `@telegram-tools/ui-kit` 0.2.4+, Tailwind CSS v4, `motion/react`, and `app.css` token system.

---

## 0. Telegram 2026 Native Illusion Contract

Starting from Telegram WebApp API 8.x/9.x, our target is not "web page in messenger", but native illusion:
the app must feel and behave like a native Telegram surface at 60 FPS+.

### 0.1 Fullscreen + Safe Areas

- Fullscreen-first for immersive flows (dashboards, editor, wallet, long-form flows):
  - preferred: `Telegram.WebApp.requestFullscreen()` (feature-detected)
  - fallback: visually sync bars via `setHeaderColor` + `setBottomBarColor`
- Safe areas are mandatory. All page shells must consume safe-area insets from Telegram CSS vars.
- For our app, source of truth remains `app.css` bridge vars:
  - `--am-safe-area-top/right/bottom/left`
  - mapped from Telegram viewport safe-area vars with `env()` fallback
- Never place CTA/tabs flush to device edges. Always include content gutter + safe-area compensation.

### 0.2 Gesture Safety

- If a screen contains vertical scroll, explicitly disable close-on-swipe conflicts:
  - `Telegram.WebApp.disableVerticalSwipes()` (or SwipeBehavior API when available)
- Re-enable only when screen has no internal scroll and closure gesture is desired.

### 0.3 Dynamic Theming (Strict)

- No hardcoded UI colors for surfaces/text/controls.
- All color decisions must originate from Telegram theme vars (`--tg-theme-*`) through our semantic bridge (`--color-*` -> `--am-*`).
- Use `color-mix(...)` for interactive states and translucent layers instead of inventing ad-hoc colors.
- In dark OLED themes: prefer elevation by surface contrast (`secondary/bg`) and borders; avoid heavy drop shadows.

### 0.4 Native Telegram Controls (Must Use)

- Primary screen action: Telegram native `BottomButton` / `MainButton` (when flow is single-primary-action).
- Secondary companion action: Telegram native `SecondaryButton` when API is available.
- Back navigation: Telegram native `BackButton` only; no custom back arrow inside content.
- Settings entry: Telegram native `SettingsButton` when settings context exists.
- If a native API is unavailable in the current client, fallback to UI Kit controls with identical hierarchy.

### 0.5 Performance + Interaction Baseline

- Motion budget: animate only `transform` and `opacity`.
- No layout-thrashing animations (`width/height/padding/top/left`) in critical paths.
- Every interactive control must trigger haptics via `useHaptic` mapping.
- Loading strategy: skeletons first, spinner second (only when skeleton is impossible).

### 0.6 Progressive Enhancement Matrix

Use runtime feature detection for advanced APIs and provide explicit fallback behavior:

| Capability | Preferred API | Fallback |
|---|---|---|
| Fullscreen | `requestFullscreen` | Header/bottom bar color sync |
| Safe area | Telegram safe-area vars | `env(safe-area-inset-*)` bridge |
| Bottom CTA | `BottomButton`/`SecondaryButton` | UI Kit + fixed bottom bar |
| Settings entry | `SettingsButton` | Profile/settings route button |
| Keyboard close | `hideKeyboard` | blur active input |
| Storage (sync) | `CloudStorage` | query cache + backend profile |
| Storage (local) | `DeviceStorage` | IndexedDB/localStorage |
| Secure secrets | `SecureStorage` | session-bound memory + backend token flow |
| Biometric confirm | `BiometricManager` | confirmation dialog + PIN/password flow |

---

## 1. Fundamental Principles

### Telegram-Native

The app must feel like a part of Telegram, not a separate website.

- Colors **only** via CSS variables (`--color-*`). No hardcoded hex/rgb (exception: brand colors like Telegram Blue `#3390EC` for `VerifiedBadge`)
- Semi-transparent tints: `color-mix(in srgb, var(--color-*) N%, transparent)`. Pre-defined tints in `app.css` (`--am-soft-*-bg`)
- `ThemeProvider` auto-syncs with Telegram theme — custom dark mode forbidden
- `body` uses `var(--color-background-secondary)` (see `app.css`)
- Back navigation via `backButton.show()` from TMA SDK, no custom back arrows

### Minimal Information Per Screen

- One screen — one question or one action
- Maximum **4 data points** per list item
- Details — on a separate page via `chevron`

### Financial Data is Sacred

- Balance — **largest** element on screen (`hero` or `title1 bold`)
- `font-variant-numeric: tabular-nums` for **all** numeric displays (NO `proportional-nums` — causes layout shift on live-updating balances)
- Green (`--color-state-success`) — only for profit/income
- Red (`--color-state-destructive`) — only for expense/loss/error
- BigInt arithmetic, format: `50.00 TON` (number + space + symbol)

### Progressive Disclosure

Complex processes are presented as simple macro-stages. Users see the big picture first, details on demand.

- Deal flow: 17 technical statuses → 3 visible stages: **Agreement → Payment → Publication**
- Macro-stage mapper: `src/features/deals/lib/deal-macro-stage.ts`
- `MiniTimeline` (compact horizontal progress): `src/features/deals/components/MiniTimeline.tsx`
- Full `DealTimeline` (vertical, detailed): `src/features/deals/components/DealTimeline.tsx`
- Status aggregation uses exhaustive `satisfies Record<DealStatus, MacroStage>` for compile-time safety

---

## 2. Atmospheric Glass

The visual layer that gives depth and spatial awareness.

### Page Atmosphere

Every page has a subtle radial gradient background via `::before` pseudo-element on `.am-page` / `.am-finance-page` (defined in `app.css`). Implemented in `AppPageShell` (`src/shared/ui/layout/app-page-shell.tsx`).

### Glass Blur Rules

| Element | Blur | Implementation |
|---------|------|----------------|
| Cards / surfaces | `blur(12px)` | `.am-surface-card` — `AppSurfaceCard` |
| Bottom tabs | `blur(20px)` | `.am-bottom-tabs` |
| Headers / navigation | `blur(20px)` | Page headers |
| Modals | `blur(12px)` | Standard glass surface |

**NEVER** use `blur(20px)` for content cards — GPU-expensive on budget Android (~70% of audience). Keep `blur(12px)` for cards, reserve `blur(20px)` for persistent chrome (tabs, headers).

### Deep Gunmetal OLED Theme

Dark mode uses Deep Gunmetal (`#1C1C1E` base), not pure black. OLED-safe with sufficient contrast. Defined in `app.css` dark theme variables.

---

## 3. Tailwind CSS v4 Conventions

The project uses **Tailwind CSS v4** with CSS-first configuration. There is no `tailwind.config.ts`.

### Architecture

| File | Purpose |
|------|---------|
| `src/app/app.css` | Single source of truth: `@import "tailwindcss"`, `@theme {}` bridge tokens, `@layer base/components`, design tokens, `.am-*` classes, keyframes |
| `@telegram-tools/ui-kit/dist/index.css` | UI Kit base styles |

### CSS Import Order (in `main.tsx`)

```
ui-kit.css → app.css
```

### Rules

1. **Utility-first** — prefer Tailwind classes over inline `style={{}}`
2. **`@theme {}` for tokens** — bridge CSS variables to Tailwind utilities
3. **`@layer components {}`** for reusable class patterns (in `app.css`)
4. **`@apply`** only inside `@layer components {}` — never in component files
5. **`data-*` attributes** for state variants: `data-[active=true]:bg-accent`
6. **NO `tailwind.config.ts`** — all config is CSS-based
7. **NO `@keyframes` inside `@theme`** — keyframes go in `app.css`
8. **`joinClasses()`** for conditional class merging (NOT `cn()` or `clsx`)

### Token Example

```css
/* app.css */
@theme {
  --color-accent: var(--color-accent-primary);
  --color-surface: var(--color-background-base);
}
```

---

## 4. Layout System

### Page Anatomy

```
[Telegram Header]           ← managed by Telegram
[Scrollable Content]        ← padding: 16px, overflow-y: auto
[Bottom Tab Bar]            ← fixed, 4 tabs, blur(20px)
```

### Tabs

4 tabs: Catalog | Deals | Wallet | Profile

### Spacing

- Between `<Group>` sections: **16px**
- Page inner padding: **16px**
- Between paired buttons: **12px** gap
- Between form and CTA: **16px**

### Sub-pages

- Navigation: `backButton.show()` / `backButton.hide()` in `useEffect`
- No custom headers with back buttons

---

## 5. Navigation

### Hierarchy

```
Tab → List → Detail → Action Sheet/Dialog
```

### Rules

- `<GroupItem chevron />` — navigates to another page
- `<GroupItem />` without `chevron` — informational (value in `after`)
- `<GroupItem after={<Toggle />} />` — toggle, no `chevron`

### MainButton

For single-action form pages, prefer Telegram native bottom controls:

- `BottomButton` (or `MainButton`) for primary action
- `SecondaryButton` for paired secondary action when supported

`<FixedBottomBar>` remains fallback for unsupported Telegram clients or custom multi-action layouts.

### Filters

Single "Filter" button → `<Sheet>` with options → "Done" button.

---

## 6. Typography

### Scale (UI Kit `<Text type="...">`)

| Type | Size | Use case |
|------|------|----------|
| `hero` | 88px | Wallet balance (main screen) |
| `largeTitle` | 34px | Full-screen empty state heading |
| `title1` | 28px | Balance (compact), detail page heading |
| `title2` | 22px | Form heading, section heading |
| `title3` | 20px | Empty state heading, subheading |
| `body` | 17px | Body text, `GroupItem.after` values |
| `callout` | 16px | Amounts in lists |
| `subheadline1` | 15px | `GroupItem.description` |
| `subheadline2` | 14px | Metadata, timestamps |
| `footnote` | 13px | `Group.footer`, tab labels |
| `caption1` | 12px | Badges, labels |
| `caption2` | 11px | Legal text |

### Three-Level Rule

Maximum **3 typography levels** per viewport.

### Weights

| Weight | Purpose |
|--------|---------|
| `regular` | Body text, descriptions |
| `medium` | Emphasis in lists |
| `bold` | Headings, amounts, balance |

---

## 7. Color System

### Semantic Variables

| Variable | Purpose |
|----------|---------|
| `--color-accent-primary` | CTA buttons, links, active elements |
| `--color-state-success` | Profit, completed deals |
| `--color-state-destructive` | Expense, cancellation, error |
| `--color-state-warning` | Pending, warning |
| `--color-foreground-primary` | Primary text |
| `--color-foreground-secondary` | Secondary text |
| `--color-foreground-tertiary` | Hints, placeholder |
| `--color-background-base` | Card/section background |
| `--color-background-secondary` | Page background |
| `--color-border-separator` | List separators |

### Background Layers

```
page (--color-background-secondary)
  → card (--color-background-base)
    → modal (--color-background-modal)
      → overlay (--color-background-overlay)
```

### Static Colors

`--color-static-white`, `--color-static-black` — for colors that must not invert with theme.

> Never use `--palette-*` directly in components — use only `--color-*`.

---

## 8. Icon Conventions

### Source

All icons from `lucide-react`, re-exported with domain aliases from `src/shared/ui/icons/index.ts`. Custom SVGs for domain-specific icons (post types, TON diamond, verified badge).

### StrokeWidth Rules

| Icon Size | `strokeWidth` | Context |
|-----------|---------------|---------|
| 24px+ | `1.5` | Page headers, empty states |
| 18–22px | `2` (default) | List items, buttons, inline |
| < 18px | `2` | Badges, compact UI |

Pass `strokeWidth` as a prop at usage site — no mass refactor.

### Custom Icons

| Icon | File | Usage |
|------|------|-------|
| `TonDiamondIcon` | `src/shared/ui/icons/ton-diamond-icon.tsx` | TON amount display |
| `VerifiedBadge` | `src/shared/ui/icons/verified-badge.tsx` | Channel verification (hardcoded `#3390EC`) |
| `PulsingDot` | `src/shared/ui/components/pulsing-dot.tsx` | Active status indicator |
| Post type icons | `src/shared/ui/icons/post-type-icons.tsx` | Deal/channel post types |

---

## 9. Cards and Lists

### Standard List Item

```tsx
<GroupItem
  text="Title"
  description="Description"
  before={<Image src={avatar} ... />}
  after={<Text type="callout" color="accent">50 TON</Text>}
  chevron
  onClick={navigateToDetail}
/>
```

### Channel Card — 4 fields max

Avatar + name + subscribers + price. Details on separate page.

### Deal Card — 4 fields + MiniTimeline

Channel avatar + channel name + status badge + MiniTimeline (3 macro-stages).

---

## 10. Escrow Safety

Financial and destructive actions require extra confirmation.

### Rules

- **Destructive actions** (cancel deal, reject offer) → `<DialogModal>` confirmation
- **Payment button** → requires connected wallet + displays commission
- **Deposit/release/refund** → `<DialogModal>` with amount and escrow address
- **Optimistic updates** → NEVER for financial operations (deposit, release, refund)
- **Payment UI** → `networkMode: 'online'`, `staleTime: 0` — never serve from cache

### Pattern

```tsx
<DialogModal
  active={showConfirm}
  title={t('deals.confirm.title')}
  description={t('deals.confirm.cancel')}
  confirmText={t('deals.actions.cancel')}
  closeText={t('common.back')}
  onConfirm={handleCancel}
  onClose={() => setShowConfirm(false)}
/>
```

---

## 11. Forms and Input

### Amounts

```tsx
<Input value={amount} onChange={setAmount} placeholder="0.00" numeric />
```

### Form Structure

```
[Heading (title2 bold)]  →  16px gap  →  [Input fields]  →  16px gap  →  [CTA button]
```

CTA is **always** full-width at the bottom.

---

## 12. Modals and Sheets

| Component | Purpose | Example |
|-----------|---------|---------|
| `<Sheet>` | Content-rich | Filters, previews |
| `<DialogModal>` | Binary yes/no | Payment confirm, deal cancel |
| `<Toast>` | Transient feedback | "Deal created", "Error" |

### Rules

- Never nest modal inside modal (Sheet → DialogModal is the only exception)
- Toast: `success`, `error`, `info`, duration 3s

---

## 13. Empty States

Every list **must** have an empty state with heading + description + CTA button.

| Screen | Title | CTA |
|--------|-------|-----|
| Deals | No deals yet | Find a channel |
| Catalog | Nothing found | Reset filters |
| Wallet | No transactions | Top up |
| Creatives | No creatives | Create creative |
| Channels | No channels | Add channel |

---

## 14. Data Display

### Deal Status

17 statuses mapped to colors via `Record<DealStatus, StatusConfig>` in `src/features/deals/lib/deal-status.ts`.

### Financial Values

| Context | Typography |
|---------|------------|
| Balance (main) | `hero` or `title1 bold` |
| Amount in list | `callout` color `accent` |
| Amount in detail | `title2 bold` |
| Positive change | `callout` color `success` |
| Negative change | `callout` color `destructive` |

`font-variant-numeric: tabular-nums` on **all** numeric content. Format: `50.00 TON`.

### Numbers

- Thousands separator: space (RU locale) — `1 250 000`
- In lists: abbreviations — `125K`, `1.2M`
- Detail page: full numbers — `125 000`

---

## 15. Action Buttons

- **One primary CTA** per viewport
- Loading: `<Button loading />`, not `disabled` + separate spinner
- `pressScale` on **all** interactive elements
- Action pair: Primary (positive) + Secondary (negative), `flex: 1` each with `gap-3`

---

## 16. Animations

All presets in `src/shared/ui/animations.ts`. Import from `motion/react` (NOT `framer-motion`).

### Preset Map

| Preset | When |
|--------|------|
| `fadeIn` | Loading → content fallback |
| `slideUp` | Card/section appearance |
| `scaleIn` | Empty state, modal content |
| `slideFromRight` | Push navigation |
| `slideFromBottom` | Sheet appearance |
| `staggerChildren` + `listItem` | Animated list (max 8–10 items) |
| `pressScale` | All interactive elements (spring stiffness 400, damping 17) |
| `shimmer` | Skeleton loading |
| `pulse` | Active process indicator |
| `toast` | Toast show/hide |

### Rules

- `pressScale` on **all** buttons and clickable cards
- `AnimatePresence mode="wait"` for loading → content
- No animations during scroll — only mount/unmount
- Spring animations via `motion/react` (NOT CSS `linear()` — JS spring is better for interruptible animations)

---

## 17. Haptic Feedback

Use `useHaptic()` from `src/shared/hooks/use-haptic.ts`.

| Method | When | Examples |
|--------|------|---------|
| `selectionChanged()` | Tab switch, segment toggle | `BottomTabs`, `SegmentControl` |
| `impactOccurred('light')` | Card tap, chip, filter | `DealListItem`, `CategoryChipRow` |
| `impactOccurred('medium')` | CTA button press | Payment confirm, major action |
| `notificationOccurred('success')` | Action success | Deal transition, negotiate |
| `notificationOccurred('error')` | Action failure | API error, validation |

### Rules

- Every card/list item `onClick` → `impactOccurred('light')`
- CTA buttons → `impactOccurred('medium')`
- Tab/segment switches → `selectionChanged()`
- Optimistic updates: success haptic fires in `onMutate` (before server response)
- Never double-fire haptic

---

## 18. Loading and Skeletons

| Level | Component |
|-------|-----------|
| App init | `<Spinner size="40px" color="accent" />` centered |
| Section | `<Group skeleton={{ show: isLoading }}>` |
| Custom | `<SkeletonElement>` matching content shape |
| Inline | `<Spinner size="20px">` |
| Button | `<Button loading />` |

Skeleton must mirror future content layout. Transition via `<AnimatePresence mode="wait">`.

---

## 19. Border Radius Tokens

| Element | Radius |
|---------|--------|
| Cards / containers | `14px` |
| CTA buttons | `12px` (UI Kit standard) |
| Inputs | `12px` |
| Chips / pills | `9999px` |
| Avatars | `50%` |
| Timeline highlight | `8px` |
| Small badges | `6px` |

---

## 20. Pre-Release Checklist

- [ ] **Information overload** — max 4 data points per list item
- [ ] **Typography** — max 3 levels per viewport, balance is bold
- [ ] **Single primary CTA** — one primary button per screen
- [ ] **No hardcoded colors** — only `--color-*` vars (except brand colors)
- [ ] **Empty state** — every list has one with CTA
- [ ] **Skeleton** — async content has loading skeleton
- [ ] **Spacing** — 16px between Groups, 12px button gap
- [ ] **Animations** — `pressScale` on interactive, `stagger` on lists
- [ ] **Telegram-native** — theme from Telegram, BackButton from SDK
- [ ] **Financial data** — `tabular-nums`, BigInt, `networkMode: 'online'`
- [ ] **Haptics** — appropriate haptic on every interaction
- [ ] **Glass blur** — 12px for cards, 20px for chrome only
- [ ] **Progressive disclosure** — macro-stages on deal cards, detail on separate page
- [ ] **Escrow safety** — DialogModal for destructive/financial actions
- [ ] **Tailwind** — utility classes, no inline styles for layout
