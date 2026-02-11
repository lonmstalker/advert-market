# TON Connect интеграция

> Сквозной документ: lifecycle TON-транзакций на фронтенде, хуки, утилиты, error handling.
> Не дублирует UI-детали из [03-deals.md](03-deals.md) (3.8) и [04-wallet.md](04-wallet.md) (4.2) — ссылается на них.
> Backend counterpart: [01-ton-sdk-integration.md](../14-implementation-specs/01-ton-sdk-integration.md).
>
> **Архитектурное решение**: платформенного кошелька нет. TON Connect используется только для оплаты сделок (escrow deposit).
> Вывод средств (withdrawal) выполняется бэкендом без TON Connect.

---

## 7.1 Архитектура

### Принцип

**Фронтенд подписывает, бэкенд верифицирует.**

Фронтенд никогда не отправляет BOC на бэкенд. Backend TON Deposit Watcher сам детектит транзакции через polling TON Center API по адресу назначения.

### Диаграмма

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

### Модель адресов

Каждая сделка получает **уникальный депозитный адрес** (subwallet_id derivation от deal ID). Backend идентифицирует депозит по адресу назначения — дополнительный payload/comment **не нужен**.

Формат: **non-bounceable** (`UQ...`) — т.к. wallet может быть неинициализирован.

### Два типа TON-операций

| # | Операция | TON Connect? | Кто подписывает | Frontend trigger |
|---|----------|:---:|---|---|
| 1 | **Escrow Deposit** | Да | Пользователь | Payment Sheet (3.8) |
| 2 | **Withdrawal** | Нет | Backend | Withdraw (4.2) → POST API |

> **Убрано**: Platform Deposit (пополнение баланса платформы). Платформенного кошелька нет — все оплаты per-deal.

---

## 7.2 Зависимости и конфигурация

### Установленные пакеты

| Пакет | Версия | Назначение |
|-------|--------|-----------|
| `@tonconnect/ui-react` | ^2.4.1 | TON Connect UI + React hooks |
| `@telegram-apps/sdk-react` | ^3.3.9 | Mini App SDK (launch params, back button) |

### `@ton/core` — НЕ нужен для MVP

Простые переводы (address + amount) не требуют Cell building. `@tonconnect/ui-react` предоставляет `sendTransaction()` с достаточным API. `@ton/core` понадобится позже для:
- Валидации адресов (bounceable/non-bounceable parsing)
- Формирования payload/comment
- Генерации `ton://transfer/...` QR-ссылок

### TonConnectUIProvider (уже настроен)

```typescript
// App.tsx — текущая конфигурация
const TON_MANIFEST_URL = `${window.location.origin}/tonconnect-manifest.json`;

<TonConnectUIProvider manifestUrl={TON_MANIFEST_URL}>
```

**TODO**: добавить `twaReturnUrl` для корректного возврата из внешнего кошелька:

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

// Ссылка на транзакцию: `${explorerBaseUrl}/transaction/${txHash}`
// Ссылка на адрес: `${explorerBaseUrl}/${address}`
```

Используется в 4.4 (Transaction Detail) — кнопка "Открыть в TON Explorer".

---

## 7.3 Хуки и утилиты

### `useTonTransaction()`

Единая обёртка над `sendTransaction` с error handling, toast, debounce.

```typescript
type UseTonTransactionOptions = {
  onSuccess?: () => void;
  onError?: (error: TonTransactionError) => void;
};

type TonTransactionParams = {
  address: string;      // non-bounceable UQ... или bounceable EQ...
  amountNano: string;   // nanoTON как строка
  validUntil?: number;  // unix timestamp, default: now + 600s
};

function useTonTransaction(options?: UseTonTransactionOptions) {
  return {
    send: (params: TonTransactionParams) => Promise<void>;
    isPending: boolean;    // блокирует кнопку, предотвращает повторные клики
    error: TonTransactionError | null;
  };
}
```

Внутри:
1. Проверяет `tonConnectUI.connected` — если нет, бросает `WALLET_NOT_CONNECTED`
2. Формирует `SendTransactionRequest`
3. Вызывает `tonConnectUI.sendTransaction()`
4. При успехе — toast success + `onSuccess()`
5. При ошибке — маппинг через `mapTonConnectError()` + toast error + `onError()`
6. `isPending` = true на всё время (предотвращает double-send)

### `useTonWalletStatus()`

Статус подключения + адрес кошелька.

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

Основан на `useTonConnectUI()` и `useTonWallet()` из `@tonconnect/ui-react`.

### `useDepositPolling(dealId, options?)`

Adaptive polling статуса депозита после отправки транзакции.

```typescript
type DepositPollingOptions = {
  enabled: boolean;            // запускать ли polling
  onConfirmed?: () => void;    // callback при FUNDED
  onTimeout?: () => void;      // callback при истечении таймаута
  timeoutMs?: number;          // default: 30 * 60 * 1000 (30 мин)
};

