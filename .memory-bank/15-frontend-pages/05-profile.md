# Profile

> Tab 4. User settings, channel management, command.

## Navigation

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

## New API endpoints

> Endpoints **missing** in `11-api-contracts.md`.

| Method | Path | Description | Auth |
|--------|------|-------------|------|
| `GET` | `/api/v1/profile` | Profile details | Authenticated |
| `PUT` | `/api/v1/profile/language` | Change language | Authenticated |
| `PUT` | `/api/v1/profile/onboarding` | Complete onboarding | Authenticated |
| `GET` | `/api/v1/profile/notifications` | Notification settings | Authenticated |
| `PUT` | `/api/v1/profile/notifications` | Update settings | Authenticated |

### New query keys (add to `query-keys.ts`)

```typescript
export const profileKeys = {
  me: ['profile'] as const,
  notifications: ['profile', 'notifications'] as const,
};
```

---

## 5.1 Home profile

| | |
|---|---|
| **Route** | `/profile` |
| **Target** | User settings, channel management |
| **Who sees** | All authorized |

### API

```
GET /api/v1/profile
GET /api/v1/channels?owner=me
```

**Query keys:** `profileKeys.me`, `channelKeys.list({ owner: 'me' })`

### UI

- **Group `t('profile.account')`**:
  - Avatar (from Telegram) + name + username
- **Group `t('profile.channels')`** — list of user channels (`GroupItem`):
  - `before`: channel avatar
  - Title: channel name
  - `subtitle`: `t('profile.channel.subscribers', { count })`
  - `after`: badge listing status (active/inactive)
  - `chevron`
- **GroupItem `t('profile.addChannel')`** — icon `+`
- **Group `t('profile.settings')`**:
  - GroupItem `t('profile.language')` — `chevron`, `after`: current language
  - GroupItem `t('profile.notifications')` — `chevron`
- **Group `t('profile.stats')`** (if there are transactions):
  - `t('profile.stats.totalDeals')`
  - `t('profile.stats.gmv')` (`<Amount>`)
  - `t('profile.stats.earned')` (`<Amount>`, for channel owners)

### Actions

| Action | Result |
|----------|-----------|
| Tap channel | → `/profile/channels/:channelId` |
| "Add channel" | → `/profile/channels/new` |
| "Language" | → `/profile/language` |
| "Notifications" | → `/profile/notifications` |

### Empty state (channels)

| Emoji | i18n title | i18n description | CTA |
|-------|------------|------------------|-----|
| `📡` | `profile.channels.empty.title` | `profile.channels.empty.description` | `profile.channels.empty.cta` → `/profile/channels/new` |

### Error states

| Error | UI |
|--------|----|
| Error loading profile | `ErrorScreen` + retry |
| Offline | Banner `t('errors.offline')` |

---

## 5.2 Channel registration

| | |
|---|---|
| **Route** | `/profile/channels/new` |
| **Target** | Register a channel on the platform |
| **Who sees** | All authorized |

### API

```
POST /api/v1/channels         # \u0420\u0435\u0433\u0438\u0441\u0442\u0440\u0430\u0446\u0438\u044f
GET  /api/v1/channels/topics   # \u0422\u0435\u043c\u0430\u0442\u0438\u043a\u0438 (enum)
```

### UI - Two-Step Form

**Step 1: Channel Check**
- **Input `t('profile.register.channelLink')`** — format `@username` or `t.me/...`
- Button `t('profile.register.verify')` (`secondary`)
- Instructions: `t('profile.register.addBotInstruction')` — "Add @AdvertMarketBot as a channel administrator"
- **Copy button** next to "@AdvertMarketBot": copy icon → `navigator.clipboard.writeText('@AdvertMarketBot')` → toast `t('common.copied')`
- **Button `t('profile.register.openBot')`** — `openTelegramLink('https://t.me/AdvertMarketBot')`

