# Actors and Personas

## Overview

The platform has **4 actor roles** with different access levels and capabilities.

> **Important**: Actors are **contextual roles**, not fixed user types. A single Telegram user can simultaneously be an Advertiser (buying ads on other channels) and a Channel Owner (selling ads on their own channel). Role is determined by the user's relationship to a specific resource (deal, channel), not by a global attribute. See [ABAC Pattern](./05-patterns-and-decisions/08-abac.md).

## Actor: Advertiser

| Attribute | Value |
|-----------|-------|
| **C4 ID** | `advertiser` |
| **Description** | Pays escrow, approves creative, manages ad requests |
| **Entry point** | Mini App (Telegram WebApp) |

### Capabilities

- Browse channel marketplace with filters (topic, audience size, price range)
- Create advertising deals (offers) for specific channels
- Submit creative briefs with requirements
- Review and approve/reject creative drafts
- Deposit TON to escrow for deals
- Monitor deal progress and delivery status
- File disputes when delivery terms are violated
- View payment history and balances

### Typical Flow

```
Browse channels → Create deal offer → Submit brief → Review creative →
Approve creative → Deposit TON → Wait for publication → Verify delivery → Complete
```

---

## Actor: Channel Owner

| Attribute | Value |
|-----------|-------|
| **C4 ID** | `channelOwner` |
| **Description** | Owns Telegram channel, delivers creative, receives payout, manages team rights |
| **Entry point** | Mini App (Telegram WebApp) |

### Capabilities

- Register and manage channel listings (description, pricing, statistics)
- Accept or negotiate deal offers from advertisers
- Prepare creative drafts based on advertiser briefs
- Publish approved creative to their Telegram channel
- Receive TON payouts after delivery verification
- Manage channel team: invite admins, assign rights, revoke access
- View earnings, deal history, and channel performance

### Team Management (RBAC)

Channel owners can delegate responsibilities via `channel_memberships`:

| Role | Description |
|------|-------------|
| **OWNER** | Full control — all rights, team management, payout settings |
| **MANAGER** | Delegated rights via JSONB `rights` field |

Manager rights are granular and stored as JSONB:

```json
{
  "publish": true,
  "moderate": true,
  "view_deals": true,
  "manage_listings": false,
  "manage_team": false
}
```

### Typical Flow

```
Register channel → Set pricing → Receive offer → Negotiate/accept →
Receive brief → Create draft → Submit for review → Publish → Get paid
```

---

## Actor: Channel Admin

| Attribute | Value |
|-----------|-------|
| **C4 ID** | `channelAdmin` |
| **Description** | PR Manager / channel admin with delegated rights from owner |
| **Entry point** | Mini App (Telegram WebApp) |

### Capabilities

Subset of Channel Owner capabilities, controlled by JSONB `rights`:

- Handle deals assigned to them (if `view_deals` right granted)
- Submit creative drafts (if `moderate` right granted)
- Publish approved creative to channel (if `publish` right granted)
- View channel statistics (if `view_deals` right granted)

### Authorization Flow

```
Channel Owner invites admin → Assigns rights via JSONB →
Admin accesses Mini App → Auth Service checks channel_memberships →
Rights-filtered actions available
```

---

## Actor: Platform Operator

| Attribute | Value |
|-----------|-------|
| **C4 ID** | `platformOperator` |
| **Description** | Risk control, dispute resolution, reconciliation oversight |
| **Entry point** | Backend API (direct access, no Mini App) |

### Capabilities

- Review and resolve escalated disputes
- Approve high-value transactions (>1000 TON) per [Confirmation Policy](./07-financial-system/06-confirmation-policy.md)
- Monitor reconciliation reports (ledger vs blockchain vs deals)
- Receive alerts on discrepancies via Telegram Bot notifications
- Override automated dispute resolution decisions
- View audit logs and financial reports

### Typical Flow

```
Receive alert → Review dispute evidence → Make resolution decision →
Trigger release or refund → Monitor reconciliation
```

---

## How Roles Are Determined (ABAC)

There is no `role` column on the `users` table. Instead, the actor role is resolved at runtime from resource attributes:

| Actor Role | Resolved From | Example |
|-----------|--------------|---------|
| **Advertiser** | `deals.advertiser_id == user.id` | User created a deal to buy ads |
| **Channel Owner** | `channel_memberships(channel_id, user_id).role == 'OWNER'` | User owns the target channel |
| **Channel Admin** | `channel_memberships(channel_id, user_id).role == 'MANAGER'` | User is a team member |
| **Platform Operator** | `users.is_operator == TRUE` | User has platform admin flag |

A single user with `id=42` can be:
- **Advertiser** in deal A (where `deal_a.advertiser_id = 42`)
- **Channel Owner** for channel X (where `channel_memberships(X, 42).role = 'OWNER'`)
- **Channel Admin** for channel Y (where `channel_memberships(Y, 42).role = 'MANAGER'`)

## Access Matrix

Capabilities are checked via ABAC policies, not fixed roles. The table below shows which **contextual role** grants each capability:

| Capability | Advertiser (in deal) | Channel Owner (of channel) | Channel Admin (of channel) | Operator |
|-----------|:---:|:---:|:---:|:---:|
| Browse marketplace | Y | Y | Y | - |
| Create deals | Y | - | - | - |
| Accept deals | - | Y | rights-based | - |
| Manage listings | - | Y | rights-based | - |
| Deposit escrow | Y | - | - | - |
| Submit creative | - | Y | rights-based | - |
| Manage team | - | Y | - | - |
| File disputes | Y | Y | - | - |
| Resolve disputes | - | - | - | Y |
| View audit logs | - | - | - | Y |
| Reconciliation | - | - | - | Y |

## Related Documents

- [ABAC Pattern](./05-patterns-and-decisions/08-abac.md)
- [Team Management](./03-feature-specs/07-team-management.md)
- [Auth & Security](./10-security-and-compliance.md)
- [Backend API Components](./04-architecture/03-backend-api-components.md) — Auth Service, Channel Team Service
