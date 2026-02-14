# advert-market-app

## Table of Contents

- [CORS](#cors)
- [Internal API](#internal-api)
- [Kafka Client](#kafka-client)


---

## CORS

CORS configuration for Telegram Mini App


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.cors.allowed-origins` | `String>` | Allowed origins for CORS requests |  | Yes | NotEmpty(must not be empty) |  |

## Internal API

Security settings for internal worker callback endpoints


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.internal-api.api-key` | `NotBlank String` | Shared secret API key for authenticating worker requests |  | Yes | NotBlank(must not be blank) |  |
| `app.internal-api.allowed-networks` | `String>` | CIDR networks allowed to access internal endpoints |  | Yes | NotEmpty(must not be empty) |  |

## Kafka Client

Kafka client connectivity configuration


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `spring.kafka.bootstrap-servers` | `NotBlank String` | Comma-separated list of Kafka broker addresses (host:port) |  | Yes | NotBlank(must not be blank) |  |
