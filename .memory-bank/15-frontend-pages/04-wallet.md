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
- **TON Connect badge** — если кошелёк не подключён: compact кнопка "Подключить кошелёк"
- **Быстрые действия** — ряд circular icon buttons:
  - Пополнить (↓ иконка) → `/wallet/top-up`
  - Вывести (↑ иконка) → `/wallet/withdraw`
- **Group "Последние операции"** — до 5 последних транзакций (`GroupItem`):
  - `before`: иконка типа (deposit/withdraw/escrow/commission/payout)
  - Заголовок: описание операции
  - `after`: сумма с цветом (зелёная = доход, красная = расход) + дата (`caption`)
- Link "Вся история" → `/wallet/history`

### Действия

| Действие | Результат |
|----------|-----------|
| "Пополнить" | → `/wallet/top-up` |
| "Вывести" | → `/wallet/withdraw` |
| Тап по транзакции | → `/wallet/history/:txId` |
| "Вся история" | → `/wallet/history` |
| Pull-to-refresh | Инвалидация `walletKeys.balance` + `walletKeys.transactions()` |

### Empty state

| Emoji | Заголовок | Описание | CTA |
|-------|-----------|----------|-----|
| `📜` | Нет операций | История платежей появится здесь | [Пополнить баланс] → `/wallet/top-up` |

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

- **Input "Сумма"** — numeric, TON, крупный шрифт (`title1`) по центру
- **Quick amount chips** — ряд: 10 / 50 / 100 / 500 TON
- **Текущий баланс** — `caption`, `secondary`: "Текущий баланс: X TON"
- Кнопка "Пополнить" (`primary`, full-width)

### Действия

| Действие | Результат |
|----------|-----------|
| Тап по chip | Заполнить Input суммой |
| "Пополнить" | TON Connect транзакция → toast "Пополнение обрабатывается" → navigate `/wallet` |

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
- TON Connect кошелёк подключён (иначе — "Подключите кошелёк")

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
- **Input "Сумма"** — numeric, max = баланс, кнопка "Макс" (inline)
- **Input "Адрес кошелька"** — если TON Connect подключён: pre-filled, иначе: ручной ввод
- **Расчёт комиссии сети** — `caption`, `secondary` (обновляется при вводе суммы)
- **Итого к получению** — `title3`
- Кнопка "Вывести" (`primary`, full-width)

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

- **Кнопка "Фильтр"** — с badge количества активных фильтров
- **Список транзакций** — `GroupItem`, группировка по дням:
  - `before`: иконка типа
  - Заголовок: описание
  - `after`: сумма (зелёная = доход, красная = расход) + дата
- **Infinite scroll** — skeleton загрузка

### Sheet фильтров

- **Тип** — multi-select: Пополнение / Вывод / Эскроу / Комиссия / Выплата
- **Период** — select: За неделю / За месяц / Всё время

### Действия

| Действие | Результат |
|----------|-----------|
| "Фильтр" | → Sheet фильтров |
| Тап по транзакции | → `/wallet/history/:txId` |

### Empty state

| Emoji | Заголовок | Описание | CTA |
|-------|-----------|----------|-----|
| `📜` | Нет операций | История платежей появится здесь | [Пополнить баланс] → `/wallet/top-up` |

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
- **Group "Детали"** — `GroupItem`:
  - Тип операции
  - Дата/время (formatted)
  - Hash транзакции (copyable, truncated с `...`)
  - Связанная сделка (link → `/deals/:dealId`, если есть)
  - Комиссия (если есть)
  - From/To аккаунт (truncated addresses)

### Действия

| Действие | Результат |
|----------|-----------|
| Копировать hash | `navigator.clipboard` → toast "Скопировано" |
| Тап "Сделка" | → `/deals/:dealId` |
| "Открыть в TON Explorer" | External link (Telegram `openLink`) |

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
