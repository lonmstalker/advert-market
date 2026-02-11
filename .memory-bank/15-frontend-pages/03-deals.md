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
      ├── [Sheet] Поддержка
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

- **Сегмент-контрол**: `t('deals.list.asAdvertiser')` / `t('deals.list.asChannel')`
  - Виден только если пользователь имеет сделки в обеих ролях
- **Список сделок** — `Group` + `GroupItem`:
  - `before`: аватар канала (40×40)
  - Заголовок: название канала
  - `subtitle`: тип поста
  - `after`: статус-badge (цветной)
- **Infinite scroll** — skeleton загрузка
- **Сортировка** по `updatedAt` (desc)

### ABAC (для вкладки "Как канал")

Менеджер видит сделки канала **только** с правом `view_deals`. Без этого права — вкладка "Как канал" скрыта.

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

| Роль | Emoji | i18n title | i18n description | CTA |
|------|-------|------------|------------------|-----|
| Рекламодатель | `📬` | `deals.empty.advertiser.title` | `deals.empty.advertiser.description` | `deals.empty.advertiser.cta` → `/catalog` |
| Канал | `📬` | `deals.empty.channel.title` | `deals.empty.channel.description` | `deals.empty.channel.cta` → `/profile/channels/new` |

### Error states

| Ошибка | UI |
|--------|----|
| Ошибка загрузки | `ErrorScreen` + retry |
| Offline | Banner `t('errors.offline')` |

---

## 3.2 Детали сделки

| | |
|---|---|
| **Route** | `/deals/:dealId` |
| **Цель** | Центральный экран сделки — статус, действия, timeline |
| **Кто видит** | Рекламодатель или владелец/менеджер канала этой сделки (`view_deals`) |

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

- **Header row:**
  - **Статус-badge** — крупный, вверху
  - **ShareButton** — deep link `t.me/AdvertMarketBot/app?startapp=deal_{dealId_short}` (см. 6.4)
- **Карточка канала** — compact, tap → `/catalog/channels/:channelId`
- **Сумма** — `title2`, bold, `tabular-nums`, `<Amount>`
- **Блок действий** — зависит от роли и статуса (матрица ниже)
- **Group `t('deals.detail.brief')`** — если есть, collapsible
- **Group `t('deals.detail.creative')`** — если есть, превью текста + медиа thumbnails
- **Group `t('deals.detail.escrow')`** — статус эскроу, баланс (для funded-статусов)
- **Group `t('deals.detail.timeline')`** — хронологический список событий
- **Кнопка `t('deals.detail.support')`** (`secondary`, small) — открывает Support Sheet

### Матрица действий

| Статус | Рекламодатель | Owner | Manager (required right) |
|--------|--------------|-------|--------------------------|
| `DRAFT` | — | — | — |
| `OFFER_PENDING` | [Отменить] `secondary destructive` | [Принять] `primary` / [Переговоры] `secondary` → 3.3 / [Отклонить] `secondary destructive` | `moderate`: то же что Owner |
| `NEGOTIATING` | [Ответить] `secondary` → 3.3 / [Отменить] `secondary destructive` | [Ответить] `secondary` → 3.3 / [Отклонить] `secondary destructive` | `moderate`: то же что Owner |
| `ACCEPTED` | — (ждёт оплаты) | [Отменить] `secondary destructive` | — |
| `AWAITING_PAYMENT` | [Оплатить] `primary` → Sheet 3.8 | — (ждёт оплаты) | — |
| `FUNDED` | [Отправить бриф] `primary` → 3.4 | [Отправить креатив] `primary` → 3.5 (если есть бриф) | `moderate`: то же что Owner |
| `CREATIVE_SUBMITTED` | [Одобрить] `primary` → 3.6 / [Ревизия] `secondary` → 3.6 | — (ждёт ревью) | — |
| `CREATIVE_APPROVED` | — | [Опубликовать] `primary` / [Запланировать] `secondary` → 3.7 | `publish`: то же что Owner |
| `SCHEDULED` | — | — | — |
| `PUBLISHED` | — | — | — |
| `DELIVERY_VERIFYING` | — | — | — |
| `COMPLETED_RELEASED` | Оставить отзыв (v2) | — | — |
| `DISPUTED` | [Добавить доказательства] `secondary` → 3.11 | [Добавить доказательства] `secondary` → 3.11 | `view_deals`: то же |
| `CANCELLED` | — | — | — |
| `REFUNDED` | — | — | — |
| `EXPIRED` | — | — | — |

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

