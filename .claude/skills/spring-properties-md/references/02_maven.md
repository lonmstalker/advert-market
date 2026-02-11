# Maven install + generation

If artifacts do not resolve, first verify coordinates and version against the repo `README.md` for the exact tag/commit you are using, and the JitPack build page for that version.

## 1) Add JitPack repository (dependencies + plugins)

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<pluginRepositories>
    <pluginRepository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </pluginRepository>
</pluginRepositories>
```

## 2) Add API dependency (custom annotations)

```xml
<dependency>
    <groupId>com.github.lonmstalker.spring-properties-md</groupId>
    <artifactId>spring-properties-md-api</artifactId>
    <version>v0.1.0</version>
</dependency>
```

## 3) Enable annotation processor (enriched metadata)

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>com.github.lonmstalker.spring-properties-md</groupId>
                <artifactId>spring-properties-md-processor</artifactId>
                <version>v0.1.0</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

Проверка после `mvn clean compile`:
- ожидаемый файл: `target/classes/META-INF/spring-properties-md/enriched-metadata.json`

## 4) Add Maven plugin (Markdown generation)

```xml
<plugin>
    <groupId>com.github.lonmstalker.spring-properties-md</groupId>
    <artifactId>spring-properties-md-maven-plugin</artifactId>
    <version>v0.1.0</version>
    <executions>
        <execution>
            <goals>
                <goal>generate-docs</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

По умолчанию output: `target/configuration-properties.md`.

## Common pitfall

Если используешь `annotationProcessorPaths`, Maven считает, что список processors "полный".
Если у тебя уже есть processors, их нужно перечислить вместе (иначе они не запустятся).