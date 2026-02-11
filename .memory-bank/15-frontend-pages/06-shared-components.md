# Общие компоненты

> Переиспользуемые UI-элементы, системные политики (локализация, ошибки, ABAC, deep links).

---

## 6.1 Локализация (i18n)

### Стратегия — двухуровневая

| Уровень | Отвечает за | Технология | Хранение |
|---------|-------------|------------|----------|
| **Frontend** | UI labels, кнопки, навигация, validation messages, empty states, placeholder-ы | `i18next` + `react-i18next` | `src/shared/i18n/ru.json`, `en.json` |
| **Backend** | Уведомления, email-шаблоны, описания ошибок API, динамические тексты | Spring `MessageSource` | `messages/` (properties-файлы) |

### Правило: ZERO hardcoded strings

Все user-facing строки — только через `t('key')`. Исключения: технические константы (форматы дат, regex patterns).

```typescript
// Правильно
<Button>{t('common.save')}</Button>
<EmptyState title={t('catalog.empty.title')} />

// Неправильно
<Button>Сохранить</Button>
<EmptyState title="Ничего не найдено" />
```

### Ошибки API

RFC 7807 `title` приходит с бэкенда **уже локализованным** (по `Accept-Language` header). Frontend НЕ переводит ошибки API — отображает `problem.title` as-is.

### Namespace structure

```
onboarding.*     — 01-onboarding
catalog.*        — 02-catalog
deals.*          — 03-deals
wallet.*         — 04-wallet
profile.*        — 05-profile
common.*         — общие (save, cancel, confirm, back, loading, copied)
errors.*         — ошибки frontend-only (validation, offline, timeout)
```

### Определение языка

1. `Telegram.WebApp.initDataUnsafe.user.language_code` при первом входе
2. `GET /api/v1/profile` → `preferredLanguage` при последующих входах
3. Fallback: `ru`

---

## 6.2 Системные ошибки (Error States)

### Глобальная таблица ошибок

| Тип ошибки | UI | Действие пользователя | Автоматическое поведение |
|------------|----|-----------------------|--------------------------|
| **Offline** (нет сети) | Banner вверху: `t('errors.offline')` + retry | Retry button | Auto-retry при восстановлении (`navigator.onLine`) |
| **500 Server Error** | Full-screen: иллюстрация + `t('errors.server')` + `t('common.retry')` | Retry → перезагрузка данных | — |
| **403 Forbidden** | Full-screen: `t('errors.forbidden.title')` + `t('errors.forbidden.description')` + `t('common.back')` | Navigate back | — |
| **404 Not Found** | Full-screen: `t('errors.notFound.title')` + `t('errors.notFound.description')` + `t('common.home')` | Navigate to tab root | — |
| **409 Conflict** (state machine) | Toast error: `t('errors.conflict')` | — | Auto-refetch данных |
| **401 Unauthorized** (token expired) | Без UI (transparent) | — | Re-auth через `initData` → повтор запроса |
| **429 Rate Limited** | Toast: `t('errors.rateLimited')` | — | Auto-retry после `Retry-After` delay |
| **Timeout** | Toast: `t('errors.timeout')` + retry | Retry button | — |

### Компоненты

```typescript
// ErrorBoundary — оборачивает каждый route
<ErrorBoundary fallback={<ErrorScreen />}>
  <Outlet />
</ErrorBoundary>

// ErrorScreen — full-screen ошибка (403, 404, 500)
type ErrorScreenProps = {
  illustration?: ReactNode;
  title: string;
  description: string;
  action?: { label: string; onClick: () => void };
};

// OfflineBanner — sticky banner при потере сети
// Отображается глобально, поверх любого контента
```

### Обработка в React Query

```typescript
// Глобальный onError в QueryClient
queryCache: new QueryCache({
  onError: (error) => {
    if (error.status === 401) reAuth();
    if (error.status === 409) queryClient.invalidateQueries();
    if (error.status === 429) /* auto-retry with delay */;
  },
}),
```

