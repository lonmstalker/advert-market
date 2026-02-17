# Tailwind CSS v4 Migration Incident (Case Study)

Date: 2026-02-16  
Type: Frontend incident post-migration

This file is a historical incident record.

For active debugging and ongoing incidents, use the canonical playbook:
[`./troubleshooting/frontend-troubleshooting.md`](./troubleshooting/frontend-troubleshooting.md).

## Incident Summary

After Tailwind CSS v4 migration, the UI showed widespread visual regressions:
- Utility spacing classes appeared ignored
- Avatars became invisible
- Dark theme contrast degraded
- Multiple layout and mobile interaction issues surfaced

## Primary Root Cause

### CSS Cascade Layers Conflict

Tailwind v4 utilities are layered (`@layer utilities`), while UI Kit CSS had unlayered global reset rules.  
By CSS cascade layer specification, unlayered rules override layered ones.

Representative conflict:

```css
/* Unlayered third-party reset (wins) */
* { padding: 0; margin: 0; box-sizing: border-box; }

/* Layered Tailwind utility (loses) */
@layer utilities {
  .p-4 { padding: 1rem; }
}
```

### Corrective Fix

- Added Vite transform plugin `cssLayerWrap('@telegram-tools/ui-kit', 'ui-kit')` in `vite.config.ts`
- Declared explicit layer order in `src/app/app.css`:

```css
@layer ui-kit, theme, base, components, utilities;
```

Important implementation detail:
- CSS `@import ... layer(...)` alone was insufficient, because package JS entrypoints imported CSS directly, producing extracted chunks outside manual CSS imports.

## Secondary Findings and Fixes

### Theme Token Gaps

- Missing token: `--am-avatar-lightness` caused transparent/invisible avatars
- Fix: token defined in `:root` and dark theme block

### Dark Theme Contrast

- Foreground tokens were too dim for key content
- Border/separator tokens were too subtle
- `--am-tabbar-border` dark override was missing
- Fix: adjusted semantic foreground and border tokens for dark mode

### Layout and Mobile Ergonomics

- `fieldset` min-content behavior broke horizontal scroll in category chips
- Class name mismatch (`am-filter-button` vs `.am-filter-btn`) left filter button unstyled
- Missing `min-w-0`/`truncate` patterns caused text overflow in rows
- Small inputs and toolbar controls reduced touch ergonomics on mobile

## Product-Specific Outcomes (Not Universal Rules)

These were intentional product/UI outcomes from this incident, retained here as historical context:

- Removed non-native `<AppSectionHeader>` usage on selected pages
- Simplified wallet page by removing irrelevant quick actions/segment control
- Compacted profile hero density
- Reduced spacing on settings pages for better information density

## Summary of Files Modified During Incident

| File | Fix |
|---|---|
| `vite.config.ts` | `cssLayerWrap` plugin wrapping UI Kit CSS in `@layer ui-kit` |
| `src/app/app.css` | Layer order, avatar lightness, dark theme contrast, input sizing |
| `src/features/channels/components/CategoryChipRow.tsx` | `min-w-0` on fieldset for scroll |
| `src/pages/catalog/components/CatalogSearchBar.tsx` | Class name mismatch fix for filter button |
| `src/features/channels/components/ChannelCatalogCard.tsx` | Metric emphasis color adjustment |
| `src/pages/deals/DealsPage.tsx` | Header simplification and filter placement |
| `src/pages/wallet/WalletPage.tsx` | Header/segment/quick actions removal |
| `src/features/wallet/components/TransactionListItem.tsx` | Text overflow and icon contrast |
| `src/pages/profile/components/ProfileHero.tsx` | Avatar/padding compaction |
| `src/pages/profile/ProfilePage.tsx` | Section gap reduction |
| `src/pages/profile/NotificationsPage.tsx` | Header removal and spacing reduction |

## Lessons Carried Into the Universal Playbook

Generalized practices from this incident are now maintained in:
[`./troubleshooting/frontend-troubleshooting.md`](./troubleshooting/frontend-troubleshooting.md)

Key promoted patterns:
1. Layer third-party CSS when using Tailwind v4 layered utilities.
2. Treat every `var(--am-*)` reference as a token contract.
3. Validate dark mode explicitly for foreground and separator semantics.
4. Use mobile-safe overflow and tap-target patterns by default.