**Step 2: Setup (after verification)**
- Channel name - read-only, from API
- Subscribers - read-only, from API
- **Input `t('profile.register.description')`** — `textarea`, max 5000 characters
- **Select `t('profile.register.topic')`** - from enum/API
- **Builder `t('profile.register.pricing')`** — dynamic list:
  - Each rule:
    - `Select` post type (`STANDARD`/`PINNED`/`STORY`/`REPOST`/`NATIVE`)
    - `Input` price in TON
    - `Input` `t('profile.pricing.description')` - `textarea`, what is included in the placement
    - **Group `t('profile.pricing.limits')`** — restrictions:
      - `Input` `t('profile.pricing.maxTextLength')` — numeric, default by post type (see table)
      - `Input` `t('profile.pricing.maxButtons')` — numeric, default by post type
      - `Input` `t('profile.pricing.maxMedia')` — numeric, default by post type
  - Button `t('profile.pricing.addRule')` (`link`)
  - Delete button (×) on each rule
  - Min 1 rule
- Button `t('profile.register.submit')` (`primary`, full-width)

### Default limits by post type

| Post type | Max. text | Max. buttons | Max. media |
|-----------|-------------|--------------|-------------|
| `STANDARD` | 4096 | 9 (3×3) | 10 |
| `PINNED` | 4096 | 9 (3×3) | 10 |
| `STORY` | 2048 | 1 | 1 |
| `REPOST` | 4096 | 0 | 0 |
| `NATIVE` | 4096 | 9 (3×3) | 10 |