---

## 6.3 ABAC-матрица

### Права менеджера — видимость UI

| Право | Что доступно | Без права |
|-------|-------------|-----------|
| `moderate` | Кнопки accept/reject/negotiate (3.2), форма креатива (3.5) | Кнопки скрыты |
| `publish` | Кнопки publish/schedule (3.7) | Кнопки скрыты |
| `view_deals` | Список сделок канала, детали, timeline (3.1, 3.2) | Сделки канала скрыты |
| `manage_listings` | — (см. OWNER-exclusive ниже) | — |
| `manage_team` | Секция "Команда" (5.5), invite (5.8), remove (5.9) | Секция скрыта |

### OWNER-exclusive (НЕ делегируемые)

| Действие | API | Почему |
|----------|-----|--------|
| Редактирование канала | `PUT /api/v1/channels/:id` | `@channelAuth.isOwner` |
| Изменение прав участников | `PUT /api/v1/channels/:id/team/:userId` | Только owner меняет toggle-ы |

> `manage_listings` в ABAC backend реализован как `@channelAuth.isOwner`. Менеджеры НЕ могут редактировать канал, несмотря на наличие этого права.

### Правило для UI

**Отсутствие права = кнопка скрыта (hidden)**, а НЕ disabled.

Единственное исключение: страница 5.9 "Права участника" — toggle-ы прав **disabled** с tooltip `t('profile.team.ownerOnly')` для менеджеров с `manage_team`.

### Проверка прав на клиенте

```typescript
// Hook для ABAC
function useChannelRights(channelId: number) {
  const { data: team } = useQuery(channelKeys.team(channelId));
  const userId = useCurrentUser().id;
  const member = team?.find(m => m.userId === userId);

  return {
    isOwner: member?.role === 'OWNER',
    hasRight: (right: ChannelRight) =>
      member?.role === 'OWNER' || member?.rights?.includes(right),
  };
}
```

### ABAC по подстраницам сделки (для менеджеров)

| Страница | Required right | Кто ещё видит |
|----------|---------------|---------------|
| 3.3 Переговоры | `moderate` | Рекламодатель |
| 3.5 Креатив | `moderate` | — |
| 3.6 Ревью | — | Рекламодатель only |
| 3.7 Планирование | `publish` | — |
| 3.8 Оплата | — | Рекламодатель only |
| 3.9 Спор (открытие) | `view_deals` | Рекламодатель |
| 3.11 Доказательства | `view_deals` | Рекламодатель |

---

## 6.4 Deep Links и Sharing

### Формат deep link

```
t.me/AdvertMarketBot/app?startapp={type}_{id}
```

| Тип | Пример | Роутинг |
|-----|--------|---------|
| `channel_{id}` | `channel_12345` | `/catalog/channels/12345` |
| `deal_{uuid_short}` | `deal_abc123` | `/deals/abc123` |
| `dispute_{uuid_short}` | `dispute_abc123` | `/deals/abc123/dispute` |

### Обработка при входе

```typescript
// В корневом роутере, до рендера
const startParam = Telegram.WebApp.initDataUnsafe.start_param;

if (startParam) {
  const [type, id] = startParam.split('_');
  switch (type) {
    case 'channel': navigate(`/catalog/channels/${id}`); break;
    case 'deal':    navigate(`/deals/${id}`); break;
    case 'dispute': navigate(`/deals/${id}/dispute`); break;
  }
}
```

### Компонент ShareButton

```typescript
type ShareButtonProps = {
  type: 'channel' | 'deal' | 'dispute';
  id: string;
};

// Поведение:
// 1. Формирует deep link URL
// 2. Вызывает switchInlineQuery() (если доступен в Telegram)
// 3. Fallback: navigator.clipboard → toast t('common.copied')
```

**Где используется:**
- 2.3 Карточка канала (рядом с заголовком)
- 3.2 Детали сделки (в header)

---

## Sheet overlays

Все sheets используют компонент `Sheet` из UI Kit.

