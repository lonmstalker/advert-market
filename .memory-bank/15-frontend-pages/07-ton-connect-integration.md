# TON Connect integration

> End-to-end document: lifecycle of TON transactions on the front end, hooks, utilities, error handling.
> Does not duplicate UI details from [03-deals.md](03-deals.md) (3.8) and [04-wallet.md](04-wallet.md) (4.2) - refers to them.
> Backend counterpart: [01-ton-sdk-integration.md](../14-implementation-specs/01-ton-sdk-integration.md).
>
> **Architectural decision**: no platform wallet. TON Connect is used only for payment of transactions (escrow deposit).
> Withdrawal is performed by the backend without TON Connect.

---

## 7.1 Architecture

### Principle

**Frontend signs, backend verifies.**

The frontend never sends BOC to the backend. Backend TON Deposit Watcher itself detects transactions via the polling TON Center API at the destination address.

### Diagram

```
Frontend                    TON Connect          Wallet App          Blockchain         Backend
   │                            │                    │                   │                 │
   ├─ GET /deposit ────────────>│                    │                   │                 │
   │<── {address, amount} ──────│                    │                   │                 │
   │                            │                    │                   │                 │
   ├─ sendTransaction() ───────>│                    │                   │                 │
   │                            ├─ open wallet ─────>│                   │                 │
   │                            │                    ├─ sign & send ────>│                 │
   │                            │                    │<── tx accepted ───│                 │
   │<── promise resolved ───────│                    │                   │                 │
   │                            │                    │                   │                 │
   │   [toast: "Tx sent"]       │                    │                   │                 │
   │                            │                    │                   │  Deposit Watcher│
   │                            │                    │                   │<── polls ────────│
   │                            │                    │                   ├── tx detected ──>│
   │                            │                    │                   │                  │
   │   [polling deal status]    │                    │                   │  confirmations++ │
   │── GET /deals/:id ─────────────────────────────────────────────────────────────────────>│
   │<── {status: FUNDED} ──────────────────────────────────────────────────────────────────│
   │                            │                    │                   │                 │
   │   [toast: "Payment OK"]    │                    │                   │                 │
```

### Address model

Each deal receives a **unique deposit address** (subwallet_id derivation from deal ID). The backend identifies the deposit at the destination address - additional payload/comment **not needed**.

Format: **non-bounceable** (`UQ...`) - because wallet may be uninitialized.

### Two types of TON operations

| # | Operation | TON Connect? | Who signs | Frontend trigger |
|---|----------|:---:|---|---|
| 1 | **Escrow Deposit** | Yes | User | Payment Sheet (3.8) |
| 2 | **Withdrawal** | No | Backend | Withdraw (4.2) → POST API |

> **Removed**: Platform Deposit (platform balance replenishment). There is no platform wallet - all payments are per-deal.

---

## 7.2 Dependencies and configuration

### Installed packages

| Package | Version | Destination |
|-------|--------|-----------|
| `@tonconnect/ui-react` | ^2.4.1 | TON Connect UI + React hooks |
| `@telegram-apps/sdk-react` | ^3.3.9 | Mini App SDK (launch params, back button) |

### `@ton/core` - NOT needed for MVP

Simple transfers (address + amount) do not require Cell building. `@tonconnect/ui-react` provides `sendTransaction()` with sufficient API. `@ton/core` will be needed later for:
- Address validation (bounceable/non-bounceable parsing)
- Formation of payload/comment
- Generation of `ton://transfer/...` QR links

### TonConnectUIProvider (already configured)

```typescript
// App.tsx — \u0442\u0435\u043a\u0443\u0449\u0430\u044f \u043a\u043e\u043d\u0444\u0438\u0433\u0443\u0440\u0430\u0446\u0438\u044f
const TON_MANIFEST_URL = `${window.location.origin}/tonconnect-manifest.json`;

<TonConnectUIProvider manifestUrl={TON_MANIFEST_URL}>
```

**TODO**: add `twaReturnUrl` for correct return from external wallet:

