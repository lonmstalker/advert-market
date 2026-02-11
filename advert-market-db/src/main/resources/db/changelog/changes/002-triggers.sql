--liquibase formatted sql

--changeset advert-market:002-trigger-functions splitStatements:false

CREATE OR REPLACE FUNCTION prevent_update_delete()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'UPDATE and DELETE are not allowed on %', TG_TABLE_NAME;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

--changeset advert-market:002-triggers splitStatements:false

-- Immutability triggers (append-only tables)
CREATE TRIGGER trg_ledger_entries_immutable
    BEFORE UPDATE OR DELETE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION prevent_update_delete();

CREATE TRIGGER trg_deal_events_immutable
    BEFORE UPDATE OR DELETE ON deal_events
    FOR EACH ROW EXECUTE FUNCTION prevent_update_delete();

CREATE TRIGGER trg_audit_log_immutable
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION prevent_update_delete();

CREATE TRIGGER trg_dispute_evidence_immutable
    BEFORE UPDATE OR DELETE ON dispute_evidence
    FOR EACH ROW EXECUTE FUNCTION prevent_update_delete();

CREATE TRIGGER trg_posting_checks_immutable
    BEFORE UPDATE OR DELETE ON posting_checks
    FOR EACH ROW EXECUTE FUNCTION prevent_update_delete();

-- Auto updated_at triggers
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_channels_updated_at
    BEFORE UPDATE ON channels FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_deals_updated_at
    BEFORE UPDATE ON deals FOR EACH ROW EXECUTE FUNCTION update_updated_at();
