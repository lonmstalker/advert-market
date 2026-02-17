# advert-market-identity

## Table of Contents

- [Authentication](#authentication)
- [Login Rate Limiter](#login-rate-limiter)
- [Locale Currency Mapping](#locale-currency-mapping)


---

## Authentication

JWT and Telegram initData validation settings


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.auth.jwt.secret` | `NonNull String` | HS256 signing secret (minimum 32 bytes) |  | Yes |  |  |
| `app.auth.jwt.expiration` | `Positive long` | Token lifetime in seconds |  | Yes |  |  |
| `app.auth.anti-replay-window-seconds` | `Positive int` | Maximum age of Telegram initData auth_date in seconds for anti-replay protection |  | Yes |  |  |

## Login Rate Limiter

Login rate limiter configuration


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.auth.rate-limiter.max-attempts` | `Positive int` | Maximum login attempts per window |  | Yes |  |  |
| `app.auth.rate-limiter.window-seconds` | `Positive int` | Rate limit window duration in seconds |  | Yes |  |  |

## Locale Currency Mapping

Server-driven mapping from language code to effective display currency


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.locale-currency.fallback-currency` | `NonNull String` | Currency code used when language mapping is missing |  | Yes |  |  |
| `app.locale-currency.language-map` | `String>` | Map of language code (e.g. ru, en) to ISO 4217 currency code |  | Yes |  |  |
