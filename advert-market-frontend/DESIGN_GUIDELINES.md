# Design Guidelines

> Living design system document for advert-market. Based on analysis of TON Wallet (reference) and ADMINOTEKA (anti-patterns).
> Bound to `@telegram-tools/ui-kit` 0.2.4+ and `animations.ts`.

---

## 1. Fundamental Principles

### Telegram-native

The app must feel like a part of Telegram, not a separate website.

- Colors and backgrounds **only** via UI Kit CSS variables (`--color-*`). No hardcoded hex/rgb
- `ThemeProvider` auto-syncs theme with Telegram — custom dark mode is forbidden
- `body` uses `var(--color-background-secondary)` (see `global.css`)
- Back navigation — via `backButton.show()` from TMA SDK, no custom back arrows

### Minimal Information Per Screen

- One screen — one question or one action
- Maximum **4 data points** per list item (e.g.: avatar + name + subscribers + price)
- Details — on a separate page accessible via `chevron`

### Financial Data is Sacred

- Balance — the **largest** element on screen (`hero` or `title1 bold`)
- `font-variant-numeric: tabular-nums` for all amounts (equal-width digits)
- Green (`--color-state-success`) — only for profit/income
- Red (`--color-state-destructive`) — only for expense/loss/error
- BigInt arithmetic, format: `50.00 TON` (number + space + symbol)

> **Anti-pattern (ADMINOTEKA):** Background darker than Telegram — the app feels foreign. Channel cards are overloaded (price + subscribers + views + rating + 3 tags simultaneously).

---

## 2. Layout System

### Page Anatomy

```
[Telegram Header]           <- managed by Telegram, do not touch
[Scrollable Content]        <- padding: 16px, overflow-y: auto
[Bottom Tab Bar]            <- fixed, 4 tabs
```

### Tabs

4 tabs (not 5 — 5 tabs are cramped on small screens):

| Tab | Icon | Purpose |
|-----|------|---------|
| Catalog | search | Channel discovery |
| Deals | document | Active and completed |
| Wallet | wallet | Balance, history, top-up |
| Profile | person | Settings, channels, statistics |

### Spacing

- Between `<Group>` sections: **16px** (`marginTop: '16px'`)
- Page inner padding: **16px**
- Between paired buttons: **12px** (`gap: '12px'`)
- Between form and CTA: **16px**

### Sub-pages

- Navigation: `backButton.show()` / `backButton.hide()` in `useEffect`
- No custom headers with back buttons

---

## 3. Navigation

### Hierarchy

```
Tab -> List -> Detail -> Action Sheet/Dialog
```

### Rules

- `<GroupItem chevron />` — element navigates to another page
- `<GroupItem />` without `chevron` — informational (value in `after`)
- `<GroupItem after={<Toggle />} />` — toggle switch, no `chevron`

### Filters

**Pattern (Wallet):** Single "Filter" button -> `<Sheet>` with options -> "Done" button at the bottom of the sheet.

```tsx
<Button text="Filter" type="secondary" icon={filterIcon} onClick={openSheet} />
```

> **Anti-pattern (ADMINOTEKA):** Multiple dropdown filters visible simultaneously on the page. Overloads the interface, steals vertical space from content.

---

## 4. Typography

### Scale (UI Kit `<Text type="...">`)

| Type | Size | Use case |
|------|------|----------|
| `hero` | 88px | Wallet balance (main screen) |
| `largeTitle` | 34px | Full-screen empty state heading |
| `title1` | 28px | Balance (compact variant), detail page heading |
| `title2` | 22px | Form heading, section heading on detail |
| `title3` | 20px | Empty state heading, detail subheading |
| `title4` | 18px | Not used in lists, reserved |
| `body` | 17px | Body text, values in `GroupItem.after` |
| `callout` | 16px | Amounts in lists, channel price |
| `subheadline1` | 15px | `GroupItem.description`, secondary info |
| `subheadline2` | 14px | Metadata, timestamps |
| `footnote` | 13px | `Group.footer`, tab labels |
| `caption1` | 12px | Badges, labels |
| `caption2` | 11px | Legal text, fine print |

### Three-Level Rule

Maximum **3 typography levels** in a single viewport. For example:
- `title2` (heading) + `body` (content) + `caption1` (metadata)

### Weights (`weight`)

| Weight | Purpose |
|--------|---------|
| `regular` | Body text, descriptions |
| `medium` | Emphasis in lists (`GroupItem.text`) |
| `bold` | Headings, amounts, balance |

> **Anti-pattern (ADMINOTEKA):** All text in one weight — nothing stands out visually, the eye has nothing to anchor on.

