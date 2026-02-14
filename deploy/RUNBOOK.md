# Deployment Runbook: Zero-Downtime, Blue-Green, Canary

## Architecture Overview

```
Mini App / Telegram ──HTTPS──▶ nginx
                              │
                              ├─ stable_backend  (active color)
                              └─ canary_backend  (inactive color; used only for Mini App API canary)

stable_backend + canary_backend resolve to app-blue/app-green via:
- nginx/upstream-active.conf (managed by switch-color.sh)

Both app containers share: PostgreSQL / Redis / Kafka
```

- **Blue-green**: Two app containers behind nginx. Only one receives traffic at a time.
- **Mini App API canary (edge)**: nginx splits `/api/**` traffic between stable/canary by `X-Canary-Key` (recommended: Telegram user.id).
- **Bot feature canary (app-level)**: feature-flag inside the app. Sticky routing by `hash(user_id) % 100`.

## Prerequisites

| Requirement | Value |
|-------------|-------|
| Docker | 27+ |
| Docker Compose | v2 |
| Env vars | See `docker-compose.prod.yml` |
| TLS certs | `deploy/nginx/ssl/fullchain.pem`, `privkey.pem` |

## Docker Compose Project Name

Use a stable Compose project name across all commands. Otherwise you may hit
container name conflicts (because `container_name:` is fixed in Compose).

Recommended:

```bash
export COMPOSE_PROJECT_NAME=advert-market
```

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `DB_PASSWORD` | Yes | PostgreSQL password |
| `JWT_SECRET` | Yes | >= 32 bytes |
| `TELEGRAM_BOT_TOKEN` | Yes | BotFather token |
| `TELEGRAM_BOT_USERNAME` | Yes | Bot username (without `@`) |
| `TELEGRAM_WEBHOOK_URL` | Yes | `https://teleinsight.in/api/v1/bot/webhook` |
| `TELEGRAM_WEBHOOK_SECRET` | Yes | Random hex for webhook verification |
| `TELEGRAM_WEBAPP_URL` | Yes | Mini App public URL (`https://teleinsight.in`) |
| `TON_API_KEY` | Yes | TON Center API key |
| `CANARY_ADMIN_TOKEN` | Yes | Bearer token for canary admin endpoint |
| `INTERNAL_API_KEY` | Yes | Shared key for internal endpoints |
| `APP_MARKETPLACE_CHANNEL_BOT_USER_ID` | Yes | Telegram bot user id (numeric) |
| `APP_IMAGE` | No | Docker image tag (default: `advertmarket:latest`) |

---

## 1. Initial Deploy

```bash
cd deploy

# Build application image
cd .. && ./gradlew :advert-market-app:bootJar && docker build -t advertmarket:latest .
cd deploy

# Start infrastructure + blue
docker compose -f docker-compose.prod.yml up -d postgres redis kafka
# Wait for health
docker compose -f docker-compose.prod.yml up -d app-blue nginx

# Deploy Mini App frontend (optional, but required for / to not return 403)
# 1) Build: (in repo root) cd advert-market-frontend && npm ci && npm run build
# 2) Copy dist/ into deploy/nginx/html/ (mounted into nginx as /usr/share/nginx/html)

# Telegram webhook is registered automatically by the application on startup.
# Manual fallback (debug only):
# curl -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/setWebhook" \
#   -H "Content-Type: application/json" \
#   -d "{\"url\":\"${TELEGRAM_WEBHOOK_URL}\",\"secret_token\":\"${TELEGRAM_WEBHOOK_SECRET}\",\"allowed_updates\":[\"message\",\"callback_query\"]}"
```

## 2. Blue-Green Deployment (New Version)

```bash
# End-to-end from dev machine (run from repo root):
# checks -> build -> git push -> upload artifacts -> blue/green
# (requires SSH access to the server and repo cloned at DEPLOY_DIR)
DEPLOY_SSH=ad-marketplace@teleinsight.in \
DEPLOY_DIR=/home/ad-marketplace/advert-market \
./scripts/deploy-prod.sh

# Automated (recommended):
./scripts/deploy-blue-green.sh advertmarket:v0.2.0

# Manual:
# 1. Check current active color
head -1 nginx/upstream-active.conf

# 2. Start inactive color with new image
APP_IMAGE=advertmarket:v0.2.0 docker compose -f docker-compose.prod.yml --profile green up -d app-green

# 3. Wait for readiness
docker exec am-app-green curl -sf http://localhost:8080/actuator/health/readiness

# 4. Switch traffic
./scripts/switch-color.sh green

# 5. Stop old color (after drain)
sleep 30
docker compose -f docker-compose.prod.yml stop app-blue
```

## 3. Rollback (Instant)

```bash
# Switch back to previous color
./scripts/switch-color.sh blue   # if current is green
./scripts/switch-color.sh green  # if current is blue
```

Rollback = one file copy + nginx reload. No container restart needed if old container is still running.

## 4. Canary

### 4.1 Mini App API Canary (Edge, nginx)

Use this when you want to roll out a **new backend image** to a **percentage of Mini App users** without switching all traffic.

**What it affects**
- Only `/api/**` requests (Mini App API).
- Telegram webhook `/api/v1/bot/webhook` stays on `stable_backend` (not affected).

