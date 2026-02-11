--liquibase formatted sql

--changeset advert-market:005-comments

-- === Tables ===

COMMENT ON TABLE users IS 'Telegram Mini App users';
COMMENT ON TABLE channels IS 'Telegram channels registered on the marketplace';
COMMENT ON TABLE channel_memberships IS 'Channel team members (owner, managers)';
COMMENT ON TABLE channel_pricing_rules IS 'Channel ad placement pricing rules';
COMMENT ON TABLE commission_tiers IS 'Platform commission tiers (basis points)';
COMMENT ON TABLE deals IS 'Ad deals between advertiser and channel owner';
COMMENT ON TABLE deal_events IS 'Deal event journal (event sourcing, immutable, partitioned)';
COMMENT ON TABLE ledger_entries IS 'Double-entry bookkeeping ledger (immutable, partitioned)';
COMMENT ON TABLE account_balances IS 'Account balances — CQRS read model from ledger_entries';
COMMENT ON TABLE ton_transactions IS 'TON blockchain transactions (deposits, payouts)';
COMMENT ON TABLE disputes IS 'Deal disputes (one dispute per deal)';
COMMENT ON TABLE dispute_evidence IS 'Dispute evidence attachments (immutable)';
COMMENT ON TABLE ledger_idempotency_keys IS 'Global ledger idempotency guard — prevents cross-partition duplicates in ledger_entries';
COMMENT ON TABLE notification_outbox IS 'Transactional outbox for Kafka event publishing';
COMMENT ON TABLE posting_checks IS 'Ad post placement verification checks (partitioned, immutable)';
COMMENT ON TABLE pii_store IS 'Encrypted personally identifiable information (PII)';
COMMENT ON TABLE audit_log IS 'Action audit log (immutable)';

-- === Key columns ===

-- Financial amounts in nanoTON
COMMENT ON COLUMN channels.price_per_post_nano IS 'Denormalized minimum ad price (nanoTON). Synced from channel_pricing_rules';
COMMENT ON COLUMN deals.amount_nano IS 'Deal amount in nanoTON (1 TON = 10^9 nanoTON)';
COMMENT ON COLUMN deals.commission_rate_bp IS 'Platform commission in basis points (1000 bp = 10%)';
COMMENT ON COLUMN deals.commission_nano IS 'Platform commission amount in nanoTON';
COMMENT ON COLUMN deals.cancellation_reason IS 'Reason for cancellation/rejection. Set on transition to CANCELLED';
COMMENT ON COLUMN deals.deposit_address IS 'TON sub-wallet address for receiving payment';
COMMENT ON COLUMN deals.subwallet_id IS 'TON sub-wallet ID for deal fund isolation';
COMMENT ON COLUMN deals.content_hash IS 'SHA-256 hash of published content for verification';

-- Double-entry bookkeeping
COMMENT ON COLUMN ledger_entries.debit_nano IS 'Debit in nanoTON. Exactly one of debit/credit must be > 0';
COMMENT ON COLUMN ledger_entries.credit_nano IS 'Credit in nanoTON. Exactly one of debit/credit must be > 0';
COMMENT ON COLUMN ledger_entries.idempotency_key IS 'Idempotency key to prevent duplicate entries';
COMMENT ON COLUMN ledger_entries.account_id IS 'Account identifier (format: type:id, e.g. escrow:deal-uuid)';

-- Versioning
COMMENT ON COLUMN deals.version IS 'Optimistic locking version (incremented by application)';

-- CQRS
COMMENT ON COLUMN account_balances.last_entry_id IS 'Last processed ledger entry ID (high watermark)';

-- Commission
COMMENT ON COLUMN commission_tiers.rate_bp IS 'Commission rate in basis points (0-5000, i.e. 0-50%)';
COMMENT ON COLUMN commission_tiers.min_amount_nano IS 'Minimum deal amount for this tier (nanoTON)';

-- PII
COMMENT ON COLUMN pii_store.encrypted_value IS 'Encrypted value (AES-256-GCM)';
COMMENT ON COLUMN pii_store.key_version IS 'Encryption key version for key rotation';

-- === Functions ===

-- Outbox columns
COMMENT ON COLUMN notification_outbox.idempotency_key IS 'Deduplication key for Kafka publishing';
COMMENT ON COLUMN notification_outbox.topic IS 'Target Kafka topic';
COMMENT ON COLUMN notification_outbox.partition_key IS 'Kafka partition key (usually deal_id)';

-- Dispute evidence
COMMENT ON COLUMN dispute_evidence.content_hash IS 'SHA-256 hash of evidence content for tamper detection';

-- === Functions ===

COMMENT ON FUNCTION prevent_update_delete() IS 'Trigger function: prevents UPDATE/DELETE on immutable tables';
COMMENT ON FUNCTION update_updated_at() IS 'Trigger function: auto-updates updated_at on row modification';