---

## 5. Color System

### Semantic Variables (use in code)

| Variable | Purpose |
|----------|---------|
| `--color-accent-primary` | CTA buttons, links, active elements |
| `--color-state-success` | Profit, completed deals, confirmation |
| `--color-state-destructive` | Expense, cancellation, error, deletion |
| `--color-state-warning` | Pending, warning |
| `--color-foreground-primary` | Primary text |
| `--color-foreground-secondary` | Secondary text, descriptions |
| `--color-foreground-tertiary` | Hints, placeholder |
| `--color-background-base` | Card/section background |
| `--color-background-secondary` | Page background |
| `--color-background-section` | Background inside `Group` |
| `--color-background-modal` | Modal background |
| `--color-background-overlay` | Dimming behind modal/sheet |
| `--color-border-separator` | List separators |
| `--color-link` | Clickable links in text |

### Background Layers (bottom to top)

```
page (--color-background-secondary)
  -> card/section (--color-background-base / --color-background-section)
    -> modal (--color-background-modal)
      -> overlay (--color-background-overlay)
```

Z-index layers: `--z-base` < `--z-content` < `--z-sheet` < `--z-modal` < `--z-overlay` < `--z-toast`

### Static Colors (theme-independent)

`--color-static-white`, `--color-static-black` and the `--color-static-*` palette — for cases where color should not invert (e.g., text over a colored background).

> Never use `--palette-*` variables directly in components — they are low-level. Use only `--color-*`.

---

## 6. Cards and Lists

### Standard List Item

```
[icon/avatar]  [title + subtitle]  ...  [value/action]  [chevron?]
```

Implementation:
```tsx
<GroupItem
  text="Title"
  description="Description"
  before={<Image src={avatar} width="40px" height="40px" borderRadius="50%" />}
  after={<Text type="callout" color="accent">50 TON</Text>}
  chevron
  onClick={navigateToDetail}
/>
```

### Channel Card (marketplace)

**Only 4 fields:** avatar + name + subscribers + price. Everything else (views, ER, topic, description) — on the detail page.

```tsx
<GroupItem
  text={channel.name}
  description={`${formatNumber(channel.subscribers)} subscribers`}
  before={<Image src={channel.avatar} width="40px" height="40px" borderRadius="50%" />}
  after={<Text type="callout" color="accent">{formatTon(channel.price)} TON</Text>}
  chevron
/>
```

### Deal Card

**4 fields:** channel avatar + channel name + post type + status badge.

```tsx
<GroupItem
  text={deal.channelName}
  description={deal.postType}
  before={<Image src={deal.channelAvatar} width="40px" height="40px" borderRadius="50%" />}
  after={<StatusBadge status={deal.status} />}
  chevron
/>
```

> **Anti-pattern (ADMINOTEKA):** 6+ data points in a single card. Dashed borders around statistics. Custom cards instead of `Group`/`GroupItem`.

---

## 7. Forms and Input

### Amounts

```tsx
<Input value={amount} onChange={setAmount} placeholder="0.00" numeric />
```

Validation — Zod schema on blur, error via `error` prop.

### Form Structure

```
[Heading (title2 bold)]
  16px gap
[Input fields]
  16px gap
[CTA button — full-width, primary]
```

CTA button is **always at the bottom** of the form, full-width.

### Components

| Component | When to use |
|-----------|-------------|
| `<Input>` | Text input, amounts (`numeric`) |
| `<Select>` | Inline list selection (within a form) |
| `<Dropdown>` | Contextual options (on trigger click) |
| `<Toggle>` | On/off settings |

---

## 8. Modals and Sheets

### When to Use What

| Component | Purpose | Example |
|-----------|---------|---------|
| `<Sheet>` | Content-rich interactions | Filters, channel preview, list selection |
| `<DialogModal>` | Binary decisions (yes/no) | Payment confirmation, deal cancellation |
| `<Toast>` (via `useToast`) | Transient feedback | "Deal created", "Payment error" |

### Rules

- **Never** nest a modal inside a modal (Sheet -> DialogModal is the only acceptable exception for confirmations within a sheet)
- Sheet closes by swiping down or "Done"/"Close" button
- DialogModal — only `onConfirm` + `onClose`, no additional buttons
- Toast: `success` (green), `error` (red), `info` (neutral), duration 3 sec

---

## 9. Empty States

### Required Structure

Every list **must** have an empty state:

