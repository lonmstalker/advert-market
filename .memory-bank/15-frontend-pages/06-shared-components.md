# Common components

> Reused UI elements, system policies (localization, errors, ABAC, deep links).

---

## 6.1 Localization (i18n)

### Strategy - two-level

| Level | Responsible for | Technology | Storage |
|---------|-------------|------------|----------|
| **Frontend** | UI labels, buttons, navigation, validation messages, empty states, placeholders | `i18next` + `react-i18next` | `src/shared/i18n/ru.json`, `en.json` |
| **Backend** | Notifications, email templates, API error descriptions, dynamic texts | Spring `MessageSource` | `messages/` (properties files) |

### Rule: ZERO hardcoded strings

All user-facing lines are only via `t('key')`. Exceptions: technical constants (date formats, regex patterns).

```typescript
// Right
<Button>{t('common.save')}</Button>
<EmptyState title={t('catalog.empty.title')} />

// Wrong
<Button>Save</Button>
<EmptyState title="Nothing found" />
```

### API errors

RFC 7807 `title` comes from the backend **already localized** (by `Accept-Language` header). Frontend does NOT translate API errors - displays `problem.title` as-is.

### Namespace structure

```
onboarding.*     — 01-onboarding
catalog.*        — 02-catalog
deals.*          — 03-deals
wallet.*         — 04-wallet
profile.*        — 05-profile
common.* - general (save, cancel, confirm, back, loading, copied)
errors.* — frontend-only errors (validation, offline, timeout)
```

### Language Definition

1. `Telegram.WebApp.initDataUnsafe.user.language_code` at first login
2. `GET /api/v1/profile` → `languageCode` is source-of-truth on subsequent logins
3. Frontend sync rule: `useAuth` applies `profile.languageCode` to both settings store and `i18n`
4. Runtime i18n bundles: both `ru` and `en` are preloaded at app init to allow immediate language switching in onboarding/profile without lazy reload races.
5. Fallback: `ru`

---

## 6.2 System errors (Error States)

### Global error table

| Error type | UI | User action | Automatic behavior |
|------------|----|-----------------------|--------------------------|
| **Offline** (no network) | Banner at the top: `t('errors.offline')` + retry | Retry button | Auto-retry on recovery (`navigator.onLine`) |
| **500 Server Error** | Full-screen: illustration + `t('errors.server')` + `t('common.retry')` | Retry → data reload | — |
| **403 Forbidden** | Full-screen: `t('errors.forbidden.title')` + `t('errors.forbidden.description')` + `t('common.back')` | Navigate back | — |
| **404 Not Found** | Full-screen: `t('errors.notFound.title')` + `t('errors.notFound.description')` + `t('common.home')` | Navigate to tab root | — |
| **409 Conflict** (state machine) | Toast error: `t('errors.conflict')` | — | Auto-refetch data |
| **401 Unauthorized** (token expired) | Without UI (transparent) | — | Re-auth via `initData` → repeat request |
| **429 Rate Limited** | Toast: `t('errors.rateLimited')` | — | Auto-retry after `Retry-After` delay |
| **Timeout** | Toast: `t('errors.timeout')` + retry | Retry button | — |

### Components

```typescript
// ErrorBoundary - wraps each route
<ErrorBoundary fallback={<ErrorScreen />}>
  <Outlet />
</ErrorBoundary>

// ErrorScreen - full-screen error (403, 404, 500)
type ErrorScreenProps = {
  illustration?: ReactNode;
  title: string;
  description: string;
  action?: { label: string; onClick: () => void };
};

// OfflineBanner - sticky banner when network is lost
// Displayed globally, on top of any content
```

### Processing in React Query

```typescript
// Global onError in QueryClient
queryCache: new QueryCache({
  onError: (error) => {
    if (error.status === 401) reAuth();
    if (error.status === 409) queryClient.invalidateQueries();
    if (error.status === 429) /* auto-retry with delay */;
  },
}),
```

---

## 6.3 ABAC matrix

### Manager rights - UI visibility

