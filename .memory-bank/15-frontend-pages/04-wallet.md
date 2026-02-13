#Finance

> Tab 3. Financial account: summary of transactions, transaction history, withdrawal of earnings (channel owner).
>
> **Architectural decision**: no platform wallet. All TON transactions are tied to transactions (per-deal escrow).
> The advertiser pays each transaction directly to a unique escrow address.
> The channel owner receives payments from `OWNER_PENDING` after transactions are completed.

## Navigation

```
/wallet
  ├── /wallet/withdraw          # \u0422\u043e\u043b\u044c\u043a\u043e \u0434\u043b\u044f \u0432\u043b\u0430\u0434\u0435\u043b\u044c\u0446\u0435\u0432 \u043a\u0430\u043d\u0430\u043b\u043e\u0432
  ├── /wallet/history
  │   └── /wallet/history/:txId
  └── [Sheet] \u0424\u0438\u043b\u044c\u0442\u0440\u044b \u0438\u0441\u0442\u043e\u0440\u0438\u0438
```

---

## API endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/api/v1/wallet/summary` | Financial Summary | Authenticated |
| `GET` | `/api/v1/wallet/transactions` | List of transactions | Authenticated |
| `GET` | `/api/v1/wallet/transactions/:txId` | Transaction details | Owner |
| `POST` | `/api/v1/wallet/withdraw` | Withdrawal request | Channel Owner |

### Query keys (add to `query-keys.ts`)

```typescript
export const walletKeys = {
  summary: ['wallet', 'summary'] as const,
  transactions: () => ['wallet', 'transactions'] as const,
  transactionList: (params?: PaginationParams & Record<string, string | undefined>) =>
    [...walletKeys.transactions(), params] as const,
  transactionDetail: (txId: string) => [...walletKeys.transactions(), txId] as const,
};
```

---

## 4.1 Home Finance

| | |
|---|---|
| **Route** | `/wallet` |
| **Target** | Financial Summary, Quick Actions, Latest Transactions |
| **Who sees** | All authorized |

### API

```
GET /api/v1/wallet/summary
GET /api/v1/wallet/transactions?limit=5
```

**Query keys:** `walletKeys.summary`, `walletKeys.transactionList({ limit: 5 })`

**Network mode:** `{ networkMode: 'online', staleTime: 0 }` for summary (financial data).

### Response: `GET /api/v1/wallet/summary`

```typescript
const WalletSummarySchema = z.object({
  // \u0414\u043b\u044f \u0432\u043b\u0430\u0434\u0435\u043b\u044c\u0446\u0430 \u043a\u0430\u043d\u0430\u043b\u0430
  earnedTotalNano: z.string(),      // \u0412\u0441\u0435\u0433\u043e \u0437\u0430\u0440\u0430\u0431\u043e\u0442\u0430\u043d\u043e \u0437\u0430 \u0432\u0441\u0451 \u0432\u0440\u0435\u043c\u044f
  pendingPayoutNano: z.string(),    // \u0414\u043e\u0441\u0442\u0443\u043f\u043d\u043e \u0434\u043b\u044f \u0432\u044b\u0432\u043e\u0434\u0430 (OWNER_PENDING)
  inEscrowNano: z.string(),        // \u0417\u0430\u043c\u043e\u0440\u043e\u0436\u0435\u043d\u043e \u0432 \u0430\u043a\u0442\u0438\u0432\u043d\u044b\u0445 \u0441\u0434\u0435\u043b\u043a\u0430\u0445
  withdrawnTotalNano: z.string(),   // \u0412\u044b\u0432\u0435\u0434\u0435\u043d\u043e \u0437\u0430 \u0432\u0441\u0451 \u0432\u0440\u0435\u043c\u044f

  // \u0414\u043b\u044f \u0440\u0435\u043a\u043b\u0430\u043c\u043e\u0434\u0430\u0442\u0435\u043b\u044f
  spentTotalNano: z.string(),       // \u0412\u0441\u0435\u0433\u043e \u043f\u043e\u0442\u0440\u0430\u0447\u0435\u043d\u043e \u0437\u0430 \u0432\u0441\u0451 \u0432\u0440\u0435\u043c\u044f
  activeEscrowNano: z.string(),     // \u0417\u0430\u043c\u043e\u0440\u043e\u0436\u0435\u043d\u043e \u0432 \u0430\u043a\u0442\u0438\u0432\u043d\u044b\u0445 \u0441\u0434\u0435\u043b\u043a\u0430\u0445

  // \u041e\u0431\u0449\u0435\u0435
  activeDealsCount: z.number(),
  completedDealsCount: z.number(),
});
```

