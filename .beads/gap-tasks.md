# CRITICAL: TON SDK Integration Spec
- type: task
- priority: critical
- label: integration, ton, financial
- estimate: 120

Write a complete TON integration specification: choose SDK (ton-kotlin vs tonweb), document address generation (bounceable vs non-bounceable), transaction building patterns for deposits/payouts/refunds, TON Center API endpoints (testnet/mainnet URLs, auth, rate limits), and provide code examples for the TON Payment Gateway service.

## Acceptance Criteria
- SDK chosen with Maven/Gradle coordinates
- Address generation example code
- Transaction submit example code
- TON Center API endpoint catalog with request/response schemas
- Testnet configuration documented

---

# CRITICAL: Telegram Bot Framework & Webhook Setup
- type: task
- priority: critical
- label: integration, telegram, bot
- estimate: 90

Choose Telegram Bot framework for Kotlin/Spring Boot, document webhook setup (TLS, path, port), define notification message templates (HTML/Markdown), specify bot token management, and document error handling for failed sends.

## Acceptance Criteria
- Bot framework chosen with dependency coordinates
- Webhook registration procedure documented
- Message templates for all 15 notification types
- Retry/fallback strategy for failed sends
- Bot token env var configuration

---

# CRITICAL: Auth Flow Spec — initData HMAC + Session
- type: task
- priority: critical
- label: security, auth, telegram
- estimate: 90

Finalize authentication architecture: Telegram initData HMAC-SHA256 validation implementation, session management choice (JWT vs opaque), token structure (claims, expiry), refresh mechanism, anti-replay window, and Spring Security integration.

## Acceptance Criteria
- initData HMAC validation code example
- JWT/session decision made with rationale
- Token claims structure defined
- Session storage choice (Redis key schema)
- Spring Security filter chain configuration

---

# CRITICAL: Kafka Event Schemas for All 8 Topics
- type: task
- priority: critical
- label: kafka, schema, infrastructure
- estimate: 120

Define complete event schemas (Avro or JSON Schema) for all 8 Kafka topics: deal.events, escrow.commands, escrow.confirmations, delivery.commands, delivery.results, notifications.outbox, reconciliation.triggers, deal.deadlines. Include serialization format choice, schema evolution rules, and Schema Registry setup.

## Acceptance Criteria
- Schema for each of 8 topics with all fields, types, required/optional
- Serialization format chosen (Avro/JSON Schema/Protobuf)
- Schema evolution compatibility mode defined
- Consumer deserialization error handling strategy
- Example producer/consumer code snippets

---

# CRITICAL: Complete DDL Migration Scripts
- type: task
- priority: critical
- label: database, postgresql, schema
- estimate: 150

Create runnable DDL scripts for all 13 tables with: indexes (for common queries), CHECK constraints (amount_nano > 0, status enum), FK cascading rules, partition creation (monthly for ledger_entries, deal_events, posting_checks), immutability triggers for append-only tables. Choose migration tool (Flyway/Liquibase).

## Acceptance Criteria
- Migration tool chosen (Flyway recommended)
- V1__init.sql with all 13 tables
- All indexes defined (deal lookups, balance queries, outbox polling)
- CHECK constraints for financial invariants
- Partition creation for 3 partitioned tables
- Immutability trigger for ledger_entries, deal_events, audit_log, dispute_evidence

---

# CRITICAL: Project Scaffold & Docker Compose
- type: task
- priority: critical
- label: devops, infrastructure, setup
- estimate: 120

Create project scaffold: build.gradle.kts with all dependencies (Kotlin 2.2, Spring Boot 4.0, Spring Kafka, jOOQ, Redis, PostgreSQL driver), docker-compose.yml for local dev (PostgreSQL 18, Redis 8, Kafka 4.1 with KRaft, Schema Registry), and application.yml with environment-based configuration.

## Acceptance Criteria
- build.gradle.kts compiles with all dependencies
- docker-compose up starts all infrastructure
- application.yml with dev/test/prod profiles
- Health checks for all services
- README with local setup instructions

---

# CRITICAL: PII Encryption & Key Management Spec
- type: task
- priority: critical
- label: security, encryption, pii
- estimate: 60

Specify PII encryption implementation: key management strategy (env var for MVP, KMS for Scaled), AES-256-GCM nonce/IV handling, encryption library (Bouncy Castle vs Java built-in), key rotation procedure with key_version, and PII Vault service code patterns.

## Acceptance Criteria
- Key storage mechanism chosen per environment
- Encryption/decryption code example
- Nonce/IV generation strategy
- Key rotation procedure documented
- pii_store.key_version usage specified

---

# HIGH: TON Center API Endpoint Catalog
- type: task
- priority: high
- label: integration, ton, api
- estimate: 60

Document TON Center API integration: exact endpoint URLs (testnet v2/v3), request/response payload structures for getTransactions, sendBoc, getAddressBalance. Include API key format, rate limiting (req/sec), error responses, and timeout handling.