### Error states

| Ошибка | UI |
|--------|----|
| 404 сделка не найдена | `ErrorScreen` `t('errors.notFound.title')` + navigate `/deals` |
| 403 нет доступа | `ErrorScreen` `t('errors.forbidden.title')` |
| 409 статус изменился | Toast `t('errors.conflict')` + auto-refetch |

---

## 3.3 Переговоры

| | |
|---|---|
| **Route** | `/deals/:dealId/negotiate` |
| **Цель** | Отправить контр-предложение по цене |
| **Кто видит** | Рекламодатель или Owner/Manager (`moderate`) в статусе `OFFER_PENDING` / `NEGOTIATING` |

### API

```
GET  /api/v1/deals/:dealId              # Текущие условия
POST /api/v1/deals/:dealId/negotiate     # Контр-предложение
```

### UI

- **Текущие условия** — read-only карточка: тип поста + текущая цена (`<Amount>`)
- **Input `t('deals.negotiate.price')`** — numeric, TON, `<Amount>` format
- **Input `t('deals.negotiate.comment')`** — `textarea`, optional, max 2000 символов
- Кнопка `t('deals.negotiate.submit')` (`primary`)

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

### ABAC

Manager: требуется `moderate`.

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

- Заголовок: `t('deals.brief.title')`
- **Input `t('deals.brief.text')`** — `textarea`, placeholder: `t('deals.brief.textPlaceholder')`
- **Input `t('deals.brief.cta')`** — URL input
- **Input `t('deals.brief.restrictions')`** — `textarea`, placeholder: `t('deals.brief.restrictionsPlaceholder')`
- **Select `t('deals.brief.tone')`** — `t('deals.brief.tone.professional')` / `t('deals.brief.tone.informal')` / `t('deals.brief.tone.neutral')`
- **Загрузка файлов** — примеры, референсы (drag & drop или file picker)
- Кнопка `t('deals.brief.submit')` (`primary`)

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
| **Кто видит** | Owner/Manager (`moderate`) в статусе `FUNDED` |

### API

```
GET  /api/v1/deals/:dealId/brief      # Бриф от рекламодателя
GET  /api/v1/deals/:dealId            # Статус
POST /api/v1/deals/:dealId/creative   # Отправка креатива
```

**Query keys:** `creativeKeys.brief(dealId)`, `creativeKeys.current(dealId)`

### UI

- **Group `t('deals.creative.brief')`** — read-only, данные от рекламодателя (collapsible)
- **Кнопка `t('deals.creative.importFromTelegram')`** (`secondary`, small) — импорт существующего поста из канала (см. "Импорт из Telegram" ниже)
- **Input `t('deals.creative.text')`** — `textarea`, max 4096 символов (Telegram limit), character counter
- **Загрузка медиа** — до 10 изображений, drag & drop, thumbnails grid
- **Builder кнопок** — опционально:
  - Каждая кнопка: Input `t('deals.creative.buttonText')` + Input `t('deals.creative.buttonUrl')`
  - До 3 рядов кнопок
  - Кнопка `t('deals.creative.addButton')` (`link`)
- **Превью** — имитация Telegram-поста (real-time обновление при вводе)
- Кнопка `t('deals.creative.submit')` (`primary`)

### Импорт из Telegram (MVP)

Флоу пересылки поста через бота:

1. Пользователь нажимает `t('deals.creative.importFromTelegram')`
2. Mini App показывает инструкцию: `t('deals.creative.importInstruction')` — "Перешлите пост боту @AdvertMarketBot"
3. Кнопка `t('deals.creative.openBot')` → `openTelegramLink('https://t.me/AdvertMarketBot')`
4. Пользователь пересылает пост боту
5. Бот парсит пост (text, media, buttons) → сохраняет по `dealId`
6. Mini App polling `GET /api/v1/deals/:dealId/creative/import` (каждые 3s, таймаут 60s)
7. При получении — автозаполнение формы

