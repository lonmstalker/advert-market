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

### UI

- **Поисковая строка** — сверху, debounce 300ms
- **Кнопка "Фильтры"** — справа от поиска, badge с количеством активных фильтров
- **Список каналов** — `Group` + `GroupItem`:
  - `before`: аватар канала (40×40)
  - Заголовок: название канала
  - `subtitle`: "{subscribers} подписчиков"
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

| Emoji | Заголовок | Описание | CTA |
|-------|-----------|----------|-----|
| `🔍` | Ничего не найдено | Попробуйте изменить фильтры | [Сбросить фильтры] |

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

- Заголовок `title2`: "Фильтры"
- **Тематика** — `Select`, список категорий из API/enum
- **Подписчики** — два `Input` (numeric): "От" / "До"
- **Цена за пост** — два `Input` (numeric, TON): "От" / "До"
- **Сортировка** — `Select`:
  - По релевантности (default)
  - По подписчикам
  - По цене (возр.)
  - По цене (убыв.)
  - По ER
- Кнопка "Показать N каналов" (`primary`, full-width) — N обновляется при изменении фильтров
- Кнопка "Сбросить" (`link`)

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
GET /api/v1/channels/:channelId/team   # Проверка: пользователь — owner?
```

**Query keys:** `channelKeys.detail(channelId)`, `channelKeys.team(channelId)`

### UI

- **Аватар** — крупный (80×80)
- **Название** — `title1`
- **Описание** — `body`
- **Group "Статистика"** — `GroupItem`:
  - Подписчики (formatted number)
  - Средний охват
  - ER (engagement rate, %)
- **Group "Цены"** — `GroupItem` для каждого pricing rule:
  - Название типа поста
  - `after`: цена в TON (`<Amount>`)
- **Тематики** — caption badges
- **Кнопка** (sticky bottom, full-width):
  - Если НЕ owner: "Создать сделку" (`primary`) → 2.4
  - Если owner: "Редактировать" (`secondary`) → `/profile/channels/:channelId/edit`

### Действия

| Действие | Результат |
|----------|-----------|
| "Создать сделку" | → `/deals/new?channelId=:channelId` |
| "Редактировать" | → `/profile/channels/:channelId/edit` (если owner) |
| BackButton | → `/catalog` |

### Определение owner

```typescript
const { data: team } = useQuery({
  queryKey: channelKeys.team(channelId),
  queryFn: () => fetchChannelTeam(channelId),
});
const isOwner = team?.some(m => m.userId === currentUser.id && m.role === 'OWNER');
```

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
- **Select "Тип поста"** — из pricing rules канала
- **Цена** — `title2`, `tabular-nums`, read-only (обновляется при выборе типа)
- **Input "Сообщение владельцу"** — `textarea`, optional, max 2000 символов, placeholder: "Опишите пожелания к рекламе"
- Кнопка "Отправить оффер" (`primary`, full-width)

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
  types/
    channel.ts                  # Zod schemas + types
```