| Sheet | Где используется | Содержимое |
|-------|-----------------|------------|
| Фильтры каталога | 2.2 (`CatalogPage`) | Тематика, подписчики, цена, сортировка |
| Оплата TON Connect | 3.8 (`DealDetailPage`) | Сумма, кошелёк, кнопка оплаты |
| Фильтры истории | 4.4 (`HistoryPage`) | Тип транзакции, период |
| Поддержка сделки | 3.2 (`DealDetailPage`) | Тема, описание, контекст сделки |

### Паттерн

```typescript
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
| Отмена сделки | `t('deals.confirm.cancel.title')` | `t('deals.confirm.cancel.description')` | destructive | secondary |
| Отклонение оффера | `t('deals.confirm.reject.title')` | `t('deals.confirm.reject.description')` | destructive | secondary |
| Подача спора | `t('deals.confirm.dispute.title')` | `t('deals.confirm.dispute.description')` | destructive | secondary |
| Вывод средств | `t('wallet.confirm.withdraw.title')` | `t('wallet.confirm.withdraw.description', { amount, address })` | primary | secondary |
| Удаление из команды | `t('profile.confirm.removeMember.title')` | `t('profile.confirm.removeMember.description', { name })` | destructive | secondary |

### Паттерн

```typescript
<DialogModal
  open={isConfirmOpen}
  onOpenChange={setIsConfirmOpen}
  title={t('deals.confirm.cancel.title')}
  description={t('deals.confirm.cancel.description')}
  confirmText={t('deals.confirm.cancel.confirm')}
  confirmVariant="destructive"
  onConfirm={handleCancel}
/>
```

---

## Toast уведомления

Используется компонент `Toast` из UI Kit.

### Типы

| Тип | Примеры ключей |
|-----|----------------|
| **Success** | `deals.toast.created`, `deals.toast.creativeApproved`, `wallet.toast.paymentSent`, `profile.toast.channelRegistered`, `common.toast.saved` |
| **Error** | `wallet.toast.paymentFailed`, `wallet.toast.insufficientFunds`, `errors.network`, `common.toast.saveFailed` |
| **Info** | `common.copied`, `profile.toast.inviteSent`, `wallet.toast.topUpProcessing` |

### Паттерн

```typescript
const { toast } = useToast();
toast({ type: 'success', message: t('deals.toast.created') });
```

Toast автоматически скрывается через 3 секунды. Error — через 5 секунд.

---

## Skeleton loading

Каждая страница с данными имеет skeleton-состояние, повторяющее структуру контента.

### Паттерн

```typescript
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
  title: string;       // i18n key
  description: string; // i18n key
  action?: {
    label: string;     // i18n key
    onClick: () => void;
  };
};
```

### Полная таблица

| Страница | Emoji | Заголовок (i18n) | Описание (i18n) | CTA (i18n) | Навигация |
|----------|-------|------------------|-----------------|------------|-----------|
| Каталог (нет результатов) | `🔍` | `catalog.empty.title` | `catalog.empty.description` | `catalog.empty.cta` | Reset filters |
| Сделки (рекламодатель) | `📬` | `deals.empty.advertiser.title` | `deals.empty.advertiser.description` | `deals.empty.advertiser.cta` | `/catalog` |
| Сделки (канал) | `📬` | `deals.empty.channel.title` | `deals.empty.channel.description` | `deals.empty.channel.cta` | `/profile/channels/new` |
| Кошелёк | `📜` | `wallet.empty.title` | `wallet.empty.description` | `wallet.empty.cta` | `/wallet/top-up` |
| История (с фильтрами) | `📜` | `wallet.history.empty.title` | `wallet.history.empty.description` | `wallet.history.empty.cta` | Reset filters |
| Каналы профиля | `📡` | `profile.channels.empty.title` | `profile.channels.empty.description` | `profile.channels.empty.cta` | `/profile/channels/new` |
| Команда канала | `👥` | `profile.team.empty.title` | `profile.team.empty.description` | `profile.team.empty.cta` | `team/invite` |

---

## Bottom Tab Navigation

4 таба, всегда видны (кроме онбординга).

| # | Label (i18n) | Иконка | Route | Badge |
|---|-------------|--------|-------|-------|
| 1 | `common.tabs.catalog` | Search / Grid | `/catalog` | — |
| 2 | `common.tabs.deals` | FileText / Handshake | `/deals` | Количество сделок, требующих действий |
| 3 | `common.tabs.wallet` | Wallet | `/wallet` | — |
| 4 | `common.tabs.profile` | User | `/profile` | — |

Badge на табе "Сделки" — количество сделок, где текущий пользователь должен выполнить действие.

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
onboarding.interest.both
onboarding.tour.slide1.title
...
catalog.search.placeholder
catalog.filters.title
catalog.empty.title
catalog.share.copied
...
deals.list.asAdvertiser
deals.list.asChannel
deals.status.{STATUS}
deals.actions.{ACTION}
deals.support.title
deals.support.topicLabel
deals.support.descriptionLabel
deals.support.submit
...
wallet.balance
wallet.topUp
wallet.withdraw
wallet.history.title
wallet.empty.title
wallet.error.walletRejected
wallet.error.insufficientTon
wallet.error.timeout
wallet.error.invalidAddress
wallet.error.withdrawLimit
...
profile.account
profile.channels
profile.settings
profile.language
profile.notifications
profile.team.ownerOnly
profile.channel.copyBot
profile.channel.openBot
...
common.save
common.cancel
common.confirm
common.back
common.loading
common.error
common.copied
common.retry
common.tabs.catalog
common.tabs.deals
common.tabs.wallet
common.tabs.profile
...
errors.offline
errors.server
errors.forbidden.title
errors.forbidden.description
errors.notFound.title
errors.notFound.description
errors.conflict
errors.rateLimited
errors.timeout
```

