# Frontend Agent Instructions

## UI Kit: @telegram-tools/ui-kit

iOS-like React component library for Telegram Mini Apps. Version: 0.2.4+

### Setup (already configured)

```tsx
// main.tsx — CSS import MUST be at the top level
import '@telegram-tools/ui-kit/dist/index.css';

// App.tsx — wrap entire app
<ThemeProvider theme={getTelegramTheme()}>
  <ToastProvider>
    {children}
  </ToastProvider>
</ThemeProvider>
```

### Component Reference

| Component | Purpose | Key Props |
|-----------|---------|-----------|
| `Button` | Primary action buttons | `text`, `type: "primary"\|"secondary"`, `icon`, `loading`, `disabled` |
| `Text` | Typography with iOS hierarchy | `type` (see below), `weight`, `color`, `align` |
| `Group` | Sectioned list container (iOS Settings-style) | `header`, `footer`, `action`, `skeleton` |
| `GroupItem` | Row inside Group | `text`, `description`, `before`, `after`, `chevron`, `onClick`, `canDrag` |
| `Input` | Text input with validation | `value`, `onChange(value, event)`, `validator`, `error`, `success`, `numeric` |
| `Select` | Dropdown selector | `options: Option[]`, `value`, `onChange` |
| `Dropdown` | Floating dropdown menu | `options`, `active`, `selectedValue`, `onSelect`, `onClose`, `triggerRef` |
| `Toggle` | Boolean switch | `isEnabled`, `onChange(boolean)`, `disabled` |
| `Image` | Optimized image with fallback | `src`, `fallback`, `aspectRatio`, `objectFit`, `borderRadius` |
| `Icon` | Built-in icon set | `name: "cross"\|"chevron"\|"doubleChevron"\|"check"`, `color`, `size`, `customIcon` |
| `DialogModal` | Confirmation dialog | `active`, `title`, `description`, `confirmText`, `closeText`, `onConfirm`, `onClose` |
| `Sheet` | Bottom sheet (portal) | `sheets: Record<string, Component>`, `activeSheet`, `opened`, `onClose` |
| `Toast` | Notification toasts (via hook) | Use `useToast().showToast(message, { type, duration })` |
| `Spinner` | Loading indicator | `size`, `color: "primary"\|"secondary"\|"accent"\|"white"` |
| `SkeletonElement` | Placeholder while loading | `style`, `className` |

### Text Types (iOS Typography Scale)

`hero` > `largeTitle` > `title1` > `title2` > `title3` > `title4` > `body` > `callout` > `subheadline1` > `subheadline2` > `footnote` > `caption1` > `caption2`

Weights: `light | regular | medium | bold`
Colors: `primary | secondary | tertiary | accent | danger | white`

### Option Type (for Select/Dropdown)

```typescript
type Option = { label: string; value: string | null; image?: string }
```

### Icon Colors

`default | primary | secondary | tertiary | accent | destructive | warning | success`

### Rules

1. **ALWAYS use UI Kit components** instead of native HTML (`<button>`, `<input>`, `<select>`, `<img>`)
2. **ALWAYS import from root**: `import { Button, Text } from '@telegram-tools/ui-kit'`
3. **NEVER create custom components** that duplicate UI Kit functionality
4. **Use `Group` + `GroupItem`** for all list/settings-style layouts (iOS convention)
5. **Use `Text` component** for all text — never raw `<p>`, `<h1>`, `<span>`
6. **Use `useToast` hook** for notifications — never custom toast implementations
7. **Use `Sheet`** for bottom sheets — it renders via portal for proper stacking
8. **Use `DialogModal`** for confirmations — never `window.confirm()`
9. **Use `SkeletonElement`** inside `Group` skeleton prop for loading states
10. **Use `Spinner`** for inline loading, `SkeletonElement` for layout-preserving loading
11. **ThemeProvider** auto-syncs with Telegram theme — do NOT implement custom dark mode
12. **Input.onChange** signature is `(value: string, event)` — NOT `(event)` like native
13. **Toggle.onChange** gives `boolean` directly — NOT event
14. **Icon `customIcon`** accepts ReactElement for custom SVG icons beyond built-in set