```tsx
<motion.div {...scaleIn} style={{ textAlign: 'center', padding: '40px 20px' }}>
  <Text type="title1" style={{ fontSize: '48px' }}>
    {emoji}
  </Text>
  <Text type="title3" weight="bold" style={{ marginTop: '16px' }}>
    {title}           {/* 3-5 words */}
  </Text>
  <Text type="body" color="secondary" style={{ marginTop: '8px' }}>
    {description}     {/* 1 sentence */}
  </Text>
  <div style={{ marginTop: '24px' }}>
    <motion.div {...pressScale}>
      <Button text={ctaText} type="primary" />
    </motion.div>
  </div>
</motion.div>
```

### Empty States Table

| Screen | Emoji | Title | CTA |
|--------|-------|-------|-----|
| Deals | :mailbox_with_no_mail: | No deals yet | Find a channel |
| Catalog (search) | :mag: | Nothing found | Reset filters |
| Wallet (history) | :scroll: | No transactions | Top up |
| Creatives | :art: | No creatives | Create creative |
| Channels (profile) | :satellite_antenna: | No channels | Add channel |
| Disputes | :balance_scale: | No disputes | — (no CTA) |

> **Anti-pattern (ADMINOTEKA):** Empty state without a CTA button — user hits a dead end.

---

## 10. Data Display and Statuses

### Deal Status Badge

Mapping of 16 statuses to colors (implemented as an exhaustive `Record<DealStatus, StatusConfig>`):

| Group | Statuses | Color (Text `color`) |
|-------|----------|----------------------|
| New | `CREATED`, `NEGOTIATION` | `accent` |
| Awaiting payment | `PENDING_DEPOSIT`, `DEPOSIT_CONFIRMING` | `secondary` |
| In progress | `ACTIVE`, `CONTENT_SUBMITTED`, `CONTENT_REVIEW` | `accent` |
| Publication | `SCHEDULED`, `PUBLISHED`, `STATS_COLLECTING` | `accent` |
| Completion | `COMPLETED`, `PAYOUT_PENDING`, `PAYOUT_SENT` | success (green) |
| Issues | `DISPUTED` | `danger` |
| Cancelled | `CANCELLED`, `REFUNDED` | `secondary` |

### Financial Values

| Context | Typography | Example |
|---------|------------|---------|
| Balance (main) | `hero` or `title1 bold` | **1 250.00 TON** |
| Amount in list | `callout` color `accent` | 50.00 TON |
| Amount in detail | `title2 bold` | 50.00 TON |
| Change (positive) | `callout` color success | +10.00 TON |
| Change (negative) | `callout` color danger | -5.00 TON |

Style: `font-variant-numeric: tabular-nums`. Format: number + space + symbol (`50.00 TON`).

### Numbers

- Thousands separator: space (RU locale) — `1 250 000`
- In lists: abbreviations — `125K`, `1.2M`
- On detail page: full numbers — `125 000`

### Channel Statistics

Via `<GroupItem>`, not custom cards:

```tsx
<Group header="Statistics">
  <GroupItem text="Subscribers" after={<Text type="body" weight="medium">125 000</Text>} />
  <GroupItem text="Post reach" after={<Text type="body" weight="medium">45 000</Text>} />
  <GroupItem text="ER" after={<Text type="body" weight="medium">3.6%</Text>} />
</Group>
```

> **Anti-pattern (ADMINOTEKA):** Statistics in custom cards with dashed borders and inconsistent styles.

---

## 11. Action Buttons

### Rules

- **One primary CTA** per viewport maximum
- Loading: `<Button loading />`, not `disabled` + separate `<Spinner>`

### Single Action

```tsx
<motion.div {...pressScale}>
  <Button text="Confirm" type="primary" />
</motion.div>
```

### Action Pair

Primary (positive) + Secondary (negative), both `flex: 1`:

```tsx
<div style={{ display: 'flex', gap: '12px' }}>
  <motion.div {...pressScale} style={{ flex: 1 }}>
    <Button text="Decline" type="secondary" />
  </motion.div>
  <motion.div {...pressScale} style={{ flex: 1 }}>
    <Button text="Accept" type="primary" />
  </motion.div>
</div>
```

### Quick Actions (wallet)

Row of circular icon buttons with labels (Wallet pattern: Top Up / Withdraw / Transfer):

```tsx
<div style={{ display: 'flex', justifyContent: 'center', gap: '24px' }}>
  {actions.map(action => (
    <motion.div {...pressScale} key={action.id} style={{ textAlign: 'center' }}>
      <div style={{
        width: '56px', height: '56px', borderRadius: '50%',
        backgroundColor: 'var(--color-accent-primary)',
        display: 'flex', alignItems: 'center', justifyContent: 'center'
      }}>
        <Icon customIcon={action.icon} color="default" />
      </div>
      <Text type="caption1" style={{ marginTop: '8px' }}>{action.label}</Text>
    </motion.div>
  ))}
</div>
```

