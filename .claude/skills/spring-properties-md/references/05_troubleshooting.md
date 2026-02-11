# Troubleshooting (common errors)

## "No enriched metadata found, skipping documentation generation."

Причина: в `classesDir` нет `META-INF/spring-properties-md/enriched-metadata.json`.

Проверки:
- dependency `spring-properties-md-processor` реально подключен как annotation processor (Maven `annotationProcessorPaths`, Gradle `annotationProcessor` или `kapt`)
- есть хотя бы один класс с `@ConfigurationProperties`, который **компилируется** в этом module
- file exists:
  - Maven: `target/classes/META-INF/spring-properties-md/enriched-metadata.json`
  - Gradle (Java): `build/classes/java/main/META-INF/spring-properties-md/enriched-metadata.json`

## Maven: plugin/dependency не резолвится

Симптомы:
- `Could not find artifact ...` для `spring-properties-md-*`
- `No plugin found for prefix ...`

Проверки:
- `jitpack.io` добавлен и в `<repositories>`, и в `<pluginRepositories>`
- версия существует в JitPack (tag/commit/branch-SNAPSHOT)

## Processor error during compilation

Симптом:
- compilation error, начинающаяся с `Failed to write enriched metadata: ...`

Причина: processor не смог записать `META-INF/spring-properties-md/enriched-metadata.json` в `CLASS_OUTPUT`.

Что проверить:
- что annotation processing включен (и processor реально подключен)
- что build directory доступен для записи
- что не запускается "другой" processor, который ломает compilation round (редко, но бывает при конфликте processors)

## Maven plugin execution fails

Симптом:
- `Failed to generate documentation` (MojoExecutionException)

Чаще всего это I/O:
- некорректный `outputFile`
- нет прав на директорию
- `outputFile` без parent директории (см. раздел про NPE ниже)

## Gradle: Kotlin module (KAPT) + wrong classesDir

Если metadata генерится KAPT'ом, файл часто окажется в `build/classes/kotlin/main/...`.
Тогда нужно:

- нацелить task на правильную папку классов
- и (при необходимости) добавить зависимость на `kaptKotlin`/`compileKotlin`, а не только `compileJava`

Пример (Groovy):

```groovy
tasks.named("generatePropertyDocs").configure {
    dependsOn(tasks.named("kaptKotlin"))
    classesDir.set(layout.buildDirectory.dir("classes/kotlin/main"))
}
```

Пример (Kotlin DSL):

```kotlin
tasks.named<io.github.springpropertiesmd.gradle.GenerateDocsTask>("generatePropertyDocs") {
    dependsOn(tasks.named("kaptKotlin"))
    classesDir.set(layout.buildDirectory.dir("classes/kotlin/main"))
}
```

## outputFile без директории (possible NPE)

Если задать `outputFile` без директории (пример: `outputFile = "configuration.md"`),
генератор может упасть на попытке `createDirectories(outputPath.getParent())`.

Workaround: всегда задавать путь с директорией (пример: `docs/configuration.md`).

## Debug commands

- Maven: `mvn -X clean compile`
- Gradle: `./gradlew generatePropertyDocs --info`