The fields depend on the user's role (the backend returns the relevant ones, the rest = "0").

### UI

**For channel owner:**
- **Available for output** - hero / `title1`, bold, centered, `tabular-nums`, `<Amount>` (format: "1 250.00 TON")
- **Summary** — `Group` with 3 `GroupItem`:
  - `t('wallet.summary.earned')` — total earned
  - `t('wallet.summary.inEscrow')` — in active transactions
  - `t('wallet.summary.withdrawn')` — displayed
- **Quick actions** - button `t('wallet.withdraw')` (↑ icon) → `/wallet/withdraw`
  - Show only if `pendingPayoutNano > 0`

**For advertiser:**
- **In active transactions** — hero / `title1`
- **Summary** — `Group` with 2 `GroupItem`:
  - `t('wallet.summary.spent')` — total spent
  - `t('wallet.summary.activeEscrow')` — in escrow
- Withdraw buttons **none** (advertiser does not receive payment)

**General:**
- **Group `t('wallet.recentTransactions')`** — up to the last 5 transactions (`GroupItem`):
  - `before`: icon type (escrow_deposit/payout/refund/commission)
  - Header: description of the operation + name of the channel/deal
  - `after`: amount with color (green = income, red = expense) + date (`caption`)
- Link `t('wallet.allHistory')` → `/wallet/history`

### Actions

| Action | Result |
|----------|-----------|
| "Withdraw" (owner) | → `/wallet/withdraw` |
| Tap on transaction | → `/wallet/history/:txId` |
| "The Whole Story" | → `/wallet/history` |
| Pull-to-refresh | Invalidation `walletKeys.summary` + `walletKeys.transactions()` |

### Empty state

| Emoji | i18n title | i18n description | CTA |
|-------|------------|------------------|-----|
| `📜` | `wallet.empty.title` | `wallet.empty.description` | `wallet.empty.cta` → channel directory |

### Error states

| Error | UI |
|--------|----|
| Loading error summary | `ErrorScreen` + retry |
| Error loading transactions | Transaction section: inline error + retry |
| Offline | Banner `t('errors.offline')` |

---

## 4.2 Withdrawal of funds (channel owner only)

| | |
|---|---|
| **Route** | `/wallet/withdraw` |
| **Target** | Withdraw earned funds from `OWNER_PENDING` to an external TON wallet |
| **Who sees** | Owners of channels with `pendingPayoutNano > 0` |

### API

```
GET  /api/v1/wallet/summary
POST /api/v1/wallet/withdraw
```

**Query keys:** `walletKeys.summary`

**Headers:** `Idempotency-Key: {uuid}` - generated when the form is mounted, updated after successful submission.

### UI

- **Available for output** — `title2`, bold, `<Amount>`
- **Input `t('wallet.withdraw.amount')`** — numeric, max = pendingPayoutNano, button `t('wallet.withdraw.max')` (inline)
- **Input `t('wallet.withdraw.address')`** — if TON Connect is connected: pre-filled, otherwise: manual input
- **Network commission calculation** — `caption`, `secondary` (fixed or updated when you enter the amount)
- **Total receivable** — `title3`
- Button `t('wallet.withdraw.submit')` (`primary`, full-width)

### Request body

```typescript
const WithdrawRequestSchema = z.object({
  amountNano: z.string(),               // bigint as string, > 0, <= pendingPayoutNano
  destinationAddress: z.string(),        // valid TON address (EQ... \u0438\u043b\u0438 UQ...)
});
```

### Response

