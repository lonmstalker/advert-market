# Онбординг

> 3 экрана при первом запуске. Показывается по флагу `onboardingCompleted == false` из ответа auth.

## Навигация

```
/onboarding → /onboarding/interest → /onboarding/tour → redirect
```

---

## 1.1 Приветствие

| | |
|---|---|
| **Route** | `/onboarding` |
| **Цель** | Познакомить с платформой при первом запуске |
| **Кто видит** | Пользователь с `onboardingCompleted == false` |
| **Данные** | Нет |

### UI

- Иллюстрация (TON/Telegram стилистика)
- Заголовок: `t('onboarding.welcome.title')` — "Ad Market"
- Подзаголовок: `t('onboarding.welcome.subtitle')` — "Безопасная реклама в Telegram с TON эскроу"
- Кнопка `t('onboarding.welcome.start')` (`primary`, full-width)

### Действия

| Действие | Результат |
|----------|-----------|
| "Начать" | → `/onboarding/interest` |

### Компоненты

- `Button` (primary)
- Статичная иллюстрация (SVG/Lottie)

---

## 1.2 Выбор интереса

| | |
|---|---|
| **Route** | `/onboarding/interest` |
| **Цель** | Понять сценарии пользователя для персонализации |
| **Кто видит** | Пользователь с `onboardingCompleted == false` |
| **Данные** | Нет |

### UI

- Заголовок: `t('onboarding.interest.title')` — "Что вас интересует?"
- Две крупные карточки (**toggle**, можно выбрать одну или обе):
  - **`t('onboarding.interest.advertiser')`** — "Хочу рекламу" — подзаголовок: `t('onboarding.interest.advertiser.description')` — "Найдите каналы и разместите рекламу"
  - **`t('onboarding.interest.owner')`** — "Владею каналом" — подзаголовок: `t('onboarding.interest.owner.description')` — "Получайте заказы на рекламу"
- Мелкий текст: `t('onboarding.interest.hint')` — "Можно выбрать оба варианта"
- Кнопка `t('common.continue')` (`primary`, full-width) — активна при выборе хотя бы одной карточки

### Действия

| Действие | Результат |
|----------|-----------|
| Тап по карточке | Toggle selection (вкл/выкл). Карточка подсвечивается при выборе |
| "Продолжить" | → `/onboarding/tour` |

### Состояние

```typescript
type OnboardingInterest = 'advertiser' | 'owner' | 'both';

// Локальный state, отправляется на сервер при завершении онбординга
const [selected, setSelected] = useState<Set<'advertiser' | 'owner'>>(new Set());

// Результат:
// selected = {'advertiser'} → 'advertiser'
// selected = {'owner'} → 'owner'
// selected = {'advertiser', 'owner'} → 'both'
```

### Компоненты

- Кастомные карточки выбора с toggle-поведением (не `GroupItem` — крупный формат с иконками)

---

## 1.3 Обзор возможностей

| | |
|---|---|
| **Route** | `/onboarding/tour` |
| **Цель** | Краткий тур по 3 ключевым функциям |
| **Кто видит** | Пользователь с `onboardingCompleted == false` |
| **Данные** | `selected` interest из предыдущего шага |

### UI

Swipeable карусель из 3 слайдов:

| # | Заголовок (i18n) | Описание (i18n) |
|---|------------------|-----------------|
| 1 | `t('onboarding.tour.slide1.title')` | `t('onboarding.tour.slide1.description')` |
| 2 | `t('onboarding.tour.slide2.title')` | `t('onboarding.tour.slide2.description')` |
| 3 | `t('onboarding.tour.slide3.title')` | `t('onboarding.tour.slide3.description')` |

- Индикатор точек (dot indicator)
- Кнопка `t('onboarding.tour.finish')` (`primary`, full-width) — видна на последнем слайде
- Кнопка `t('onboarding.tour.skip')` (`link`, `secondary`) — видна на 1 и 2 слайде

### Действия

| Действие | Результат |
|----------|-----------|
| Свайп | Переход между слайдами |
| "Завершить" / "Пропустить" | `PUT /api/v1/profile/onboarding` → redirect по interest |

### Сохранение на сервере

```
PUT /api/v1/profile/onboarding
```

```typescript
{
  interest: 'advertiser' | 'owner' | 'both';
}
```

Сервер устанавливает `onboardingCompleted = true` + сохраняет `interest` в профиле пользователя.

### Логика редиректа

```typescript
const interest = getOnboardingInterest(); // из state предыдущего шага

switch (interest) {
  case 'advertiser':
    navigate('/catalog');
    break;
  case 'owner':
    navigate('/profile/channels/new');
    break;
  case 'both':
    navigate('/catalog'); // основной сценарий — поиск каналов
    break;
}
```

### Компоненты

- Карусель (swipeable, touch-friendly)
- Dot indicator
- `Button` (primary + link)

---

## Общие заметки

### Guard route

Пользователь **уже создан** при команде `/start` в боте или при `POST /api/v1/auth/login` (upsert). Онбординг показывается **только** если `onboardingCompleted == false` в ответе auth.

```typescript
// В корневом роутере
const { data: authData } = useAuth();

if (!authData.onboardingCompleted) {
  return <Navigate to="/onboarding" />;
}
```

После завершения онбординга (`PUT /api/v1/profile/onboarding`) — инвалидация auth query, guard пропускает пользователя дальше.

### Анимации

- Fade-in при первом появлении
- Slide-transition между экранами
- Карусель с momentum scrolling

### Error states

| Ошибка | UI |
|--------|----|
| `PUT /api/v1/profile/onboarding` failed | Toast `t('errors.network')` + retry |
| Offline | Banner `t('errors.offline')` |

### Файловая структура

```
src/pages/onboarding/
  OnboardingPage.tsx        # Route: /onboarding
  OnboardingInterestPage.tsx # Route: /onboarding/interest
  OnboardingTourPage.tsx     # Route: /onboarding/tour
```