| Right | What's available | Without right |
|-------|-------------|-----------|
| `moderate` | Accept/reject/negotiate buttons (3.2), creative form (3.5) | Buttons hidden |
| `publish` | Publish/schedule buttons (3.7) | Buttons hidden |
| `view_deals` | List of channel deals, details, timeline (3.1, 3.2) | Channel deals hidden |
| `manage_listings` | — (see OWNER-exclusive below) | — |
| `manage_team` | Section "Team" (5.5), invite (5.8), remove (5.9) | Section hidden |

### OWNER-exclusive (NOT delegated)

| Action | API | Why |
|----------|-----|--------|
| Editing a channel | `PUT /api/v1/channels/:id` | `@channelAuth.isOwner` |
| Changing participant rights | `PUT /api/v1/channels/:id/team/:userId` | Only the owner changes toggle s |

> `manage_listings` in ABAC backend is implemented as `@channelAuth.isOwner`. Managers cannot edit the channel, despite having this right.

### Rule for UI

**No right = button is hidden**, NOT disabled.

The only exception: page 5.9 "Participant rights" - toggle rights **disabled** with tooltip `t('profile.team.ownerOnly')` for managers with `manage_team`.

### Checking rights on the client

```typescript
// Hook for ABAC
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

### ABAC by deal subpages (for managers)

| Page | Required right | Who else sees |
|----------|---------------|---------------|
| 3.3 Negotiations | `moderate` | Advertiser |
| 3.5 Creative | `moderate` | — |
| 3.6 Review | — | Advertiser only |
| 3.7 Planning | `publish` | — |
| 3.8 Payment | — | Advertiser only |
| 3.9 Dispute (opening) | `view_deals` | Advertiser |
| 3.11 Evidence | `view_deals` | Advertiser |

---

## 6.4 Deep Links and Sharing

### Deep link format

```
t.me/AdvertMarketBot/app?startapp={type}_{id}
```

| Type | Example | Routing |
|-----|--------|---------|
| `channel_{id}` | `channel_12345` | `/catalog/channels/12345` |
| `deal_{uuid_short}` | `deal_abc123` | `/deals/abc123` |
| `dispute_{uuid_short}` | `dispute_abc123` | `/deals/abc123/dispute` |

### Login Processing

```typescript
// Deep link handling should be a launch-time concern. Avoid overriding in-app navigation
// or refreshes on nested routes (e.g. /profile/channels/new), since Telegram launch params
// may still be available outside the initial landing page.
useEffect(() => {
  if (location.pathname !== '/' && location.pathname !== '/catalog') return;

  const lp = retrieveLaunchParams(true);
  const startParam = lp.tgWebAppStartParam ?? lp.tgWebAppData?.startParam;
  if (typeof startParam !== 'string' || startParam.length === 0) return;

  if (startParam.startsWith('channel_')) {
    navigate(`/catalog/channels/${startParam.replace('channel_', '')}`, { replace: true });
  } else if (startParam.startsWith('deal_')) {
    navigate(`/deals/${startParam.replace('deal_', '')}`, { replace: true });
  }
}, [location.pathname, navigate]);
```

### ShareButton Component

```typescript
type ShareButtonProps = {
  type: 'channel' | 'deal' | 'dispute';
  id: string;
};

// Behavior:
// 1. Forms a deep link URL
// 2. Calls switchInlineQuery() (if available in Telegram)
// 3. Fallback: navigator.clipboard → toast t('common.copied')
```

**Where used:**
- 2.3 Channel card (next to the title)
- 3.2 Transaction details (in header)

---

## Sheet overlays

All sheets use the `Sheet` component from the UI Kit.

| Sheet | Where is it used | Contents |
|-------|-----------------|------------|
| Catalog filters | 2.2 (`CatalogPage`) | Subject, subscribers, price, sorting |
| Payment TON Connect | 3.8 (`DealDetailPage`) | Amount, wallet, payment button |
| History filters | 4.4 (`HistoryPage`) | Transaction type, period |
| Deal support | 3.2 (`DealDetailPage`) | Subject, description, context of the transaction |

### Pattern

```typescript
<Sheet open={isOpen} onOpenChange={setIsOpen}>
  <SheetContent />