```typescript
const WithdrawResponseSchema = z.object({
  withdrawalId: z.string(),              // UUID \u0434\u043b\u044f \u043e\u0442\u0441\u043b\u0435\u0436\u0438\u0432\u0430\u043d\u0438\u044f
  status: z.enum(['PENDING', 'SUBMITTED', 'CONFIRMED', 'FAILED']),
  estimatedFeeNano: z.string(),
});
```

### Idempotency-Key

```typescript
// \u0413\u0435\u043d\u0435\u0440\u0438\u0440\u0443\u0435\u043c \u043f\u0440\u0438 \u043c\u043e\u043d\u0442\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0438 \u0444\u043e\u0440\u043c\u044b, \u041d\u0415 \u043f\u0440\u0438 \u043a\u043b\u0438\u043a\u0435
const idempotencyKey = useRef(crypto.randomUUID());

const withdraw = useMutation({
  mutationFn: (data: WithdrawRequest) =>
    api.post('/wallet/withdraw', data, {
      headers: { 'Idempotency-Key': idempotencyKey.current },
    }),
  onSuccess: () => {
    idempotencyKey.current = crypto.randomUUID(); // \u043d\u043e\u0432\u044b\u0439 \u043a\u043b\u044e\u0447 \u0434\u043b\u044f \u0441\u043b\u0435\u0434\u0443\u044e\u0449\u0435\u0439 \u043e\u043f\u0435\u0440\u0430\u0446\u0438\u0438
    queryClient.invalidateQueries({ queryKey: walletKeys.summary });
  },
});
```

Backend when receiving `Idempotency-Key`:
- First request - executes, saves `(key, response)` in Redis (TTL 24h)
- Repeated - returns the saved response without re-executing

### Actions

| Action | Result |
|----------|-----------|
| "Max" | Fill in the maximum amount (pendingPayout - commission) |
| "Withdraw" | → `DialogModal` confirmations → `POST /api/v1/wallet/withdraw` → toast → navigate `/wallet` |

### Validation

- Sum > 0 and <= available for withdrawal (`pendingPayoutNano`)
- Address - valid TON address (format `EQ...` or `UQ...`)

### Error states

| Error | UI | Description |
|--------|----|----------|
| Insufficient funds | Toast `t('wallet.error.insufficientFunds')` | The balance has changed between downloading and sending |
| Invalid address | Inline error `t('wallet.error.invalidAddress')` | The address format does not match TON |
| Withdrawal limit | Toast `t('wallet.error.withdrawLimit')` | Daily/one-time limit exceeded |
| 429 rate limit | Toast `t('errors.rateLimited')` | Too frequent withdrawal requests |
| Duplicate (Idempotency-Key) | Returns the previous response | Transparent to the user |

---

## 4.3 Transaction history

| | |
|---|---|
| **Route** | `/wallet/history` |
| **Target** | Complete history of financial transactions for transactions |
| **Who sees** | All authorized |

### API

```
GET /api/v1/wallet/transactions?cursor=&limit=20&type=&from=&to=
```

**Query keys:** `walletKeys.transactionList(params)`

### Transaction types

| Type | Description | Who sees |
|-----|----------|-----------|
| `escrow_deposit` | Payment for the transaction (TON → escrow) | Advertiser |
| `payout` | Payment to the channel owner | Owner |
| `withdrawal` | Withdrawal from OWNER_PENDING to an external wallet | Owner |
| `refund` | Return from escrow to advertiser | Advertiser |
| `commission` | Platform commission | Both (informational) |

### UI

- **Button `t('wallet.history.filter')`** — with badge number of active filters
- **List of transactions** — `GroupItem`, grouped by day:
  - `before`: type icon
  - Title: description + related deal/channel
  - `after`: amount (green = income, red = expense) + date
- **Infinite scroll** — skeleton loading

### Sheet of filters

- **Type** - multi-select: `escrow_deposit` / `payout` / `withdrawal` / `refund` / `commission`
- **Period** — select: `t('wallet.history.filter.week')` / `t('wallet.history.filter.month')` / `t('wallet.history.filter.all')`

### Actions