```typescript
<TonConnectUIProvider
  manifestUrl={TON_MANIFEST_URL}
  actionsConfiguration={{
    twaReturnUrl: 'https://t.me/AdvertMarketBot/app',
  }}
>
```

### Env

| Variable | Values | Default |
|----------|--------|---------|
| `VITE_TON_NETWORK` | `testnet` / `mainnet` | `testnet` |

### TON Explorer URL

```typescript
const explorerBaseUrl = import.meta.env.VITE_TON_NETWORK === 'mainnet'
  ? 'https://tonviewer.com'
  : 'https://testnet.tonviewer.com';

// \u0421\u0441\u044b\u043b\u043a\u0430 \u043d\u0430 \u0442\u0440\u0430\u043d\u0437\u0430\u043a\u0446\u0438\u044e: `${explorerBaseUrl}/transaction/${txHash}`
// \u0421\u0441\u044b\u043b\u043a\u0430 \u043d\u0430 \u0430\u0434\u0440\u0435\u0441: `${explorerBaseUrl}/${address}`
```

Used in 4.4 (Transaction Detail) - the "Open in TON Explorer" button.

---

## 7.3 Hooks and utilities

### `useTonTransaction()`

A single wrapper over `sendTransaction` with error handling, toast, debounce.

```typescript
type UseTonTransactionOptions = {
  onSuccess?: () => void;
  onError?: (error: TonTransactionError) => void;
};

type TonTransactionParams = {
  address: string;      // non-bounceable UQ... \u0438\u043b\u0438 bounceable EQ...
  amountNano: string;   // nanoTON \u043a\u0430\u043a \u0441\u0442\u0440\u043e\u043a\u0430
  validUntil?: number;  // unix timestamp, default: now + 600s
};

function useTonTransaction(options?: UseTonTransactionOptions) {
  return {
    send: (params: TonTransactionParams) => Promise<void>;
    isPending: boolean;    // \u0431\u043b\u043e\u043a\u0438\u0440\u0443\u0435\u0442 \u043a\u043d\u043e\u043f\u043a\u0443, \u043f\u0440\u0435\u0434\u043e\u0442\u0432\u0440\u0430\u0449\u0430\u0435\u0442 \u043f\u043e\u0432\u0442\u043e\u0440\u043d\u044b\u0435 \u043a\u043b\u0438\u043a\u0438
    error: TonTransactionError | null;
  };
}
```

Inside:
1. Checks `tonConnectUI.connected` - if not, throws `WALLET_NOT_CONNECTED`
2. Forms `SendTransactionRequest`
3. Calls `tonConnectUI.sendTransaction()`
4. If successful - toast success + `onSuccess()`
5. In case of error - mapping via `mapTonConnectError()` + toast error + `onError()`
6. `isPending` = true for all time (prevents double-send)

### `useTonWalletStatus()`

Connection status + wallet address.

```typescript
function useTonWalletStatus() {
  return {
    isConnected: boolean;
    address: string | null;        // raw address
    friendlyAddress: string | null; // UQ.../EQ... format
    wallet: Wallet | null;         // full wallet info
    connect: () => Promise<void>;
    disconnect: () => Promise<void>;
  };
}
```

Based on `useTonConnectUI()` and `useTonWallet()` from `@tonconnect/ui-react`.

### `useDepositPolling(dealId, options?)`

Adaptive polling of deposit status after sending a transaction.

```typescript
type DepositPollingOptions = {
  enabled: boolean;            // \u0437\u0430\u043f\u0443\u0441\u043a\u0430\u0442\u044c \u043b\u0438 polling
  onConfirmed?: () => void;    // callback \u043f\u0440\u0438 FUNDED
  onTimeout?: () => void;      // callback \u043f\u0440\u0438 \u0438\u0441\u0442\u0435\u0447\u0435\u043d\u0438\u0438 \u0442\u0430\u0439\u043c\u0430\u0443\u0442\u0430
  timeoutMs?: number;          // default: 30 * 60 * 1000 (30 \u043c\u0438\u043d)
};

function useDepositPolling(dealId: string, options: DepositPollingOptions) {
  return {
    depositStatus: DepositStatus;   // \u0441\u043c. 7.4
    confirmations: number | null;
    requiredConfirmations: number | null;
    isPolling: boolean;
    elapsed: number;                // ms \u0441 \u043d\u0430\u0447\u0430\u043b\u0430 polling
  };
}
```

