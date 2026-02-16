# Actors and Personas

## Overview

`advert-market` has four runtime actor groups:

1. Advertiser
2. Channel owner
3. Channel manager (delegated rights)
4. Operator (platform admin)

The core business flow is **Approve -> Pay**:

- owner accepts final terms (including creative and slot)
- advertiser pays escrow
- platform executes publication and verification

## 1) Advertiser

### Description

A user buying ad placement in Telegram channels.

### Goals

- Find relevant channels
- Negotiate terms and creative
- Pay only after owner approval
- Verify delivery and open dispute when needed

### Typical flow

Browse channels -> create offer -> negotiate -> owner accepts -> pay -> wait for publish -> verify -> complete/review.

## 2) Channel Owner

### Description

A user controlling one or more Telegram channels and selling inventory.

### Goals

- Approve/reject offers
- Confirm final terms before payment
- Publish according to agreed slot
- Receive payout after verification

### Typical flow

Receive offer -> accept or request revision -> wait for funding -> publish/schedule -> monitor verification -> receive payout.

## 3) Channel Manager

### Description

Delegated collaborator of the owner with scoped rights.

### Rights model

Rights are explicit and scoped to channel:

- `manage_listings`
- `manage_team`
- `moderate`
- `publish`
- `view_stats`

Managers cannot bypass owner-only financial or security controls.

## 4) Operator

### Description

Privileged platform actor for disputes and treasury risk controls.

### Target constraints

- separate admin workspace (outside TMA)
- strong auth (TOTP/WebAuthn)
- role split (`L1_SUPPORT`, `L2_ARBITRATOR`, `TREASURY_MASTER`)
- maker-checker for high-value actions

## Responsibility Matrix

| Capability | Advertiser | Owner | Manager | Operator |
|---|---:|---:|---:|---:|
| Create offer | Y | - | - | - |
| Accept/reject offer | - | Y | rights-based | - |
| Request revision / negotiate | Y | Y | rights-based | - |
| Trigger payment | Y | - | - | - |
| Publish/schedule | - | Y | rights-based | - |
| Open dispute | Y | Y | rights-based | - |
| Resolve dispute | - | - | - | Y |
| Emergency financial halt | - | - | - | Y |

## Related docs

- [Deal lifecycle](./03-feature-specs/02-deal-lifecycle.md)
- [Deal state machine](./06-deal-state-machine.md)
- [Security and compliance](./10-security-and-compliance.md)
