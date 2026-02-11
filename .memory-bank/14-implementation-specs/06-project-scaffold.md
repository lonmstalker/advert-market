# Project Scaffold & Docker Compose

## build.gradle (Groovy DSL)

### Plugins

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '4.0.0'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'nu.studer.jooq' version '9.0'
    id 'org.liquibase.gradle' version '3.0.1'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
```

### Dependencies

```groovy
dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // Kafka
    implementation 'org.springframework.kafka:spring-kafka'

    // Database
    implementation 'org.liquibase:liquibase-core'
    implementation 'org.jooq:jooq'
    implementation 'org.jooq:jooq-meta'
    runtimeOnly 'org.postgresql:postgresql'

    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'

    // Telegram Bot
    implementation 'com.github.pengrad:java-telegram-bot-api:7.11.0'

    // TON Blockchain
    implementation 'io.github.neodix42:ton4j-smartcontract:0.8.0'
    implementation 'io.github.neodix42:ton4j-tonlib:0.8.0'
    implementation 'io.github.neodix42:ton4j-address:0.8.0'

    // Monitoring
    implementation 'io.micrometer:micrometer-registry-prometheus'

    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.kafka:spring-kafka-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'org.testcontainers:kafka'
    testImplementation 'com.redis:testcontainers-redis'
}
```

---

## docker-compose.yml

```yaml
services:
  postgres:
    image: postgres:18
    environment:
      POSTGRES_DB: advertmarket
      POSTGRES_USER: app
      POSTGRES_PASSWORD: dev_password
    ports:
      - "5432:5432"
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U app -d advertmarket"]
      interval: 5s
      timeout: 3s
      retries: 5

  redis:
    image: redis:8-alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5

  kafka:
    image: apache/kafka:4.1.0
    ports:
      - "9092:9092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_PROCESS_ROLES: broker,controller
      KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT
      KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_NUM_PARTITIONS: 3
      CLUSTER_ID: "MkU3OEVBNTcwNTJENDM2Qk"
    healthcheck:
      test: ["CMD-SHELL", "/opt/kafka/bin/kafka-cluster.sh cluster-id --bootstrap-server localhost:9092"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  pgdata:
```

### Topic Creation (init script)

```bash
#!/bin/bash
# scripts/create-kafka-topics.sh
KAFKA_BIN="/opt/kafka/bin"
BOOTSTRAP="localhost:9092"

topics=(
  "deal.events:6"
  "escrow.commands:3"
  "escrow.confirmations:3"
  "delivery.commands:3"
  "delivery.results:3"
  "notifications.outbox:3"
  "reconciliation.triggers:1"
  "deal.deadlines:3"
)

for topic_parts in "${topics[@]}"; do
  IFS=':' read -r topic partitions <<< "$topic_parts"
  $KAFKA_BIN/kafka-topics.sh --create \
    --bootstrap-server $BOOTSTRAP \
    --topic "$topic" \
    --partitions "$partitions" \
    --replication-factor 1 \
    --if-not-exists
done
```

---

## application.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:advertmarket}
    username: ${DB_USER:app}
    password: ${DB_PASSWORD:dev_password}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false

  jackson:
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false

# Application
auth:
  jwt:
    secret: ${JWT_SECRET:dev-secret-must-be-at-least-32-bytes-long}
    expiry: 24h
  anti-replay-window: 300

telegram:
  bot:
    token: ${TELEGRAM_BOT_TOKEN:}
    webhook:
      url: ${TELEGRAM_WEBHOOK_URL:}
      secret: ${TELEGRAM_WEBHOOK_SECRET:}

ton:
  api:
    url: ${TON_API_URL:https://testnet.toncenter.com/api/v2/}
    key: ${TON_API_KEY:}
  wallet:
    mnemonic: ${TON_WALLET_MNEMONIC:}

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  endpoint:
    health:
      show-details: when-authorized

---
spring.config.activate.on-profile: prod
ton:
  api:
    url: https://toncenter.com/api/v2/
```

---

## Project Structure

```
src/
  main/
    java/com/advertmarket/
      AdvertMarketApplication.java
      config/          # SecurityConfig, KafkaConfig, RedisConfig, JooqConfig
      auth/            # JwtService, TelegramInitDataValidator, JwtFilter
      deal/            # DealService, DealStateMachine, DealController
      channel/         # ChannelService, ChannelController
      escrow/          # EscrowService, LedgerService
      delivery/        # DeliveryService, DeliveryVerifier
      dispute/         # DisputeService, DisputeController
      notification/    # NotificationService, OutboxPoller
      ton/             # TonPaymentGateway, DepositWatcher
      reconciliation/  # ReconciliationService
      common/          # ErrorResponse, ApiException, BaseEntity
    resources/
      db/changelog/    # Liquibase changelogs
      application.yml
  test/
    java/com/advertmarket/
      ...
```

---

## Related Documents

- [DDL Migrations](./05-ddl-migrations.md)
- [Tech Stack](../08-tech-stack.md)
- [Deployment](../09-deployment.md)