function useDepositPolling(dealId: string, options: DepositPollingOptions) {
  return {
    depositStatus: DepositStatus;   // см. 7.4
    confirmations: number | null;
    requiredConfirmations: number | null;
    isPolling: boolean;
    elapsed: number;                // ms с начала polling
  };
}
```

Внутри:
- `refetchInterval: 10_000` (10s) на `dealKeys.detail(dealId)` или `dealKeys.deposit(dealId)`
- Стартует по `enabled: true` (после успешного `sendTransaction`)
- Останавливается при: статус `FUNDED`, таймаут 30 мин, `enabled: false`
- Сохраняет `pollingStartedAt` в `sessionStorage` для resume (см. 7.7)

### Утилиты

#### `ton-amount.ts`

```typescript
// nanoTON → "1 250.00 TON"
function formatTonAmount(nanoTon: bigint | string): string;

// nanoTON → строка для sendTransaction
function toNanoString(nanoTon: bigint): string;

// TON (float input) → nanoTON
function parseUserTonInput(input: string): bigint;
```

#### `ton-errors.ts`

```typescript
type TonTransactionError =
  | { code: 'WALLET_NOT_CONNECTED' }
  | { code: 'USER_REJECTED' }            // пользователь отменил в кошельке
  | { code: 'INSUFFICIENT_FUNDS' }
  | { code: 'TIMEOUT' }                  // validUntil истёк
  | { code: 'NETWORK_MISMATCH' }         // кошелёк в другой сети
  | { code: 'DISCONNECTED_DURING_TX' }   // disconnect во время транзакции
  | { code: 'UNKNOWN'; message: string };

// TON Connect error → наш тип + i18n key
function mapTonConnectError(error: unknown): TonTransactionError;

// TonTransactionError → i18n key для toast
function getErrorI18nKey(error: TonTransactionError): string;
```

Маппинг на i18n:

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

## 7.4 Flow 1: Оплата сделки (Escrow Deposit)

> UI-детали: [03-deals.md#3.8](03-deals.md#38-оплата-sheet--ton-connect)

### Trigger

Кнопка `t('deals.payment.pay')` на Payment Sheet (3.8).

### Предусловия

- Сделка в статусе `AWAITING_PAYMENT`
- TON Connect кошелёк подключён
- Роль: рекламодатель

### Шаги

```
1. Фронт: GET /api/v1/deals/:dealId/deposit
   → { escrowAddress, amountNano, status, expiresAt }

2. Фронт: useTonTransaction().send({
     address: escrowAddress,
     amountNano: amountNano.toString(),
   })

3. TON Connect открывает кошелёк → пользователь подтверждает

4. Promise resolved → toast t('wallet.toast.paymentSent')
   Сохраняем в sessionStorage: { dealId, sentAt: Date.now() }

5. useDepositPolling(dealId, { enabled: true })
   Polling GET /api/v1/deals/:dealId каждые 10s

6. Backend Deposit Watcher детектит tx → считает confirmations →
   при достижении required → deal.status = FUNDED

