# Parent Instructions
- Parent: `/Users/nikitakocnev/.codex/AGENTS.md`
- This file contains only module-local deltas.

# Identity API — Agent Instructions

Pure API module: DTOs, port interfaces, no implementation.

## Rules
- DTOs are Java records with Jakarta Validation annotations
- Port interfaces use @NonNull/@Nullable on all parameters and return types
- No Spring/infrastructure dependencies (compileOnly only)
- Error codes use @Fenum(FenumGroup.ERROR_CODE) from ErrorCodes