Inside:
- `refetchInterval: 10_000` (10s) to `dealKeys.detail(dealId)` or `dealKeys.deposit(dealId)`
- Starts on `enabled: true` (after successful `sendTransaction`)
- Stops at: status `FUNDED`, timeout 30 min, `enabled: false`
- Stores `pollingStartedAt` in `sessionStorage` for resume (see 7.7)

### Utilities

#### `ton-amount.ts`

```typescript
// nanoTON → "1 250.00 TON"
function formatTonAmount(nanoTon: bigint | string): string;

// nanoTON → \u0441\u0442\u0440\u043e\u043a\u0430 \u0434\u043b\u044f sendTransaction
function toNanoString(nanoTon: bigint): string;

// TON (float input) → nanoTON
function parseUserTonInput(input: string): bigint;
```

#### `ton-errors.ts`

```typescript
type TonTransactionError =
  | { code: 'WALLET_NOT_CONNECTED' }
  | { code: 'USER_REJECTED' }            // \u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u0435\u043b\u044c \u043e\u0442\u043c\u0435\u043d\u0438\u043b \u0432 \u043a\u043e\u0448\u0435\u043b\u044c\u043a\u0435
  | { code: 'INSUFFICIENT_FUNDS' }
  | { code: 'TIMEOUT' }                  // validUntil \u0438\u0441\u0442\u0451\u043a
  | { code: 'NETWORK_MISMATCH' }         // \u043a\u043e\u0448\u0435\u043b\u0451\u043a \u0432 \u0434\u0440\u0443\u0433\u043e\u0439 \u0441\u0435\u0442\u0438
  | { code: 'DISCONNECTED_DURING_TX' }   // disconnect \u0432\u043e \u0432\u0440\u0435\u043c\u044f \u0442\u0440\u0430\u043d\u0437\u0430\u043a\u0446\u0438\u0438
  | { code: 'UNKNOWN'; message: string };

// TON Connect error → \u043d\u0430\u0448 \u0442\u0438\u043f + i18n key
function mapTonConnectError(error: unknown): TonTransactionError;

// TonTransactionError → i18n key \u0434\u043b\u044f toast
function getErrorI18nKey(error: TonTransactionError): string;
```

Mapping on i18n:

| Code | i18n key |
|------|----------|
| `WALLET_NOT_CONNECTED` | `wallet.error.connectFirst` |
| `USER_REJECTED` | `wallet.error.walletRejected` |
| `INSUFFICIENT_FUNDS` | `wallet.error.insufficientTon` |
| `TIMEOUT` | `wallet.error.timeout` |
| `NETWORK_MISMATCH` | `wallet.error.networkMismatch` |
| `DISCONNECTED_DURING_TX` | `wallet.error.disconnected` |
| `UNKNOWN` | `wallet.error.transactionFailed` |

---

## 7.4 Flow 1: Payment for the transaction (Escrow Deposit)

> UI details: [03-deals.md](03-deals.md) (section 3.8)

### Trigger

Button `t('deals.payment.pay')` on Payment Sheet (3.8).

### Preconditions

- Transaction in status `AWAITING_PAYMENT`
- TON Connect wallet is connected
- Role: advertiser

### Steps

