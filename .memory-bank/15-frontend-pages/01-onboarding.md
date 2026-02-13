# Onboarding

> 3 screens on first launch. Indicated by the `onboardingCompleted == false` flag from the auth response.

## Navigation

```
/onboarding → /onboarding/interest → /onboarding/tour → redirect
```

---

## 1.1 Greetings

| | |
|---|---|
| **Route** | `/onboarding` |
| **Target** | Get to know the platform when you first launch |
| **Who sees** | User with `onboardingCompleted == false` |
| **Data** | No |

### UI

- Illustration (TON/Telegram style)
- Header: `t('onboarding.welcome.title')` - "Ad Market"
- Subtitle: `t('onboarding.welcome.subtitle')` - "Safe advertising in Telegram with TON escrow"
- Button `t('onboarding.welcome.start')` (`primary`, full-width)

### Actions

| Action | Result |
|----------|-----------|
| "Start" | → `/onboarding/interest` |

### Components

- `Button` (primary)
- Static illustration (SVG/Lottie)

---

## 1.2 Selecting an interest

| | |
|---|---|
| **Route** | `/onboarding/interest` |
| **Target** | Understand User Scenarios for Personalization |
| **Who sees** | User with `onboardingCompleted == false` |
| **Data** | No |

### UI

- Title: `t('onboarding.interest.title')` - "What are you interested in?"
- Two large cards (**toggle**, you can choose one or both):
  - **`t('onboarding.interest.advertiser')`** - "I want advertising" - subtitle: `t('onboarding.interest.advertiser.description')` - "Find channels and place advertising"
  - **`t('onboarding.interest.owner')`** - "I own the channel" - subtitle: `t('onboarding.interest.owner.description')` - "Receive orders for advertising"
- Small text: `t('onboarding.interest.hint')` - "You can choose both options"
- Button `t('common.continue')` (`primary`, full-width) - active when at least one card is selected

### Actions

| Action | Result |
|----------|-----------|
| Tap on card | Toggle selection (on/off). The card is highlighted when selected |
| "Continue" | → `/onboarding/tour` |

### State

```typescript
type OnboardingInterest = 'advertiser' | 'owner' | 'both';

// Local state, sent to the server when onboarding is completed
const [selected, setSelected] = useState<Set<'advertiser' | 'owner'>>(new Set());

// Result:
// selected = {'advertiser'} → 'advertiser'
// selected = {'owner'} → 'owner'
// selected = {'advertiser', 'owner'} → 'both'
```

### Components

- Custom choice cards with toggle behavior (not `GroupItem` - large format with icons)

---

## 1.3 Features overview

| | |
|---|---|
| **Route** | `/onboarding/tour` |
| **Target** | A Quick Tour of 3 Key Features |
| **Who sees** | User with `onboardingCompleted == false` |
| **Data** | `selected` interest from the previous step |

### UI

Swipeable carousel of 3 slides:

| # | Header (i18n) | Description (i18n) |
|---|------------------|-----------------|
| 1 | `t('onboarding.tour.slide1.title')` | `t('onboarding.tour.slide1.description')` |
| 2 | `t('onboarding.tour.slide2.title')` | `t('onboarding.tour.slide2.description')` |
| 3 | `t('onboarding.tour.slide3.title')` | `t('onboarding.tour.slide3.description')` |

- Dot indicator
- Button `t('onboarding.tour.finish')` (`primary`, full-width) - visible on the last slide
- Button `t('onboarding.tour.skip')` (`link`, `secondary`) - visible on slides 1 and 2

### Actions

| Action | Result |
|----------|-----------|
| Swipe | Transition between slides |
| "Finish" / "Skip" | `PUT /api/v1/profile/onboarding` → redirect by interest |

### Saving on the server

```
PUT /api/v1/profile/onboarding
```

```typescript
{
  interest: 'advertiser' | 'owner' | 'both';
}
```

The server sets `onboardingCompleted = true` + stores `interest` in the user profile.

### Redirect logic

```typescript
const interest = getOnboardingInterest(); // from state of the previous step

switch (interest) {
  case 'advertiser':
    navigate('/catalog');
    break;
  case 'owner':
    navigate('/profile/channels/new');
    break;
  case 'both':
    navigate('/catalog'); // main scenario - searching for channels
    break;
}
```

### Components

- Carousel (swipeable, touch-friendly)
- Dot indicator
- `Button` (primary + link)

---

## General notes

### Guard route

The user has **already been created** with the `/start` command in the bot or with `POST /api/v1/auth/login` (upsert). Onboarding is shown **only** if `onboardingCompleted == false` is in the auth response.

```typescript
// In the root router
const { data: authData } = useAuth();

if (!authData.onboardingCompleted) {
  return <Navigate to="/onboarding" />;
}
```

After onboarding is completed (`PUT /api/v1/profile/onboarding`) - auth query invalidation, guard allows the user to continue.

### Animations

- Fade-in on first appearance
- Slide-transition between screens
- Carousel with momentum scrolling

### Error states

| Error | UI |
|--------|----|
| `PUT /api/v1/profile/onboarding` failed | Toast `t('errors.network')` + retry |
| Offline | Banner `t('errors.offline')` |

### File structure

```
src/pages/onboarding/
  OnboardingPage.tsx        # Route: /onboarding
  OnboardingInterestPage.tsx # Route: /onboarding/interest
  OnboardingTourPage.tsx     # Route: /onboarding/tour
```
