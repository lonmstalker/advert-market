# Общие компоненты

> Переиспользуемые UI-элементы: sheets, modals, empty states, skeleton, toast.

---

## Sheet overlays

Все sheets используют компонент `Sheet` из UI Kit.

| Sheet | Где используется | Содержимое |
|-------|-----------------|------------|
| Фильтры каталога | 2.2 (`CatalogPage`) | Тематика, подписчики, цена, сортировка |
| Оплата TON Connect | 3.8 (`DealDetailPage`) | Сумма, кошелёк, кнопка оплаты |
| Фильтры истории | 4.4 (`HistoryPage`) | Тип транзакции, период |

### Паттерн

```typescript
// Каждый sheet — отдельный компонент
<Sheet open={isOpen} onOpenChange={setIsOpen}>
  <SheetContent />
</Sheet>
```

Sheets закрываются:
- Свайп вниз
- Кнопка действия (применить/оплатить)
- Тап по backdrop

---

## DialogModal (подтверждение)

Для деструктивных действий. Используется компонент `DialogModal` из UI Kit.

### Случаи использования

| Действие | Заголовок | Описание | Confirm | Cancel |
|----------|-----------|----------|---------|--------|
| Отмена сделки | Отменить сделку? | Это действие нельзя отменить | "Отменить сделку" (destructive) | "Назад" |
| Отклонение оффера | Отклонить предложение? | Рекламодатель будет уведомлён | "Отклонить" (destructive) | "Назад" |
| Подача спора | Открыть спор? | Эскроу будет заморожен до разрешения | "Открыть спор" (destructive) | "Назад" |
| Вывод средств | Подтвердите вывод | {amount} TON на адрес {address} | "Подтвердить" (primary) | "Отмена" |
| Удаление из команды | Удалить участника? | {name} потеряет доступ к каналу | "Удалить" (destructive) | "Отмена" |

### Паттерн

```typescript
<DialogModal
  open={isConfirmOpen}
  onOpenChange={setIsConfirmOpen}
  title="Отменить сделку?"
  description="Это действие нельзя отменить"
  confirmText="Отменить сделку"
  confirmVariant="destructive"
  onConfirm={handleCancel}
/>
```

---

## Toast уведомления

Используется компонент `Toast` из UI Kit.

### Типы

| Тип | Сообщения |
|-----|-----------|
| **Success** | "Сделка создана", "Креатив одобрен", "Оплата отправлена", "Канал зарегистрирован", "Настройки сохранены" |
| **Error** | "Ошибка оплаты", "Недостаточно средств", "Ошибка сети", "Не удалось сохранить" |
| **Info** | "Скопировано", "Приглашение отправлено", "Пополнение обрабатывается" |

### Паттерн

```typescript
const { toast } = useToast();
toast({ type: 'success', message: t('deal.created') });
```

Toast автоматически скрывается через 3 секунды. Error — через 5 секунд.

---

## Skeleton loading

Каждая страница с данными имеет skeleton-состояние, повторяющее структуру контента.

### Паттерн

```typescript
// В каждой странице
if (isLoading) return <PageSkeleton />;
```

### Skeleton-компоненты

| Страница | Skeleton |
|----------|----------|
| Список каналов | 3× `GroupItem` skeleton (avatar circle + 2 text lines + price rect) |
| Карточка канала | Large avatar circle + text blocks + stats grid + pricing list |
| Список сделок | 3× `GroupItem` skeleton (avatar + text + badge rect) |
| Детали сделки | Badge rect + card skeleton + amount rect + action buttons + timeline list |
| Кошелёк | Balance rect (large) + 2 circle buttons + 5× transaction items |
| Профиль | Avatar + text + channel list + settings list |

Используется `SkeletonElement` из UI Kit с `pulse` анимацией.

---

## Empty states

Единый компонент для всех пустых состояний.

### Компонент

```typescript
type EmptyStateProps = {
  emoji: string;
  title: string;
  description: string;
  action?: {
    label: string;
    onClick: () => void;
  };
};
```

