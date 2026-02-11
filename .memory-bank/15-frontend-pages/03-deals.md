# Сделки

> Tab 2. Центральный модуль — полный deal flow от оффера до completion/dispute.

## Навигация

```
/deals
  └── /deals/:dealId
      ├── /deals/:dealId/negotiate
      ├── /deals/:dealId/brief
      ├── /deals/:dealId/creative
      ├── /deals/:dealId/creative/review
      ├── /deals/:dealId/schedule
      ├── [Sheet] Оплата (TON Connect)
      ├── /deals/:dealId/dispute
      └── /deals/:dealId/dispute/evidence
```

---

## 3.1 Список сделок

| | |
|---|---|
| **Route** | `/deals` |
| **Цель** | Все сделки пользователя — как рекламодателя и как владельца каналов |
| **Кто видит** | Все авторизованные |

### API

```
GET /api/v1/deals?role=advertiser&cursor=&limit=20
GET /api/v1/deals?role=channel&cursor=&limit=20
```

**Query keys:** `dealKeys.list({ role: 'advertiser', ... })`, `dealKeys.list({ role: 'channel', ... })`

### UI

- **Сегмент-контрол**: "Как рекламодатель" / "Как канал"
  - Виден только если пользователь имеет сделки в обеих ролях
- **Список сделок** — `Group` + `GroupItem`:
  - `before`: аватар канала (40×40)
  - Заголовок: название канала
  - `subtitle`: тип поста
  - `after`: статус-badge (цветной)
- **Infinite scroll** — skeleton загрузка
- **Сортировка** по `updatedAt` (desc)

### Статус-badges

| Группа | Статусы | Цвет |
|--------|---------|------|
| Новые | `DRAFT`, `OFFER_PENDING` | `accent` |
| В процессе | `NEGOTIATING`, `ACCEPTED`, `AWAITING_PAYMENT`, `FUNDED` | `accent` (muted) |
| Креатив | `CREATIVE_SUBMITTED`, `CREATIVE_APPROVED`, `SCHEDULED` | `accent` |
| Доставка | `PUBLISHED`, `DELIVERY_VERIFYING` | `warning` |
| Завершено | `COMPLETED_RELEASED` | `success` |
| Проблемы | `DISPUTED` | `destructive` |
| Неактивные | `CANCELLED`, `REFUNDED`, `EXPIRED` | `secondary` |

**Exhaustive mapping** — `Record<DealStatus, StatusConfig>`, compile-time check.

### Действия

| Действие | Результат |
|----------|-----------|
| Переключение сегмента | Переключение между списками |
| Тап по сделке | → `/deals/:dealId` |
| Pull-to-refresh | Инвалидация `dealKeys.lists()` |

### Empty states

| Роль | Emoji | Заголовок | Описание | CTA |
|------|-------|-----------|----------|-----|
| Рекламодатель | `📬` | Нет сделок | Найдите канал для рекламы | [Каталог каналов] → `/catalog` |
| Канал | `📬` | Нет предложений | Зарегистрируйте канал, чтобы получать заказы | [Добавить канал] → `/profile/channels/new` |

---

## 3.2 Детали сделки

| | |
|---|---|
| **Route** | `/deals/:dealId` |
| **Цель** | Центральный экран сделки — статус, действия, timeline |
| **Кто видит** | Рекламодатель или владелец/менеджер канала этой сделки |

### API

```
GET /api/v1/deals/:dealId
GET /api/v1/deals/:dealId/timeline
GET /api/v1/deals/:dealId/escrow     # Для funded-статусов
```

**Query keys:** `dealKeys.detail(dealId)`, `dealKeys.timeline(dealId)`, `dealKeys.escrow(dealId)`

**Polling:** adaptive на основе статуса:
- `AWAITING_PAYMENT`, `DELIVERY_VERIFYING`: 10s
- `PUBLISHED`: 30s
- Остальные: manual refetch

### UI