7. Фронт видит FUNDED → toast t('wallet.toast.paymentConfirmed') → закрыть sheet
```

### API контракт: `GET /api/v1/deals/:dealId/deposit`

**Response** (Zod schema в `src/features/ton/types/ton.ts`):

```typescript
const DepositInfoSchema = z.object({
  escrowAddress: z.string(),           // non-bounceable UQ...
  amountNano: z.string(),              // bigint as string
  dealId: z.string(),
  status: z.enum([
    'AWAITING_PAYMENT',                // ожидает отправки
    'TX_DETECTED',                     // tx обнаружена, ждём confirmations
    'CONFIRMING',                      // набираем confirmations
    'AWAITING_OPERATOR_REVIEW',        // >1000 TON, ожидает оператора
    'CONFIRMED',                       // подтверждено (= deal FUNDED)
    'EXPIRED',                         // таймаут (30 мин без tx)
    'UNDERPAID',                       // сумма меньше ожидаемой
    'OVERPAID',                        // сумма больше ожидаемой (manual review)
    'REJECTED',                        // оператор отклонил
  ]),
  currentConfirmations: z.number().nullable(),
  requiredConfirmations: z.number().nullable(),
  receivedAmountNano: z.string().nullable(),   // сколько фактически получено
  txHash: z.string().nullable(),               // hash если tx обнаружена
  expiresAt: z.string().nullable(),            // ISO 8601, когда deposit expire
});
```

### Промежуточные статусы (UI)

| Deposit status | UI на Payment Sheet / Deal Detail |
|---|---|
| `AWAITING_PAYMENT` | Кнопка "Оплатить" активна |
| `TX_DETECTED` | Spinner + `t('wallet.status.txDetected')` |
| `CONFIRMING` | Progress: `t('wallet.status.confirming', { current, required })` |
| `AWAITING_OPERATOR_REVIEW` | `t('wallet.status.operatorReview')` — info text, НЕ ошибка |
| `CONFIRMED` | Toast success → закрыть sheet |
| `EXPIRED` | Toast error `t('wallet.error.depositExpired')` + кнопка retry |
| `UNDERPAID` | Alert `t('wallet.error.underpaid', { received, expected })` + инструкция дослать |
| `OVERPAID` | Info `t('wallet.status.overpaid')` — "разница будет возвращена" |
| `REJECTED` | Alert error `t('wallet.error.depositRejected')` + support link |

### Tiered confirmations (отображение)

Из [06-confirmation-policy.md](../07-financial-system/06-confirmation-policy.md):

| Amount | Required | UI |
|--------|:---:|----|
| <= 100 TON | 1 | Быстро, обычно без видимого progress |
| <= 1,000 TON | 3 | Progress bar "2/3 confirmations" |
| > 1,000 TON | 5 + operator | Progress bar + info "Ожидает проверки оператором" |

### Защита от двойной отправки

| Уровень | Механизм | Описание |
|---------|----------|----------|
| **UI** | `isPending` от `useTonTransaction` | Дизейбл кнопки на время операции |
| **Блокчейн** | TON wallet seqno | Wallet contract отклоняет replay (seqno mismatch) |
| **Backend** | `tx_hash` PK в `ton_transactions` | Дублирующий deposit игнорируется |
| **Backend** | Optimistic lock на `deals.version` | Повторный FUNDED — idempotent no-op |
| **Backend** | Redis lock `lock:escrow:{deal_id}` | Предотвращает concurrent processing |

### Error states

| Ошибка | Код | UI |
|--------|-----|-----|
| Кошелёк отклонил | `USER_REJECTED` | Toast `t('wallet.error.walletRejected')` |
| Недостаточно TON | `INSUFFICIENT_FUNDS` | Toast `t('wallet.error.insufficientTon')` |
| Таймаут подписания (10 мин) | `TIMEOUT` | Toast `t('wallet.error.timeout')` + retry |
| Disconnect во время tx | `DISCONNECTED_DURING_TX` | Toast `t('wallet.error.disconnected')` |
| Network mismatch | `NETWORK_MISMATCH` | Toast `t('wallet.error.networkMismatch')` |
| Polling таймаут (30 мин) | — | Alert `t('wallet.error.pollingTimeout')` + support link |
| Deposit expired (бэкенд) | — | Alert `t('wallet.error.depositExpired')` |

---

## 7.5 Flow 2: Вывод средств (Owner Withdrawal)

> UI-детали: [04-wallet.md#4.2](04-wallet.md#42-вывод-средств-только-владелец-канала)

### Особенность

**НЕ использует TON Connect** для подписания. Транзакцию формирует и подписывает бэкенд через ton4j.

### Шаги

```
1. Пользователь вводит сумму + адрес (pre-filled из TON Connect или ручной)

2. DialogModal подтверждения

3. Фронт: POST /api/v1/wallet/withdraw
   Headers: { Idempotency-Key: uuid }
   Body: { amountNano, destinationAddress }
   → { withdrawalId, status: 'PENDING', estimatedFeeNano }

4. Toast t('wallet.toast.withdrawProcessing') → navigate /wallet

5. Backend подписывает, отправляет через ton4j

