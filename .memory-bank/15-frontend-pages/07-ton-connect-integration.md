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
// App.tsx - current configuration
const TON_MANIFEST_URL = `${window.location.origin}/tonconnect-manifest.json`;

<TonConnectUIProvider manifestUrl={TON_MANIFEST_URL}>
```

**TODO**: add `twaReturnUrl` for correct return from external wallet:

```typescript
<TonConnectUIProvider
  manifestUrl={TON_MANIFEST_URL}
  actionsConfiguration={{
    twaReturnUrl: 'https://t.me/adv_markt_bot/app',
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

// Transaction link: `${explorerBaseUrl}/transaction/${txHash}`
// Link to address: `${explorerBaseUrl}/${address}`
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
  address: string;      // non-bounceable UQ... or bounceable EQ...
  amountNano: string;   // nanoTON as a string
  validUntil?: number;  // unix timestamp, default: now + 600s
};

function useTonTransaction(options?: UseTonTransactionOptions) {
  return {
    send: (params: TonTransactionParams) => Promise<void>;
    isPending: boolean;    // locks the button, prevents repeated clicks
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
  enabled: boolean;            // whether to run polling
  onConfirmed?: () => void;    // callback when FUNDED
  onTimeout?: () => void;      // callback when timeout expires
  timeoutMs?: number;          // default: 30 * 60 * 1000 (30 min)
};

function useDepositPolling(dealId: string, options: DepositPollingOptions) {
  return {
    depositStatus: DepositStatus;   // see 7.4
    confirmations: number | null;
    requiredConfirmations: number | null;
    isPolling: boolean;
    elapsed: number;                // ms from the beginning polling
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

// nanoTON → string for sendTransaction
function toNanoString(nanoTon: bigint): string;

// TON (float input) → nanoTON
function parseUserTonInput(input: string): bigint;
```

#### `ton-errors.ts`

```typescript
type TonTransactionError =
  | { code: 'WALLET_NOT_CONNECTED' }
  | { code: 'USER_REJECTED' } // user canceled in wallet
  | { code: 'INSUFFICIENT_FUNDS' }
  | { code: 'TIMEOUT' } // validUntil has expired
  | { code: 'NETWORK_MISMATCH' } // wallet on another network
  | { code: 'DISCONNECTED_DURING_TX' }   // disconnect RU_TEXT
  | { code: 'UNKNOWN'; message: string };

// TON Connect error → our type + i18n key
function mapTonConnectError(error: unknown): TonTransactionError;

// TonTransactionError → i18n key for toast
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
1. Front: GET /api/v1/deals/:dealId/deposit
   → { escrowAddress, amountNano, status, expiresAt }

2. Front: useTonTransaction().send({
     address: escrowAddress,
     amountNano: amountNano.toString(),
   })

3. TON Connect opens the wallet → user confirms

4. Promise resolved → toast t('wallet.toast.paymentSent')
   Save in sessionStorage: { dealId, sentAt: Date.now() }

5. useDepositPolling(dealId, { enabled: true })
   Polling GET /api/v1/deals/:dealId every 10s

6. Backend Deposit Watcher detects tx → counts confirmations →
   upon reaching required → deal.status = FUNDED

7. Front sees FUNDED → toast t('wallet.toast.paymentConfirmed') → close sheet
```

### API contract: `GET /api/v1/deals/:dealId/deposit`

**Response** (Zod schema in `src/features/ton/types/ton.ts`):

```typescript
const DepositInfoSchema = z.object({
  escrowAddress: z.string(),           // non-bounceable UQ...
  amountNano: z.string(),              // bigint as string
  dealId: z.string(),
  status: z.enum([
    'AWAITING_PAYMENT', // awaits sending
    'TX_DETECTED', // tx detected, wait for confirmations
    'CONFIRMING', // type confirmations
    'AWAITING_OPERATOR_REVIEW', // >1000 TON, awaits operator
    'CONFIRMED', // confirmed (= deal FUNDED)
    'EXPIRED', // timeout (30 min without tx)
    'UNDERPAID', // amount less than expected
    'OVERPAID', // amount is greater than expected (manual review)
    'REJECTED', // operator rejected
  ]),
  currentConfirmations: z.number().nullable(),
  requiredConfirmations: z.number().nullable(),
  receivedAmountNano: z.string().nullable(), // how much was actually received
  txHash: z.string().nullable(), // hash if tx is detected
  expiresAt: z.string().nullable(), // ISO 8601 when deposit expires
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
1. User enters amount + address (pre-filled from TON Connect or manual)

2. DialogModal confirmation

3. Front: POST /api/v1/wallet/withdraw
   Headers: { Idempotency-Key: uuid }
   Body: { amountNano, destinationAddress }
   → { withdrawalId, status: 'PENDING', estimatedFeeNano }

4. Toast t('wallet.toast.withdrawProcessing') → navigate /wallet

5. Backend signs, sends via ton4j

6. With the next GET /wallet/summary - pendingPayoutNano has decreased
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
  address: string;           // where they were sent
  amountNano: string;
};

// Save before sendTransaction
sessionStorage.setItem('ton_pending_intent', JSON.stringify(intent));

// Delete when confirmed or timeout
```

### When mounting a page

```
1. Check sessionStorage for pending intent
2. If there is and sentAt < 30 minutes ago:
   - escrow_deposit: run useDepositPolling(dealId)
3. If sentAt > 30 min: remove intent, show toast "timeout"
```

### `refetchOnWindowFocus`

The current `App.tsx` is set to `refetchOnWindowFocus: false`. This is correct for the general case, but for financial queries an override is needed:

```typescript
// In useDepositPolling
useQuery({
  queryKey: dealKeys.deposit(dealId),
  refetchOnWindowFocus: true, // override global false
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
    useTonTransaction.ts # wrapper around sendTransaction
    useTonWalletStatus.ts # connection status + address
    useDepositPolling.ts # polling escrow deposit status
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
wallet.status.confirming # "Confirming {current}/{required}"
wallet.status.operatorReview # "Pending operator review"
wallet.status.overpaid # "The amount is greater - the difference will be returned"
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
wallet.error.underpaid # "Received {received}, expected {expected}"
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