- **Статус-badge** — крупный, вверху
- **Карточка канала** — compact, tap → `/catalog/channels/:channelId`
- **Сумма** — `title2`, bold, `tabular-nums`, `<Amount>`
- **Блок действий** — зависит от роли и статуса (матрица ниже)
- **Group "Бриф"** — если есть, collapsible
- **Group "Креатив"** — если есть, превью текста + медиа thumbnails
- **Group "Эскроу"** — статус эскроу, баланс (для funded-статусов)
- **Group "Таймлайн"** — хронологический список событий

### Матрица действий

| Статус | Рекламодатель | Владелец канала |
|--------|--------------|-----------------|
| `OFFER_PENDING` | [Отменить] `secondary destructive` | [Принять] `primary` / [Переговоры] `secondary` → 3.3 / [Отклонить] `secondary destructive` |
| `NEGOTIATING` | [Ответить] `secondary` → 3.3 / [Отменить] `secondary destructive` | [Ответить] `secondary` → 3.3 / [Отклонить] `secondary destructive` |
| `ACCEPTED` | — (ждёт оплаты) | [Отменить] `secondary destructive` |
| `AWAITING_PAYMENT` | [Оплатить] `primary` → Sheet 3.8 | — (ждёт оплаты) |
| `FUNDED` | [Отправить бриф] `primary` → 3.4 | [Отправить креатив] `primary` → 3.5 (если есть бриф) |
| `CREATIVE_SUBMITTED` | [Одобрить] `primary` → 3.6 / [Ревизия] `secondary` → 3.6 | — (ждёт ревью) |
| `CREATIVE_APPROVED` | — | [Опубликовать] `primary` / [Запланировать] `secondary` → 3.7 |
| `SCHEDULED` | — | — (ожидание публикации) |
| `PUBLISHED` | — | — |
| `DELIVERY_VERIFYING` | — | — |
| `COMPLETED_RELEASED` | Оставить отзыв (v2) | — |
| `DISPUTED` | [Добавить доказательства] `secondary` → 3.11 | [Добавить доказательства] `secondary` → 3.11 |
| `CANCELLED` | — | — |
| `REFUNDED` | — | — |
| `EXPIRED` | — | — |

**Реализация:** exhaustive `switch` с `default: never`.

### Определение роли

```typescript
type DealRole = 'advertiser' | 'channel_owner' | 'channel_manager';

function getDealRole(deal: Deal, userId: number): DealRole {
  if (deal.advertiserId === userId) return 'advertiser';
  // channel membership check from deal data
}
```

### API-вызовы действий

| Действие | Endpoint |
|----------|----------|
| Принять | `POST /api/v1/deals/:id/accept` |
| Отклонить | `POST /api/v1/deals/:id/reject` |
| Отменить | `POST /api/v1/deals/:id/cancel` |
| Одобрить креатив | `POST /api/v1/deals/:id/creative/approve` |
| Опубликовать | `POST /api/v1/deals/:id/publish` |

Деструктивные действия (отмена, отклонение) требуют `DialogModal` подтверждения.

---

## 3.3 Переговоры

| | |
|---|---|
| **Route** | `/deals/:dealId/negotiate` |
| **Цель** | Отправить контр-предложение по цене |
| **Кто видит** | Обе стороны в статусе `OFFER_PENDING` / `NEGOTIATING` |

### API

```
GET  /api/v1/deals/:dealId              # Текущие условия
POST /api/v1/deals/:dealId/negotiate     # Контр-предложение
```

### UI

- **Текущие условия** — read-only карточка: тип поста + текущая цена (`<Amount>`)
- **Input "Ваша цена"** — numeric, TON, `<Amount>` format
- **Input "Комментарий"** — `textarea`, optional, max 2000 символов
- Кнопка "Отправить предложение" (`primary`)

### Request body

```typescript
{
  proposedAmountNano: bigint;  // > 0
  pricingRuleId?: number;      // опционально: сменить тип поста
  message?: string;            // max 2000
}
```

### Действия

| Действие | Результат |
|----------|-----------|
| "Отправить" | `POST /api/v1/deals/:id/negotiate` → navigate back to `/deals/:dealId` |

---

## 3.4 Отправка брифа

| | |
|---|---|
| **Route** | `/deals/:dealId/brief` |
| **Цель** | Рекламодатель описывает требования к креативу |
| **Кто видит** | Рекламодатель в статусе `FUNDED` |

