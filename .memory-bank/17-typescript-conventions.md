# TypeScript Conventions

> Mandatory reference for all frontend code in advert-market. React 19 + Vite 7 + TypeScript.

## 1. Testing

### Test Layers

| Level | Tool | Scope |
|---|---|---|
| Unit | Vitest + Testing Library | Components, hooks, utils |
| Integration | Vitest + MSW | API interaction, TanStack Query |
| E2E | Playwright | Critical user flows |

### Mandatory 100% Coverage

- `shared/lib/ton-*` (financial utilities)
- Zod schemas for API responses
- Deal state machine UI logic

### API Test Rules

- MSW is mandatory for API tests
- Direct `fetch` mocking is FORBIDDEN

## 2. Performance

### Bundle

- Route-level `lazy()` for code splitting
- Vendor chunks: react, query, tma, i18n
- Initial bundle target: < 50KB gzip

### Virtual Lists

`@tanstack/react-virtual` for lists with > 20 elements.

### TanStack Query

- `staleTime: 0` for financial data
- Adaptive polling based on deal status
- Prefetch on hover for navigation targets

### Telegram Mini App

- First paint target: < 300ms
- Skeleton screens for all async content
- `WebApp.expand()` called immediately on mount

## 3. Code Quality

### TypeScript Strict Mode

```json
{
  "strict": true,
  "noUncheckedIndexedAccess": true,
  "exactOptionalPropertyTypes": true
}
```

Zero `any`. Biome `noExplicitAny: error`.

### Project Structure (Feature-Based)

```
src/
  app/          # Router, providers
  pages/        # Lazy-loaded route entry points
  features/     # Domain modules
    {feature}/
      api/      # Feature-specific API calls
      components/
      hooks/
      lib/
      types/
  shared/
    api/        # Client, query keys, Zod schemas
    ui/         # Design system (Amount, StatusBadge, etc.)
    lib/        # ton-format, date-format
    i18n/       # i18next (ru, en)
    hooks/
```

Features: `channels`, `deals`, `escrow`, `creative`, `disputes`, `auth`.

### Zod Schemas

All API responses validated with Zod schemas. Types derived via `z.infer<>`. Validation happens in API client middleware.

### State Management

- **Server state**: TanStack Query (mandatory)
- **Client state**: Zustand (split by domain, one store per bounded context)
- Redux, MobX, Recoil are FORBIDDEN

### Query Key Factory

All keys through factory functions:
```typescript
dealKeys.detail(id)
channelKeys.list(filters)
```

Inline query keys are FORBIDDEN.

## 4. Financial Data in UI

### nanoTON → TON Conversion

ONLY `BigInt` arithmetic. `Number()` for monetary values is FORBIDDEN.

Single `<Amount>` component for all monetary display:
```typescript
const NANO_PER_TON = 1_000_000_000n;
function nanoToTon(nano: bigint): string {
  const whole = nano / NANO_PER_TON;
  const remainder = nano % NANO_PER_TON;
  return `${whole}.${remainder.toString().padStart(9, '0')}`;
}
```

### Deal Status Mapping

Exhaustive `Record<DealStatus, StatusConfig>` — TypeScript breaks compilation when a new status is added without mapping.

### Allowed Actions

Exhaustive `switch` with `default: never` — mirrors backend state machine.

### Offline Policy

Financial data NEVER served from cache without revalidation:
```typescript
{ networkMode: 'online' }  // for escrow/balance queries
```

## 5. Biome Rules (Key Enforcement)

| Rule | Setting |
|---|---|
| `noExplicitAny` | `error` |
| `noUnusedVariables` | `error` |
| `noUnusedImports` | `error` |
| `useExhaustiveDependencies` | `warn` |
| `useHookAtTopLevel` | `error` |
| `noDefaultExport` | `error` (except `src/pages/**`) |
| `useImportType` | `error` |
| Restricted imports | Redux, axios forbidden |
