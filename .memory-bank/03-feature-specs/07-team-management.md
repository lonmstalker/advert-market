# Feature: Team Management

## Overview

Channel owners can delegate responsibilities to team members (admins/managers) with granular, rights-based access control. This enables larger channels to have dedicated PR managers handle day-to-day deal operations while owners retain full control.

## Channel-Scoped Roles (ABAC Subject Attributes)

> **Note**: Roles in `channel_memberships` are **channel-scoped**, not global user types. The same user can be OWNER of channel A and MANAGER of channel B, while also being an Advertiser in unrelated deals. See [ABAC Pattern](../05-patterns-and-decisions/08-abac.md).

### Roles

| Role | Description | Scope |
|------|-------------|-------|
| **OWNER** | Channel creator with full control | All operations + team management for this channel |
| **MANAGER** | Delegated team member | Rights-based subset of operations for this channel |

### Rights (JSONB)

Manager rights are stored as JSONB in `channel_memberships.rights`:

```json
{
  "publish": true,
  "moderate": true,
  "view_deals": true,
  "manage_listings": false,
  "manage_team": false
}
```

| Right | Grants |
|-------|--------|
| `publish` | Publish creative to channel, schedule posts |
| `moderate` | Submit creative drafts, handle deal negotiations |
| `view_deals` | View deal details, timeline, and statistics |
| `manage_listings` | Update channel listing information |
| `manage_team` | Invite/remove other managers (delegated by owner) |

## Data Model

### channel_memberships Table

| Column | Type | Description |
|--------|------|-------------|
| `channel_id` | `BIGINT` | Telegram channel ID |
| `user_id` | `BIGINT` | Telegram user ID |
| `role` | `VARCHAR` | `OWNER` or `MANAGER` |
| `rights` | `JSONB` | Granular rights (NULL for OWNER = all rights) |
| `invited_by` | `BIGINT` | User who created this membership |
| `created_at` | `TIMESTAMPTZ` | Membership creation timestamp |
| `updated_at` | `TIMESTAMPTZ` | Last rights update |

**Composite primary key**: `(channel_id, user_id)`

## Workflows

### Invite Flow

```
Owner opens Team Management UI →
Enters Telegram username/ID →
Selects rights checkboxes →
POST /api/v1/channels/{id}/team (invite) →
Channel Team Service creates channel_membership →
Invited user receives notification via Bot
```

### Rights Update Flow

```
Owner opens Team Management UI →
Selects existing member →
Updates rights checkboxes →
PUT /api/v1/channels/{id}/team/{userId} →
Channel Team Service updates JSONB rights →
Member's active sessions reflect new rights
```

### Revoke Flow

```
Owner opens Team Management UI →
Selects member → Revoke access →
DELETE /api/v1/channels/{id}/team/{userId} →
Channel Team Service removes membership →
Revoked user loses access immediately
```

## Authorization Enforcement

The **Auth Service** enforces team rights on every API request:

1. Validate Telegram `initData` (HMAC verification)
2. Extract `user_id` from session
3. For channel-scoped operations:
   - Query `channel_memberships` for `(channel_id, user_id)`
   - If role = `OWNER` → allow all operations
   - If role = `MANAGER` → check specific right in JSONB
   - If no membership → deny access
4. Return `403 Forbidden` if insufficient rights

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/channels/{id}/team` | List team members |
| `POST` | `/api/v1/channels/{id}/team` | Invite new member |
| `PUT` | `/api/v1/channels/{id}/team/{userId}` | Update member rights |
| `DELETE` | `/api/v1/channels/{id}/team/{userId}` | Remove team member |

## Components Involved

| Component | Role |
|-----------|------|
| **Team Management UI** | Invite admins, assign rights, revoke access |
| **Channel Team Service** | CRUD for channel_memberships |
| **Auth Service** | Reads channel_memberships for authorization checks |
| **channel_memberships** | RBAC storage: role + JSONB rights |

## Related Documents

- [Actors and Personas](../02-actors-and-personas.md) — Channel Admin capabilities
- [Security & Compliance](../10-security-and-compliance.md) — auth model
- [Backend API Components](../04-architecture/03-backend-api-components.md) — Auth Service, Channel Team Service