```
1. \u0424\u0440\u043e\u043d\u0442: GET /api/v1/deals/:dealId/deposit
   → { escrowAddress, amountNano, status, expiresAt }

2. \u0424\u0440\u043e\u043d\u0442: useTonTransaction().send({
     address: escrowAddress,
     amountNano: amountNano.toString(),
   })

3. TON Connect \u043e\u0442\u043a\u0440\u044b\u0432\u0430\u0435\u0442 \u043a\u043e\u0448\u0435\u043b\u0451\u043a → \u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u0435\u043b\u044c \u043f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0430\u0435\u0442

4. Promise resolved → toast t('wallet.toast.paymentSent')
   \u0421\u043e\u0445\u0440\u0430\u043d\u044f\u0435\u043c \u0432 sessionStorage: { dealId, sentAt: Date.now() }

5. useDepositPolling(dealId, { enabled: true })
   Polling GET /api/v1/deals/:dealId \u043a\u0430\u0436\u0434\u044b\u0435 10s

6. Backend Deposit Watcher \u0434\u0435\u0442\u0435\u043a\u0442\u0438\u0442 tx → \u0441\u0447\u0438\u0442\u0430\u0435\u0442 confirmations →
   \u043f\u0440\u0438 \u0434\u043e\u0441\u0442\u0438\u0436\u0435\u043d\u0438\u0438 required → deal.status = FUNDED

7. \u0424\u0440\u043e\u043d\u0442 \u0432\u0438\u0434\u0438\u0442 FUNDED → toast t('wallet.toast.paymentConfirmed') → \u0437\u0430\u043a\u0440\u044b\u0442\u044c sheet
```

### API contract: `GET /api/v1/deals/:dealId/deposit`

**Response** (Zod schema in `src/features/ton/types/ton.ts`):

```typescript
const DepositInfoSchema = z.object({
  escrowAddress: z.string(),           // non-bounceable UQ...
  amountNano: z.string(),              // bigint as string
  dealId: z.string(),
  status: z.enum([
    'AWAITING_PAYMENT',                // \u043e\u0436\u0438\u0434\u0430\u0435\u0442 \u043e\u0442\u043f\u0440\u0430\u0432\u043a\u0438
    'TX_DETECTED',                     // tx \u043e\u0431\u043d\u0430\u0440\u0443\u0436\u0435\u043d\u0430, \u0436\u0434\u0451\u043c confirmations
    'CONFIRMING',                      // \u043d\u0430\u0431\u0438\u0440\u0430\u0435\u043c confirmations
    'AWAITING_OPERATOR_REVIEW',        // >1000 TON, \u043e\u0436\u0438\u0434\u0430\u0435\u0442 \u043e\u043f\u0435\u0440\u0430\u0442\u043e\u0440\u0430
    'CONFIRMED',                       // \u043f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u043e (= deal FUNDED)
    'EXPIRED',                         // \u0442\u0430\u0439\u043c\u0430\u0443\u0442 (30 \u043c\u0438\u043d \u0431\u0435\u0437 tx)
    'UNDERPAID',                       // \u0441\u0443\u043c\u043c\u0430 \u043c\u0435\u043d\u044c\u0448\u0435 \u043e\u0436\u0438\u0434\u0430\u0435\u043c\u043e\u0439
    'OVERPAID',                        // \u0441\u0443\u043c\u043c\u0430 \u0431\u043e\u043b\u044c\u0448\u0435 \u043e\u0436\u0438\u0434\u0430\u0435\u043c\u043e\u0439 (manual review)
    'REJECTED',                        // \u043e\u043f\u0435\u0440\u0430\u0442\u043e\u0440 \u043e\u0442\u043a\u043b\u043e\u043d\u0438\u043b
  ]),
  currentConfirmations: z.number().nullable(),
  requiredConfirmations: z.number().nullable(),
  receivedAmountNano: z.string().nullable(),   // \u0441\u043a\u043e\u043b\u044c\u043a\u043e \u0444\u0430\u043a\u0442\u0438\u0447\u0435\u0441\u043a\u0438 \u043f\u043e\u043b\u0443\u0447\u0435\u043d\u043e
  txHash: z.string().nullable(),               // hash \u0435\u0441\u043b\u0438 tx \u043e\u0431\u043d\u0430\u0440\u0443\u0436\u0435\u043d\u0430
  expiresAt: z.string().nullable(),            // ISO 8601, \u043a\u043e\u0433\u0434\u0430 deposit expire
});
```

