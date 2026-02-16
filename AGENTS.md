# Agent Instructions

- Use Hindsight and find relevant information from .memory-bank when start session
- Use .asisstant/SKILL.md
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

## Language-Specific MUST/NEVER (Java + TypeScript)

### Java

#### MUST

- MUST keep control flow linear with guard clauses instead of deep nesting. Example: invalid status exits early before main transition logic.
- MUST handle exceptions only at unstable boundaries (I/O, network, external systems). Example: repository timeout is mapped once, not wrapped repeatedly in service internals.
- MUST normalize types once at the boundary and pass strongly-typed values deeper. Example: request string ID is parsed once, downstream logic operates on a typed ID.
- MUST extract complex boolean logic into named domain predicates. Example: canReleaseEscrow is evaluated once and reused.
- MUST define fallback/default decisions in one place before business branching. Example: default payout policy is resolved before choosing transition path.
- MUST separate validation, decision, and side effects into sequential steps. Example: first validate, then decide state, then publish event.

#### NEVER

- NEVER use catch-all handling in business flow. Example: broad exception catch around pure domain checks.
- NEVER repeat parsing/casting/conversion in multiple branches. Example: converting the same ID in every if-condition.
- NEVER keep branch depth greater than 2 when guard clauses can flatten logic. Example: nested state/role checks that can be split into early exits.
- NEVER duplicate null/state guards in each branch. Example: same entity exists and active check copied across paths.
- NEVER swallow errors with silent defaults that hide root causes. Example: replacing failed state validation with generic success fallback.
- NEVER mix mutation and external calls before invariants are confirmed. Example: persisting partial state before all preconditions pass.

### TypeScript

#### MUST

- MUST narrow unknown input at boundaries and use typed models afterward. Example: API payload validated once, UI consumes typed shape.
- MUST centralize defaulting and optional resolution near data ingress. Example: missing optional fields normalized in mapper, not in every component.
- MUST use exhaustive state handling for domain/status branches. Example: every deal status has explicit UI behavior.
- MUST flatten render/business branches with early-return structure. Example: loading/error/forbidden branches return early before main view.
- MUST isolate async side effects from pure view logic. Example: retry and mutation flow handled in dedicated action handler.
- MUST name non-trivial conditions with intent-revealing predicates. Example: isEditableByActor is used instead of inline compound expression.

#### NEVER

- NEVER scatter repeated primitive coercions across flow. Example: converting the same value to number/string in several branches.
- NEVER bypass type safety as a control-flow shortcut. Example: forcing assumptions instead of narrowing/validating input.
- NEVER place deeply nested branch logic inside render paths. Example: multi-level conditional trees directly in JSX decision flow.
- NEVER duplicate fallback literals/messages across components. Example: same status fallback text copied in multiple feature screens.
- NEVER silently ignore async failures. Example: empty error handling that hides API failure state.
- NEVER combine fetch, retry, state mutation, and UI reaction in one branch. Example: one handler performing all concerns without separation.

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
