# Каталог каналов

> Tab 1. Поиск и выбор каналов для размещения рекламы + создание сделки.

## Навигация

```
/catalog
  ├── [Sheet] Фильтры
  ├── /catalog/channels/:channelId
  └── /deals/new?channelId=:channelId
```

---

## 2.1 Список каналов

| | |
|---|---|
| **Route** | `/catalog` |
| **Цель** | Поиск и просмотр каналов для размещения рекламы |
| **Кто видит** | Все авторизованные |

### API

```
GET /api/v1/channels?cursor=&limit=20&q=&topic=&minSubs=&maxSubs=&minPrice=&maxPrice=&sort=
```

**Query keys:** `channelKeys.list(params)`

### Deep link при входе

При наличии `startapp=channel_{id}` в параметрах Telegram Mini App — автоматический роутинг на `/catalog/channels/:id` (обработка в корневом роутере, см. 6.4).

### UI

- **Поисковая строка** — сверху, debounce 300ms, placeholder: `t('catalog.search.placeholder')`
- **Кнопка `t('catalog.filters.button')`** — справа от поиска, badge с количеством активных фильтров
- **Список каналов** — `Group` + `GroupItem`:
  - `before`: аватар канала (40×40)
  - Заголовок: название канала
  - `subtitle`: `t('catalog.channel.subscribers', { count })`
  - `after`: цена (мин. из pricing rules, формат `<Amount>`)
- **Infinite scroll** — skeleton загрузка (3 GroupItem placeholder)
- **Pull-to-refresh**

### Действия

| Действие | Результат |
|----------|-----------|
| Ввод в поиск | Debounce → перезапрос с `q=` |
| "Фильтры" | → Sheet 2.2 |
| Тап по каналу | → `/catalog/channels/:channelId` |
| Pull-to-refresh | Инвалидация `channelKeys.lists()` |

### Empty state

| Emoji | i18n title | i18n description | CTA |
|-------|------------|------------------|-----|
| `🔍` | `catalog.empty.title` | `catalog.empty.description` | `catalog.empty.cta` → Reset filters |

### Состояние фильтров

```typescript
type CatalogFilters = {
  q?: string;
  topic?: string;
  minSubs?: number;
  maxSubs?: number;
  minPrice?: bigint; // nanoTON
  maxPrice?: bigint; // nanoTON
  sort?: 'relevance' | 'subscribers' | 'price_asc' | 'price_desc' | 'er';
};
```

Хранение: URL search params (shareable, back-compatible).

### Error states

| Ошибка | UI |
|--------|----|
| Ошибка загрузки списка | `ErrorScreen` + retry |
| Offline | Banner `t('errors.offline')` |

---

## 2.2 Фильтры (Sheet)

| | |
|---|---|
| **Route** | N/A (Sheet overlay над каталогом) |
| **Цель** | Настройка параметров поиска каналов |
| **Кто видит** | Все, кто открыл фильтры |

### API

```
GET /api/v1/channels/topics   # Список тематик (или enum на клиенте)
```

### UI

- Заголовок: `t('catalog.filters.title')`
- **Тематика** — `Select`, `t('catalog.filters.topic')`
- **Подписчики** — два `Input` (numeric): `t('catalog.filters.from')` / `t('catalog.filters.to')`
- **Цена за пост** — два `Input` (numeric, TON): `t('catalog.filters.from')` / `t('catalog.filters.to')`
- **Сортировка** — `Select`, `t('catalog.filters.sort')`:
  - `t('catalog.filters.sort.relevance')` (default)
  - `t('catalog.filters.sort.subscribers')`
  - `t('catalog.filters.sort.priceAsc')`
  - `t('catalog.filters.sort.priceDesc')`
  - `t('catalog.filters.sort.er')`
- Кнопка `t('catalog.filters.show', { count: N })` (`primary`, full-width) — N обновляется при изменении фильтров
- Кнопка `t('catalog.filters.reset')` (`link`)

### Действия

| Действие | Результат |
|----------|-----------|
| Изменение фильтров | Локальное состояние sheet. Prefetch count для "Показать N" |
| "Показать" | Применить фильтры → закрыть sheet → перезагрузить каталог |
| "Сбросить" | Очистить все фильтры |
| Свайп вниз | Закрыть без применения |

### Компоненты

- `Sheet`
- `Select`
- `Input` (numeric)
- `Button` (primary + link)

### Error states

| Ошибка | UI |
|--------|----|
| Ошибка загрузки тематик | Inline error + retry |

---

## 2.3 Карточка канала

| | |
|---|---|
| **Route** | `/catalog/channels/:channelId` |
| **Цель** | Полная информация о канале перед созданием сделки |
| **Кто видит** | Все |

### API

```
GET /api/v1/channels/:channelId
GET /api/v1/channels/:channelId/team   # Проверка: роль пользователя
```