---

## 12. Animations

All presets are defined in `src/shared/ui/animations.ts`. Use via `motion/react`.

### Preset-to-Use-Case Mapping

| Preset | Parameters | When to use |
|--------|-----------|-------------|
| `fadeIn` | opacity 0->1, 200ms | Loading -> content transition (fallback) |
| `slideUp` | opacity + y:20->0, 250ms | Card/section appearance |
| `slideDown` | opacity + y:-20->0, 250ms | Dropdown notifications |
| `scaleIn` | opacity + scale:0.9->1, 200ms | Empty state, modal content |
| `slideFromRight` | opacity + x:30->0, 250ms | Push navigation (new page) |
| `slideFromBottom` | y:100%->0, 300ms, custom ease | `<Sheet>` appearance |
| `staggerChildren` | stagger: 50ms | Container for animated list |
| `listItem` | opacity + x:-10->0, 200ms | Each item in a stagger list |
| `tapScale` | scale->0.97, 100ms | Light press feedback (small elements) |
| `pressScale` | scale->0.95, spring | Primary press feedback (buttons, cards) |
| `shimmer` | backgroundPosition cycle, 1.5s loop | Skeleton elements during loading |
| `pulse` | opacity 1->0.5->1, 1.5s loop | Active process indicator |
| `toast` | opacity + y:-40->0 + scale:0.95->1, 250ms | Toast show/hide |

### Rules

- `pressScale` on **all** interactive elements (buttons, cards with `onClick`)
- `staggerChildren` + `listItem` for lists on first load (maximum **8-10** items, beyond that — no animation)
- `AnimatePresence mode="wait"` for loading -> content transitions
- **No animations** during scroll — only on mount/unmount
- Sheet: `slideFromBottom`, Dialog: `scaleIn`, Toast: `toast`

### Example: Animated List

```tsx
<motion.div {...staggerChildren} initial="initial" animate="animate">
  <Group header="Channels">
    {channels.map(ch => (
      <motion.div key={ch.id} {...listItem}>
        <GroupItem text={ch.name} chevron />
      </motion.div>
    ))}
  </Group>
</motion.div>
```

---

## 13. Loading and Skeletons

### Loading Hierarchy

| Level | Component | When |
|-------|-----------|------|
| App initialization | `<Spinner size="40px" color="accent" />` centered | First launch only |
| Section | `<Group skeleton={{ show: isLoading }}>` | Section data loading |
| Custom card | `<SkeletonElement>` matching content shape | Complex layouts |
| Inline | `<Spinner size="20px">` | Inside a button or row |
| Button | `<Button loading />` | Form submission |

### Rules

- **Never** show a blank screen — always a placeholder matching the content shape
- Skeleton must mirror the layout of future content (height, width, position)
- Skeleton -> content transition via `<AnimatePresence mode="wait">`

### Example

```tsx
<AnimatePresence mode="wait">
  {isLoading ? (
    <motion.div key="skeleton" {...fadeIn}>
      <Group skeleton={{ show: true }}>
        <GroupItem text="" />
        <GroupItem text="" />
        <GroupItem text="" />
      </Group>
    </motion.div>
  ) : (
    <motion.div key="content" {...scaleIn}>
      <Group header="My Deals">
        {deals.map(d => <GroupItem key={d.id} text={d.title} chevron />)}
      </Group>
    </motion.div>
  )}
</AnimatePresence>
```

---

## 14. Pre-Release Screen Checklist

Before every PR that adds or modifies a screen, verify:

- [ ] **Information overload** — no more than 4 data points per list item
- [ ] **Typographic hierarchy** — maximum 3 typography levels in viewport, balance/amounts are bold
- [ ] **Single primary CTA** — no more than one primary button on screen
- [ ] **Hardcoded colors** — no hex/rgb in code, only `--color-*` variables or Text `color` prop
- [ ] **Empty state** — every list has an empty state with CTA
- [ ] **Skeleton** — every async content has a loading skeleton matching its shape
- [ ] **Spacing** — 16px between `Group` sections, 12px gap between paired buttons
- [ ] **Animations** — `pressScale` on interactive elements, `staggerChildren` on lists
- [ ] **Telegram-native** — background from Telegram, BackButton instead of custom arrows
- [ ] **Financial data** — `tabular-nums`, BigInt, `networkMode: 'online'`, format `XX.XX TON`