### API

```
GET  /api/v1/deals/:dealId        # Проверка статуса
POST /api/v1/deals/:dealId/brief  # Отправка брифа
```

**Query keys:** `creativeKeys.brief(dealId)`

### UI

- Заголовок `title2`: "Бриф для креатива"
- **Input "Текст поста"** — `textarea`, placeholder: "Опишите что должно быть в посте"
- **Input "Ссылка/CTA"** — URL input
- **Input "Ограничения"** — `textarea`, placeholder: "Что НЕ включать"
- **Select "Тон"** — профессиональный / неформальный / нейтральный
- **Загрузка файлов** — примеры, референсы (drag & drop или file picker)
- Кнопка "Отправить бриф" (`primary`)

### Действия

| Действие | Результат |
|----------|-----------|
| "Отправить" | `POST /api/v1/deals/:id/brief` → navigate `/deals/:dealId` |

---

## 3.5 Отправка креатива

| | |
|---|---|
| **Route** | `/deals/:dealId/creative` |
| **Цель** | Владелец канала создаёт черновик поста по брифу |
| **Кто видит** | Владелец/менеджер (right: `moderate`) в статусе `FUNDED` |

### API

```
GET  /api/v1/deals/:dealId/brief      # Бриф от рекламодателя
GET  /api/v1/deals/:dealId            # Статус
POST /api/v1/deals/:dealId/creative   # Отправка креатива
```

**Query keys:** `creativeKeys.brief(dealId)`, `creativeKeys.current(dealId)`

### UI

- **Group "Бриф"** — read-only, данные от рекламодателя (collapsible)
- **Input "Текст поста"** — `textarea`, max 4096 символов (Telegram limit), character counter
- **Загрузка медиа** — до 10 изображений, drag & drop, thumbnails grid
- **Builder кнопок** — опционально:
  - Каждая кнопка: Input "Текст" + Input "URL"
  - До 3 рядов кнопок
  - Кнопка "Добавить кнопку" (`link`)
- **Превью** — имитация Telegram-поста (real-time обновление при вводе)
- Кнопка "Отправить на ревью" (`primary`)

### Request body

```typescript
{
  text: string;                           // max 4096
  mediaUrls?: string[];                   // max 10
  buttons?: { text: string; url: string }[]; // max 9 (3×3)
  format?: 'STANDARD' | 'PINNED' | 'STORY' | 'REPOST' | 'NATIVE';
}
```

### Действия

| Действие | Результат |
|----------|-----------|
| Ввод текста | Real-time обновление превью |
| "Отправить на ревью" | `POST /api/v1/deals/:id/creative` → navigate `/deals/:dealId` |

---

## 3.6 Ревью креатива

| | |
|---|---|
| **Route** | `/deals/:dealId/creative/review` |
| **Цель** | Рекламодатель оценивает черновик и принимает решение |
| **Кто видит** | Рекламодатель в статусе `CREATIVE_SUBMITTED` |

### API

```
GET  /api/v1/deals/:dealId/creative         # Текущий черновик
GET  /api/v1/deals/:dealId/brief            # Для сравнения
POST /api/v1/deals/:dealId/creative/approve  # Одобрить
POST /api/v1/deals/:dealId/creative/revision # Запросить ревизию
```

**Query keys:** `creativeKeys.current(dealId)`, `creativeKeys.brief(dealId)`

### UI

- **Превью креатива** — как в Telegram: текст + медиа + кнопки
- **Group "Бриф"** — read-only, для сравнения (collapsible)
- **Input "Комментарий к ревизии"** — `textarea`, появляется при нажатии "Запросить ревизию"
- Две кнопки:
  - "Запросить ревизию" (`secondary`)
  - "Одобрить" (`primary`)

### Действия

| Действие | Результат |
|----------|-----------|
| "Одобрить" | `POST /api/v1/deals/:id/creative/approve` → navigate `/deals/:dealId` |
| "Запросить ревизию" | Показать поле комментария → `POST /api/v1/deals/:id/creative/revision` → navigate `/deals/:dealId` |

---

## 3.7 Планирование публикации