## Acceptance Criteria
- All used endpoints documented with URL, method, headers
- Request/response JSON examples for each
- Rate limit values and throttling strategy
- Error response codes and handling
- Testnet vs mainnet configuration

---

# HIGH: Redis Distributed Lock Implementation
- type: task
- priority: high
- label: redis, idempotency, financial
- estimate: 60

Specify Redis lock implementation: SET NX EX with fencing token, lock release Lua script, deadlock/stale lock cleanup, lock acquisition timeout, Redis client choice (Lettuce vs Jedis), and Redlock for Scaled deployment.

## Acceptance Criteria
- Lock acquire/release code with Lua scripts
- Fencing token algorithm specified
- Stale lock cleanup mechanism
- Lock timeout values per operation type
- Redis client library chosen with config

---

# HIGH: Worker Callback Payload Schemas (All 6 Types)
- type: task
- priority: high
- label: api, workers, schema
- estimate: 60

Define complete JSON schemas for all 6 worker callback event types: DEPOSIT_CONFIRMED, PAYOUT_COMPLETED, REFUND_COMPLETED, PUBLICATION_RESULT, VERIFICATION_RESULT, RECONCILIATION_START. Include retry policy, timeout, idempotency key format.

## Acceptance Criteria
- JSON schema for each of 6 event types
- Idempotency key generation algorithm per type
- Retry policy (max retries, backoff)
- Timeout values
- Error response handling for failed callbacks

---

# HIGH: Outbox Poller Implementation Spec
- type: task
- priority: high
- label: kafka, outbox, infrastructure
- estimate: 60

Specify transactional outbox poller: polling interval, batch size, FOR UPDATE SKIP LOCKED query, Kafka producer error handling, outbox record lifecycle (PENDING → PROCESSING → DELIVERED/FAILED), cleanup/archival strategy, and DLQ for permanently failed records.

## Acceptance Criteria
- Polling SQL query with SKIP LOCKED
- Batch size and interval configuration
- Kafka produce failure handling
- Record cleanup/archival strategy
- Monitoring metrics (lag, failure rate)

---

# HIGH: Error Code Catalog
- type: task
- priority: high
- label: api, error-handling
- estimate: 45

Create complete error code enum covering all domains: Auth (INVALID_INIT_DATA, SESSION_EXPIRED), Deal (INVALID_TRANSITION, DEAL_NOT_FOUND), Financial (INSUFFICIENT_BALANCE, ESCROW_ALREADY_FUNDED), Dispute (DISPUTE_ALREADY_OPEN), Team (INSUFFICIENT_RIGHTS). Map to HTTP status codes.

## Acceptance Criteria
- Error codes grouped by domain
- HTTP status code mapping for each
- RFC 7807 response body example for each
- Error messages (English, developer-friendly)
- Enum class ready for implementation

---

# HIGH: ABAC Policy Rules — Spring Security Mapping
- type: task
- priority: high
- label: security, abac, auth
- estimate: 90

Map ABAC policy rules to Spring Security implementation: custom authorization annotations or SpEL expressions, policy decision point architecture, attribute loading optimization (request-scoped cache), permission evaluator for deal/channel/operator actions.

## Acceptance Criteria
- Spring Security filter chain configuration
- Custom @PreAuthorize expressions for each action type
- PermissionEvaluator implementation pattern
- Attribute caching strategy (per-request)
- Integration test examples

---

# HIGH: Reconciliation SQL Queries
- type: task
- priority: high
- label: financial, reconciliation, database
- estimate: 60

Write production-ready SQL for all 4 reconciliation checks: ledger self-balance (SUM debits = credits), ledger vs TON transactions, ledger vs deal aggregates, CQRS projection consistency. Include tolerance handling, time range filtering, and performance optimization.

## Acceptance Criteria
- Runnable SQL for each of 4 checks
- Time-range filtering (last 24h, all-time)
- Tolerance thresholds for amount matching
- Performance analysis (EXPLAIN ANALYZE)
- Alert severity classification per check

---

# HIGH: Confirmation Policy — TON Block Height Tracking
- type: task
- priority: high
- label: financial, ton, confirmation
- estimate: 45

Specify how TON Deposit Watcher tracks block confirmations: block height query via TON Center API, confirmation counting logic, operator review workflow for >1000 TON (notification, approval endpoint, timeout SLA), and amount validation (exact match, over/underpayment).

## Acceptance Criteria
- Block height query implementation
- Confirmation counting algorithm
- Operator review notification and approval flow
- Over/underpayment handling rules
- Amount validation tolerance (if any)

---

# MEDIUM: Dispute Auto-Resolution Rules Engine
- type: task
- priority: medium
- label: dispute, business-logic
- estimate: 60

Define concrete auto-resolution rules: rule conditions (post deleted → auto-refund, content edited → auto-refund, creative timeout → auto-refund), rule priority/conflict resolution, operator escalation triggers, rules configuration (code vs database), and evidence validation requirements.

## Acceptance Criteria
- Complete rule catalog with conditions and outcomes
- Priority order for conflicting rules
- Escalation criteria defined
- Configuration approach chosen
- Evidence type requirements per rule