**Query keys:** `channelKeys.detail(channelId)`, `channelKeys.team(channelId)`

### UI

- **Header row:**
  - **Аватар** — крупный (80×80)
  - **Название** — `title1`
  - **ShareButton** — иконка share, рядом с заголовком (см. 6.4)
- **Описание** — `body`
- **Group `t('catalog.channel.stats')`** — `GroupItem`:
  - `t('catalog.channel.subscribers')` (formatted number)
  - `t('catalog.channel.avgReach')`
  - `t('catalog.channel.er')` (%)
- **Group `t('catalog.channel.pricing')`** — `GroupItem` для каждого pricing rule:
  - Название типа поста
  - `after`: цена в TON (`<Amount>`)
- **Тематики** — caption badges
- **Кнопки** (sticky bottom, full-width):
  - Если НЕ member: `t('catalog.channel.createDeal')` (`primary`) → 2.4
  - Если **owner**: ОБЕ кнопки — `t('catalog.channel.edit')` (`secondary`) + `t('catalog.channel.createDeal')` (`primary`)
  - Если manager: только `t('catalog.channel.createDeal')` (`primary`)

### ABAC

| Роль | Кнопки |
|------|--------|
| Посторонний | "Создать сделку" |
| Owner | "Редактировать" + "Создать сделку" (может тестировать как рекламодатель) |
| Manager (любые права) | "Создать сделку" |

> "Редактировать" — **OWNER-ONLY** (`@channelAuth.isOwner` на бэкенде). Менеджеры НЕ видят эту кнопку, даже с `manage_listings`.

### Действия

| Действие | Результат |
|----------|-----------|
| "Создать сделку" | → `/deals/new?channelId=:channelId` |
| "Редактировать" | → `/profile/channels/:channelId/edit` (только owner) |
| ShareButton | Deep link `t.me/AdvertMarketBot/app?startapp=channel_{channelId}` → `switchInlineQuery()` или clipboard + toast |
| BackButton | → `/catalog` |

### Определение роли

```typescript
const { isOwner, hasRight } = useChannelRights(channelId);
```

### Error states

| Ошибка | UI |
|--------|----|
| 404 канал не найден | `ErrorScreen` `t('errors.notFound.title')` + navigate `/catalog` |
| Ошибка загрузки | `ErrorScreen` + retry |

---

## 2.4 Создание сделки

| | |
|---|---|
| **Route** | `/deals/new?channelId=:channelId` |
| **Цель** | Создать оффер на размещение рекламы |
| **Кто видит** | Любой авторизованный (становится рекламодателем) |

### API

```
GET  /api/v1/channels/:channelId    # Pricing rules
POST /api/v1/deals                   # Создание сделки
```

**Mutation:** invalidates `dealKeys.lists()`

### UI

- **Карточка канала** (read-only, compact): аватар + название + подписчики
- **Select `t('deals.create.postType')`** — из pricing rules канала
- **Цена** — `title2`, `tabular-nums`, read-only (обновляется при выборе типа)
- **Input `t('deals.create.message')`** — `textarea`, optional, max 2000 символов, placeholder: `t('deals.create.messagePlaceholder')`
- Кнопка `t('deals.create.submit')` (`primary`, full-width)

### Действия

| Действие | Результат |
|----------|-----------|
| Выбор типа поста | Обновление отображаемой цены из pricing rule |
| "Отправить оффер" | `POST /api/v1/deals` → navigate `/deals/:newDealId` |

### Request body

```typescript
{
  channelId: number;      // из URL params
  pricingRuleId: number;  // из Select
  message?: string;       // из textarea
}
```

### Валидация

- `pricingRuleId` — обязательно (Select не может быть пустым)
- `message` — опционально, max 2000 символов

### Error states

| Ошибка | UI |
|--------|----|
| 409 (deal already exists) | Toast `t('deals.error.alreadyExists')` |
| Ошибка создания | Toast `t('common.toast.saveFailed')` |

---

## Файловая структура

```
src/pages/catalog/
  CatalogPage.tsx              # Route: /catalog
  ChannelDetailPage.tsx        # Route: /catalog/channels/:channelId

src/pages/deals/
  CreateDealPage.tsx           # Route: /deals/new

src/features/channels/
  api/
    channels.ts                # API calls
  components/
    ChannelCard.tsx             # Compact channel card (reused in deal pages)
    ChannelFiltersSheet.tsx     # Sheet overlay
    ChannelListItem.tsx         # GroupItem для списка
    ChannelStats.tsx            # Группа статистики
    PricingRulesList.tsx        # Список цен
  hooks/
    useChannelFilters.ts        # URL search params state
    useChannelRights.ts         # ABAC hook (isOwner, hasRight)
  types/
    channel.ts                  # Zod schemas + types
```