</Sheet>
```

Sheets close:
- Swipe down
- Action button (apply/pay)
- Tap on backdrop

---

## DialogModal (confirmation)

For destructive actions. The `DialogModal` component from the UI Kit is used.

### Use Cases

| Action | Title | Description | Confirm | Cancel |
|----------|-----------|----------|---------|--------|
| Cancel deal | `t('deals.confirm.cancel.title')` | `t('deals.confirm.cancel.description')` | destructive | secondary |
| Offer Rejection | `t('deals.confirm.reject.title')` | `t('deals.confirm.reject.description')` | destructive | secondary |
| Filing a Dispute | `t('deals.confirm.dispute.title')` | `t('deals.confirm.dispute.description')` | destructive | secondary |
| Withdrawal | `t('wallet.confirm.withdraw.title')` | `t('wallet.confirm.withdraw.description', { amount, address })` | primary | secondary |
| Removal from a team | `t('profile.confirm.removeMember.title')` | `t('profile.confirm.removeMember.description', { name })` | destructive | secondary |

### Pattern

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

## Toast notifications

The `Toast` component from the UI Kit is used.

### Types

| Type | Key examples |
|-----|----------------|
| **Success** | `deals.toast.created`, `deals.toast.creativeApproved`, `wallet.toast.paymentSent`, `profile.toast.channelRegistered`, `common.toast.saved` |
| **Error** | `wallet.toast.paymentFailed`, `wallet.toast.insufficientFunds`, `errors.network`, `common.toast.saveFailed` |
| **Info** | `common.copied`, `profile.toast.inviteSent`, `wallet.toast.topUpProcessing` |

### Pattern

```typescript
const { toast } = useToast();
toast({ type: 'success', message: t('deals.toast.created') });
```

Toast automatically hides after 3 seconds. Error - after 5 seconds.

---

## Skeleton loading

Each page with data has a skeleton state that follows the structure of the content.

### Pattern

```typescript
if (isLoading) return <PageSkeleton />;
```

### Skeleton components

| Page | Skeleton |
|----------|----------|
| Channel list | 3× `GroupItem` skeleton (avatar circle + 2 text lines + price rect) |
| Channel card | Large avatar circle + text blocks + stats grid + pricing list |
| List of deals | 3× `GroupItem` skeleton (avatar + text + badge rect) |
| Deal details | Badge rect + card skeleton + amount rect + action buttons + timeline list |
| Wallet | Balance rect (large) + 2 circle buttons + 5× transaction items |
| Profile | Avatar + text + channel list + settings list |

Used `SkeletonElement` from UI Kit with `pulse` animation.

---

## Empty states

A single component for all empty states.

### Component

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

### Full table

| Page | Emoji | Header (i18n) | Description (i18n) | CTA (i18n) | Navigation |
|----------|-------|------------------|-----------------|------------|-----------|
| Catalog (no results) | `🔍` | `catalog.empty.title` | `catalog.empty.description` | `catalog.empty.cta` | Reset filters |
| Deals (advertiser) | `📬` | `deals.empty.advertiser.title` | `deals.empty.advertiser.description` | `deals.empty.advertiser.cta` | `/catalog` |
| Transactions (channel) | `📬` | `deals.empty.channel.title` | `deals.empty.channel.description` | `deals.empty.channel.cta` | `/profile/channels/new` |
| Finance | `📜` | `wallet.empty.title` | `wallet.empty.description` | `wallet.empty.cta` | channel directory |
| History (with filters) | `📜` | `wallet.history.empty.title` | `wallet.history.empty.description` | `wallet.history.empty.cta` | Reset filters |
| Profile Channels | `📡` | `profile.channels.empty.title` | `profile.channels.empty.description` | `profile.channels.empty.cta` | `/profile/channels/new` |
| Channel Team | `👥` | `profile.team.empty.title` | `profile.team.empty.description` | `profile.team.empty.cta` | `team/invite` |

---

## Bottom Tab Navigation

4 tabs, always visible (except for onboarding).

| # | Label (i18n) | Icon | Route | Badge |
|---|-------------|--------|-------|-------|
| 1 | `common.tabs.catalog` | Search / Grid | `/catalog` | — |
| 2 | `common.tabs.deals` | FileText/Handshake | `/deals` | Number of transactions requiring action |
| 3 | `common.tabs.wallet` | Wallet | `/wallet` | — |
| 4 | `common.tabs.profile` | User | `/profile` | — |

Badge on the "Transactions" tab - the number of transactions where the current user must perform an action.

---

## Telegram BackButton

`@tma.js/sdk-react` `BackButton` is used.

| Route | BackButton target |
|-------|-------------------|
| `/catalog` | No (tab root) |
| `/catalog/channels/:id` | `/catalog` |
| `/deals` | No (tab root) |
| `/deals/:id` | `/deals` |
| `/deals/:id/*` | `/deals/:id` |
| `/wallet` | No (tab root) |
| `/wallet/*` | `/wallet` |
| `/profile` | No (tab root) |
| `/profile/*` | `/profile` (or parent level) |
| `/onboarding/*` | No (disabled) |

---

## Routing

### Full routes table

```typescript
const routes = [
  // Onboarding
  { path: '/onboarding', page: 'OnboardingPage' },
  { path: '/onboarding/interest', page: 'OnboardingInterestPage' },
  { path: '/onboarding/tour', page: 'OnboardingTourPage' },

  // Directory (Tab 1)
  { path: '/catalog', page: 'CatalogPage' },
  { path: '/catalog/channels/:channelId', page: 'ChannelDetailPage' },

  // Trades (Tab 2)
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

  // Finance (Tab 3)
  { path: '/wallet', page: 'WalletPage' },
  { path: '/wallet/withdraw', page: 'WithdrawPage' },       // Channel Owner only
  { path: '/wallet/history', page: 'HistoryPage' },
  { path: '/wallet/history/:txId', page: 'TransactionDetailPage' },

  // Profile (Tab 4)
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

**Total: 27 routes** (3 onboarding + 2 catalog + 10 deals + 4 finance + 8 profile).

All pages are `lazy()` for code splitting (except root layout).

---

## i18n keys (structure)

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

## Verification

### 1. Deal state machine - all 16 statuses are covered in matrix 3.2

| # | Status | Advertiser | Channel (Owner) | Channel (Manager) | Covered |
|---|--------|--------------|---------------|-----------------|--------|
| 1 | `DRAFT` | — | — | — | N/A |
| 2 | `OFFER_PENDING` | Cancel | Accept/Negotiate/Reject | moderate: same | Yes |
| 3 | `NEGOTIATING` | Reply/Cancel | Reply/Reject | moderate: same | Yes |
| 4 | `ACCEPTED` | — | Cancel | — | Yes |
| 5 | `AWAITING_PAYMENT` | Pay | — | — | Yes |
| 6 | `FUNDED` | Send brief | Submit creative | moderate: same | Yes |
| 7 | `CREATIVE_SUBMITTED` | Approve/Revision | — | — | Yes |
| 8 | `CREATIVE_APPROVED` | — | Publish/Schedule | publish: same | Yes |
| 9 | `SCHEDULED` | — | — | — | Yes |
| 10 | `PUBLISHED` | — | — | — | Yes |
| 11 | `DELIVERY_VERIFYING` | — | — | — | Yes |
| 12 | `COMPLETED_RELEASED` | Feedback (v2) | — | — | Yes |
| 13 | `DISPUTED` | Evidence | Evidence | view_deals: same | Yes |
| 14 | `CANCELLED` | — | — | — | Yes |
| 15 | `REFUNDED` | — | — | — | Yes |
| 16 | `EXPIRED` | — | — | — | Yes |

### 2. Empty states - all 7 are documented (table above), all via i18n

### 3. ABAC - full matrix in 6.3 + per-page instructions in each file

### 4. Deep links - all 3 types are documented in 6.4, input processing is described

### 5. Localization - ZERO hardcoded strings, namespace structure in 6.1

### 6. Error states - global table in 6.2 + per-page errors in all files (01-06)
