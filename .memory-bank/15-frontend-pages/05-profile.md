# Профиль

> Tab 4. Настройки пользователя, управление каналами, команда.

## Навигация

```
/profile
  ├── /profile/language
  ├── /profile/notifications
  ├── /profile/channels/new
  └── /profile/channels/:channelId
      ├── /profile/channels/:channelId/edit
      └── /profile/channels/:channelId/team
          ├── /profile/channels/:channelId/team/invite
          └── /profile/channels/:channelId/team/:userId
```

---

## Новые API endpoints

> Endpoints **отсутствующие** в `11-api-contracts.md`.

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/api/v1/profile` | Данные профиля | Authenticated |
| `PUT` | `/api/v1/profile/language` | Сменить язык | Authenticated |
| `PUT` | `/api/v1/profile/onboarding` | Завершить онбординг | Authenticated |
| `GET` | `/api/v1/profile/notifications` | Настройки уведомлений | Authenticated |
| `PUT` | `/api/v1/profile/notifications` | Обновить настройки | Authenticated |

### Новые query keys (добавить в `query-keys.ts`)

```typescript
export const profileKeys = {
  me: ['profile'] as const,
  notifications: ['profile', 'notifications'] as const,
};
```

---

## 5.1 Главная профиля

| | |
|---|---|
| **Route** | `/profile` |
| **Цель** | Настройки пользователя, управление каналами |
| **Кто видит** | Все авторизованные |

### API

```
GET /api/v1/profile
GET /api/v1/channels?owner=me
```

**Query keys:** `profileKeys.me`, `channelKeys.list({ owner: 'me' })`

### UI

- **Group `t('profile.account')`**:
  - Аватар (из Telegram) + имя + username
- **Group `t('profile.channels')`** — список каналов пользователя (`GroupItem`):
  - `before`: аватар канала
  - Заголовок: название канала
  - `subtitle`: `t('profile.channel.subscribers', { count })`
  - `after`: статус листинга badge (active/inactive)
  - `chevron`
- **GroupItem `t('profile.addChannel')`** — иконка `+`
- **Group `t('profile.settings')`**:
  - GroupItem `t('profile.language')` — `chevron`, `after`: текущий язык
  - GroupItem `t('profile.notifications')` — `chevron`
- **Group `t('profile.stats')`** (если есть сделки):
  - `t('profile.stats.totalDeals')`
  - `t('profile.stats.gmv')` (`<Amount>`)
  - `t('profile.stats.earned')` (`<Amount>`, для владельцев каналов)

### Действия

| Действие | Результат |
|----------|-----------|
| Тап канал | → `/profile/channels/:channelId` |
| "Добавить канал" | → `/profile/channels/new` |
| "Язык" | → `/profile/language` |
| "Уведомления" | → `/profile/notifications` |

### Empty state (каналы)

| Emoji | i18n title | i18n description | CTA |
|-------|------------|------------------|-----|
| `📡` | `profile.channels.empty.title` | `profile.channels.empty.description` | `profile.channels.empty.cta` → `/profile/channels/new` |

### Error states

| Ошибка | UI |
|--------|----|
| Ошибка загрузки профиля | `ErrorScreen` + retry |
| Offline | Banner `t('errors.offline')` |

---

## 5.2 Регистрация канала

| | |
|---|---|
| **Route** | `/profile/channels/new` |
| **Цель** | Зарегистрировать канал на платформе |
| **Кто видит** | Все авторизованные |

### API

```
POST /api/v1/channels         # Регистрация
GET  /api/v1/channels/topics   # Тематики (enum)
```

### UI — Двухшаговая форма

**Шаг 1: Проверка канала**
- **Input `t('profile.register.channelLink')`** — формат `@username` или `t.me/...`
- Кнопка `t('profile.register.verify')` (`secondary`)
- Инструкция: `t('profile.register.addBotInstruction')` — "Добавьте @AdvertMarketBot как администратора канала"
- **Кнопка копирования** рядом с "@AdvertMarketBot": иконка copy → `navigator.clipboard.writeText('@AdvertMarketBot')` → toast `t('common.copied')`
- **Кнопка `t('profile.register.openBot')`** — `openTelegramLink('https://t.me/AdvertMarketBot')`

**Шаг 2: Настройка (после проверки)**
- Название канала — read-only, из API
- Подписчики — read-only, из API
- **Input `t('profile.register.description')`** — `textarea`, max 5000 символов
- **Select `t('profile.register.topic')`** — из enum/API
- **Builder `t('profile.register.pricing')`** — динамический список:
  - Каждое правило:
    - `Select` тип поста (`STANDARD`/`PINNED`/`STORY`/`REPOST`/`NATIVE`)
    - `Input` цена в TON
    - `Input` `t('profile.pricing.description')` — `textarea`, что включено в размещение
    - **Group `t('profile.pricing.limits')`** — ограничения:
      - `Input` `t('profile.pricing.maxTextLength')` — числовой, default по типу поста (см. таблицу)
      - `Input` `t('profile.pricing.maxButtons')` — числовой, default по типу поста
      - `Input` `t('profile.pricing.maxMedia')` — числовой, default по типу поста
  - Кнопка `t('profile.pricing.addRule')` (`link`)
  - Кнопка удаления (×) на каждом правиле
  - Min 1 правило
- Кнопка `t('profile.register.submit')` (`primary`, full-width)

### Дефолтные лимиты по типу поста

| Тип поста | Макс. текст | Макс. кнопок | Макс. медиа |
|-----------|-------------|--------------|-------------|
| `STANDARD` | 4096 | 9 (3×3) | 10 |
| `PINNED` | 4096 | 9 (3×3) | 10 |
| `STORY` | 2048 | 1 | 1 |
| `REPOST` | 4096 | 0 | 0 |
| `NATIVE` | 4096 | 9 (3×3) | 10 |

> Дефолты заполняются автоматически при выборе типа поста. Пользователь может ужесточить (уменьшить), но НЕ может превысить Telegram-лимиты.

### Request body

```typescript
{
  channelId: number;
  description?: string;
  topic: string;
  pricingRules: {
    name: string;
    postType: 'STANDARD' | 'PINNED' | 'STORY' | 'REPOST' | 'NATIVE';
    priceNano: bigint;
    description?: string;  // что включено
    limits: {
      maxTextLength: number;   // <= Telegram limit
      maxButtons: number;      // <= Telegram limit
      maxMedia: number;        // <= Telegram limit
    };
  }[];
}
```

### Действия

| Действие | Результат |
|----------|-----------|
| "Проверить" | Валидация через API → показать Шаг 2 |
| Выбор типа поста в правиле | Автозаполнение дефолтных лимитов |
| "Зарегистрировать" | `POST /api/v1/channels` → navigate `/profile/channels/:newId` |

### Предусловие

Бот `@AdMarketBot` должен быть добавлен как admin в канал. Если нет:
- Ошибка с инструкцией: `t('profile.register.botNotAdmin')`
- Кнопки копирования и открытия бота

### Error states

| Ошибка | UI |
|--------|----|
| Бот не admin | Inline error `t('profile.register.botNotAdmin')` |
| Канал не найден | Inline error `t('profile.register.channelNotFound')` |
| Канал уже зарегистрирован | Toast `t('profile.register.alreadyRegistered')` |

---

## 5.3 Язык

| | |
|---|---|
| **Route** | `/profile/language` |
| **Цель** | Переключение языка интерфейса |
| **Кто видит** | Все |

### API

```
PUT /api/v1/profile/language
```

### UI

- **Group** с `RadioGroup`:
  - `t('profile.language.ru')` (default из `Telegram.WebApp.initDataUnsafe.user.language_code`)
  - `t('profile.language.en')`

### Действия

| Действие | Результат |
|----------|-----------|
| Выбор языка | `i18n.changeLanguage()` + `PUT /api/v1/profile/language` + BackButton |

### Error states

| Ошибка | UI |
|--------|----|
| Ошибка сохранения языка | Toast `t('common.toast.saveFailed')` + rollback `i18n.changeLanguage()` |

---

## 5.4 Уведомления

| | |
|---|---|
| **Route** | `/profile/notifications` |
| **Цель** | Настроить какие уведомления получать в боте |
| **Кто видит** | Все |

### API

```
GET /api/v1/profile/notifications
PUT /api/v1/profile/notifications
```

**Query keys:** `profileKeys.notifications`

### UI

- **Group `t('profile.notifications.deals')`**:
  - Toggle: `t('profile.notifications.newOffers')`
  - Toggle: `t('profile.notifications.acceptReject')`
  - Toggle: `t('profile.notifications.deliveryStatus')`
- **Group `t('profile.notifications.financial')`**:
  - Toggle: `t('profile.notifications.deposits')`
  - Toggle: `t('profile.notifications.payouts')`
  - Toggle: `t('profile.notifications.escrow')`
- **Group `t('profile.notifications.disputes')`**:
  - Toggle: `t('profile.notifications.opened')`
  - Toggle: `t('profile.notifications.resolved')`

### Действия

| Действие | Результат |
|----------|-----------|
| Toggle | Автосохранение: optimistic update + `PUT /api/v1/profile/notifications` |

### Request body

```typescript
{
  deals: {
    newOffers: boolean;
    acceptReject: boolean;
    deliveryStatus: boolean;
  };
  financial: {
    deposits: boolean;
    payouts: boolean;
    escrow: boolean;
  };
  disputes: {
    opened: boolean;
    resolved: boolean;
  };
}
```

### Error states

| Ошибка | UI |
|--------|----|
| Ошибка сохранения | Toast `t('common.toast.saveFailed')` + rollback optimistic update |

---

## 5.5 Управление каналом

| | |
|---|---|
| **Route** | `/profile/channels/:channelId` |
| **Цель** | Управление листингом, статистика, команда |
| **Кто видит** | Owner или Manager (с любым правом) |

### API

```
GET /api/v1/channels/:channelId
GET /api/v1/channels/:channelId/team
PUT /api/v1/channels/:channelId       # Toggle листинга
```

**Query keys:** `channelKeys.detail(channelId)`, `channelKeys.team(channelId)`

### UI

- **Аватар + название**
- **Toggle `t('profile.channel.active')`** — вкл/выкл листинг в каталоге (**OWNER-ONLY**)
- **Group `t('profile.channel.stats')`** — `GroupItem` (видна всем members):
  - `t('profile.channel.subscribers')`
  - `t('profile.channel.dealCount')`
  - `t('profile.channel.earned')` (`<Amount>`)
- **Group `t('profile.channel.pricing')`** — список правил: тип поста + цена, `chevron`
- **Group `t('profile.channel.team')`** — список участников (`GroupItem`):
  - `before`: аватар
  - Заголовок: имя
  - `after`: роль badge
  - `chevron`
- **GroupItem `t('profile.channel.invite')`** — иконка `+`
- Кнопка `t('profile.channel.edit')` (`secondary`) — **OWNER-ONLY**

### ABAC — видимость секций для менеджеров

| Право менеджера | Видимые секции |
|-----------------|----------------|
| `view_deals` | Статистика (количество сделок, заработок) |
| `manage_listings` | — (OWNER-ONLY, см. 6.3) |
| `manage_team` | Секция "Команда" + "Пригласить" |
| Без прав | Только базовая информация (аватар, название, статус) |

> Toggle листинга и кнопка "Редактировать" — **OWNER-ONLY**. Manager видит текущий статус листинга (read-only badge), но не может его изменить.

### Действия

| Действие | Результат |
|----------|-----------|
| Toggle листинг | `PUT /api/v1/channels/:id` (optimistic update) — только owner |
| Тап участник | → `/profile/channels/:channelId/team/:userId` |
| "Пригласить" | → `/profile/channels/:channelId/team/invite` |
| "Редактировать" | → `/profile/channels/:channelId/edit` — только owner |

### Error states

| Ошибка | UI |
|--------|----|
| 403 нет доступа | `ErrorScreen` `t('errors.forbidden.title')` + navigate back |
| Ошибка загрузки | `ErrorScreen` + retry |

---

## 5.6 Редактирование канала

| | |
|---|---|
| **Route** | `/profile/channels/:channelId/edit` |
| **Цель** | Обновить описание, тематику, цены |
| **Кто видит** | **Owner ONLY** (`@channelAuth.isOwner`) |

### API

```
GET /api/v1/channels/:channelId
PUT /api/v1/channels/:channelId
```

### UI

- **Input `t('profile.edit.description')`** — `textarea`, pre-filled, max 5000
- **Select `t('profile.edit.topic')`** — pre-filled
- **Builder `t('profile.edit.pricing')`** — редактируемый список:
  - Каждое правило:
    - `Select` тип + `Input` цена
    - `Input` `t('profile.pricing.description')` — что включено
    - **Group `t('profile.pricing.limits')`**: макс. текст / кнопки / медиа (с дефолтами по типу, см. 5.2)
  - `t('profile.pricing.addRule')` (`link`)
  - Кнопка удаления (×)
- Кнопка `t('common.save')` (`primary`)

### Действия

| Действие | Результат |
|----------|-----------|
| "Сохранить" | `PUT /api/v1/channels/:channelId` → navigate back `/profile/channels/:channelId` |

### Error states

| Ошибка | UI |
|--------|----|
| 403 не owner | `ErrorScreen` `t('errors.forbidden.title')` + navigate back |
| Ошибка сохранения | Toast `t('common.toast.saveFailed')` |

---

## 5.7 Команда канала

| | |
|---|---|
| **Route** | `/profile/channels/:channelId/team` |
| **Цель** | Управление менеджерами канала |
| **Кто видит** | Owner или Manager (`manage_team`) |

### API

```
GET /api/v1/channels/:channelId/team
```

**Query keys:** `channelKeys.team(channelId)`

### UI

- Список участников (`GroupItem`):
  - `before`: аватар
  - Заголовок: имя
  - `subtitle`: роль badge + права summary (например: `t('profile.team.rightsSummary', { rights })`)
  - `chevron`
- **GroupItem `t('profile.team.invite')`** — иконка `+`

### Действия

| Действие | Результат |
|----------|-----------|
| Тап участник | → `/profile/channels/:channelId/team/:userId` |
| "Пригласить" | → `/profile/channels/:channelId/team/invite` |

### Empty state

| Emoji | i18n title | i18n description | CTA |
|-------|------------|------------------|-----|
| `👥` | `profile.team.empty.title` | `profile.team.empty.description` | `profile.team.empty.cta` → `invite` |

### Error states

| Ошибка | UI |
|--------|----|
| Ошибка загрузки команды | `ErrorScreen` + retry |
| 403 нет доступа | `ErrorScreen` `t('errors.forbidden.title')` + navigate back |

---

## 5.8 Приглашение в команду

| | |
|---|---|
| **Route** | `/profile/channels/:channelId/team/invite` |
| **Цель** | Пригласить нового менеджера |
| **Кто видит** | Owner или Manager (`manage_team`) |

### API

```
POST /api/v1/channels/:channelId/team
```

### UI

- **Input `t('profile.invite.username')`** — text
- **Group `t('profile.invite.rights')`** — Toggle для каждого:
  - `publish` — `t('profile.rights.publish')`
  - `moderate` — `t('profile.rights.moderate')`
  - `view_deals` — `t('profile.rights.viewDeals')`
  - `manage_listings` — `t('profile.rights.manageListings')` (**скрыт** если inviter — manager; виден только owner, т.к. OWNER-ONLY на бэкенде)
  - `manage_team` — `t('profile.rights.manageTeam')`
- Кнопка `t('profile.invite.submit')` (`primary`)

### Request body

```typescript
{
  username: string;  // или userId
  rights: {
    publish: boolean;
    moderate: boolean;
    viewDeals: boolean;
    manageListings: boolean;
    manageTeam: boolean;
  };
}
```

### Действия

| Действие | Результат |
|----------|-----------|
| "Пригласить" | `POST /api/v1/channels/:id/team` → toast `t('profile.toast.inviteSent')` → navigate back |

### Error states

| Ошибка | UI |
|--------|----|
| Пользователь не найден | Inline error `t('profile.invite.userNotFound')` |
| Уже в команде | Toast `t('profile.invite.alreadyMember')` |

---

## 5.9 Права участника

| | |
|---|---|
| **Route** | `/profile/channels/:channelId/team/:userId` |
| **Цель** | Просмотр и редактирование прав менеджера |
| **Кто видит** | Owner или Manager (`manage_team`) |

### API

```
GET    /api/v1/channels/:channelId/team           # Найти участника
PUT    /api/v1/channels/:channelId/team/:userId    # Обновить права
DELETE /api/v1/channels/:channelId/team/:userId    # Удалить
```

### UI

- **Аватар + имя + роль** — read-only
- **Group `t('profile.member.rights')`** — Toggle для каждого (как в 5.8):
  - **Если owner**: toggle-ы активны, изменения сохраняются
  - **Если manager с `manage_team`**: toggle-ы **disabled** с tooltip `t('profile.team.ownerOnly')` — "Только владелец может изменять права". Права видны read-only
- Кнопка `t('common.save')` (`primary`) — **только для owner**
- Кнопка `t('profile.member.remove')` (`secondary`, `destructive`) — доступна owner И manager с `manage_team`

### ABAC

| Роль просматривающего | Toggle-ы прав | Кнопка "Сохранить" | Кнопка "Удалить" |
|-----------------------|---------------|--------------------|--------------------|
| Owner | Enabled (editable) | Visible | Visible |
| Manager (`manage_team`) | **Disabled** + tooltip | **Hidden** | Visible |

### Действия

| Действие | Результат |
|----------|-----------|
| "Сохранить" | `PUT /api/v1/channels/:id/team/:userId` → navigate back — только owner |
| "Удалить" | → `DialogModal` подтверждения → `DELETE` → navigate `/profile/channels/:channelId/team` |

### Error states

| Ошибка | UI |
|--------|----|
| 403 не owner (при PUT) | Toast `t('errors.forbidden.title')` |
| Удаление последнего менеджера | Toast `t('profile.member.cannotRemoveSelf')` (если пытается удалить себя) |

---

## Файловая структура

```
src/pages/profile/
  ProfilePage.tsx              # Route: /profile
  LanguagePage.tsx              # Route: /profile/language
  NotificationsPage.tsx        # Route: /profile/notifications
  RegisterChannelPage.tsx      # Route: /profile/channels/new
  ChannelManagePage.tsx        # Route: /profile/channels/:channelId
  ChannelEditPage.tsx          # Route: /profile/channels/:channelId/edit
  TeamPage.tsx                 # Route: /profile/channels/:channelId/team
  TeamInvitePage.tsx           # Route: /profile/channels/:channelId/team/invite
  TeamMemberPage.tsx           # Route: /profile/channels/:channelId/team/:userId

src/features/channels/
  components/
    PricingRulesBuilder.tsx     # Динамический builder цен с лимитами (reused in new + edit)
    TeamMemberListItem.tsx
    RightToggles.tsx            # Группа Toggle-ов прав (reused in invite + member)
  hooks/
    useChannelRights.ts         # ABAC hook (isOwner, hasRight) — shared with catalog
```