### Intermediate statuses (UI)

| Deposit status | UI on Payment Sheet / Deal Detail |
|---|---|
| `AWAITING_PAYMENT` | The "Pay" button is active |
| `TX_DETECTED` | Spinner + `t('wallet.status.txDetected')` |
| `CONFIRMING` | Progress: `t('wallet.status.confirming', { current, required })` |
| `AWAITING_OPERATOR_REVIEW` | `t('wallet.status.operatorReview')` - info text, NOT error |
| `CONFIRMED` | Toast success → close sheet |
| `EXPIRED` | Toast error `t('wallet.error.depositExpired')` + retry button |
| `UNDERPAID` | Alert `t('wallet.error.underpaid', { received, expected })` + send instructions |
| `OVERPAID` | Info `t('wallet.status.overpaid')` — "the difference will be returned" |
| `REJECTED` | Alert error `t('wallet.error.depositRejected')` + support link |

### Tiered confirmations (display)

From [06-confirmation-policy.md](../07-financial-system/06-confirmation-policy.md):

| Amount | Required | UI |
|--------|:---:|----|
| <= 100 TON | 1 | Fast, usually without visible progress |
| <= 1,000 TON | 3 | Progress bar "2/3 confirmations" |
| > 1,000 TON | 5 + operator | Progress bar + info "Awaiting operator verification" |

###Double send protection

| Level | Mechanism | Description |
|---------|----------|----------|
| **UI** | `isPending` from `useTonTransaction` | Disable buttons during surgery |
| **Blockchain** | TON wallet seqno | Wallet contract rejects replay (seqno mismatch) |
| **Backend** | `tx_hash` PK to `ton_transactions` | Duplicate deposit is ignored |
| **Backend** | Optimistic lock on `deals.version` | Repeated FUNDED - idempotent no-op |
| **Backend** | Redis lock `lock:escrow:{deal_id}` | Prevents concurrent processing |

### Error states

| Error | Code | UI |
|--------|-----|-----|
| Wallet rejected | `USER_REJECTED` | Toast `t('wallet.error.walletRejected')` |
| Not enough TON | `INSUFFICIENT_FUNDS` | Toast `t('wallet.error.insufficientTon')` |
| Signing timeout (10 min) | `TIMEOUT` | Toast `t('wallet.error.timeout')` + retry |
| Disconnect during tx | `DISCONNECTED_DURING_TX` | Toast `t('wallet.error.disconnected')` |
| Network mismatch | `NETWORK_MISMATCH` | Toast `t('wallet.error.networkMismatch')` |
| Polling timeout (30 min) | — | Alert `t('wallet.error.pollingTimeout')` + support link |
| Deposit expired (backend) | — | Alert `t('wallet.error.depositExpired')` |

---

## 7.5 Flow 2: Withdrawal of funds (Owner Withdrawal)

> UI details: [04-wallet.md](04-wallet.md) (section 4.2)

### Peculiarity

**DOES NOT use TON Connect** for signing. The transaction is generated and signed by the backend via ton4j.

### Steps

```
1. \u041f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u0435\u043b\u044c \u0432\u0432\u043e\u0434\u0438\u0442 \u0441\u0443\u043c\u043c\u0443 + \u0430\u0434\u0440\u0435\u0441 (pre-filled \u0438\u0437 TON Connect \u0438\u043b\u0438 \u0440\u0443\u0447\u043d\u043e\u0439)

2. DialogModal \u043f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u0438\u044f

3. \u0424\u0440\u043e\u043d\u0442: POST /api/v1/wallet/withdraw
   Headers: { Idempotency-Key: uuid }
   Body: { amountNano, destinationAddress }
   → { withdrawalId, status: 'PENDING', estimatedFeeNano }

4. Toast t('wallet.toast.withdrawProcessing') → navigate /wallet

5. Backend \u043f\u043e\u0434\u043f\u0438\u0441\u044b\u0432\u0430\u0435\u0442, \u043e\u0442\u043f\u0440\u0430\u0432\u043b\u044f\u0435\u0442 \u0447\u0435\u0440\u0435\u0437 ton4j

6. \u041f\u0440\u0438 \u0441\u043b\u0435\u0434\u0443\u044e\u0449\u0435\u043c GET /wallet/summary — pendingPayoutNano \u0443\u043c\u0435\u043d\u044c\u0448\u0438\u043b\u0441\u044f
```