---

# MEDIUM: Delivery Verifier — Telegram API Details
- type: task
- priority: medium
- label: telegram, delivery, workers
- estimate: 45

Specify Telegram API calls for delivery verification: getMessages/getMessage by message_id, content hash comparison algorithm (text + media), handling of partial edits, API failure retry logic, timezone handling for 24h retention calculation.

## Acceptance Criteria
- Telegram Bot API methods for message verification
- Content hash algorithm (SHA-256 of text + media file_ids)
- Partial edit detection logic
- API failure retry with exponential backoff
- 24h calculation with timezone handling

---

# MEDIUM: Kafka Consumer Error Handling Strategy
- type: task
- priority: medium
- label: kafka, error-handling, workers
- estimate: 45

Define Kafka consumer error handling: dead letter topic naming and schema, poison message detection and isolation, retry with exponential backoff, consumer lag alerting thresholds, offset commit strategy (manual after processing), and rebalancing strategy.

## Acceptance Criteria
- DLT naming convention and schema
- Poison message handling procedure
- Retry policy per consumer group
- Consumer lag alert thresholds
- Offset commit strategy documented

---

# MEDIUM: Deal Timeout Scheduler Implementation
- type: task
- priority: medium
- label: workers, scheduling, deals
- estimate: 45

Specify Deal Timeout Worker implementation: scheduler technology (Spring Scheduler with Kafka delay vs dedicated cron), deadline storage and querying, timeout configuration per state, grace period handling, timezone considerations.

## Acceptance Criteria
- Scheduler technology chosen
- Deadline storage in deals.deadline_at
- deal.deadlines Kafka topic message format
- Timeout values per state (configurable)
- Grace period rules

---

# MEDIUM: Creative Brief & Draft JSONB Schemas
- type: task
- priority: medium
- label: schema, creative, deals
- estimate: 30

Define JSON schemas for deals.creative_brief and deals.creative_draft JSONB columns: required/optional fields, content type restrictions, media format specs, size limits, versioning strategy for draft revisions.

## Acceptance Criteria
- JSON Schema for creative_brief (all fields typed)
- JSON Schema for creative_draft (all fields typed)
- Validation rules (max text length, media count)
- Version tracking approach for draft revisions
- Migration path for schema evolution

---

# LOW: Metrics, SLOs & Monitoring Definitions
- type: task
- priority: low
- label: monitoring, observability, devops
- estimate: 60

Define monitoring strategy: Micrometer metric names and tags, SLI/SLO definitions (API latency, deal completion rate, escrow funding time), Prometheus alert rules with thresholds, Grafana dashboard specs, structured logging format with correlation IDs.

## Acceptance Criteria
- Metric catalog (name, type, tags)
- SLO targets for key flows
- Alert rules with severity levels
- Dashboard layout sketches
- Log format specification (JSON fields)

---

# LOW: Notification Templates & i18n
- type: task
- priority: low
- label: notifications, i18n, telegram
- estimate: 30

Define notification message templates for all 15 event types: template format (Mustache/Thymeleaf), variable placeholders, HTML/Markdown formatting, multi-language support (/language command), and message payload JSONB schema for notification_outbox.

## Acceptance Criteria
- Templates for all 15 notification types
- Template engine chosen
- Multi-language support architecture
- notification_outbox.payload JSONB schema
- Example rendered messages

---

# LOW: Deployment Runbook & Troubleshooting Guide
- type: task
- priority: low
- label: devops, deployment, operations
- estimate: 45

Create MVP deployment runbook: step-by-step deployment procedure, health check endpoints, rollback procedure, common failure scenarios and remediation, incident response playbook for financial operations.

## Acceptance Criteria
- Step-by-step MVP deployment guide
- Health check endpoint list
- Rollback procedure for each component
- Top 10 failure scenarios with remediation
- Financial incident escalation procedure

---

# LOW: PostgreSQL Sharding & Routing (Scaled)
- type: task
- priority: low
- label: database, scaling, postgresql
- estimate: 60

Design ShardedDslContextProvider: shard key routing algorithm (deal_id hash for financial, user_id for core), cross-shard query handling, data migration procedure from single DB to 3 shards, consistent hashing implementation.

## Acceptance Criteria
- Routing algorithm with code example
- Cross-shard query strategy (scatter-gather or avoid)
- Migration procedure from MVP → Scaled
- Shard rebalancing strategy
- jOOQ DSL context routing integration

---

# LOW: Commission Rounding & Sweep Mechanism
- type: task
- priority: low
- label: financial, commission
- estimate: 30

Specify commission calculation edge cases: integer arithmetic rounding direction (floor for commission, ceil for owner payout), commission sweep schedule (daily batch), COMMISSION:{deal_id} → PLATFORM_TREASURY sweep SQL, and future segment configuration storage.

## Acceptance Criteria
- Rounding rules documented with examples
- Sweep schedule and trigger mechanism
- Sweep ledger entry pattern
- Segment configuration table design (future)
- Edge case: deal amount not evenly divisible