### Полная таблица

| Страница | Emoji | Заголовок | Описание | CTA | Навигация |
|----------|-------|-----------|----------|-----|-----------|
| Каталог (нет результатов) | `🔍` | Ничего не найдено | Попробуйте изменить фильтры | Сбросить фильтры | Reset filters |
| Сделки (рекламодатель) | `📬` | Нет сделок | Найдите канал для рекламы | Каталог каналов | `/catalog` |
| Сделки (канал) | `📬` | Нет предложений | Зарегистрируйте канал, чтобы получать заказы | Добавить канал | `/profile/channels/new` |
| Кошелёк | `📜` | Нет операций | История платежей появится здесь | Пополнить баланс | `/wallet/top-up` |
| История (с фильтрами) | `📜` | Нет операций | Попробуйте изменить фильтры | Сбросить фильтры | Reset filters |
| Каналы профиля | `📡` | Нет каналов | Зарегистрируйте канал для получения заказов | Добавить канал | `/profile/channels/new` |
| Команда канала | `👥` | Нет менеджеров | Пригласите админов для помощи с каналом | Пригласить | `team/invite` |

---

## Bottom Tab Navigation

4 таба, всегда видны (кроме онбординга).

| # | Label | Иконка | Route | Badge |
|---|-------|--------|-------|-------|
| 1 | Каталог | Search / Grid | `/catalog` | — |
| 2 | Сделки | FileText / Handshake | `/deals` | Количество сделок, требующих действий |
| 3 | Кошелёк | Wallet | `/wallet` | — |
| 4 | Профиль | User | `/profile` | — |

Badge на табе "Сделки" — количество сделок, где текущий пользователь должен выполнить действие (accept, review, pay, etc.).

---

## Telegram BackButton

Используется `@tma.js/sdk-react` `BackButton`.

| Route | BackButton target |
|-------|-------------------|
| `/catalog` | Нет (tab root) |
| `/catalog/channels/:id` | `/catalog` |
| `/deals` | Нет (tab root) |
| `/deals/:id` | `/deals` |
| `/deals/:id/*` | `/deals/:id` |
| `/wallet` | Нет (tab root) |
| `/wallet/*` | `/wallet` |
| `/profile` | Нет (tab root) |
| `/profile/*` | `/profile` (или parent level) |
| `/onboarding/*` | Нет (disabled) |

---

## Маршрутизация

### Полная таблица routes

```typescript
const routes = [
  // Онбординг
  { path: '/onboarding', page: 'OnboardingPage' },
  { path: '/onboarding/interest', page: 'OnboardingInterestPage' },
  { path: '/onboarding/tour', page: 'OnboardingTourPage' },

  // Каталог (Tab 1)
  { path: '/catalog', page: 'CatalogPage' },
  { path: '/catalog/channels/:channelId', page: 'ChannelDetailPage' },

  // Сделки (Tab 2)
  { path: '/deals', page: 'DealsPage' },
  { path: '/deals/new', page: 'CreateDealPage' },
  { path: '/deals/:dealId', page: 'DealDetailPage' },
  { path: '/deals/:dealId/negotiate', page: 'NegotiatePage' },
  { path: '/deals/:dealId/brief', page: 'BriefPage' },
  { path: '/deals/:dealId/creative', page: 'CreativePage' },
  { path: '/deals/:dealId/creative/review', page: 'CreativeReviewPage' },
  { path: '/deals/:dealId/schedule', page: 'SchedulePage' },
  { path: '/deals/:dealId/dispute', page: 'DisputePage' },
  { path: '/deals/:dealId/dispute/evidence', page: 'DisputeEvidencePage' },

  // Кошелёк (Tab 3)
  { path: '/wallet', page: 'WalletPage' },
  { path: '/wallet/top-up', page: 'TopUpPage' },
  { path: '/wallet/withdraw', page: 'WithdrawPage' },
  { path: '/wallet/history', page: 'HistoryPage' },
  { path: '/wallet/history/:txId', page: 'TransactionDetailPage' },

  // Профиль (Tab 4)
  { path: '/profile', page: 'ProfilePage' },
  { path: '/profile/language', page: 'LanguagePage' },
  { path: '/profile/notifications', page: 'NotificationsPage' },
  { path: '/profile/channels/new', page: 'RegisterChannelPage' },
  { path: '/profile/channels/:channelId', page: 'ChannelManagePage' },
  { path: '/profile/channels/:channelId/edit', page: 'ChannelEditPage' },
  { path: '/profile/channels/:channelId/team', page: 'TeamPage' },
  { path: '/profile/channels/:channelId/team/invite', page: 'TeamInvitePage' },
  { path: '/profile/channels/:channelId/team/:userId', page: 'TeamMemberPage' },
];
```

