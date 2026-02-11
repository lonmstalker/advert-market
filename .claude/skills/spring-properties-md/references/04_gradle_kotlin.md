# Gradle (Kotlin DSL) install + generation

If artifacts do not resolve, first verify coordinates and version against the repo `README.md` for the exact tag/commit you are using, and the JitPack build page for that version.

## 0) Add JitPack repository

```kotlin
repositories {
    maven(url = "https://jitpack.io")
    mavenCentral()
}
```

## 1) Add dependencies (annotations + processor)

```kotlin
dependencies {
    implementation("com.github.lonmstalker.spring-properties-md:spring-properties-md-api:v0.1.0")
    annotationProcessor("com.github.lonmstalker.spring-properties-md:spring-properties-md-processor:v0.1.0")
}
```

Kotlin projects: use `kapt(...)` instead of `annotationProcessor(...)`.

## 2) Apply Gradle plugin (from JitPack)

```kotlin
buildscript {
    repositories {
        maven(url = "https://jitpack.io")
        mavenCentral()
    }
    dependencies {
        classpath("com.github.lonmstalker.spring-properties-md:spring-properties-md-gradle-plugin:v0.1.0")
    }
}

apply(plugin = "io.github.spring-properties-md")

configure<io.github.springpropertiesmd.gradle.SpringPropertiesMdExtension> {
    title.set("My App Configuration")
    outputFile.set("docs/configuration.md")
}
```

## 3) Generate

```bash
./gradlew generatePropertyDocs
```