### Double Output Protection

| Level | Mechanism | Description |
|---------|----------|----------|
| **UI** | `isPending` from `useMutation` | Disable buttons for the duration of the request |
| **API** | `Idempotency-Key` header | Backend deduplicates by key (Redis, TTL 24h) |
| **Backend** | Redis lock `lock:payout:{user_id}` | One output at a time |
| **Backend** | `OWNER_PENDING` balance check | You cannot withdraw more than you have |

### Destination address

- If TON Connect is connected: pre-filled from `useTonWalletStatus().friendlyAddress`
- Otherwise: manual input with format validation `EQ...` / `UQ...` (regex, length 48 base64 characters)
- User can change pre-filled address

### Error states

| Error | HTTP | UI |
|--------|------|----|
| Insufficient funds | 400 | Toast `t('wallet.error.insufficientFunds')` |
| Invalid address | 400 | Inline error `t('wallet.error.invalidAddress')` |
| Withdrawal limit | 400 | Toast `t('wallet.error.withdrawLimit')` |
| Rate limit | 429 | Toast `t('errors.rateLimited')` |
| Output failed (backend) | — | Toast `t('wallet.error.withdrawFailed')` + support link |

---

## 7.6 Wallet connection status

### TonConnectButton

| Place | When to show | Component |
|-------|-----------------|-----------|
| Payment Sheet (3.8) | Wallet not connected | Button `t('wallet.connectWallet')` |
| Wallet Main (4.1) | Always | Badge: address (truncated) or connect button |
| Withdraw (4.2) | Optional | Pre-fill addresses if connected |

### Auto-reconnect

`@tonconnect/ui-react` automatically restores the session from localStorage. When returning to the Mini App, the wallet is usually already connected.

### Disconnect flow

- Disconnect button available on Wallet Main (4.1)
- When disconnected: clearing `friendlyAddress`, updating UI
- **Does not affect** transactions already sent

---

## 7.7 Resume / Recovery

### Problem

Telegram can kill Mini App while the user is in the wallet. On return, React state is reset.

### Solution

We save the "pending intent" in `sessionStorage`:

```typescript
type PendingTonIntent = {
  type: 'escrow_deposit';
  dealId: string;
  sentAt: number;            // timestamp
  address: string;           // \u043a\u0443\u0434\u0430 \u043e\u0442\u043f\u0440\u0430\u0432\u043b\u044f\u043b\u0438
  amountNano: string;
};

// \u0421\u043e\u0445\u0440\u0430\u043d\u044f\u0442\u044c \u043f\u0435\u0440\u0435\u0434 sendTransaction
sessionStorage.setItem('ton_pending_intent', JSON.stringify(intent));

// \u0423\u0434\u0430\u043b\u044f\u0442\u044c \u043f\u0440\u0438 confirmed \u0438\u043b\u0438 timeout
```

### When mounting a page

```
1. \u041f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c sessionStorage \u043d\u0430 pending intent
2. \u0415\u0441\u043b\u0438 \u0435\u0441\u0442\u044c \u0438 sentAt < 30 \u043c\u0438\u043d \u043d\u0430\u0437\u0430\u0434:
   - escrow_deposit: \u0437\u0430\u043f\u0443\u0441\u0442\u0438\u0442\u044c useDepositPolling(dealId)
3. \u0415\u0441\u043b\u0438 sentAt > 30 \u043c\u0438\u043d: \u0443\u0434\u0430\u043b\u0438\u0442\u044c intent, \u043f\u043e\u043a\u0430\u0437\u0430\u0442\u044c toast "\u0442\u0430\u0439\u043c\u0430\u0443\u0442"
```

