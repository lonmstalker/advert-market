---
name: spring-properties-md
description: "Use spring-properties-md in another Spring Boot project via JitPack (Maven/Gradle): add annotations (spring-properties-md-api), enable annotation processor (spring-properties-md-processor) to produce META-INF/spring-properties-md/enriched-metadata.json, generate Markdown via Maven goal generate-docs or Gradle task generatePropertyDocs, and troubleshoot common failures (resolution, missing metadata, outputFile path). Triggers: spring-properties-md, jitpack, configuration properties markdown, generate-docs, generatePropertyDocs."
---

# spring-properties-md (Consumer via JitPack)

## Workflow

1) Choose a version (JitPack)
- Prefer git tags like `v0.1.0`
- If you must pin: use commit SHA
- For a moving branch: `main-SNAPSHOT` (or `master-SNAPSHOT`)
Details: `references/01_versioning_jitpack.md`

2) Install and enable annotation processing
- Maven: read `references/02_maven.md`
- Gradle:
  - Groovy DSL: read `references/03_gradle_groovy.md`
  - Kotlin DSL: read `references/04_gradle_kotlin.md`

3) Generate docs and verify outputs
- Maven default output: `target/configuration-properties.md`
- Gradle default output: `build/configuration-properties.md`
- The generators will **skip** if metadata is missing:
  - Maven: `target/classes/META-INF/spring-properties-md/enriched-metadata.json`
  - Gradle (Java): `build/classes/java/main/META-INF/spring-properties-md/enriched-metadata.json`

4) If anything fails, debug with:
- `references/05_troubleshooting.md`

## High-signal notes (current behavior)

- The annotation processor runs for types annotated with `@ConfigurationProperties` and extracts **fields only** (FIELD).
- Property names are computed as: `prefix + "." + kebab-case(fieldName)` (example: `maxRetries` -> `max-retries`).
- For the full annotation reference and best practices, read:
  - `../spring-properties-md-annotations/SKILL.md`