**How users are bucketed**
- Sticky key: `X-Canary-Key` header (recommended: Telegram user.id).
- Fallback: client IP (if header is missing).
- Tester override:
  - Cookie `am_canary=1` forces canary
  - Cookie `am_canary=0` forces stable

**Rollout steps**

```bash
cd deploy

# 1) Start the inactive color with the NEW image (do not switch yet)
#    Figure out which color is active:
head -1 nginx/upstream-active.conf

# Example: if active=blue -> start green
APP_IMAGE=advertmarket:v0.2.0 docker compose -f docker-compose.prod.yml --profile green up -d app-green

# 2) Wait for readiness
docker exec am-app-green curl -sf http://localhost:8080/actuator/health/readiness

# 3) Start canary at 1%
./scripts/api-canary-set.sh 1

# 4) Ramp up: 5 -> 10 -> 25 -> 50 -> 100
./scripts/api-canary-set.sh 5
./scripts/api-canary-set.sh 10
```

**Rollback (instant)**

```bash
./scripts/api-canary-set.sh 0
```

**Promote (finish rollout)**
1. Set `100%` canary for `/api/**`.
2. Switch active color to the new container (`./scripts/switch-color.sh <new-color>`).
3. Set canary back to `0%` (now stable points to the new color).
4. Stop old color.

### 4.2 Bot Feature Canary (App-level)

Bot canary is a feature-flag mechanism **inside** the running app. It determines which code path a user gets based on `hash(user_id) % 100`.

### Set Canary Percent (App-level)

```bash
# Via script
CANARY_ADMIN_TOKEN=<token> ./scripts/canary-set.sh 5

# Via curl
curl -X PUT http://localhost:8080/internal/v1/canary \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"percent": 5}'
```

### Recommended Rollout Schedule

| Step | Percent | Wait | Monitor |
|------|---------|------|---------|
| 1 | 1% | 30 min | Error rate, latency |
| 2 | 5% | 1 hour | Same + business metrics |
| 3 | 10% | 2 hours | All dashboards |
| 4 | 25% | 4 hours | All dashboards |
| 5 | 50% | 8 hours | All dashboards |
| 6 | 100% | - | Full rollout |

### Canary Rollback

```bash
# Instant — set to 0%
curl -X PUT http://localhost:8080/internal/v1/canary \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"percent": 0}'
```

### Change Hash Salt (Redistribute Buckets)

```bash
curl -X PUT http://localhost:8080/internal/v1/canary \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"salt": "v3-rollout"}'
```

## 5. Health Checks

| Endpoint | Purpose | Expected |
|----------|---------|----------|
| `/actuator/health/liveness` | Is JVM alive? | `{"status":"UP"}` |
| `/actuator/health/readiness` | Can accept traffic? | `{"status":"UP"}` |
| `/actuator/health` | Overall health | `{"status":"UP"}` |
| `/actuator/prometheus` | Metrics scrape | Prometheus text format |

## 6. Graceful Shutdown

On `SIGTERM` (docker stop):
1. Readiness probe returns 503 → nginx stops sending new traffic
2. In-flight requests complete (up to 30s grace period)
3. Kafka consumers commit offsets and leave group
4. Redis connections release locks
5. DB connections returned to pool
6. JVM exits

`stop_grace_period: 45s` in Compose > `spring.lifecycle.timeout-per-shutdown-phase: 30s`.

## 7. Monitoring

### Key Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `canary.route.decision{route=stable\|canary}` | Counter | Routing decisions |
| `canary.percent.current` | Gauge | Current canary % |
| `telegram.update.duplicates` | Counter | Deduplicated update_ids |
| `telegram.webhook.latency` | Timer | Webhook ack time |
| `telegram.handler.errors` | Counter | Processing errors |
| `telegram.update.dedup.acquired` | Counter | Unique updates processed |

### Alert Thresholds

| Condition | Action |
|-----------|--------|
| `telegram.handler.errors` spike > 5/min | Rollback canary to 0% |
| `telegram.webhook.latency` p99 > 5s | Check app health, scale |
| `canary.route.decision{route=canary}` suddenly 0 | Check Redis connectivity |
| Readiness probe failing | Check DB/Redis connectivity |

## 8. Database Migrations (Expand/Contract)

When deploying schema changes with blue-green:

**Phase 1 (expand)** — deploy with old + new code running:
- ADD new columns/tables/indexes only
- DO NOT drop/rename anything old code uses

**Phase 2 (contract)** — after full cutover:
- Remove unused columns/tables
- Only when old version is completely stopped

Liquibase runs on startup automatically. Ensure migrations are additive.

## 9. Telegram Webhook Idempotency

- Every `update_id` is deduplicated via Redis `SET NX` with 24h TTL
- Key format: `tg:update:<update_id>`
- Duplicate updates return `200 OK` immediately (Telegram expects this)
- Processing happens async after fast ack

## 10. Sticky Canary Routing

- Bucket = `SHA-256(user_id + ":" + salt) mod 100`
- Same user always lands in the same bucket
- Changing `salt` redistributes all users to new buckets
- Works across multiple app instances (stateless — reads config from Redis)