**Итого: 28 routes** (3 онбординг + 2 каталог + 10 сделки + 5 кошелёк + 8 профиль).

Все pages — `lazy()` для code splitting (кроме root layout).

---

## i18n ключи (структура)

```
onboarding.welcome.title
onboarding.welcome.subtitle
onboarding.interest.title
onboarding.interest.advertiser
onboarding.interest.owner
onboarding.tour.slide1.title
...
catalog.search.placeholder
catalog.filters.title
catalog.empty.title
...
deals.list.asAdvertiser
deals.list.asChannel
deals.status.{STATUS}
deals.actions.{ACTION}
...
wallet.balance
wallet.topUp
wallet.withdraw
wallet.history.title
wallet.empty.title
...
profile.account
profile.channels
profile.settings
profile.language
profile.notifications
...
common.save
common.cancel
common.confirm
common.back
common.loading
common.error
common.copied
```

---

## Верификация

### 1. Deal state machine — все 16 статусов покрыты в матрице 3.2

| # | Статус | Рекламодатель | Канал | Покрыт |
|---|--------|--------------|-------|--------|
| 1 | `DRAFT` | — (internal) | — | N/A (не виден в UI) |
| 2 | `OFFER_PENDING` | Отменить | Принять/Переговоры/Отклонить | Yes |
| 3 | `NEGOTIATING` | Ответить/Отменить | Ответить/Отклонить | Yes |
| 4 | `ACCEPTED` | — | Отменить | Yes |
| 5 | `AWAITING_PAYMENT` | Оплатить | — | Yes |
| 6 | `FUNDED` | Отправить бриф | Отправить креатив | Yes |
| 7 | `CREATIVE_SUBMITTED` | Одобрить/Ревизия | — | Yes |
| 8 | `CREATIVE_APPROVED` | — | Опубликовать/Запланировать | Yes |
| 9 | `SCHEDULED` | — | — | Yes |
| 10 | `PUBLISHED` | — | — | Yes |
| 11 | `DELIVERY_VERIFYING` | — | — | Yes |
| 12 | `COMPLETED_RELEASED` | Отзыв (v2) | — | Yes |
| 13 | `DISPUTED` | Доказательства | Доказательства | Yes |
| 14 | `CANCELLED` | — | — | Yes |
| 15 | `REFUNDED` | — | — | Yes |
| 16 | `EXPIRED` | — | — | Yes |

### 2. Empty states — все 7 задокументированы (таблица выше)

### 3. ABAC — проверка "Кто видит"

- Нет фиксированных ролей, только contextual checks
- `deal.advertiserId === user.id` → рекламодатель
- `channelMembership.role` → owner/manager
- `channelMembership.rights` → granular permissions (publish, moderate, manage_team, etc.)

### 4. API endpoints — сверка с `query-keys.ts`

Существующие keys покрывают: deals, channels, creative, disputes, auth.
**Требуется добавить:** `walletKeys`, `profileKeys` (описаны в 04-wallet.md и 05-profile.md).

### 5. Routes — все уникальны, конфликтов нет

28 routes, проверены на уникальность. `/deals/:dealId/dispute` использует conditional rendering (форма vs детали) на основе наличия спора.
