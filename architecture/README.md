# Architecture Diagrams

LikeC4 architecture diagrams for the advert-market platform.

## Structure

```
banking/
  model.c4           — Component definitions and relationships
  specs.c4           — Element specifications and metadata
  views.c4           — Static and dynamic architecture views
  deployment.c4      — Deployment diagram
  likec4.config.json — LikeC4 configuration
```

## Diagrams

- **System context** — high-level view: Telegram Mini App, TON blockchain, backend services
- **Container** — bounded contexts: Identity, Marketplace, Communication, Financial, Deal, Delivery
- **Deployment** — Docker services: PostgreSQL, Redis, Kafka, Spring Boot app

## Usage

```bash
npx likec4 start    # Interactive diagram viewer (http://localhost:3000)
npx likec4 export   # Export static images
```

## References

- [Architecture Docs](../.memory-bank/04-architecture/)
- [Module Architecture](../.memory-bank/15-module-architecture.md)
