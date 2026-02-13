# advert-market-shared

## Table of Contents

- [Canary Deployment](#canary-deployment)
- [Deployment Instance](#deployment-instance)
- [Outbox Poller](#outbox-poller)


---

## Canary Deployment

Feature-flag canary routing configuration


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.canary.admin-token` | `String` | Static bearer token for canary admin API |  | Yes |  |  |

## Deployment Instance

Blue-green deployment instance identification


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.deploy.instance-id` | `String` | Unique instance identifier |  | No |  |  |
| `app.deploy.color` | `String` | Blue-green deployment color |  | No |  | `blue` (Blue instance), `green` (Green instance) |

## Outbox Poller

Transactional outbox polling configuration


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.outbox.poll-interval` | `Duration` | Polling interval between outbox scans |  | No |  |  |
| `app.outbox.batch-size` | `int` | Maximum number of entries per poll batch |  | No |  |  |
| `app.outbox.max-retries` | `int` | Maximum number of retry attempts before marking as failed |  | No |  |  |
| `app.outbox.initial-backoff` | `Duration` | Initial backoff duration before first retry |  | No |  |  |
| `app.outbox.publish-timeout` | `Duration` | Timeout for publishing a single outbox entry to Kafka |  | No |  |  |