### Patterns

#### Settings Page

```tsx
<Group header="Account">
  <GroupItem text="Username" after={<Text type="body" color="secondary">@user</Text>} />
  <GroupItem text="Notifications" after={<Toggle isEnabled={on} onChange={setOn} />} />
  <GroupItem text="Language" chevron onClick={openLang} />
</Group>
```

#### Confirmation Flow

```tsx
<DialogModal
  active={showConfirm}
  title="Confirm Payment"
  description="Send 100 TON to escrow?"
  confirmText="Confirm"
  closeText="Cancel"
  onConfirm={handlePay}
  onClose={() => setShowConfirm(false)}
/>
```

#### Loading State

```tsx
<Group header="Deals" skeleton={{ show: isLoading }}>
  {deals.map(d => <GroupItem key={d.id} text={d.title} chevron />)}
</Group>
```

## Telegram Mini Apps SDK: @telegram-apps/sdk-react

React bindings for TMA client SDK. Version: 3.3.9+

### Setup (already configured)

```tsx
// main.tsx — init BEFORE rendering
import { init } from '@telegram-apps/sdk-react';
init();

// Components — use useSignal + useLaunchParams hooks
import { useLaunchParams, useSignal, useRawInitData } from '@telegram-apps/sdk-react';
import * as backButton from '@telegram-apps/sdk-react'; // namespace import for components
```

### Key APIs

| API | Purpose | Pattern |
|-----|---------|---------|
| `useLaunchParams(true)` | Get launch params (camelCased) | `const lp = useLaunchParams(true)` |
| `useSignal(signal)` | Track signal reactively | `const isVisible = useSignal(backButton.isVisible)` |
| `useRawInitData()` | Raw initData for auth | `const initData = useRawInitData()` |
| `backButton.show()` / `.hide()` | Back navigation | Call in useEffect |
| `mainButton.setParams({text, isVisible})` | Primary CTA | Bind to route/page |
| `hapticFeedback.impactOccurred('medium')` | Tactile feedback | On payment confirm |
| `cloudStorage.setItem(key, value)` | Persistent storage | User preferences |
| `popup.open({title, message, buttons})` | Native popup | Confirmations |
| `closingBehavior.enableConfirmation()` | Prevent accidental close | During active deal flow |
| `viewport.expand()` | Expand to full height | On app init |
| `miniApp.ready()` | Signal app is ready | After init |

### Rules

1. **ALWAYS use SDK hooks** — never access `window.Telegram.WebApp` directly
2. **Initialize in main.tsx** — `init()` must run before React renders
3. **Use `useSignal`** to read signal values in components — signals are NOT React state
4. **Use namespace imports** for components: `import * as backButton from '@telegram-apps/sdk-react'`
5. **Mount components before use**: some require `await viewport.mount()` before reading values
6. **eruda** auto-loads in dev mode for mobile debugging — no manual setup needed

## TON Connect: @tonconnect/ui-react

TON wallet connection for React. Version: 2.4.1+

### Setup (already configured)

```tsx
// App.tsx — TonConnectUIProvider wraps the entire app
<TonConnectUIProvider manifestUrl={TON_MANIFEST_URL}>
  {children}
</TonConnectUIProvider>

// public/tonconnect-manifest.json — served statically
```

### Key APIs

| Hook/Component | Purpose |
|---------------|---------|
| `<TonConnectButton />` | Pre-built connect/disconnect button |
| `useTonConnectUI()` | `[tonConnectUI, setOptions]` — modal control, send transactions |
| `useTonWallet()` | Connected wallet info (account, chain, device) |
| `useTonAddress()` | Shortcut for wallet address |
| `useIsConnectionRestored()` | Whether previous session was restored |

### Transaction Pattern

```tsx
const [tonConnectUI] = useTonConnectUI();
await tonConnectUI.sendTransaction({
  validUntil: Math.floor(Date.now() / 1000) + 300,
  messages: [{
    address: escrowAddress,
    amount: String(amountNano), // nanoTON as string
  }],
});
```