| | |
|---|---|
| **Route** | `/deals/:dealId/schedule` |
| **Цель** | Владелец канала выбирает время публикации |
| **Кто видит** | Владелец/менеджер (right: `publish`) в статусе `CREATIVE_APPROVED` |

### API

```
GET  /api/v1/deals/:dealId          # Статус + креатив
POST /api/v1/deals/:dealId/publish   # Опубликовать сейчас
POST /api/v1/deals/:dealId/schedule  # Запланировать
```

### UI

- **Превью креатива** — compact
- **Date picker** — до 30 дней вперёд, min = сегодня
- **Time picker** — hour:minute
- **Timezone** — авто-определение, read-only (из `Intl.DateTimeFormat().resolvedOptions().timeZone`)
- Две кнопки:
  - "Опубликовать сейчас" (`primary`)
  - "Запланировать" (`secondary`) — активна только после выбора даты/времени

### Request body (schedule)

```typescript
{
  scheduledAt: string;  // ISO 8601, в будущем, max 30 дней
}
```

### Действия

| Действие | Результат |
|----------|-----------|
| "Опубликовать сейчас" | `POST /api/v1/deals/:id/publish` → navigate `/deals/:dealId` |
| "Запланировать" | `POST /api/v1/deals/:id/schedule` → navigate `/deals/:dealId` |

---

## 3.8 Оплата (Sheet — TON Connect)

| | |
|---|---|
| **Route** | N/A (Sheet overlay на 3.2) |
| **Цель** | Оплата сделки через TON Connect |
| **Кто видит** | Рекламодатель в статусе `AWAITING_PAYMENT` |

### API

```
GET /api/v1/deals/:dealId/deposit   # escrow address, amount
```

**Query keys:** `dealKeys.deposit(dealId)`

### UI

- **Сумма** — hero, `tabular-nums`, `<Amount>`
- **Комиссия платформы** — `caption`, `secondary` (10%)
- **Итого** — `title2`, bold
- **Статус кошелька** — иконка + адрес (truncated), если подключён
- Кнопка "Подключить кошелёк" (`secondary`) — если не подключён
- Кнопка "Оплатить" (`primary`) — доступна после подключения
- Текст `caption`, `secondary`: "Средства будут заморожены в эскроу до завершения сделки"

### Действия

| Действие | Результат |
|----------|-----------|
| "Подключить кошелёк" | TON Connect flow (tonConnectUI.connectWallet()) |
| "Оплатить" | Подписать транзакцию → toast "Оплата обрабатывается" → закрыть sheet |

### TON Connect интеграция

```typescript
// Отправка транзакции через TON Connect
const transaction = {
  validUntil: Math.floor(Date.now() / 1000) + 600, // 10 min
  messages: [{
    address: depositData.escrowAddress,
    amount: depositData.amountNano.toString(),
  }],
};
await tonConnectUI.sendTransaction(transaction);
```

После отправки — polling `dealKeys.detail(dealId)` до смены статуса на `FUNDED`.

---

## 3.9 Открытие спора

| | |
|---|---|
| **Route** | `/deals/:dealId/dispute` (POST-форма, когда спора ещё нет) |
| **Цель** | Подать спор по сделке |
| **Кто видит** | Обе стороны в funded-статусах (`FUNDED`...`DELIVERY_VERIFYING`) |

### API

```
GET  /api/v1/deals/:dealId           # Проверка статуса
POST /api/v1/deals/:dealId/dispute   # Открыть спор
```

### UI

- **Select "Причина"** — enum:
  - `POST_DELETED` — Пост удалён
  - `POST_EDITED` — Пост изменён
  - `WRONG_CONTENT` — Неправильный контент
  - `QUALITY_ISSUE` — Проблемы с качеством
  - `OTHER` — Другое
- **Input "Описание"** — `textarea`, max 5000 символов
- **Загрузка доказательств** — скриншоты (file picker)
- **Предупреждение** — `destructive` text: "Эскроу будет заморожен до разрешения спора"
- Кнопка "Подать спор" (`primary`, destructive color)

### Действия

| Действие | Результат |
|----------|-----------|
| "Подать спор" | → `DialogModal` подтверждения → `POST /api/v1/deals/:id/dispute` → navigate `/deals/:dealId/dispute` |

