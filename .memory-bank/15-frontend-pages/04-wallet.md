# Финансы

> Tab 3. Финансовый кабинет: сводка по сделкам, история транзакций, вывод заработка (владелец канала).
>
> **Архитектурное решение**: платформенного кошелька нет. Все TON-операции привязаны к сделкам (per-deal escrow).
> Рекламодатель оплачивает каждую сделку напрямую на уникальный эскроу-адрес.
> Владелец канала получает выплаты из `OWNER_PENDING` после завершения сделок.

## Навигация

```
/wallet
  ├── /wallet/withdraw          # Только для владельцев каналов
  ├── /wallet/history
  │   └── /wallet/history/:txId
  └── [Sheet] Фильтры истории
```

---

## API endpoints

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/api/v1/wallet/summary` | Финансовая сводка | Authenticated |
| `GET` | `/api/v1/wallet/transactions` | Список транзакций | Authenticated |
| `GET` | `/api/v1/wallet/transactions/:txId` | Детали транзакции | Owner |
| `POST` | `/api/v1/wallet/withdraw` | Запрос на вывод | Channel Owner |

### Query keys (добавить в `query-keys.ts`)

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

## 4.1 Главная финансов

| | |
|---|---|
| **Route** | `/wallet` |
| **Цель** | Финансовая сводка, быстрые действия, последние транзакции |
| **Кто видит** | Все авторизованные |

### API

```
GET /api/v1/wallet/summary
GET /api/v1/wallet/transactions?limit=5
```

**Query keys:** `walletKeys.summary`, `walletKeys.transactionList({ limit: 5 })`

**Network mode:** `{ networkMode: 'online', staleTime: 0 }` для summary (финансовые данные).

### Response: `GET /api/v1/wallet/summary`

```typescript
const WalletSummarySchema = z.object({
  // Для владельца канала
  earnedTotalNano: z.string(),      // Всего заработано за всё время
  pendingPayoutNano: z.string(),    // Доступно для вывода (OWNER_PENDING)
  inEscrowNano: z.string(),        // Заморожено в активных сделках
  withdrawnTotalNano: z.string(),   // Выведено за всё время

  // Для рекламодателя
  spentTotalNano: z.string(),       // Всего потрачено за всё время
  activeEscrowNano: z.string(),     // Заморожено в активных сделках

  // Общее
  activeDealsCount: z.number(),
  completedDealsCount: z.number(),
});
```

Поля зависят от роли пользователя (бэкенд возвращает релевантные, остальные = "0").

### UI

**Для владельца канала:**
- **Доступно для вывода** — hero / `title1`, bold, по центру, `tabular-nums`, `<Amount>` (формат: "1 250.00 TON")
- **Сводка** — `Group` с 3 `GroupItem`:
  - `t('wallet.summary.earned')` — всего заработано
  - `t('wallet.summary.inEscrow')` — в активных сделках
  - `t('wallet.summary.withdrawn')` — выведено
- **Быстрые действия** — кнопка `t('wallet.withdraw')` (↑ иконка) → `/wallet/withdraw`
  - Показывать только если `pendingPayoutNano > 0`

**Для рекламодателя:**
- **В активных сделках** — hero / `title1`
- **Сводка** — `Group` с 2 `GroupItem`:
  - `t('wallet.summary.spent')` — всего потрачено
  - `t('wallet.summary.activeEscrow')` — в эскроу
- Кнопки вывода **нет** (рекламодатель не получает выплаты)

**Общее:**
- **Group `t('wallet.recentTransactions')`** — до 5 последних транзакций (`GroupItem`):
  - `before`: иконка типа (escrow_deposit/payout/refund/commission)
  - Заголовок: описание операции + название канала/сделки
  - `after`: сумма с цветом (зелёная = доход, красная = расход) + дата (`caption`)
- Link `t('wallet.allHistory')` → `/wallet/history`

### Действия

| Действие | Результат |
|----------|-----------|
| "Вывести" (владелец) | → `/wallet/withdraw` |
| Тап по транзакции | → `/wallet/history/:txId` |
| "Вся история" | → `/wallet/history` |
| Pull-to-refresh | Инвалидация `walletKeys.summary` + `walletKeys.transactions()` |

### Empty state

| Emoji | i18n title | i18n description | CTA |
|-------|------------|------------------|-----|
| `📜` | `wallet.empty.title` | `wallet.empty.description` | `wallet.empty.cta` → каталог каналов |

### Error states

| Ошибка | UI |
|--------|----|
| Ошибка загрузки summary | `ErrorScreen` + retry |
| Ошибка загрузки транзакций | Секция транзакций: inline error + retry |
| Offline | Banner `t('errors.offline')` |

---

## 4.2 Вывод средств (только владелец канала)

| | |
|---|---|
| **Route** | `/wallet/withdraw` |
| **Цель** | Вывести заработанные средства из `OWNER_PENDING` на внешний TON-кошелёк |
| **Кто видит** | Владельцы каналов с `pendingPayoutNano > 0` |

### API

```
GET  /api/v1/wallet/summary
POST /api/v1/wallet/withdraw
```

**Query keys:** `walletKeys.summary`

**Headers:** `Idempotency-Key: {uuid}` — генерируется при монтировании формы, обновляется после успешной отправки.

### UI

- **Доступно для вывода** — `title2`, bold, `<Amount>`
- **Input `t('wallet.withdraw.amount')`** — numeric, max = pendingPayoutNano, кнопка `t('wallet.withdraw.max')` (inline)
- **Input `t('wallet.withdraw.address')`** — если TON Connect подключён: pre-filled, иначе: ручной ввод
- **Расчёт комиссии сети** — `caption`, `secondary` (фиксированная или обновляется при вводе суммы)
- **Итого к получению** — `title3`
- Кнопка `t('wallet.withdraw.submit')` (`primary`, full-width)

### Request body

```typescript
const WithdrawRequestSchema = z.object({
  amountNano: z.string(),               // bigint as string, > 0, <= pendingPayoutNano
  destinationAddress: z.string(),        // valid TON address (EQ... или UQ...)
});
```

### Response

```typescript
const WithdrawResponseSchema = z.object({
  withdrawalId: z.string(),              // UUID для отслеживания
  status: z.enum(['PENDING', 'SUBMITTED', 'CONFIRMED', 'FAILED']),
  estimatedFeeNano: z.string(),
});
```

### Idempotency-Key

```typescript
// Генерируем при монтировании формы, НЕ при клике
const idempotencyKey = useRef(crypto.randomUUID());

