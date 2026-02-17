# Frontend Troubleshooting Playbook

Canonical troubleshooting entrypoint for frontend incidents in `advert-market-frontend`.

For the historical Tailwind v4 migration incident, see the case study:
[`../css-issues-tailwind-v4-migration.md`](../css-issues-tailwind-v4-migration.md).

## Scope and Usage

Use this playbook for recurring frontend problems across:
- CSS cascade and Tailwind v4 behavior
- Theme tokens and dark mode readability
- Layout and overflow issues on mobile
- Runtime differences between Telegram, web fallback, and E2E mode
- Build/tooling regressions caused by configuration drift

How to use:
1. Start from the `Fast Triage Matrix` and pick the closest symptom.
2. Run `Quick Checks` exactly as written.
3. Apply the minimal fix and run `Verification`.
4. If unresolved, prepare an `Escalation Packet`.

## Fast Triage Matrix

| Symptom | First Check | Likely Root Cause | Fix Path |
|---|---|---|---|
| Tailwind spacing/utilities do not apply | `vite.config.ts` has `cssLayerWrap` and `app.css` has layer order | Unlayered third-party reset overrides layered utilities | [CSS Cascade & Tailwind v4](#css-cascade--tailwind-v4) |
| Avatar is transparent/invisible | Search for missing `--am-*` variable in `app.css` | Token used in component but not defined in theme vars | [Theme Tokens and Dark Mode](#theme-tokens-and-dark-mode) |
| Dark theme text/borders are low-contrast | Inspect dark block overrides for semantic tokens | Dark overrides incomplete or too subtle | [Theme Tokens and Dark Mode](#theme-tokens-and-dark-mode) |
| Horizontal chip row does not scroll | Check if container is `<fieldset>` without `min-w-0` | Browser `fieldset` min-content behavior blocks overflow | [Layout, Overflow, and Mobile Constraints](#layout-overflow-and-mobile-constraints) |
| Row text/amount overflows on narrow screens | Check flex children for `min-w-0` and `truncate` | Flex min-width defaults prevent shrink/truncation | [Layout, Overflow, and Mobile Constraints](#layout-overflow-and-mobile-constraints) |
| Issue reproduces only outside Telegram | Check SDK init fallback and viewport CSS vars | Telegram runtime vars unavailable in web fallback | [Runtime/Environment (Telegram vs Web vs E2E)](#runtimeenvironment-telegram-vs-web-vs-e2e) |
| E2E visuals differ from local dev | Check `mode === 'e2e'` branch in Vite config | E2E disables `mkcert` and `eruda`, changing runtime behavior | [Runtime/Environment (Telegram vs Web vs E2E)](#runtimeenvironment-telegram-vs-web-vs-e2e) |
| Build passes, but styles are inconsistent after changes | Validate Tailwind v4 CSS-first rules | Config drift (e.g. adding `tailwind.config.ts`, broken import order) | [Build/Tooling Regressions](#buildtooling-regressions) |

## CSS Cascade & Tailwind v4

### Entry: Tailwind utilities are not taking effect

- Symptom:
  - Spacing, background, badge, and utility classes appear ignored.
- Quick Checks:
  - `rg -n "cssLayerWrap\\(|@telegram-tools/ui-kit" vite.config.ts`
  - `rg -n "^@layer ui-kit, theme, base, components, utilities;" src/app/app.css`
  - `sed -n '1,40p' src/main.tsx` and confirm CSS import order is `ui-kit.css -> app.css`
- Root Cause:
  - Unlayered third-party CSS (notably universal `*` resets) beats Tailwind utilities because Tailwind utilities are layered (`@layer utilities`).
- Fix:
  - Keep `cssLayerWrap('@telegram-tools/ui-kit', 'ui-kit')` in `vite.config.ts`.
  - Keep explicit layer order in `src/app/app.css`:
    - `@layer ui-kit, theme, base, components, utilities;`
- Prevention:
  - Wrap any third-party package CSS in a named layer before introducing it.
  - Never assume CSS `@import ... layer(...)` can capture CSS emitted by JS package entrypoints.
- Verification:
  - Confirm utility classes (`p-*`, `m-*`, `gap-*`, `bg-*`) visibly apply on affected screens.

### Entry: Utility class exists but component still looks unstyled

- Symptom:
  - One element remains unstyled while neighboring elements are correct.
- Quick Checks:
  - `rg -n "am-filter-button|am-filter-btn" src/`
- Root Cause:
  - JSX/CSS class name mismatch.
- Fix:
  - Align class names across JSX and CSS.
- Prevention:
  - Co-locate class name constants or keep component-level naming conventions strict.
- Verification:
  - Element adopts expected visual style in both light and dark themes.

## Theme Tokens and Dark Mode

### Entry: Missing token variable breaks visuals

- Symptom:
  - Element becomes transparent, colorless, or defaults unexpectedly.
- Quick Checks:
  - `rg -n "var\\(--am-" src/`
  - `rg -n "--am-avatar-lightness|--am-" src/app/app.css`
- Root Cause:
  - Token referenced in component styles but missing in `:root` and/or theme override blocks.
- Fix:
  - Define token in `:root`.
  - Add corresponding dark theme override where needed.
- Prevention:
  - Treat every new `var(--am-*)` usage as a contract requiring definition in `app.css`.
- Verification:
  - Affected element renders correctly in light and dark themes.

### Entry: Dark theme has weak contrast or invisible borders

- Symptom:
  - Secondary text blends into background; separators and tabbar borders disappear.
- Quick Checks:
  - `rg -n "color-foreground-secondary|color-foreground-tertiary|color-border-separator|am-tabbar-border" src/app/app.css`
- Root Cause:
  - Dark-mode token overrides are incomplete or values are too close to background luminance.
- Fix:
  - Increase contrast for `--color-foreground-secondary` and `--color-foreground-tertiary`.
  - Ensure dark override exists for `--color-border-separator` and `--am-tabbar-border`.
- Prevention:
  - Validate all semantic foreground and border tokens in dark mode whenever introducing new surfaces.
- Verification:
  - Readability and separators remain visible on channel cards, transaction rows, and settings surfaces.

## Layout, Overflow, and Mobile Constraints

### Entry: Horizontal scroll fails in chip/filter groups

- Symptom:
  - Chip row does not scroll horizontally on small screens.
- Quick Checks:
  - Inspect container tag; if `<fieldset>`, verify `min-w-0` is applied.
- Root Cause:
  - `<fieldset>` defaults to `min-width: min-content`, blocking overflow mechanics.
- Fix:
  - Add `min-w-0` to `<fieldset>` or replace with `<div role="group">`.
- Prevention:
  - Avoid `<fieldset>` for horizontal scroll containers unless min-width is explicitly controlled.
- Verification:
  - Chip row scrolls with touch drag on narrow viewport.

### Entry: Text and amounts overflow in rows/cards

- Symptom:
  - Titles or amounts overflow outside cards on narrow devices.
- Quick Checks:
  - Confirm flex children have `min-w-0` and text nodes use `truncate` where appropriate.
- Root Cause:
  - Flex child cannot shrink due to min-content sizing.
- Fix:
  - Add `min-w-0` to shrinking flex containers and `truncate` to bounded text nodes.
- Prevention:
  - Treat `min-w-0` as mandatory for any horizontal row with flexible content.
- Verification:
  - No clipping/overflow for long titles and amounts on narrow screens.

### Entry: Inputs are hard to tap or trigger iOS zoom

- Symptom:
  - Tap targets are too small; iOS zoom appears during input focus.
- Quick Checks:
  - Inspect control height and input font size in `src/app/app.css`.
- Root Cause:
  - Control sizes below mobile ergonomics baseline; font size below iOS zoom threshold.
- Fix:
  - Keep interactive heights at least `44px` (prefer `48px` for form controls).
  - Use `font-size: 16px` on text inputs/textarea for iOS compatibility.
- Prevention:
  - Apply mobile interaction contract before visual polish.
- Verification:
  - Comfortable touch interaction; no unexpected zoom on iOS.

## Runtime/Environment (Telegram vs Web vs E2E)

### Entry: Layout issue reproduces only in web fallback

- Symptom:
  - Height/safe-area behavior differs outside Telegram container.
- Quick Checks:
  - Review fallback in `src/main.tsx` (`bindWebViewportCssVarsFallback`).
  - Confirm CSS uses `--tg-viewport-*` and safe-area fallback vars from `src/app/app.css`.
- Root Cause:
  - Telegram runtime vars are unavailable outside app container; fallback binding may be missing or incomplete.
- Fix:
  - Keep/repair fallback viewport CSS var binding for non-Telegram environments.
- Prevention:
  - Test key pages in both Telegram and plain web.
- Verification:
  - Bottom bars and CTA areas remain visible in both environments.

### Entry: E2E run behavior differs from local dev

- Symptom:
  - Visual or interaction differences appear only under Playwright.
- Quick Checks:
  - Review `mode === 'e2e'` conditional in `vite.config.ts`.
  - Confirm `eruda` is disabled in E2E path in `src/main.tsx`.
- Root Cause:
  - E2E runtime intentionally disables `mkcert` and dev overlays to stabilize tests.
- Fix:
  - Validate behavior under `npm run test:e2e` assumptions before changing runtime code.
- Prevention:
  - Keep explicit comments and mode checks near runtime branches.
- Verification:
  - E2E scenario is deterministic across reruns.

## Build/Tooling Regressions

### Entry: Tailwind configuration drift after migration

- Symptom:
  - Build succeeds, but expected utilities/themes do not behave as project conventions define.
- Quick Checks:
  - `rg --files | rg "tailwind\\.config\\.(js|cjs|mjs|ts)$"`
  - `rg -n "@import \\\"tailwindcss/theme\\\"|@import \\\"tailwindcss/utilities\\\"" src/app/app.css`
- Root Cause:
  - Project expects Tailwind v4 CSS-first setup in `app.css`; extra config files or altered imports cause drift.
- Fix:
  - Remove unsupported `tailwind.config.*` usage.
  - Keep Tailwind imports and theme tokens in `src/app/app.css` as source of truth.
- Prevention:
  - Follow Tailwind rules in `DESIGN_GUIDELINES.md` and `AGENTS.md`.
- Verification:
  - Utilities, tokens, and component layers behave consistently across pages.

### Entry: Styling changed after dependency/package updates

- Symptom:
  - New package version introduces unexpected global style behavior.
- Quick Checks:
  - Inspect lockfile diff and verify package CSS entrypoints.
  - Confirm third-party CSS is layered via Vite transform if needed.
- Root Cause:
  - Dependency introduced/changed unlayered global styles.
- Fix:
  - Add/adjust `cssLayerWrap` for the affected package.
- Prevention:
  - Treat dependency CSS updates as high-risk for cascade regressions.
- Verification:
  - No regressions in spacing/background/utility application.

## Verification Checklist

- Confirm affected issue in both `light` and `dark` themes.
- Confirm behavior in Telegram runtime and plain web fallback when relevant.
- Confirm behavior in E2E mode for runtime-sensitive changes.
- Check for class-name mismatch (`rg` search) when only one element is broken.
- Check for missing `--am-*` tokens when color/visibility is wrong.
- Check overflow (`min-w-0`, `truncate`) on narrow viewport for list rows/cards.
- Validate no unexpected layer order changes in `src/app/app.css`.
- Validate `cssLayerWrap` remains in `vite.config.ts` for UI Kit CSS.

## Escalation Packet

When escalating unresolved frontend incidents, include:
- Symptom statement (one sentence) and exact page/route.
- Reproduction environment: Telegram / web / E2E, browser, OS, device.
- Theme and viewport: light/dark, dimensions, safe-area behavior.
- Visual evidence: screenshot or short video.
- Changed files in branch (`git status --short`).
- Quick check outputs used from this playbook.
- Suspected boundary: CSS layer, tokens, layout, runtime, or build config.

## Related Docs and Case Studies

- Tailwind migration incident case study:
  [`../css-issues-tailwind-v4-migration.md`](../css-issues-tailwind-v4-migration.md)
- Tailwind conventions and design rules:
  [`../../DESIGN_GUIDELINES.md`](../../DESIGN_GUIDELINES.md)
- Runtime sources of truth:
  - `src/app/app.css`
  - `vite.config.ts`
  - `src/main.tsx`
