# Server Setup (Production)

This folder contains everything needed to run Advert Market on a Linux server using Docker Compose:

- `docker-compose.prod.yml` (PostgreSQL + Redis + Kafka + MinIO + app blue/green + nginx)
- `nginx/` (reverse proxy, TLS termination, SPA hosting)
- `scripts/` (blue/green switch + canary admin helper)

This document describes **server preparation**. For deployment steps, see `RUNBOOK.md`.

For a one-command deploy from a dev machine (checks -> build -> git push -> upload artifacts -> blue/green),
use `scripts/deploy-prod.sh` from the repo root. For convenience, you can keep
server connection vars in `.env.server` (developer-local, not committed).

## 1) DNS

Point these records to your server public IP:

- `teleinsight.in` → `A <server-ip>`
- `www.teleinsight.in` → `A <server-ip>`

## 2) Firewall

Open:

- `22/tcp` (SSH)
- `80/tcp` (HTTP, used for redirect and ACME challenges)
- `443/tcp` (HTTPS)

Example (UFW):

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status
```

## 3) Install Docker + Compose

Install Docker Engine and Docker Compose v2 (Ubuntu packages or Docker's official repo are both fine).

Verify:

```bash
docker --version
docker compose version
```

## 4) Create Deployment Directory

Recommended structure on server:

```text
/home/ad-marketplace/advert-market/
  Dockerfile
  advert-market-app/build/libs/advert-market-app-0.1.0-SNAPSHOT.jar
  deploy/
    .env                         # secrets (DO NOT COMMIT)
    docker-compose.prod.yml
    nginx/
      nginx.conf
      upstream-active.conf
      ssl/
        fullchain.pem
        privkey.pem
      html/                      # Mini App static build (Vite dist/)
        index.html
        assets/...
```

## 5) TLS Certificates

nginx expects:

- `deploy/nginx/ssl/fullchain.pem`
- `deploy/nginx/ssl/privkey.pem`

You can use either:

### Option A: Let's Encrypt (recommended)

Use `certbot` to issue and renew `teleinsight.in` + `www.teleinsight.in`.

Important: port `80` must be reachable from the Internet during issuance/renewal.

### Option B: Bring Your Own Certificate

Place your provided certificate chain into `fullchain.pem` and the private key into `privkey.pem`.

Security requirements:

- `privkey.pem` must be readable only by the deployment user
- never paste private keys into chat or commit them to git

## 6) Required Environment Variables

Create `deploy/.env` (never commit it). Minimum required variables:

- `DB_PASSWORD`
- `JWT_SECRET`
- `INTERNAL_API_KEY`
- `CANARY_ADMIN_TOKEN`
- `TELEGRAM_BOT_TOKEN`
- `TELEGRAM_BOT_USERNAME`
- `TELEGRAM_WEBHOOK_URL` (must be public HTTPS URL)
- `TELEGRAM_WEBHOOK_SECRET`
- `TELEGRAM_WEBAPP_URL`
- `APP_MARKETPLACE_CHANNEL_BOT_USER_ID`
- `TON_API_KEY`
- `TON_WALLET_MNEMONIC`
- `PII_ENCRYPTION_KEY`
- `CREATIVES_STORAGE_ACCESS_KEY`
- `CREATIVES_STORAGE_SECRET_KEY`

Optional:

- `APP_IMAGE` (defaults to `advertmarket:latest`)
- `TON_NETWORK` (defaults to `testnet`)
- `APP_TELEGRAM_WELCOME_CUSTOM_EMOJI_ID` (defaults to empty)
- `CREATIVES_STORAGE_ENABLED` (defaults to `true`)
- `CREATIVES_STORAGE_BUCKET` (defaults to `creative-media`)
- `CREATIVES_STORAGE_REGION` (defaults to `us-east-1`)
- `CREATIVES_STORAGE_PUBLIC_BASE_URL` (defaults to `https://teleinsight.in/creative-media`)
- `CREATIVES_STORAGE_KEY_PREFIX` (defaults to `creatives`)

## 7) Frontend (Mini App)

The Vite build output (`advert-market-frontend/dist/`) must be copied to:

- `deploy/nginx/html/`

If you don't deploy the frontend, nginx will return `403` for `/`.

## 8) Telegram Webhook

Webhook endpoint:

- `POST /api/v1/bot/webhook`

The application registers the webhook automatically on startup using:

- `TELEGRAM_WEBHOOK_URL`
- `TELEGRAM_WEBHOOK_SECRET` (Telegram will send it as `X-Telegram-Bot-Api-Secret-Token`)

## 9) Start

From `deploy/`:

```bash
docker compose -f docker-compose.prod.yml --env-file .env up -d
docker ps
```

Health checks:

- nginx: `http://<domain>/health`
- app readiness (from inside container): `curl -sf http://localhost:8080/actuator/health/readiness`