### Request body

```typescript
{
  reason: 'POST_DELETED' | 'POST_EDITED' | 'WRONG_CONTENT' | 'QUALITY_ISSUE' | 'OTHER';
  description: string;  // max 5000
}
```

---

## 3.10 Детали спора

| | |
|---|---|
| **Route** | `/deals/:dealId/dispute` (GET-вид, когда спор уже открыт) |
| **Цель** | Просмотр статуса спора и доказательств |
| **Кто видит** | Обе стороны в статусе `DISPUTED` |

### API

```
GET /api/v1/deals/:dealId/dispute
```

**Query keys:** `disputeKeys.detail(dealId)`

### UI — определение вида

Route `/deals/:dealId/dispute` показывает:
- **Форму 3.9** — если спора ещё нет (`GET` вернул 404 или deal status != `DISPUTED`)
- **Детали 3.10** — если спор открыт (`GET` вернул данные)

### UI (детали)

- **Статус спора** — badge
- **Причина и описание** — от инициатора
- **Group "Доказательства"** — timeline (append-only):
  - Каждый элемент: автор + тип + содержимое + время
- **Результат** — если разрешён: решение + обоснование
- Кнопка "Добавить доказательства" (`secondary`) — если спор открыт

### Действия

| Действие | Результат |
|----------|-----------|
| "Добавить доказательства" | → `/deals/:dealId/dispute/evidence` |

---

## 3.11 Подача доказательств

| | |
|---|---|
| **Route** | `/deals/:dealId/dispute/evidence` |
| **Цель** | Добавить доказательства к открытому спору |
| **Кто видит** | Обе стороны в статусе `DISPUTED` |

### API

```
GET  /api/v1/deals/:dealId/dispute            # Контекст спора
POST /api/v1/deals/:dealId/dispute/evidence   # Отправка доказательства
```

### UI

- **Select "Тип"** — `SCREENSHOT` / `TEXT` / `LINK`
- **Контент-поле** — зависит от типа:
  - `SCREENSHOT`: file upload
  - `TEXT`: `textarea`
  - `LINK`: URL input
- **Input "Комментарий"** — `textarea`
- Кнопка "Отправить" (`primary`)

### Request body

```typescript
{
  evidenceType: 'SCREENSHOT' | 'TEXT' | 'LINK';
  content: {
    url?: string;     // для SCREENSHOT и LINK
    text?: string;    // для TEXT
    caption?: string; // комментарий
  };
}
```

### Действия

| Действие | Результат |
|----------|-----------|
| "Отправить" | `POST /api/v1/deals/:id/dispute/evidence` → navigate `/deals/:dealId/dispute` |

---

## Файловая структура

```
src/pages/deals/
  DealsPage.tsx                # Route: /deals
  DealDetailPage.tsx           # Route: /deals/:dealId
  CreateDealPage.tsx           # Route: /deals/new
  NegotiatePage.tsx            # Route: /deals/:dealId/negotiate
  BriefPage.tsx                # Route: /deals/:dealId/brief
  CreativePage.tsx             # Route: /deals/:dealId/creative
  CreativeReviewPage.tsx       # Route: /deals/:dealId/creative/review
  SchedulePage.tsx             # Route: /deals/:dealId/schedule
  DisputePage.tsx              # Route: /deals/:dealId/dispute (form + details)
  DisputeEvidencePage.tsx      # Route: /deals/:dealId/dispute/evidence

src/features/deals/
  api/
    deals.ts
  components/
    DealListItem.tsx
    DealActions.tsx             # Матрица действий
    DealTimeline.tsx
    DealStatusBadge.tsx
    PaymentSheet.tsx            # TON Connect sheet
    TelegramPostPreview.tsx     # Превью креатива
    ButtonBuilder.tsx           # Builder кнопок для креатива
    EvidenceTimeline.tsx
  hooks/
    useDealRole.ts
    useDealActions.ts
  lib/
    deal-status.ts              # StatusConfig mapping
    deal-actions.ts             # Action matrix
  types/
    deal.ts                     # Zod schemas
    creative.ts
    dispute.ts
```
