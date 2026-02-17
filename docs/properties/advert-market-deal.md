# advert-market-deal

## Table of Contents

- [Deal Timeout](#deal-timeout)


---

## Deal Timeout

Deadline durations for deal states and scheduler settings


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.deal.timeout.offer-pending` | `Duration` | Timeout for OFFER_PENDING status |  | No |  |  |
| `app.deal.timeout.negotiating` | `Duration` | Timeout for NEGOTIATING status |  | No |  |  |
| `app.deal.timeout.awaiting-payment` | `Duration` | Timeout for AWAITING_PAYMENT status |  | No |  |  |
| `app.deal.timeout.funded` | `Duration` | Timeout for FUNDED status |  | No |  |  |
| `app.deal.timeout.creative-approved` | `Duration` | Timeout for CREATIVE_APPROVED status |  | No |  |  |
| `app.deal.timeout.scheduled` | `Duration` | Timeout for SCHEDULED status |  | No |  |  |
| `app.deal.timeout.grace-period` | `Duration` | Grace period before processing expired deals |  | No |  |  |
| `app.deal.timeout.batch-size` | `int` | Maximum deals to process per poll cycle |  | No |  |  |
| `app.deal.timeout.lock-ttl` | `Duration` | Distributed lock TTL for scheduler execution |  | No |  |  |