> Defaults are filled in automatically when you select a post type. The user can tighten (decrease), but cannot exceed Telegram limits.

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
    description?: string;  // \u0447\u0442\u043e \u0432\u043a\u043b\u044e\u0447\u0435\u043d\u043e
    limits: {
      maxTextLength: number;   // <= Telegram limit
      maxButtons: number;      // <= Telegram limit
      maxMedia: number;        // <= Telegram limit
    };
  }[];
}
```

### Actions

| Action | Result |
|----------|-----------|
| "Check" | Validation via API → show Step 2 |
| Selecting a post type in a rule | Autofill default limits |
| "Register" | `POST /api/v1/channels` → navigate `/profile/channels/:newId` |

### Precondition

The bot `@AdMarketBot` must be added as admin to the channel. If not:
- Error with instructions: `t('profile.register.botNotAdmin')`
- Copy and open bot buttons

### Error states

| Error | UI |
|--------|----|
| Bot is not admin | Inline error `t('profile.register.botNotAdmin')` |
| Channel not found | Inline error `t('profile.register.channelNotFound')` |
| The channel is already registered | Toast `t('profile.register.alreadyRegistered')` |

---

## 5.3 Language

| | |
|---|---|
| **Route** | `/profile/language` |
| **Target** | Switching interface language |
| **Who sees** | All |

### API

```
PUT /api/v1/profile/language
```

### UI

- **Group** with `RadioGroup`:
  - `t('profile.language.ru')` (default from `Telegram.WebApp.initDataUnsafe.user.language_code`)
  - `t('profile.language.en')`

### Actions

| Action | Result |
|----------|-----------|
| Language selection | `i18n.changeLanguage()` + `PUT /api/v1/profile/language` + BackButton |

### Error states

| Error | UI |
|--------|----|
| Error saving language | Toast `t('common.toast.saveFailed')` + rollback `i18n.changeLanguage()` |

---

## 5.4 Notifications

| | |
|---|---|
| **Route** | `/profile/notifications` |
| **Target** | Configure which notifications to receive in the bot |
| **Who sees** | All |

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

### Actions

| Action | Result |
|----------|-----------|
| Toggle | Autosave: optimistic update + `PUT /api/v1/profile/notifications` |

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

| Error | UI |
|--------|----|
| Save error | Toast `t('common.toast.saveFailed')` + rollback optimistic update |

---

## 5.5 Channel management

| | |
|---|---|
| **Route** | `/profile/channels/:channelId` |
| **Target** | Listing management, statistics, team |
| **Who sees** | Owner or Manager (with any right) |

### API

```
GET /api/v1/channels/:channelId
GET /api/v1/channels/:channelId/team
PUT /api/v1/channels/:channelId       # Toggle \u043b\u0438\u0441\u0442\u0438\u043d\u0433\u0430
```

**Query keys:** `channelKeys.detail(channelId)`, `channelKeys.team(channelId)`

### UI

- **Avatar + name**
- **Toggle `t('profile.channel.active')`** — on/off listing in the directory (**OWNER-ONLY**)
- **Group `t('profile.channel.stats')`** — `GroupItem` (visible to all members):
  - `t('profile.channel.subscribers')`
  - `t('profile.channel.dealCount')`
  - `t('profile.channel.earned')` (`<Amount>`)
- **Group `t('profile.channel.pricing')`** — list of rules: post type + price, `chevron`
- **Group `t('profile.channel.team')`** — list of participants (`GroupItem`):
  - `before`: avatar
  - Title: name
  - `after`: badge role
  - `chevron`
- **GroupItem `t('profile.channel.invite')`** — icon `+`
- Button `t('profile.channel.edit')` (`secondary`) - **OWNER-ONLY**

### ABAC - visibility of sections for managers

| Manager's right | Visible sections |
|-----------------|----------------|
| `view_deals` | Statistics (number of transactions, earnings) |
| `manage_listings` | — (OWNER-ONLY, see 6.3) |
| `manage_team` | Section "Team" + "Invite" |
| Without rights | Only basic information (avatar, title, status) |

> Listing Toggle and "Edit" button - **OWNER-ONLY**. Manager sees the current status of the listing (read-only badge), but cannot change it.

### Actions

| Action | Result |
|----------|-----------|
| Toggle listing | `PUT /api/v1/channels/:id` (optimistic update) — owner only |
| Tap Member | → `/profile/channels/:channelId/team/:userId` |
| "Invite" | → `/profile/channels/:channelId/team/invite` |
| "Edit" | → `/profile/channels/:channelId/edit` — owner only |

### Error states

| Error | UI |
|--------|----|
| 403 no access | `ErrorScreen` `t('errors.forbidden.title')` + navigate back |
| Loading Error | `ErrorScreen` + retry |

---

## 5.6 Editing a channel

| | |
|---|---|
| **Route** | `/profile/channels/:channelId/edit` |
| **Target** | Update description, subject, prices |
| **Who sees** | **Owner ONLY** (`@channelAuth.isOwner`) |

### API

```
GET /api/v1/channels/:channelId
PUT /api/v1/channels/:channelId
```

### UI

- **Input `t('profile.edit.description')`** — `textarea`, pre-filled, max 5000
- **Select `t('profile.edit.topic')`** — pre-filled
- **Builder `t('profile.edit.pricing')`** - editable list:
  - Each rule:
    - `Select` type + `Input` price
    - `Input` `t('profile.pricing.description')` — what is included
    - **Group `t('profile.pricing.limits')`**: max. text / buttons / media (with defaults by type, see 5.2)
  - `t('profile.pricing.addRule')` (`link`)
  - Delete button (×)
- Button `t('common.save')` (`primary`)

### Actions

| Action | Result |
|----------|-----------|
| "Save" | `PUT /api/v1/channels/:channelId` → navigate back `/profile/channels/:channelId` |

### Error states

| Error | UI |
|--------|----|
| 403 not owner | `ErrorScreen` `t('errors.forbidden.title')` + navigate back |
| Save error | Toast `t('common.toast.saveFailed')` |

---

## 5.7 Channel command

| | |
|---|---|
| **Route** | `/profile/channels/:channelId/team` |
| **Target** | Channel Manager Management |
| **Who sees** | Owner or Manager (`manage_team`) |

### API

```
GET /api/v1/channels/:channelId/team
```

**Query keys:** `channelKeys.team(channelId)`

### UI

- List of participants (`GroupItem`):
  - `before`: avatar
  - Title: name
  - `subtitle`: badge role + summary rights (for example: `t('profile.team.rightsSummary', { rights })`)
  - `chevron`
- **GroupItem `t('profile.team.invite')`** — icon `+`

### Actions

| Action | Result |
|----------|-----------|
| Tap Member | → `/profile/channels/:channelId/team/:userId` |
| "Invite" | → `/profile/channels/:channelId/team/invite` |

### Empty state

| Emoji | i18n title | i18n description | CTA |
|-------|------------|------------------|-----|
| `👥` | `profile.team.empty.title` | `profile.team.empty.description` | `profile.team.empty.cta` → `invite` |

### Error states

| Error | UI |
|--------|----|
| Command loading error | `ErrorScreen` + retry |
| 403 no access | `ErrorScreen` `t('errors.forbidden.title')` + navigate back |

---

## 5.8 Invitation to the team

| | |
|---|---|
| **Route** | `/profile/channels/:channelId/team/invite` |
| **Target** | Invite a new manager |
| **Who sees** | Owner or Manager (`manage_team`) |

### API

```
POST /api/v1/channels/:channelId/team
```

### UI

- **Input `t('profile.invite.username')`** — text
- **Group `t('profile.invite.rights')`** — Toggle for each:
  - `publish` — `t('profile.rights.publish')`
  - `moderate` — `t('profile.rights.moderate')`
  - `view_deals` — `t('profile.rights.viewDeals')`
  - `manage_listings` — `t('profile.rights.manageListings')` (**hidden** if inviter is manager; only owner is visible, because OWNER-ONLY on the backend)
  - `manage_team` — `t('profile.rights.manageTeam')`
- Button `t('profile.invite.submit')` (`primary`)

### Request body

```typescript
{
  username: string;  // \u0438\u043b\u0438 userId
  rights: {
    publish: boolean;
    moderate: boolean;
    viewDeals: boolean;
    manageListings: boolean;
    manageTeam: boolean;
  };
}
```

### Actions

| Action | Result |
|----------|-----------|
| "Invite" | `POST /api/v1/channels/:id/team` → toast `t('profile.toast.inviteSent')` → navigate back |

### Error states

| Error | UI |
|--------|----|
| User not found | Inline error `t('profile.invite.userNotFound')` |
| Already on the team | Toast `t('profile.invite.alreadyMember')` |

---

## 5.9 Participant rights

| | |
|---|---|
| **Route** | `/profile/channels/:channelId/team/:userId` |
| **Target** | Viewing and editing manager rights |
| **Who sees** | Owner or Manager (`manage_team`) |

### API

```
GET    /api/v1/channels/:channelId/team           # \u041d\u0430\u0439\u0442\u0438 \u0443\u0447\u0430\u0441\u0442\u043d\u0438\u043a\u0430
PUT    /api/v1/channels/:channelId/team/:userId    # \u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c \u043f\u0440\u0430\u0432\u0430
DELETE /api/v1/channels/:channelId/team/:userId    # \u0423\u0434\u0430\u043b\u0438\u0442\u044c
```

### UI

- **Avatar + name + role** — read-only
- **Group `t('profile.member.rights')`** — Toggle for each (as in 5.8):
  - **If owner**: toggles are active, changes are saved
  - **If manager with `manage_team`**: toggle-s **disabled** with tooltip `t('profile.team.ownerOnly')` - “Only the owner can change rights.” Rights are visible read-only
- Button `t('common.save')` (`primary`) — **only for owner**
- Button `t('profile.member.remove')` (`secondary`, `destructive`) - available to owner AND manager with `manage_team`

### ABAC

| Role of the reviewer | Toggle rights | Save button | Delete button |
|-----------------------|---------------|--------------------|--------------------|
| Owner | Enabled (editable) | Visible | Visible |
| Manager (`manage_team`) | **Disabled** + tooltip | **Hidden** | Visible |

### Actions

| Action | Result |
|----------|-----------|
| "Save" | `PUT /api/v1/channels/:id/team/:userId` → navigate back — owner only |
| "Delete" | → `DialogModal` confirmations → `DELETE` → navigate `/profile/channels/:channelId/team` |

### Error states

| Error | UI |
|--------|----|
| 403 not owner (on PUT) | Toast `t('errors.forbidden.title')` |
| Removing the last manager | Toast `t('profile.member.cannotRemoveSelf')` (if trying to delete itself) |

---

## File structure

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
    PricingRulesBuilder.tsx     # \u0414\u0438\u043d\u0430\u043c\u0438\u0447\u0435\u0441\u043a\u0438\u0439 builder \u0446\u0435\u043d \u0441 \u043b\u0438\u043c\u0438\u0442\u0430\u043c\u0438 (reused in new + edit)
    TeamMemberListItem.tsx
    RightToggles.tsx            # \u0413\u0440\u0443\u043f\u043f\u0430 Toggle-\u043e\u0432 \u043f\u0440\u0430\u0432 (reused in invite + member)
  hooks/
    useChannelRights.ts         # ABAC hook (isOwner, hasRight) — shared with catalog
```