### `refetchOnWindowFocus`

The current `App.tsx` is set to `refetchOnWindowFocus: false`. This is correct for the general case, but for financial queries an override is needed:

```typescript
// \u0412 useDepositPolling
useQuery({
  queryKey: dealKeys.deposit(dealId),
  refetchOnWindowFocus: true,     // override \u0433\u043b\u043e\u0431\u0430\u043b\u044c\u043d\u043e\u0433\u043e false
  refetchInterval: 10_000,
});
```

---

## 7.8 Error handling (general)

### Retry strategy

| Situation | Retry |
|----------|-------|
| `sendTransaction` rejected | No auto-retry. The user presses the button again |
| Polling 4xx/5xx | React Query retry: 1 attempt, then show error |
| Network offline | Pause polling, resume when online |

### `validUntil`

All transactions: `Math.floor(Date.now() / 1000) + 600` (10 minutes). If the user “thinks” longer, the wallet will be rejected, you need to request again.

### Debounce / double-send prevention

`useTonTransaction().isPending` blocks the button for the entire duration of the operation. Additionally - `disabled` on the button while `isPending === true`.

### Fallback when disconnected during a transaction

If `sendTransaction()` throws a disconnect error:
1. Toast `t('wallet.error.disconnected')`
2. **Don't** try to reconnect automatically
3. Show the “Connect wallet” button again
4. The transaction on the blockchain may already have been sent - we check it through polling

---

## 7.9 Testing

### Testnet configuration

- `VITE_TON_NETWORK=testnet`
- Backend: `ton.api.url=https://testnet.toncenter.com/api/v2/`
- Manifest: `tonconnect-manifest.json` from testnet network

### Test wallet

- Tonkeeper (testnet mode) or MyTonWallet
- Testnet TON faucet: `@testgiver_ton_bot` in Telegram
- Recommended test balance: 10+ TON

### Manual QA checklist

**Flow 1: Escrow Deposit**
- [ ] Wallet is not connected → show the "Connect" button
- [ ] Connect wallet → address is displayed
- [ ] Click “Pay” → wallet opens
- [ ] Confirm in wallet → toast "Transaction sent"
- [ ] Wait FUNDED → toast "Payment confirmed", sheet closed
- [ ] Cancel in wallet → toast "Rejected", the button is active
- [ ] Close Mini App while waiting → return → polling continues

**Flow 2: Withdrawal**
- [ ] Enter amount + address → "Max" fills in the maximum
- [ ] Confirm → DialogModal → POST → navigate /wallet
- [ ] pendingPayoutNano decreased
- [ ] Repeated POST with the same Idempotency-Key → does not create a duplicate

**Edge cases**
- [ ] Network mismatch (mainnet wallet + testnet backend)
- [ ] Double click "Pay" - only one transaction
- [ ] Timeout 30 min without confirmation
- [ ] Underpayment (send less than the required amount)
- [ ] Offline → recovery → polling resumes

---

## 7.10 File structure

```
src/features/ton/
  hooks/
    useTonTransaction.ts          # \u043e\u0431\u0451\u0440\u0442\u043a\u0430 \u043d\u0430\u0434 sendTransaction
    useTonWalletStatus.ts         # \u0441\u0442\u0430\u0442\u0443\u0441 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u044f + \u0430\u0434\u0440\u0435\u0441
    useDepositPolling.ts          # polling \u0441\u0442\u0430\u0442\u0443\u0441\u0430 escrow deposit
  lib/
    ton-amount.ts                 # formatTonAmount, toNanoString, parseUserTonInput
    ton-errors.ts                 # mapTonConnectError, getErrorI18nKey
    ton-intent.ts                 # PendingTonIntent: save/load/clear sessionStorage
    ton-explorer.ts               # getExplorerTxUrl, getExplorerAddressUrl
  types/
    ton.ts                        # Zod schemas: DepositInfoSchema, etc.
```