### ABAC

Manager: требуется `moderate`.

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
| "Импорт из Telegram" | Инструкция + polling → автозаполнение |
| "Отправить на ревью" | `POST /api/v1/deals/:id/creative` → navigate `/deals/:dealId` |

---

## 3.6 Ревью креатива

| | |
|---|---|
| **Route** | `/deals/:dealId/creative/review` |
| **Цель** | Рекламодатель оценивает черновик и принимает решение |
| **Кто видит** | **Рекламодатель only** в статусе `CREATIVE_SUBMITTED` |

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
- **Group `t('deals.review.brief')`** — read-only, для сравнения (collapsible)
- **Input `t('deals.review.revisionComment')`** — `textarea`, появляется при нажатии "Запросить ревизию"
- Две кнопки:
  - `t('deals.review.requestRevision')` (`secondary`)
  - `t('deals.review.approve')` (`primary`)

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
| **Кто видит** | Owner/Manager (`publish`) в статусе `CREATIVE_APPROVED` |

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
  - `t('deals.schedule.publishNow')` (`primary`)
  - `t('deals.schedule.schedule')` (`secondary`) — активна только после выбора даты/времени

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

### ABAC

Manager: требуется `publish`.

---

## 3.8 Оплата (Sheet — TON Connect)

| | |
|---|---|
| **Route** | N/A (Sheet overlay на 3.2) |
| **Цель** | Оплата сделки через TON Connect |
| **Кто видит** | **Рекламодатель only** в статусе `AWAITING_PAYMENT` |

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
- Кнопка `t('wallet.connectWallet')` (`secondary`) — если не подключён
- Кнопка `t('deals.payment.pay')` (`primary`) — доступна после подключения
- Текст `caption`: `t('deals.payment.escrowNote')`

### Действия

| Действие | Результат |
|----------|-----------|
| "Подключить кошелёк" | TON Connect flow (tonConnectUI.connectWallet()) |
| "Оплатить" | Подписать транзакцию → toast `t('wallet.toast.paymentSent')` → закрыть sheet |

### TON Connect интеграция

