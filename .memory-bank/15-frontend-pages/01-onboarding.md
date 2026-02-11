# Онбординг

> 3 экрана при первом запуске. Показывается только новым пользователям (нет записи в `users`).

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
| **Кто видит** | Новый пользователь (нет записи в `users`) |
| **Данные** | Нет |

### UI

- Иллюстрация (TON/Telegram стилистика)
- Заголовок `title1`: "Ad Market"
- Подзаголовок `body`, `secondary`: "Безопасная реклама в Telegram с TON эскроу"
- Кнопка "Начать" (`primary`, full-width)

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
| **Цель** | Понять главный сценарий пользователя для персонализации |
| **Кто видит** | Новый пользователь |
| **Данные** | Нет (сохраняет в `localStorage`) |

### UI

- Заголовок `title2`: "Что вас интересует?"
- Две крупные карточки (tappable):
  - **"Хочу рекламу"** — подзаголовок: "Найдите каналы и разместите рекламу"
  - **"Владею каналом"** — подзаголовок: "Получайте заказы на рекламу"
- Мелкий текст `caption`, `secondary`: "Можно и то, и другое"

### Действия

| Действие | Результат |
|----------|-----------|
| Тап по карточке | `localStorage.setItem('onboarding_interest', 'advertiser' \| 'owner')` → `/onboarding/tour` |

### Состояние

```typescript
type OnboardingInterest = 'advertiser' | 'owner';
// localStorage key: 'onboarding_interest'
```

### Компоненты

- Кастомные карточки выбора (не `GroupItem` — крупный формат с иконками)

---

## 1.3 Обзор возможностей

| | |
|---|---|
| **Route** | `/onboarding/tour` |
| **Цель** | Краткий тур по 3 ключевым функциям |
| **Кто видит** | Новый пользователь |
| **Данные** | `onboarding_interest` из `localStorage` |

### UI

Swipeable карусель из 3 слайдов:

| # | Заголовок | Описание |
|---|-----------|----------|
| 1 | Каталог каналов | Найдите идеальную площадку для рекламы |
| 2 | Безопасные сделки | TON эскроу защищает обе стороны |
| 3 | Прозрачная доставка | Автоматическая проверка размещения |

- Индикатор точек (dot indicator)
- Кнопка "Завершить" (`primary`, full-width) — видна на последнем слайде
- Кнопка "Пропустить" (`link`, `secondary`) — видна на 1 и 2 слайде

### Действия

| Действие | Результат |
|----------|-----------|
| Свайп | Переход между слайдами |
| "Завершить" / "Пропустить" | Redirect по `onboarding_interest` |

### Логика редиректа

```typescript
const interest = localStorage.getItem('onboarding_interest');
if (interest === 'owner') {
  navigate('/profile/channels/new');
} else {
  navigate('/catalog');
}
```

### Компоненты

- Карусель (swipeable, touch-friendly)
- Dot indicator
- `Button` (primary + link)

---

## Общие заметки

### Guard route

Онбординг показывается **только** если `POST /api/v1/auth/validate` вернул пользователя без флага `onboardingCompleted`. После завершения онбординга флаг сохраняется в `localStorage` и на сервере.

### Анимации

- Fade-in при первом появлении
- Slide-transition между экранами
- Карусель с momentum scrolling

### Файловая структура

```
src/pages/onboarding/
  OnboardingPage.tsx        # Route: /onboarding
  OnboardingInterestPage.tsx # Route: /onboarding/interest
  OnboardingTourPage.tsx     # Route: /onboarding/tour
```
