# Gradle (Groovy DSL) install + generation

If artifacts do not resolve, first verify coordinates and version against the repo `README.md` for the exact tag/commit you are using, and the JitPack build page for that version.

## 0) Add JitPack repository

```groovy
repositories {
    maven { url "https://jitpack.io" }
    mavenCentral()
}
```

## 1) Add dependencies (annotations + processor)

```groovy
dependencies {
    implementation "com.github.lonmstalker.spring-properties-md:spring-properties-md-api:v0.1.0"
    annotationProcessor "com.github.lonmstalker.spring-properties-md:spring-properties-md-processor:v0.1.0"
}
```

Если проект на Kotlin и `@ConfigurationProperties` классы компилируются Kotlin'ом:
- используй `kapt` вместо `annotationProcessor`

## 2) Apply Gradle plugin (from JitPack)

```groovy
buildscript {
    repositories {
        maven { url "https://jitpack.io" }
        mavenCentral()
    }
    dependencies {
        classpath "com.github.lonmstalker.spring-properties-md:spring-properties-md-gradle-plugin:v0.1.0"
    }
}

apply plugin: "io.github.spring-properties-md"

springPropertiesMd {
    title = "My App Configuration"
    outputFile = "docs/configuration.md"
}
```

## 3) Generate

```bash
./gradlew generatePropertyDocs
```

Default output (if not set): `build/configuration-properties.md`.

## Verify metadata exists

- expected file (Java): `build/classes/java/main/META-INF/spring-properties-md/enriched-metadata.json`

If you use Kotlin, the metadata may end up under `build/classes/kotlin/main` instead.
In that case, configure the task `classesDir` (see `references/05_troubleshooting.md`).