```typescript
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

### Error states

| Ошибка | UI |
|--------|----|
| Кошелёк отклонил | Toast `t('wallet.error.walletRejected')` |
| Недостаточно TON | Toast `t('wallet.error.insufficientTon')` |
| Таймаут | Toast `t('wallet.error.timeout')` |

---

## 3.9 Открытие спора

| | |
|---|---|
| **Route** | `/deals/:dealId/dispute` (POST-форма, когда спора ещё нет) |
| **Цель** | Подать спор по сделке |
| **Кто видит** | Рекламодатель или Owner/Manager (`view_deals`) в funded-статусах (`FUNDED`...`DELIVERY_VERIFYING`) |

### API

```
GET  /api/v1/deals/:dealId           # Проверка статуса
POST /api/v1/deals/:dealId/dispute   # Открыть спор
```

### UI

- **Select `t('deals.dispute.reason')`** — enum:
  - `POST_DELETED` — `t('deals.dispute.reason.postDeleted')`
  - `POST_EDITED` — `t('deals.dispute.reason.postEdited')`
  - `WRONG_CONTENT` — `t('deals.dispute.reason.wrongContent')`
  - `QUALITY_ISSUE` — `t('deals.dispute.reason.qualityIssue')`
  - `OTHER` — `t('deals.dispute.reason.other')`
- **Input `t('deals.dispute.description')`** — `textarea`, max 5000 символов
- **Загрузка доказательств** — скриншоты (file picker)
- **Предупреждение** — `destructive` text: `t('deals.dispute.warning')`
- Кнопка `t('deals.dispute.submit')` (`primary`, destructive color)

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

### ABAC

Manager: требуется `view_deals` (минимум — участник сделки).

---

## 3.10 Детали спора

| | |
|---|---|
| **Route** | `/deals/:dealId/dispute` (GET-вид, когда спор уже открыт) |
| **Цель** | Просмотр статуса спора и доказательств |
| **Кто видит** | Рекламодатель или Owner/Manager (`view_deals`) в статусе `DISPUTED` |

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
- **Group `t('deals.dispute.evidence')`** — timeline (append-only):
  - Каждый элемент: автор + содержимое (скрины + текст + ссылки) + время
- **Результат** — если разрешён: решение + обоснование
- Кнопка `t('deals.dispute.addEvidence')` (`secondary`) — если спор открыт

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
| **Кто видит** | Рекламодатель или Owner/Manager (`view_deals`) в статусе `DISPUTED` |

### API

```
GET  /api/v1/deals/:dealId/dispute            # Контекст спора
POST /api/v1/deals/:dealId/dispute/evidence   # Отправка доказательства
```

### UI — комбинированная форма

Одна подача = комбинация всех типов (хотя бы одно поле обязательно):

- **Секция `t('deals.evidence.screenshots')`** — file upload, до 5 скриншотов, thumbnails grid
- **Секция `t('deals.evidence.description')`** — `textarea`, max 5000 символов
- **Секция `t('deals.evidence.links')`** — до 3 URL inputs, кнопка `t('deals.evidence.addLink')` (`link`)
- **Input `t('deals.evidence.comment')`** — `textarea`, общий комментарий
- Кнопка `t('deals.evidence.submit')` (`primary`) — активна если хотя бы одно поле заполнено

### Request body

```typescript
{
  screenshots?: string[];  // URLs после загрузки, max 5
  description?: string;    // max 5000
  links?: string[];        // max 3, valid URLs
  comment?: string;        // общий комментарий
}
```

### Валидация

- Хотя бы одно из полей (`screenshots`, `description`, `links`) должно быть заполнено
- Скриншоты: max 5, форматы: JPEG/PNG/WebP, max 10MB каждый
- Ссылки: max 3, valid URL format

### Действия

| Действие | Результат |
|----------|-----------|
| "Отправить" | `POST /api/v1/deals/:id/dispute/evidence` → navigate `/deals/:dealId/dispute` |

---

## 3.12 Support Sheet

| | |
|---|---|
| **Route** | N/A (Sheet overlay на 3.2) |
| **Цель** | Обращение в поддержку по сделке |
| **Кто видит** | Все участники сделки |

### API

```
POST /api/v1/support   # Создаёт тикет
```

### UI

- Заголовок: `t('deals.support.title')`
- **Select `t('deals.support.topicLabel')`**:
  - `PAYMENT_ISSUE` — `t('deals.support.topic.payment')`
  - `CREATIVE_ISSUE` — `t('deals.support.topic.creative')`
  - `OTHER` — `t('deals.support.topic.other')`
- **Input `t('deals.support.descriptionLabel')`** — `textarea`, max 5000
- **Read-only контекст** (подставляется автоматически):
  - Deal ID
  - Текущий статус
  - Сумма сделки
  - Роль обращающегося
- Кнопка `t('deals.support.submit')` (`primary`)

### Request body

```typescript
{
  dealId: string;
  topic: 'PAYMENT_ISSUE' | 'CREATIVE_ISSUE' | 'OTHER';
  description: string;  // max 5000
  context: {
    dealStatus: DealStatus;
    amountNano: bigint;
    role: DealRole;
  };
}
```

### Действия

| Действие | Результат |
|----------|-----------|
| "Отправить" | `POST /api/v1/support` → toast `t('deals.support.sent')` → закрыть sheet |

Бот пересылает тикет операторам в группу поддержки.

### Error states

| Ошибка | UI |
|--------|----|
| Ошибка отправки | Toast `t('common.toast.saveFailed')` |

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
    SupportSheet.tsx            # Support ticket sheet
    TelegramPostPreview.tsx     # Превью креатива
    ButtonBuilder.tsx           # Builder кнопок для креатива
    EvidenceForm.tsx            # Комбинированная форма доказательств
    EvidenceTimeline.tsx
    CreativeImportFlow.tsx      # Импорт поста через бота
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
    support.ts
```