6. При следующем GET /wallet/summary — pendingPayoutNano уменьшился
```

### Защита от двойного вывода

| Уровень | Механизм | Описание |
|---------|----------|----------|
| **UI** | `isPending` от `useMutation` | Дизейбл кнопки на время запроса |
| **API** | `Idempotency-Key` header | Backend дедуплицирует по ключу (Redis, TTL 24h) |
| **Backend** | Redis lock `lock:payout:{user_id}` | Один вывод одновременно |
| **Backend** | `OWNER_PENDING` balance check | Нельзя вывести больше, чем есть |

### Адрес назначения

- Если TON Connect подключён: pre-filled из `useTonWalletStatus().friendlyAddress`
- Иначе: ручной ввод с валидацией формата `EQ...` / `UQ...` (regex, длина 48 символов base64)
- Пользователь может изменить pre-filled адрес

### Error states

| Ошибка | HTTP | UI |
|--------|------|----|
| Недостаточно средств | 400 | Toast `t('wallet.error.insufficientFunds')` |
| Невалидный адрес | 400 | Inline error `t('wallet.error.invalidAddress')` |
| Лимит вывода | 400 | Toast `t('wallet.error.withdrawLimit')` |
| Rate limit | 429 | Toast `t('errors.rateLimited')` |
| Вывод failed (бэкенд) | — | Toast `t('wallet.error.withdrawFailed')` + support link |

---

## 7.6 Состояние подключения кошелька

### TonConnectButton

| Место | Когда показывать | Компонент |
|-------|-----------------|-----------|
| Payment Sheet (3.8) | Кошелёк не подключён | Кнопка `t('wallet.connectWallet')` |
| Wallet Main (4.1) | Всегда | Badge: адрес (truncated) или кнопка подключения |
| Withdraw (4.2) | Опционально | Pre-fill адреса если подключён |

### Auto-reconnect

`@tonconnect/ui-react` автоматически восстанавливает сессию из localStorage. При возврате в Mini App кошелёк обычно уже подключён.

### Disconnect flow

- Кнопка disconnect доступна на Wallet Main (4.1)
- При disconnect: очистка `friendlyAddress`, обновление UI
- **Не влияет** на уже отправленные транзакции

---

## 7.7 Resume / Recovery

### Проблема

Telegram может убить Mini App, пока пользователь в кошельке. При возврате React state сброшен.

### Решение

Сохраняем "pending intent" в `sessionStorage`:

```typescript
type PendingTonIntent = {
  type: 'escrow_deposit';
  dealId: string;
  sentAt: number;            // timestamp
  address: string;           // куда отправляли
  amountNano: string;
};

// Сохранять перед sendTransaction
sessionStorage.setItem('ton_pending_intent', JSON.stringify(intent));

// Удалять при confirmed или timeout
```

### При монтировании страницы

```
1. Проверить sessionStorage на pending intent
2. Если есть и sentAt < 30 мин назад:
   - escrow_deposit: запустить useDepositPolling(dealId)
3. Если sentAt > 30 мин: удалить intent, показать toast "таймаут"
```

### `refetchOnWindowFocus`

В текущем `App.tsx` установлен `refetchOnWindowFocus: false`. Это корректно для общего случая, но для финансовых queries нужен override:

```typescript
// В useDepositPolling
useQuery({
  queryKey: dealKeys.deposit(dealId),
  refetchOnWindowFocus: true,     // override глобального false
  refetchInterval: 10_000,
});
```

---

## 7.8 Error handling (общее)

### Retry стратегия

| Ситуация | Retry |
|----------|-------|
| `sendTransaction` rejected | Нет auto-retry. Пользователь нажимает кнопку заново |
| Polling 4xx/5xx | React Query retry: 1 attempt, потом показать error |
| Network offline | Приостановить polling, возобновить при online |

### `validUntil`

Все транзакции: `Math.floor(Date.now() / 1000) + 600` (10 минут). Если пользователь "думает" дольше — кошелёк отклонит, нужно запросить заново.

### Debounce / double-send prevention

`useTonTransaction().isPending` блокирует кнопку на всё время операции. Дополнительно — `disabled` на кнопке пока `isPending === true`.

### Fallback при disconnect во время транзакции

Если `sendTransaction()` бросает ошибку disconnect:
1. Toast `t('wallet.error.disconnected')`
2. **Не** пытаемся reconnect автоматически
3. Показываем кнопку "Подключить кошелёк" заново
4. Транзакция на блокчейне может быть уже отправлена — проверяем через polling

---

## 7.9 Тестирование

### Testnet конфигурация

- `VITE_TON_NETWORK=testnet`
- Backend: `ton.api.url=https://testnet.toncenter.com/api/v2/`
- Manifest: `tonconnect-manifest.json` с testnet network

### Test wallet

- Tonkeeper (testnet mode) или MyTonWallet
- Testnet TON faucet: `@testgiver_ton_bot` в Telegram
- Рекомендуемый тестовый баланс: 10+ TON

### Ручной чеклист QA

**Flow 1: Escrow Deposit**
- [ ] Кошелёк не подключён → показать кнопку "Подключить"
- [ ] Подключить кошелёк → адрес отображается
- [ ] Нажать "Оплатить" → открывается кошелёк
- [ ] Подтвердить в кошельке → toast "Транзакция отправлена"
- [ ] Дождаться FUNDED → toast "Оплата подтверждена", sheet закрыт
- [ ] Отменить в кошельке → toast "Отклонено", кнопка активна
- [ ] Закрыть Mini App во время ожидания → вернуться → polling продолжается

