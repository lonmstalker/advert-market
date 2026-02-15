# Frontend Instructions

- ALWAYS follow instructions in [AGENTS.md](AGENTS.md) for component APIs, SDK usage, and coding conventions
- ALWAYS follow [DESIGN_GUIDELINES.md](DESIGN_GUIDELINES.md) for UI design decisions, typography, colors, spacing, animations
- Design system patterns in [.interface-design/system.md](.interface-design/system.md) — update when new patterns are established during development
- Linter: **Biome** (NOT ESLint). Run `npm run lint` before committing
- All user-facing strings through i18next `t()` function
- Tests: Vitest + Testing Library (unit), Playwright (E2E). Run `npm test` before committing
- Financial values: BigInt only, format `XX.XX TON`, `font-variant-numeric: tabular-nums`
- Path alias: `@/` maps to `src/`
- `noDefaultExport` everywhere EXCEPT `src/pages/**` (lazy routes need default exports)
- Server state: TanStack Query (mandatory). Client state: Zustand (split by domain, one store per bounded context)
- NEVER use raw `fetch()`, `axios`, native HTML elements (`<button>`, `<input>`, `<select>`) — use `api.*` and UI Kit components
- SHOULD use `codex` skill when you have feature architecture, plan, definition of done and you need implement and write tests