### Rules

1. **Amounts in nanoTON** — `1 TON = 1_000_000_000 nanoTON`, pass as string
2. **Manifest must be publicly accessible** — no auth, no CORS, direct GET
3. **ALWAYS check `useIsConnectionRestored()`** before showing wallet-dependent UI
4. **Never store private keys** — TON Connect handles wallet interaction securely
5. **Use `<TonConnectButton />`** — never build custom connect/disconnect UI

## Conventions

MUST follow these documents:
- [Design Guidelines](./DESIGN_GUIDELINES.md) — layout, typography, colors, animations, anti-patterns checklist
- [TypeScript Conventions](../.memory-bank/17-typescript-conventions.md) — strict TS, testing, performance, financial data rules
- [API Contracts](../.memory-bank/11-api-contracts.md) — endpoints, request/response schemas, pagination, errors
- [Module Architecture](../.memory-bank/15-module-architecture.md) — project structure, dependency rules

## Environment Variables

Defined in `.env.development` / `.env.production`, typed in `src/vite-env.d.ts`:

| Variable | Description | Example |
|----------|-------------|---------|
| `VITE_API_BASE_URL` | Backend API origin | `http://localhost:8080` |
| `VITE_TON_NETWORK` | TON blockchain network | `testnet` / `mainnet` |
| `VITE_TON_MANIFEST_URL` | TON Connect manifest URL (optional) | `https://admarket.app/tonconnect-manifest.json` |

Access: `import.meta.env.VITE_API_BASE_URL`. Vite proxy forwards `/api` → backend in dev.

## API Client (`src/shared/api/`)

### Structure

| File | Purpose |
|------|---------|
| `client.ts` | `api.get/post/put/delete` — typed fetch wrapper with auth and Zod validation |
| `types.ts` | `PaginatedResponse`, `ProblemDetail`, `ApiError`, `AuthResponse` + Zod schemas |
| `query-keys.ts` | TanStack Query key factories: `dealKeys`, `channelKeys`, `creativeKeys`, `disputeKeys`, `authKeys` |
| `index.ts` | Public barrel export |

### Usage

```tsx
import { api, dealKeys } from '@/shared/api';
import { useQuery } from '@tanstack/react-query';
import { z } from 'zod/v4';

const dealSchema = z.object({ id: z.string(), status: z.string(), amountNano: z.number() });

// GET with Zod validation
const { data } = useQuery({
  queryKey: dealKeys.detail(id),
  queryFn: () => api.get(`/deals/${id}`, { schema: dealSchema }),
});

// POST
const mutation = useMutation({
  mutationFn: (body: CreateDealRequest) => api.post('/deals', body, { schema: dealSchema }),
});
```

### Rules

1. **ALWAYS use `api.*` methods** — never raw `fetch()` or `axios`
2. **ALWAYS use query key factories** — inline keys are FORBIDDEN
3. **ALWAYS validate responses with Zod schemas** for any data displayed to users
4. **Auth is automatic** — client attaches JWT from `sessionStorage` or Telegram `initData`
5. **Errors are `ApiError`** with RFC 7807 `ProblemDetail` — handle in `onError` callbacks
6. **Pagination**: pass `{ params: { cursor, limit } }` to `api.get`
7. **Financial queries**: use `{ networkMode: 'online', staleTime: 0 }` — NEVER serve from cache

## General Rules

- Linter/formatter: **Biome** (NOT ESLint). `npm run lint` / `npm run lint:fix` / `npm run format`
- `noDefaultExport` enforced everywhere EXCEPT `src/pages/**` (lazy routes need default exports)
- Path alias: `@/` → `src/`
- Feature-based structure: `src/features/{name}/`
- Shared code: `src/shared/`
- Pages (lazy-loaded): `src/pages/{name}/`
- Server state: TanStack Query (mandatory)
- Client state: Zustand (max 1 store)
- Financial values: BigInt only, never Number
- All strings through i18next `t()` function
- MSW for API mocking in tests
