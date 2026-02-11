# Кошелёк

> Tab 3. Баланс платформы, пополнение, вывод, история транзакций.

## Навигация

```
/wallet
  ├── /wallet/top-up
  ├── /wallet/withdraw
  ├── /wallet/history
  │   └── /wallet/history/:txId
  └── [Sheet] Фильтры истории
```

---

## Новые API endpoints

> Эти endpoints **отсутствуют** в текущем `11-api-contracts.md` и требуют добавления.

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/api/v1/wallet/balance` | Текущий баланс | Authenticated |
| `GET` | `/api/v1/wallet/transactions` | Список транзакций | Authenticated |
| `GET` | `/api/v1/wallet/transactions/:txId` | Детали транзакции | Owner |
| `POST` | `/api/v1/wallet/withdraw` | Запрос на вывод | Authenticated |
| `GET` | `/api/v1/wallet/deposit-address` | Адрес для пополнения | Authenticated |

### Новые query keys (добавить в `query-keys.ts`)

```typescript
export const walletKeys = {
  balance: ['wallet', 'balance'] as const,
  transactions: () => ['wallet', 'transactions'] as const,
  transactionList: (params?: PaginationParams & Record<string, string | undefined>) =>
    [...walletKeys.transactions(), params] as const,
  transactionDetail: (txId: string) => [...walletKeys.transactions(), txId] as const,
  depositAddress: ['wallet', 'deposit-address'] as const,
};
```

---

## 4.1 Главная кошелька

| | |
|---|---|
| **Route** | `/wallet` |
| **Цель** | Баланс, быстрые действия, последние транзакции |
| **Кто видит** | Все авторизованные |

### API

```
GET /api/v1/wallet/balance
GET /api/v1/wallet/transactions?limit=5
```

**Query keys:** `walletKeys.balance`, `walletKeys.transactionList({ limit: 5 })`

**Network mode:** `{ networkMode: 'online', staleTime: 0 }` для баланса (финансовые данные).

### UI

- **Баланс** — hero / `title1`, bold, по центру, `tabular-nums`, `<Amount>` (формат: "1 250.00 TON")
- **TON Connect badge** — если кошелёк не подключён: compact кнопка `t('wallet.connectWallet')`
- **Быстрые действия** — ряд circular icon buttons:
  - `t('wallet.topUp')` (↓ иконка) → `/wallet/top-up`
  - `t('wallet.withdraw')` (↑ иконка) → `/wallet/withdraw`
- **Group `t('wallet.recentTransactions')`** — до 5 последних транзакций (`GroupItem`):
  - `before`: иконка типа (deposit/withdraw/escrow/commission/payout)
  - Заголовок: описание операции
  - `after`: сумма с цветом (зелёная = доход, красная = расход) + дата (`caption`)
- Link `t('wallet.allHistory')` → `/wallet/history`

### Действия

| Действие | Результат |
|----------|-----------|
| "Пополнить" | → `/wallet/top-up` |
| "Вывести" | → `/wallet/withdraw` |
| Тап по транзакции | → `/wallet/history/:txId` |
| "Вся история" | → `/wallet/history` |
| Pull-to-refresh | Инвалидация `walletKeys.balance` + `walletKeys.transactions()` |

### Empty state

| Emoji | i18n title | i18n description | CTA |
|-------|------------|------------------|-----|
| `📜` | `wallet.empty.title` | `wallet.empty.description` | `wallet.empty.cta` → `/wallet/top-up` |

### Error states

| Ошибка | UI |
|--------|----|
| Ошибка загрузки баланса | `ErrorScreen` + retry |
| Ошибка загрузки транзакций | Секция транзакций: inline error + retry |
| Offline | Banner `t('errors.offline')` |

---

## 4.2 Пополнение баланса

| | |
|---|---|
| **Route** | `/wallet/top-up` |
| **Цель** | Пополнить баланс платформы через TON Connect |
| **Кто видит** | Все авторизованные |

### API

```
GET /api/v1/wallet/balance
GET /api/v1/wallet/deposit-address
```

**Query keys:** `walletKeys.balance`, `walletKeys.depositAddress`

### UI

- **Input `t('wallet.topUp.amount')`** — numeric, TON, крупный шрифт (`title1`) по центру
- **Quick amount chips** — ряд: 10 / 50 / 100 / 500 TON
- **Текущий баланс** — `caption`, `secondary`: `t('wallet.topUp.currentBalance', { amount })`
- Кнопка `t('wallet.topUp.submit')` (`primary`, full-width)

### Действия

| Действие | Результат |
|----------|-----------|
| Тап по chip | Заполнить Input суммой |
| "Пополнить" | TON Connect транзакция → toast `t('wallet.toast.topUpProcessing')` → navigate `/wallet` |

### TON Connect

```typescript
const transaction = {
  validUntil: Math.floor(Date.now() / 1000) + 600,
  messages: [{
    address: depositAddress,
    amount: amountNano.toString(),
  }],
};
await tonConnectUI.sendTransaction(transaction);
```

### Валидация

- Сумма > 0
- TON Connect кошелёк подключён (иначе — `t('wallet.error.connectFirst')`)

### Error states

| Ошибка | UI | Описание |
|--------|----|----------|
| Кошелёк отклонил транзакцию | Toast `t('wallet.error.walletRejected')` | Пользователь отменил в кошельке |
| Недостаточно TON на кошельке | Toast `t('wallet.error.insufficientTon')` | Баланс внешнего кошелька < суммы |
| Таймаут транзакции | Toast `t('wallet.error.timeout')` + retry | Транзакция не подтвердилась за 10 мин |
| TON Connect disconnect | Toast `t('wallet.error.disconnected')` | Кошелёк отключился во время операции |

---

## 4.3 Вывод средств

| | |
|---|---|
| **Route** | `/wallet/withdraw` |
| **Цель** | Вывести средства на внешний TON-кошелёк |
| **Кто видит** | Все авторизованные с положительным балансом |

### API

```
GET  /api/v1/wallet/balance
POST /api/v1/wallet/withdraw
```

**Query keys:** `walletKeys.balance`

### UI

- **Доступный баланс** — `title2`, bold
- **Input `t('wallet.withdraw.amount')`** — numeric, max = баланс, кнопка `t('wallet.withdraw.max')` (inline)
- **Input `t('wallet.withdraw.address')`** — если TON Connect подключён: pre-filled, иначе: ручной ввод
- **Расчёт комиссии сети** — `caption`, `secondary` (обновляется при вводе суммы)
- **Итого к получению** — `title3`
- Кнопка `t('wallet.withdraw.submit')` (`primary`, full-width)

### Request body

```typescript
{
  amountNano: bigint;       // > 0, <= balance
  destinationAddress: string; // valid TON address
}
```

### Действия

| Действие | Результат |
|----------|-----------|
| "Макс" | Заполнить максимальную сумму (баланс - комиссия) |
| "Вывести" | → `DialogModal` подтверждения → `POST /api/v1/wallet/withdraw` → toast → navigate `/wallet` |

### Валидация

- Сумма > 0 и <= доступного баланса
- Адрес — валидный TON address (формат `EQ...` или `UQ...`)

### Error states

| Ошибка | UI | Описание |
|--------|----|----------|
| Недостаточно средств | Toast `t('wallet.error.insufficientFunds')` | Баланс изменился между загрузкой и отправкой |
| Невалидный адрес | Inline error `t('wallet.error.invalidAddress')` | Формат адреса не соответствует TON |
| Лимит вывода | Toast `t('wallet.error.withdrawLimit')` | Превышен дневной/разовый лимит |
| 429 rate limit | Toast `t('errors.rateLimited')` | Слишком частые запросы на вывод |

---

## 4.4 История транзакций

| | |
|---|---|
| **Route** | `/wallet/history` |
| **Цель** | Полная история финансовых операций |
| **Кто видит** | Все авторизованные |

### API

```
GET /api/v1/wallet/transactions?cursor=&limit=20&type=&from=&to=
```

**Query keys:** `walletKeys.transactionList(params)`

### UI

- **Кнопка `t('wallet.history.filter')`** — с badge количества активных фильтров
- **Список транзакций** — `GroupItem`, группировка по дням:
  - `before`: иконка типа
  - Заголовок: описание
  - `after`: сумма (зелёная = доход, красная = расход) + дата
- **Infinite scroll** — skeleton загрузка

### Sheet фильтров

- **Тип** — multi-select: `t('wallet.history.filter.deposit')` / `t('wallet.history.filter.withdraw')` / `t('wallet.history.filter.escrow')` / `t('wallet.history.filter.commission')` / `t('wallet.history.filter.payout')`
- **Период** — select: `t('wallet.history.filter.week')` / `t('wallet.history.filter.month')` / `t('wallet.history.filter.all')`

### Действия

| Действие | Результат |
|----------|-----------|
| "Фильтр" | → Sheet фильтров |
| Тап по транзакции | → `/wallet/history/:txId` |

### Empty state

| Emoji | i18n title | i18n description | CTA |
|-------|------------|------------------|-----|
| `📜` | `wallet.history.empty.title` | `wallet.history.empty.description` | `wallet.history.empty.cta` → Reset filters / `/wallet/top-up` |

### Error states

| Ошибка | UI |
|--------|----|
| Ошибка загрузки | `ErrorScreen` + retry button |
| Offline | Banner `t('errors.offline')` |

---

## 4.5 Детали транзакции

| | |
|---|---|
| **Route** | `/wallet/history/:txId` |
| **Цель** | Полная информация о транзакции |
| **Кто видит** | Владелец транзакции |

### API

```
GET /api/v1/wallet/transactions/:txId
```

**Query keys:** `walletKeys.transactionDetail(txId)`

### UI

- **Сумма** — `title1`, bold, цветовая: +зелёная / -красная, `<Amount>`
- **Статус** — badge: `pending` / `confirmed` / `failed`
- **Group `t('wallet.transaction.details')`** — `GroupItem`:
  - `t('wallet.transaction.type')`
  - `t('wallet.transaction.date')` (formatted)
  - `t('wallet.transaction.hash')` (copyable, truncated с `...`)
  - `t('wallet.transaction.deal')` (link → `/deals/:dealId`, если есть)
  - `t('wallet.transaction.commission')` (если есть)
  - `t('wallet.transaction.from')` / `t('wallet.transaction.to')` (truncated addresses)

### Действия

| Действие | Результат |
|----------|-----------|
| Копировать hash | `navigator.clipboard` → toast `t('common.copied')` |
| Тап "Сделка" | → `/deals/:dealId` |
| "Открыть в TON Explorer" | External link (Telegram `openLink`) |

### Error states

| Ошибка | UI |
|--------|----|
| 404 транзакция не найдена | `ErrorScreen` `t('errors.notFound.title')` + navigate `/wallet/history` |
| Ошибка загрузки | `ErrorScreen` + retry |

---

## Файловая структура

```
src/pages/wallet/
  WalletPage.tsx              # Route: /wallet
  TopUpPage.tsx               # Route: /wallet/top-up
  WithdrawPage.tsx            # Route: /wallet/withdraw
  HistoryPage.tsx             # Route: /wallet/history
  TransactionDetailPage.tsx   # Route: /wallet/history/:txId

src/features/wallet/
  api/
    wallet.ts
  components/
    BalanceHero.tsx
    QuickActions.tsx
    TransactionListItem.tsx
    TransactionFiltersSheet.tsx
    QuickAmountChips.tsx
    AmountInput.tsx
  hooks/
    useWalletBalance.ts
  types/
    wallet.ts                  # Zod schemas + types
```
