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

- **Group "Аккаунт"**:
  - Аватар (из Telegram) + имя + username
- **Group "Мои каналы"** — список каналов пользователя (`GroupItem`):
  - `before`: аватар канала
  - Заголовок: название канала
  - `subtitle`: "{subscribers} подписчиков"
  - `after`: статус листинга badge (active/inactive)
  - `chevron`
- **GroupItem "Добавить канал"** — иконка `+`
- **Group "Настройки"**:
  - GroupItem "Язык" — `chevron`, `after`: текущий язык
  - GroupItem "Уведомления" — `chevron`
- **Group "Статистика"** (если есть сделки):
  - Всего сделок
  - GMV (`<Amount>`)
  - Заработано (`<Amount>`, для владельцев каналов)

### Действия

| Действие | Результат |
|----------|-----------|
| Тап канал | → `/profile/channels/:channelId` |
| "Добавить канал" | → `/profile/channels/new` |
| "Язык" | → `/profile/language` |
| "Уведомления" | → `/profile/notifications` |

### Empty state (каналы)

| Emoji | Заголовок | Описание | CTA |
|-------|-----------|----------|-----|
| `📡` | Нет каналов | Зарегистрируйте канал для получения заказов | [Добавить канал] → `/profile/channels/new` |

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
- **Input "Ссылка на канал"** — формат `@username` или `t.me/...`
- Кнопка "Проверить" (`secondary`)
- Инструкция: "Добавьте @AdMarketBot как администратора канала"

**Шаг 2: Настройка (после проверки)**
- Название канала — read-only, из API
- Подписчики — read-only, из API
- **Input "Описание"** — `textarea`, max 5000 символов
- **Select "Тематика"** — из enum/API
- **Builder "Цены"** — динамический список:
  - Каждое правило: `Select` тип поста (`STANDARD`/`PINNED`/`STORY`/`REPOST`/`NATIVE`) + `Input` цена в TON
  - Кнопка "Добавить тариф" (`link`)
  - Кнопка удаления (×) на каждом правиле
  - Min 1 правило
- Кнопка "Зарегистрировать" (`primary`, full-width)

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
  }[];
}
```

### Действия

| Действие | Результат |
|----------|-----------|
| "Проверить" | Валидация через API → показать Шаг 2 |
| "Зарегистрировать" | `POST /api/v1/channels` → navigate `/profile/channels/:newId` |

### Предусловие

Бот `@AdMarketBot` должен быть добавлен как admin в канал. Если нет:
- Ошибка с инструкцией: "Добавьте @AdMarketBot как администратора вашего канала"
- Link на бота для удобства

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
  - Русский (default из `Telegram.WebApp.initDataUnsafe.user.language_code`)
  - English

### Действия

| Действие | Результат |
|----------|-----------|
| Выбор языка | `i18n.changeLanguage()` + `PUT /api/v1/profile/language` + BackButton |

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

- **Group "Сделки"**:
  - Toggle: Новые офферы
  - Toggle: Принятие/отклонение
  - Toggle: Статус доставки
- **Group "Финансы"**:
  - Toggle: Пополнения
  - Toggle: Выплаты
  - Toggle: Эскроу
- **Group "Споры"**:
  - Toggle: Открытие
  - Toggle: Решение

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

---

## 5.5 Управление каналом

| | |
|---|---|
| **Route** | `/profile/channels/:channelId` |
| **Цель** | Управление листингом, статистика, команда |
| **Кто видит** | Owner или Manager (`manage_listings`) |

### API

```
GET /api/v1/channels/:channelId
GET /api/v1/channels/:channelId/team
PUT /api/v1/channels/:channelId       # Toggle листинга
```

**Query keys:** `channelKeys.detail(channelId)`, `channelKeys.team(channelId)`

### UI

- **Аватар + название**
- **Toggle "Активен"** — вкл/выкл листинг в каталоге
- **Group "Статистика"** — `GroupItem`:
  - Подписчики
  - Количество сделок
  - Заработано (`<Amount>`)
- **Group "Цены"** — список правил: тип поста + цена, `chevron`
- **Group "Команда"** — список участников (`GroupItem`):
  - `before`: аватар
  - Заголовок: имя
  - `after`: роль badge
  - `chevron`
- **GroupItem "Пригласить"** — иконка `+`
- Кнопка "Редактировать" (`secondary`)

### Действия

| Действие | Результат |
|----------|-----------|
| Toggle листинг | `PUT /api/v1/channels/:id` (optimistic update) |
| Тап участник | → `/profile/channels/:channelId/team/:userId` |
| "Пригласить" | → `/profile/channels/:channelId/team/invite` |
| "Редактировать" | → `/profile/channels/:channelId/edit` |

---

## 5.6 Редактирование канала

| | |
|---|---|
| **Route** | `/profile/channels/:channelId/edit` |
| **Цель** | Обновить описание, тематику, цены |
| **Кто видит** | Owner или Manager (`manage_listings`) |

### API

```
GET /api/v1/channels/:channelId
PUT /api/v1/channels/:channelId
```

### UI

- **Input "Описание"** — `textarea`, pre-filled, max 5000
- **Select "Тематика"** — pre-filled
- **Builder "Цены"** — редактируемый список:
  - Каждое правило: `Select` тип + `Input` цена
  - "Добавить тариф" (`link`)
  - Кнопка удаления (×)
- Кнопка "Сохранить" (`primary`)

### Действия

| Действие | Результат |
|----------|-----------|
| "Сохранить" | `PUT /api/v1/channels/:channelId` → navigate back `/profile/channels/:channelId` |

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
  - `subtitle`: роль badge + права summary (например: "публикация, модерация")
  - `chevron`
- **GroupItem "Пригласить участника"** — иконка `+`

### Действия

| Действие | Результат |
|----------|-----------|
| Тап участник | → `/profile/channels/:channelId/team/:userId` |
| "Пригласить" | → `/profile/channels/:channelId/team/invite` |

### Empty state

| Emoji | Заголовок | Описание | CTA |
|-------|-----------|----------|-----|
| `👥` | Нет менеджеров | Пригласите админов для помощи с каналом | [Пригласить] → `invite` |

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

- **Input "Username или ID"** — text
- **Group "Права"** — Toggle для каждого:
  - `publish` — Публикация
  - `moderate` — Модерация креативов
  - `view_deals` — Просмотр сделок
  - `manage_listings` — Управление листингом
  - `manage_team` — Управление командой
- Кнопка "Пригласить" (`primary`)

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
| "Пригласить" | `POST /api/v1/channels/:id/team` → toast "Приглашение отправлено" → navigate back |

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
- **Group "Права"** — Toggle для каждого (как в 5.8)
- Кнопка "Сохранить" (`primary`)
- Кнопка "Удалить из команды" (`secondary`, `destructive`)

### Действия

| Действие | Результат |
|----------|-----------|
| "Сохранить" | `PUT /api/v1/channels/:id/team/:userId` → navigate back |
| "Удалить" | → `DialogModal` подтверждения → `DELETE` → navigate `/profile/channels/:channelId/team` |

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
    PricingRulesBuilder.tsx     # Динамический builder цен (reused in new + edit)
    TeamMemberListItem.tsx
    RightToggles.tsx            # Группа Toggle-ов прав (reused in invite + member)
```