---

## Верификация

### 1. Deal state machine — все 16 статусов покрыты в матрице 3.2

| # | Статус | Рекламодатель | Канал (Owner) | Канал (Manager) | Покрыт |
|---|--------|--------------|---------------|-----------------|--------|
| 1 | `DRAFT` | — | — | — | N/A |
| 2 | `OFFER_PENDING` | Отменить | Принять/Переговоры/Отклонить | moderate: то же | Yes |
| 3 | `NEGOTIATING` | Ответить/Отменить | Ответить/Отклонить | moderate: то же | Yes |
| 4 | `ACCEPTED` | — | Отменить | — | Yes |
| 5 | `AWAITING_PAYMENT` | Оплатить | — | — | Yes |
| 6 | `FUNDED` | Отправить бриф | Отправить креатив | moderate: то же | Yes |
| 7 | `CREATIVE_SUBMITTED` | Одобрить/Ревизия | — | — | Yes |
| 8 | `CREATIVE_APPROVED` | — | Опубликовать/Запланировать | publish: то же | Yes |
| 9 | `SCHEDULED` | — | — | — | Yes |
| 10 | `PUBLISHED` | — | — | — | Yes |
| 11 | `DELIVERY_VERIFYING` | — | — | — | Yes |
| 12 | `COMPLETED_RELEASED` | Отзыв (v2) | — | — | Yes |
| 13 | `DISPUTED` | Доказательства | Доказательства | view_deals: то же | Yes |
| 14 | `CANCELLED` | — | — | — | Yes |
| 15 | `REFUNDED` | — | — | — | Yes |
| 16 | `EXPIRED` | — | — | — | Yes |

### 2. Empty states — все 7 задокументированы (таблица выше), все через i18n

### 3. ABAC — полная матрица в 6.3 + per-page указания в каждом файле

### 4. Deep links — все 3 типа задокументированы в 6.4, обработка входа описана

### 5. Локализация — ZERO hardcoded strings, namespace structure в 6.1

### 6. Error states — глобальная таблица в 6.2 + per-page ошибки во всех файлах (01-06)
