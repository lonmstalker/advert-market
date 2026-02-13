---
name: codex
description: >
  Codex CLI — a peer developer powered by gpt-5.3-codex. Use PROACTIVELY:
  after plan approval for implementation, as a second opinion during code review,
  for bulk refactoring (>3 files), test generation, convention/security audits.
  Reactive triggers: "codex exec", "codex resume", "run codex", "ask codex".
---

# Codex Skill Guide

## 1. Model Tiers

Two models, three usage modes. Choose by **uncertainty + cost of error**, not task size.

| Mode | Model | Reasoning | When |
|------|-------|-----------|------|
| Flagship (max) | `gpt-5.3-codex` | `xhigh` | Architecture, root-cause analysis, security, competing hypotheses, missing context |
| Flagship (standard) | `gpt-5.3-codex` | `high` | Cross-module changes, business logic, migrations, feature implementation, code review |
| Spark | `gpt-5.3-codex-spark` | `medium`/`low` | Boilerplate, uniform transformations, simple refactoring (rename/extract), test stubs |

**Reasoning effort heuristic:**
- `xhigh` — root-cause search, tangled behavior, insufficient context, architectural tradeoffs
- `high` — clear task but many conditions/modules, or high cost of error (security/data loss)
- `medium` — task with clear DoD, known files, and validation commands
- `low` — mechanical edits, formatting, single-file local fix

Default: `gpt-5.3-codex` + `xhigh`. Lower autonomously when context permits — do not ask the user each time.

## 2. Proactive Delegation

Claude MUST **propose** (never silently run) Codex in these situations:

| Trigger | Mode | Sandbox |
|---------|------|---------|
| Plan approved → implementation | Flagship xhigh | workspace-write |
| Code review / second opinion | Flagship high | read-only |
| Convention / security audit | Flagship high | read-only |
| Test generation for existing code | Flagship high | workspace-write |
| Uniform work (>5 files, same pattern) | Spark medium | workspace-write |
| Complex debugging / root-cause analysis | Flagship xhigh | read-only |

**Proposal format:**
> I suggest delegating to Codex (`<model>`, `<reasoning>`, `<sandbox>`):
> - Goal: `<what will be done>`
> - Scope: `<files/modules affected>`
> - Verification: `<how we'll validate>`
> Proceed?

**Rules:**
- Initiative comes from Claude, but execution ONLY after user confirmation
- Exception: user explicitly asked ("run codex", "ask codex") — execute immediately
- For write operations: list affected files/directories
- Do not propose Codex for trivial tasks (1-3 lines of code, obvious fix)

## 3. Spark Supervisor Protocol

Spark NEVER works autonomously. Chain: Claude formulates → Spark executes → Claude verifies.

### Prompt Template for Spark

```
## Mode
Implement | Tests | Refactor

## Task
[One concrete action, no ambiguity]

## Definition of Done
- [Verifiable criterion 1]
- [Verifiable criterion 2]

## Files (Allowed to edit)
- path/to/file1
- path/to/file2

## Steps
1. [Step 1]
2. [Step 2]

## Constraints
- MUST keep diff minimal; no unrelated refactors.
- MUST NOT change public APIs unless explicitly listed.
- MUST NOT add new dependencies.

## Example
Before: [code before]
After: [code after]

## Validation
Run: `<command>`
Expect: [what should pass]

## Forbidden
- [what not to touch]
- [which files/patterns are off-limits]

## If Blocked
Ask up to 3 questions, then stop.
```

### Spark Scope Policy

**Spark IS suitable for:**
- New files: DTOs, mappers, CRUD skeletons, test stubs
- Uniform transformations: adding annotations, renaming symbols across files
- Simple refactoring: rename, extract method, extract constant
- Config migrations: properties → records, annotation updates

**Spark IS NOT suitable for:**
- Architecture, complex business logic, security/auth, cryptography
- Database migrations and schema changes
- Financial/payment logic
- Build system, CI/CD, deployment configs
- Multi-file coordinated changes requiring semantic understanding
- Debugging, root-cause analysis

**Sandbox for Spark:**
- `workspace-write` — only for new file creation and purely mechanical edits
- `read-only` — if the task touches existing logic in any way

### After Spark Execution

Claude MUST run verification gates (section 5), then:
- Fix trivially if needed
- Resume Spark with a refined prompt (max 1 retry)
- Escalate to Flagship if Spark fails

## 4. Flagship Collaboration Protocol

Flagship works with greater autonomy but within defined boundaries.

### Prompt Template for Flagship

```
## Objective
[What must be achieved]

## Context
[2-8 lines: relevant facts, domain constraints, symptoms/errors, code references]

## Acceptance Criteria
- [Criterion 1]
- [Criterion 2]

## Non-goals
- [What NOT to do]

## Constraints
- [Boundaries: APIs, deps, migrations, backward compatibility]
- [Quality gates: TDD, linting, test requirements]

## Scope / Files
- Known relevant: [...]
- If more needed: search, then list before editing.

## Validation Plan
- Commands: [...]
- Expected results: [...]
```

**Flagship MAY:** search files independently, propose approaches, expand scope within constraints.
**Flagship MAY NOT:** modify build/CI/deps without explicit instruction, ignore non-goals.

## 5. Verification Gates

Mandatory after EVERY Codex execution (both Spark and Flagship).

### Checklist

1. **Scope check** — `git diff --stat`: only expected files changed
2. **Red flags** — no changes to build/CI/deps/secrets/migrations unless part of the task
3. **Tests** — run the project's test suite for affected modules
4. **Test integrity** — no disabled tests, no removed assertions, no excessive mocking
5. **Conventions** — diff follows project rules (CLAUDE.md / AGENTS.md / linter config)

If verification fails — do NOT accept the result. Fix or redo.

## 6. Running Tasks & Error Handling

### Execution

1. Choose model and reasoning per the matrix in section 1 (do not ask the user if context is clear)
2. Choose sandbox per task: `read-only` by default, `workspace-write` for edits, add `--full-auto` for write operations
3. Always use `--skip-git-repo-check`
4. Suppress thinking tokens with `2>/dev/null`. On errors (non-zero exit), rerun WITHOUT suppression for diagnostics

### Commands

**New task:**
```bash
echo '<prompt>' | codex exec --skip-git-repo-check \
  -m <model> \
  --config model_reasoning_effort="<effort>" \
  --sandbox <sandbox> \
  --full-auto \
  -C <dir> 2>/dev/null
```

**Multiline prompt (heredoc):**
```bash
codex exec --skip-git-repo-check \
  -m gpt-5.3-codex \
  --config model_reasoning_effort="xhigh" \
  --sandbox read-only \
  --full-auto \
  -C /path/to/project 2>/dev/null <<'PROMPT'
<multiline prompt here>
PROMPT
```

**Resume:**
```bash
echo '<prompt>' | codex exec --skip-git-repo-check resume --last 2>/dev/null
```
On resume, do NOT pass model/reasoning/sandbox — they inherit from the original session. Exception: user explicitly specified different params — insert flags between `exec` and `resume`.

### Error Handling

- **Non-zero exit** — stop, rerun without `2>/dev/null`, show stderr to user
- **Partial results / warnings** — summarize and propose resume with refinements
- **Spark failed after 1 retry** — escalate to Flagship or manual fix
- **`--full-auto` / `danger-full-access`** — request permission via AskUserQuestion

### After Completion

Inform the user: "You can resume this session with 'codex resume' or ask for additional analysis."

### CLI

Requires Codex CLI v0.101.0+. Check: `codex --version`.