Does not conflict with:
- `src/features/deals/` — PaymentSheet.tsx imports from `features/ton/`
- `src/features/wallet/` — WithdrawPage imports `useTonWalletStatus` for pre-fill address

---

## 7.11 New i18n keys

Everything in namespace `wallet.*` (already defined in [06-shared-components.md](06-shared-components.md)):

```
wallet.connectWallet
wallet.status.txDetected
wallet.status.confirming              # "\u041f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u0438\u0435 {current}/{required}"
wallet.status.operatorReview          # "\u041e\u0436\u0438\u0434\u0430\u0435\u0442 \u043f\u0440\u043e\u0432\u0435\u0440\u043a\u0438 \u043e\u043f\u0435\u0440\u0430\u0442\u043e\u0440\u043e\u043c"
wallet.status.overpaid                # "\u0421\u0443\u043c\u043c\u0430 \u0431\u043e\u043b\u044c\u0448\u0435 — \u0440\u0430\u0437\u043d\u0438\u0446\u0430 \u0431\u0443\u0434\u0435\u0442 \u0432\u043e\u0437\u0432\u0440\u0430\u0449\u0435\u043d\u0430"
wallet.toast.paymentSent
wallet.toast.paymentConfirmed
wallet.toast.withdrawProcessing
wallet.toast.withdrawCompleted
wallet.error.connectFirst
wallet.error.walletRejected
wallet.error.insufficientTon
wallet.error.timeout
wallet.error.networkMismatch
wallet.error.disconnected
wallet.error.transactionFailed        # generic fallback
wallet.error.depositExpired
wallet.error.pollingTimeout
wallet.error.underpaid                # "\u041f\u043e\u043b\u0443\u0447\u0435\u043d\u043e {received}, \u043e\u0436\u0438\u0434\u0430\u043b\u043e\u0441\u044c {expected}"
wallet.error.depositRejected
wallet.error.insufficientFunds
wallet.error.invalidAddress
wallet.error.withdrawLimit
wallet.error.withdrawFailed
```

---

## Links to other documents

| Document | What uses |
|----------|---------------|
| [03-deals.md](03-deals.md) (3.8) | Flow 1, hooks, error mapping |
| [04-wallet.md](04-wallet.md) (4.1, 4.2) | Flow 2, wallet badge |
| [06-shared-components.md](06-shared-components.md) | Error states (6.2), i18n namespace (6.1), toast patterns |
| [01-ton-sdk-integration.md](../14-implementation-specs/01-ton-sdk-integration.md) | Backend: address generation, deposit watcher, tx lifecycle |
| [02-escrow-flow.md](../07-financial-system/02-escrow-flow.md) | Backend: full escrow lifecycle |
| [06-confirmation-policy.md](../07-financial-system/06-confirmation-policy.md) | Backend: tiered confirmations, operator review |
| [07-idempotency-strategy.md](../05-patterns-and-decisions/07-idempotency-strategy.md) | Idempotency-Key for withdrawal |

---

## Verification

1. **Both flows are covered** - escrow deposit (7.4), withdrawal (7.5)
2. **Each flow**: trigger → precondition → steps → API contract → intermediate states → error states
3. **No duplication** - UI details in 03-deals.md and 04-wallet.md, here only lifecycle and integration
4. **Confirmations as first-class** - intermediate statuses (TX_DETECTED, CONFIRMING, AWAITING_OPERATOR_REVIEW)
5. **Operator review covered** - UI for >1000 TON (7.4 table of intermediate statuses)
6. **Underpayment/overpayment covered** — statuses UNDERPAID/OVERPAID + i18n
7. **Resume/recovery covered** - sessionStorage intent (7.7)
8. **Double-send prevention** - protection matrix by level (7.4, 7.5)
9. **Idempotency-Key** - for withdrawal (7.5), not needed for escrow deposit
10. **File structure** `src/features/ton/` does not conflict with existing features
11. **i18n keys** from `wallet.*` namespace
12. **Platform Deposit removed** - no platform wallet