| Action | Result |
|----------|-----------|
| "Filter" | → Sheet of filters |
| Tap on transaction | → `/wallet/history/:txId` |

### Empty state

| Emoji | i18n title | i18n description | CTA |
|-------|------------|------------------|-----|
| `📜` | `wallet.history.empty.title` | `wallet.history.empty.description` | `wallet.history.empty.cta` → Reset filters |

### Error states

| Error | UI |
|--------|----|
| Loading Error | `ErrorScreen` + retry button |
| Offline | Banner `t('errors.offline')` |

---

## 4.4 Transaction details

| | |
|---|---|
| **Route** | `/wallet/history/:txId` |
| **Target** | Full transaction details |
| **Who sees** | Participant in a related transaction |

### API

```
GET /api/v1/wallet/transactions/:txId
```

**Query keys:** `walletKeys.transactionDetail(txId)`

### UI

- **Amount** — `title1`, bold, color: +green / -red, `<Amount>`
- **Status** — badge: `pending` / `confirmed` / `failed`
- **Group `t('wallet.transaction.details')`** — `GroupItem`:
  - `t('wallet.transaction.type')`
  - `t('wallet.transaction.date')` (formatted)
  - `t('wallet.transaction.hash')` (copyable, truncated with `...`)
  - `t('wallet.transaction.deal')` (link → `/deals/:dealId`, if available)
  - `t('wallet.transaction.commission')` (if available)
  - `t('wallet.transaction.from')` / `t('wallet.transaction.to')` (truncated addresses)

### Actions

| Action | Result |
|----------|-----------|
| Copy hash | `navigator.clipboard` → toast `t('common.copied')` |
| Tap "Deal" | → `/deals/:dealId` |
| "Open in TON Explorer" | External link (Telegram `openLink`) |

### Error states

| Error | UI |
|--------|----|
| 404 transaction not found | `ErrorScreen` `t('errors.notFound.title')` + navigate `/wallet/history` |
| Loading Error | `ErrorScreen` + retry |

---

## File structure

```
src/pages/wallet/
  WalletPage.tsx              # Route: /wallet
  WithdrawPage.tsx            # Route: /wallet/withdraw
  HistoryPage.tsx             # Route: /wallet/history
  TransactionDetailPage.tsx   # Route: /wallet/history/:txId

src/features/wallet/
  api/
    contracts.ts              # Zod schemas: WalletSummary, WithdrawRequest/Response, Transaction
    wallet-api.ts             # API functions: getWalletSummary, getTransactions, withdraw
    wallet-queries.ts         # TanStack Query hooks: useWalletSummary, useTransactions
    wallet-mutations.ts       # useMutation: useWithdraw (\u0441 Idempotency-Key)
  components/
    SummaryHero.tsx           # Hero-\u0431\u043b\u043e\u043a \u0441 \u043e\u0441\u043d\u043e\u0432\u043d\u043e\u0439 \u0441\u0443\u043c\u043c\u043e\u0439 (\u0437\u0430\u0432\u0438\u0441\u0438\u0442 \u043e\u0442 \u0440\u043e\u043b\u0438)
    SummaryStats.tsx          # \u0421\u0432\u043e\u0434\u043a\u0430 (earned/spent/escrow/withdrawn)
    TransactionListItem.tsx
    TransactionFiltersSheet.tsx
  types/
    wallet.ts                 # \u041e\u0431\u0449\u0438\u0435 \u0442\u0438\u043f\u044b (TransactionType enum, etc.)
```

---

## Links to other documents

| Document | What uses |
|----------|---------------|
| [07-ton-connect-integration.md](07-ton-connect-integration.md) | Flow 1 (escrow deposit), Flow 2 (withdrawal) |
| [06-shared-components.md](06-shared-components.md) | Error states, i18n namespace, Amount, toast |
| [05-account-types.md](../07-financial-system/05-account-types.md) | OWNER_PENDING, ESCROW, ledger model |
| [07-idempotency-strategy.md](../05-patterns-and-decisions/07-idempotency-strategy.md) | Idempotency-Key for withdraw |