# Parent Instructions
- Parent: `/Users/nikitakocnev/.codex/AGENTS.md`
- This file contains only module-local deltas.

# Platform BOM — Agent Instructions

Bill of Materials: centralized dependency version management using Gradle Java Platform plugin.

## Rules

- NEVER add implementation code — BOM only
- Versions defined as Gradle properties, synchronized with root `build.gradle`
- All version changes must be tested against full project build
