# Agent Instructions

- Use Hindsight and find relevant information from .memory-bank when start session
- This project uses **bd** (beads) for issue tracking. Run `bd onboard` to get started.
- MUST use TDD when write code
- MUST update .memory-bank and Hindsight when have new insights, opinions, decisions
- MUST use English for new or heavily rewritten `.memory-bank` and docs; legacy bilingual content is temporarily allowed until migration is complete
- NEVER overengineering
- ALWAYS remember KISS, DRY, YAGNI, SOLID, high-cohesion, low coupling
- MUST remember `agents-md` skill when write new large module/package
- MUST use maximum 3 subagents
- MUST follow [Module Architecture](.memory-bank/15-module-architecture.md) for project structure and dependencies
- MUST follow [Java Conventions](.memory-bank/16-java-conventions.md) for all backend code
- MUST follow [TypeScript Conventions](.memory-bank/17-typescript-conventions.md) for all frontend code
- SHOULD use `codex` skill when you have feature architecture, plan, definition of done and you need implement and write tests

## Quick Reference

```bash
bd ready              # Find available work
bd show <id>          # View issue details
bd update <id> --status in_progress  # Claim work
bd close <id>         # Complete work
bd sync               # Sync with git
```

## Landing the Plane (Session Completion)

**When ending a work session**, you MUST complete ALL steps below. Work is NOT complete until `git push` succeeds.

**MANDATORY WORKFLOW:**

1. **File issues for remaining work** - Create issues for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **PUSH TO REMOTE** - This is MANDATORY:
   ```bash
   git pull --rebase
   bd sync
   git push
   git status  # MUST show "up to date with origin"
   ```
5. **Clean up** - Clear stashes, prune remote branches
6. **Verify** - All changes committed AND pushed
7. **Hand off** - Provide context for next session

**CRITICAL RULES:**
- Work is NOT complete until `git push` succeeds
- NEVER stop before pushing - that leaves work stranded locally
- NEVER say "ready to push when you are" - YOU must push
- If push fails, resolve and retry until it succeeds