const withdraw = useMutation({
  mutationFn: (data: WithdrawRequest) =>
    api.post('/wallet/withdraw', data, {
      headers: { 'Idempotency-Key': idempotencyKey.current },
    }),
  onSuccess: () => {
    idempotencyKey.current = crypto.randomUUID(); // новый ключ для следующей операции
    queryClient.invalidateQueries({ queryKey: walletKeys.summary });
  },
});
```

Backend при получении `Idempotency-Key`:
- Первый запрос — выполняет, сохраняет `(key, response)` в Redis (TTL 24h)
- Повторный — возвращает сохранённый response без повторного выполнения

### Действия

| Действие | Результат |
|----------|-----------|
| "Макс" | Заполнить максимальную сумму (pendingPayout - комиссия) |
| "Вывести" | → `DialogModal` подтверждения → `POST /api/v1/wallet/withdraw` → toast → navigate `/wallet` |

### Валидация

- Сумма > 0 и <= доступного для вывода (`pendingPayoutNano`)
- Адрес — валидный TON address (формат `EQ...` или `UQ...`)

### Error states

| Ошибка | UI | Описание |
|--------|----|----------|
| Недостаточно средств | Toast `t('wallet.error.insufficientFunds')` | Баланс изменился между загрузкой и отправкой |
| Невалидный адрес | Inline error `t('wallet.error.invalidAddress')` | Формат адреса не соответствует TON |
| Лимит вывода | Toast `t('wallet.error.withdrawLimit')` | Превышен дневной/разовый лимит |
| 429 rate limit | Toast `t('errors.rateLimited')` | Слишком частые запросы на вывод |
| Дубликат (Idempotency-Key) | Возвращает предыдущий ответ | Прозрачно для пользователя |

---

## 4.3 История транзакций

| | |
|---|---|
| **Route** | `/wallet/history` |
| **Цель** | Полная история финансовых операций по сделкам |
| **Кто видит** | Все авторизованные |

### API

```
GET /api/v1/wallet/transactions?cursor=&limit=20&type=&from=&to=
```

**Query keys:** `walletKeys.transactionList(params)`

### Типы транзакций

| Тип | Описание | Кто видит |
|-----|----------|-----------|
| `escrow_deposit` | Оплата сделки (TON → эскроу) | Рекламодатель |
| `payout` | Выплата владельцу канала | Владелец |
| `withdrawal` | Вывод из OWNER_PENDING на внешний кошелёк | Владелец |
| `refund` | Возврат из эскроу рекламодателю | Рекламодатель |
| `commission` | Комиссия платформы | Оба (информационно) |

### UI

- **Кнопка `t('wallet.history.filter')`** — с badge количества активных фильтров
- **Список транзакций** — `GroupItem`, группировка по дням:
  - `before`: иконка типа
  - Заголовок: описание + связанная сделка/канал
  - `after`: сумма (зелёная = доход, красная = расход) + дата
- **Infinite scroll** — skeleton загрузка

### Sheet фильтров

- **Тип** — multi-select: `escrow_deposit` / `payout` / `withdrawal` / `refund` / `commission`
- **Период** — select: `t('wallet.history.filter.week')` / `t('wallet.history.filter.month')` / `t('wallet.history.filter.all')`

### Действия

| Действие | Результат |
|----------|-----------|
| "Фильтр" | → Sheet фильтров |
| Тап по транзакции | → `/wallet/history/:txId` |

### Empty state

| Emoji | i18n title | i18n description | CTA |
|-------|------------|------------------|-----|
| `📜` | `wallet.history.empty.title` | `wallet.history.empty.description` | `wallet.history.empty.cta` → Reset filters |

### Error states

| Ошибка | UI |
|--------|----|
| Ошибка загрузки | `ErrorScreen` + retry button |
| Offline | Banner `t('errors.offline')` |

---

## 4.4 Детали транзакции

| | |
|---|---|
| **Route** | `/wallet/history/:txId` |
| **Цель** | Полная информация о транзакции |
| **Кто видит** | Участник связанной сделки |

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
  WithdrawPage.tsx            # Route: /wallet/withdraw
  HistoryPage.tsx             # Route: /wallet/history
  TransactionDetailPage.tsx   # Route: /wallet/history/:txId

src/features/wallet/
  api/
    contracts.ts              # Zod schemas: WalletSummary, WithdrawRequest/Response, Transaction
    wallet-api.ts             # API functions: getWalletSummary, getTransactions, withdraw
    wallet-queries.ts         # TanStack Query hooks: useWalletSummary, useTransactions
    wallet-mutations.ts       # useMutation: useWithdraw (с Idempotency-Key)
  components/
    SummaryHero.tsx           # Hero-блок с основной суммой (зависит от роли)
    SummaryStats.tsx          # Сводка (earned/spent/escrow/withdrawn)
    TransactionListItem.tsx
    TransactionFiltersSheet.tsx
  types/
    wallet.ts                 # Общие типы (TransactionType enum, etc.)
```

---

## Связи с другими документами

| Документ | Что использует |
|----------|---------------|
| [07-ton-connect-integration.md](07-ton-connect-integration.md) | Flow 1 (escrow deposit), Flow 2 (withdrawal) |
| [06-shared-components.md](06-shared-components.md) | Error states, i18n namespace, Amount, toast |
| [05-account-types.md](../07-financial-system/05-account-types.md) | OWNER_PENDING, ESCROW, ledger model |
| [07-idempotency-strategy.md](../05-patterns-and-decisions/07-idempotency-strategy.md) | Idempotency-Key для withdraw |