**Flow 2: Withdrawal**
- [ ] Ввести сумму + адрес → "Макс" заполняет максимум
- [ ] Подтвердить → DialogModal → POST → navigate /wallet
- [ ] pendingPayoutNano уменьшился
- [ ] Повторный POST с тем же Idempotency-Key → не создаёт дубликат

**Edge cases**
- [ ] Network mismatch (mainnet wallet + testnet backend)
- [ ] Двойной клик "Оплатить" — только одна транзакция
- [ ] Таймаут 30 мин без подтверждения
- [ ] Underpayment (отправить меньше требуемой суммы)
- [ ] Offline → восстановление → polling возобновляется

---

## 7.10 Файловая структура

```
src/features/ton/
  hooks/
    useTonTransaction.ts          # обёртка над sendTransaction
    useTonWalletStatus.ts         # статус подключения + адрес
    useDepositPolling.ts          # polling статуса escrow deposit
  lib/
    ton-amount.ts                 # formatTonAmount, toNanoString, parseUserTonInput
    ton-errors.ts                 # mapTonConnectError, getErrorI18nKey
    ton-intent.ts                 # PendingTonIntent: save/load/clear sessionStorage
    ton-explorer.ts               # getExplorerTxUrl, getExplorerAddressUrl
  types/
    ton.ts                        # Zod schemas: DepositInfoSchema, etc.
```

Не конфликтует с:
- `src/features/deals/` — PaymentSheet.tsx импортирует из `features/ton/`
- `src/features/wallet/` — WithdrawPage импортирует `useTonWalletStatus` для pre-fill адреса

---

## 7.11 Новые i18n ключи

Все в namespace `wallet.*` (уже определён в [06-shared-components.md](06-shared-components.md)):

```
wallet.connectWallet
wallet.status.txDetected
wallet.status.confirming              # "Подтверждение {current}/{required}"
wallet.status.operatorReview          # "Ожидает проверки оператором"
wallet.status.overpaid                # "Сумма больше — разница будет возвращена"
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
wallet.error.underpaid                # "Получено {received}, ожидалось {expected}"
wallet.error.depositRejected
wallet.error.insufficientFunds
wallet.error.invalidAddress
wallet.error.withdrawLimit
wallet.error.withdrawFailed
```

---

## Связи с другими документами

| Документ | Что использует |
|----------|---------------|
| [03-deals.md](03-deals.md) (3.8) | Flow 1, hooks, error mapping |
| [04-wallet.md](04-wallet.md) (4.1, 4.2) | Flow 2, wallet badge |
| [06-shared-components.md](06-shared-components.md) | Error states (6.2), i18n namespace (6.1), toast patterns |
| [01-ton-sdk-integration.md](../14-implementation-specs/01-ton-sdk-integration.md) | Backend: address generation, deposit watcher, tx lifecycle |
| [02-escrow-flow.md](../07-financial-system/02-escrow-flow.md) | Backend: full escrow lifecycle |
| [06-confirmation-policy.md](../07-financial-system/06-confirmation-policy.md) | Backend: tiered confirmations, operator review |
| [07-idempotency-strategy.md](../05-patterns-and-decisions/07-idempotency-strategy.md) | Idempotency-Key для withdrawal |

---

## Верификация

1. **Оба flow покрыты** — escrow deposit (7.4), withdrawal (7.5)
2. **Каждый flow**: trigger → предусловие → шаги → API контракт → промежуточные статусы → error states
3. **Нет дублирования** — UI-детали в 03-deals.md и 04-wallet.md, здесь только lifecycle и интеграция
4. **Confirmations как first-class** — промежуточные статусы (TX_DETECTED, CONFIRMING, AWAITING_OPERATOR_REVIEW)
5. **Operator review покрыт** — UI для >1000 TON (7.4 таблица промежуточных статусов)
6. **Underpayment/overpayment покрыт** — статусы UNDERPAID/OVERPAID + i18n
7. **Resume/recovery покрыт** — sessionStorage intent (7.7)
8. **Double-send prevention** — матрица защиты по уровням (7.4, 7.5)
9. **Idempotency-Key** — для withdrawal (7.5), не нужен для escrow deposit
10. **Файловая структура** `src/features/ton/` не конфликтует с existing features
11. **i18n ключи** из `wallet.*` namespace
12. **Убран Platform Deposit** — платформенного кошелька нет