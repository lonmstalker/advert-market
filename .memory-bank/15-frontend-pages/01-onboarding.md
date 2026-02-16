# Onboarding

> Direct-replace onboarding flow for Telegram Mini App.  
> Current UX contract: **welcome (+ locale/currency sheet) -> interest -> tour (3 slides)** with **soft-gated tasks**, **sticky primary CTA**, **skip confirm** (tour only), and **role-aware finish route**.

## Routes

```
/onboarding -> /onboarding/interest -> /onboarding/tour -> redirect
```

Guard entry remains unchanged:
- Onboarding is shown only when `onboardingCompleted == false`.
- Completion still calls `PUT /api/v1/profile/onboarding`.

## Step 1 — Welcome

| Field | Value |
|---|---|
| Route | `/onboarding` |
| Goal | Explain value in <10s and start onboarding |
| Primary action | `onboarding.welcome.start` (opens locale/currency sheet) |
| Secondary action | None |

### UX Contract

- Uses shared `OnboardingShell` with unified layout and sticky footer.
- Top-right language icon/trigger is removed.
- Primary CTA is rendered in sticky footer (not Telegram MainButton).
- Tracks analytics:
  - `onboarding_view(step=welcome)`
  - `onboarding_primary_click(step=welcome)`

## Step 1.1 — Locale & Currency (Bottom Sheet)

| Field | Value |
|---|---|
| Surface | In-flow bottom sheet opened from Welcome CTA |
| Goal | Make locale and currency explicit without adding a route |
| Primary action | `onboarding.locale.continue` |
| Secondary action | None (`Skip` is not available) |

### UX Contract

- Sheet is prefilled from profile settings.
  - `languageCode` is seeded from Telegram `language_code` on first login only.
  - `displayCurrency` + `currencyMode` come from profile state.
  - Frontend source of truth is `settings-store.languageCode` (not raw `i18n.language`).
- Two selector rows are shown:
  - Language
  - Currency (`AUTO` by language or manual currency selection)
- `AUTO` behavior:
  - language update may change effective currency from backend mapping.
  - UI shows temporary undo banner when currency changes due to `AUTO`.
- `MANUAL` behavior:
  - language change does not overwrite selected currency.
  - microcopy and `Reset to auto` action are shown.
- Continue closes sheet and navigates to `/onboarding/interest`.
- Continue is disabled while locale/currency mutation is pending.

### Analytics

- `locale_step_shown`
- `locale_continue`
- `language_changed`
- `currency_mode_changed`
- `currency_changed`

## Step 2 — Interest Selection

| Field | Value |
|---|---|
| Route | `/onboarding/interest` |
| Goal | Capture user role intent |
| Role cards | `advertiser`, `owner` |
| Continue button | Enabled when at least one role selected |

### UX Contract

- Multi-select role cards are preserved.
- `selectionChanged` haptic is fired on role toggle.
- `role_selected` analytics fires with resolved role:
  - `advertiser`, `owner`, `both`.
- Continue action sets tour slide to `0` and navigates to `/onboarding/tour`.

## Step 3 — Tour (3 slides)

| Slide | Task | Completion signal |
|---|---|---|
| Catalog | Open channel detail | `taskStatus.completed` |
| Deal | Tap Approve | `taskStatus.completed` |
| Wallet | Open escrow info | `taskStatus.completed` |

### Soft-Gated Behavior (Current)

- Next is always enabled on every slide.
- Task completion is recommended, not mandatory.
- Task status line is shown for slides 1-2:
  - `onboarding.tour.taskStatus.recommended`
  - `onboarding.tour.taskStatus.completed`
- Dot progress indicator remains visual-only (no hard gate on dots).

### Skip / Finish Contract

- `Skip tutorial` action is always visible in tour header.
- Skip opens confirmation dialog (`skipConfirm` keys).
- Skip confirmation and Finish both run the same completion pipeline:
  1. `PUT /api/v1/profile/onboarding` with `interests: string[]`
  2. update profile cache
  3. navigate by role resolver
  4. reset onboarding local session state

### Role-Aware Finish

Resolved primary role:

```ts
type OnboardingPrimaryRole = 'advertiser' | 'owner' | 'both';
```

Route resolver:

```ts
owner -> /profile/channels/new
advertiser|both -> /catalog
```

Finish button text:

- owner: `onboarding.tour.finishOwner`
- advertiser/both: `onboarding.tour.finishAdvertiser`

### Completion Error Handling

- Unified on tour page:
  - inline error state in footer (`onboarding.tour.error`)
  - retry via the same primary button action.

## Shared State and Resume

Store: `src/features/onboarding/store/onboarding-store.ts`

### Persisted in `sessionStorage`

- `interests`
- `activeSlide`
- `completedTasks`

Storage key:

```ts
const ONBOARDING_RESUME_KEY = 'am_onboarding_resume_v1';
```

### Store API (current additions)

- `setActiveSlide(slideIndex)`
- `getTaskState(slideIndex): 'pending' | 'completed'`
- `getPrimaryRole(): OnboardingPrimaryRole`
- `rehydrateFromSession()`
- `reset()` (also clears session storage)

## API Contract (unchanged)

```
PUT /api/v1/profile/onboarding
```

Payload:

```json
{
  "interests": ["advertiser", "owner"]
}
```

Notes:
- Backend contract remains unchanged in this redesign.
- No analytics server contract introduced in this iteration.

## Analytics (frontend typed hook)

Module:
- `src/shared/lib/analytics/onboarding.ts`

Events:
- `onboarding_view`
- `onboarding_primary_click`
- `onboarding_skip`
- `role_selected`
- `tour_task_complete`
- `onboarding_complete`

Transport:
- No-op by default.
- Dev mode logs diagnostics to console.

## Layout and Safe Area

Tokens in `src/app/global.css`:
- `--am-onboarding-max-width`
- `--am-onboarding-page-padding`
- `--am-onboarding-footer-padding-bottom`
- safe area via `--am-safe-area-bottom`

Behavior:
- Sticky footer always padded by safe area.
- Desktop and mobile use the same shell contract with tokenized spacing.
- Locale/currency sheet keeps bottom CTA within safe area constraints.

## Accessibility and Motion

- Language action target >= 44x44.
- Task status uses `aria-live="polite"`.
- Heavy/infinite effects replaced by bounded animations:
  - no infinite logo spin loop
  - no box-shadow pulse animation for timeline
  - motion-safe behavior for reduced-motion users.

## Test Coverage (expected baseline)

- Unit:
  - Welcome/Interest/Tour page contracts
  - store persistence/resume/reset and route resolver
  - slide-level task completion UI behavior
- E2E:
  - advertiser flow -> `/catalog`
  - owner flow -> `/profile/channels/new`
  - both flow -> `/catalog`
  - skip confirm from tour
  - tour progression without hard-gating.
