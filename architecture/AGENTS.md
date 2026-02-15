# Architecture — Agent Instructions

LikeC4 architecture diagrams for the advert-market platform.

## Structure

| File | Purpose |
|------|---------|
| `banking/model.c4` | Component definitions and relationships |
| `banking/specs.c4` | Element specifications and metadata |
| `banking/views.c4` | Static and dynamic architecture views |
| `banking/deployment.c4` | Deployment diagram (server, Docker services) |
| `banking/likec4.config.json` | LikeC4 configuration |

## Conventions

- Element IDs: camelCase (e.g., `advertMarket`, `financialService`)
- Relationship verbs: descriptive present tense (e.g., "sends events to", "reads from")
- Views: one view per bounded context + system-level overview
- Keep diagrams synchronized with module structure in code

## Usage

```bash
npx likec4 start    # Interactive diagram viewer
npx likec4 export   # Export static images
```

## References

- [Architecture Docs](../.memory-bank/04-architecture/)
- [Module Architecture](../.memory-bank/15-module-architecture.md)
- [LikeC4 Documentation](https://likec4.dev/docs/)
