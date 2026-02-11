# Commission, Pricing Rules & Sweep Mechanism

## Channel Pricing Rules

Each channel can have **multiple pricing rules** for different post types.

### Table `channel_pricing_rules`

```sql
CREATE TABLE channel_pricing_rules (
    id              BIGSERIAL PRIMARY KEY,
    channel_id      BIGINT NOT NULL REFERENCES channels(id),
    name            VARCHAR(100) NOT NULL,
    description     TEXT,
    post_type       VARCHAR(50) NOT NULL,
    price_nano      BIGINT NOT NULL CHECK (price_nano > 0),
    is_active       BOOLEAN DEFAULT TRUE,
    sort_order      INTEGER DEFAULT 0,
    created_at      TIMESTAMPTZ DEFAULT now(),
    updated_at      TIMESTAMPTZ DEFAULT now()
);
```

### Post Types (post_type)

| post_type | Description | Example Price |
|-----------|-------------|---------------|
| `STANDARD` | Regular advertising post | 50 TON |
| `PINNED` | Pinned post (24h) | 150 TON |
| `STORY` | Stories / short format | 20 TON |
| `REPOST` | Repost from advertiser's channel | 30 TON |
| `NATIVE` | Native integration (by channel author) | 200 TON |
| `CUSTOM` | Custom type (defined by owner) | -- |

### Example: channel with multiple pricing rules

```json
[
  {"name": "Standard post", "post_type": "STANDARD", "price_nano": 50000000000},
  {"name": "Pinned for 24h", "post_type": "PINNED", "price_nano": 150000000000},
  {"name": "Native integration", "post_type": "NATIVE", "price_nano": 200000000000},
  {"name": "Repost", "post_type": "REPOST", "price_nano": 30000000000}
]
```

### Pricing Management API

```
GET    /api/v1/channels/{id}/pricing         -- All channel pricing rules
POST   /api/v1/channels/{id}/pricing         -- Add pricing rule
PUT    /api/v1/channels/{id}/pricing/{ruleId} -- Update pricing rule
DELETE /api/v1/channels/{id}/pricing/{ruleId} -- Delete pricing rule
```

**Authorization**: `@channelAuth.isOwner(#id)` or `hasRight(#id, 'manage_team')`

### Deal Creation Flow

Advertiser selects a specific pricing rule:
1. Display all `is_active` rules for the channel
2. Advertiser selects post type
3. `deal.pricing_rule_id` = selected rule
4. `deal.amount_nano` = `pricing_rule.price_nano`

---

## Tiered Commission Rates

Platform commission depends on the deal amount. Rates are stored in the `commission_tiers` table.

### Table `commission_tiers`

```sql
CREATE TABLE commission_tiers (
    id              SERIAL PRIMARY KEY,
    min_amount_nano BIGINT NOT NULL DEFAULT 0,
    max_amount_nano BIGINT,                    -- NULL = no upper limit
    rate_bp         INTEGER NOT NULL,          -- basis points (100 bp = 1%)
    description     VARCHAR(200),
    created_at      TIMESTAMPTZ DEFAULT now(),
    CHECK (rate_bp >= 0 AND rate_bp <= 5000),
    CHECK (max_amount_nano IS NULL OR max_amount_nano > min_amount_nano)
);
```

### Basis Points (bp)

| bp | Percentage | Example |
|----|------------|---------|
| 500 | 5% | Large deals |
| 1000 | 10% | Standard |
| 1500 | 15% | Small deals |

### Example Tiers

| Range (TON) | Rate (bp) | Percentage | Description |
|-------------|-----------|------------|-------------|
| 0 -- 50 | 1500 | 15% | Small deals |
| 50 -- 500 | 1000 | 10% | Standard |
| 500 -- 5000 | 750 | 7.5% | Large |
| 5000+ | 500 | 5% | Premium |

### Rate Determination Algorithm

```java
// Find tier by deal amount (jOOQ query)
// SELECT rate_bp FROM commission_tiers
// WHERE min_amount_nano <= :amount
//   AND (max_amount_nano IS NULL OR max_amount_nano > :amount)
// ORDER BY min_amount_nano DESC LIMIT 1
```

The rate is determined **once** at deal creation and stored in `deals.commission_rate_bp`.

---

## Commission Calculation

### Formula (basis points)

```
commission_nano = deal_amount_nano * commission_rate_bp / 10000
owner_payout_nano = deal_amount_nano - commission_nano
```

### Rounding Rules

All amounts in nanoTON (long). Integer division:

| Value | Rule | Example (1.5 TON, rate 1000bp = 10%) |
|-------|------|------|
| Commission | **Floor** (round down) | 1_500_000_001 * 1000 / 10000 = 150_000_000 |
| Owner payout | **Amount - Commission** | 1_500_000_001 - 150_000_000 = 1_350_000_001 |

**Invariant**: `commission_nano + owner_payout_nano == deal_amount_nano` (always exact)

### Integer Arithmetic

```java
long commissionNano = dealAmountNano * commissionRateBp / 10_000L;  // floor
long ownerPayoutNano = dealAmountNano - commissionNano;              // exact
```

No floating point. No `BigDecimal`. Pure `long` arithmetic.

### Edge Cases

| Amount | Rate (bp) | Commission | Owner | Note |
|--------|-----------|------------|-------|------|
| 1_000_000_000 | 1000 | 100_000_000 | 900_000_000 | Clean 10% |
| 1_000_000_000 | 750 | 75_000_000 | 925_000_000 | 7.5% |
| 1_000_000_001 | 1000 | 100_000_000 | 900_000_001 | Owner gets remainder |
| 50_000_000 | 1500 | 7_500_000 | 42_500_000 | Small deal, 15% |
| 1 | 1000 | 0 | 1 | Min: zero commission |

**Rule**: rounding always favors the channel owner (commission is rounded down).

---

## Ledger Entries for Commission

For a completed deal:

| Entry | Account | debit_nano | credit_nano | entry_type |
|-------|---------|------------|-------------|------------|
| Deposit | `EXTERNAL_TON` | amount | 0 | ESCROW_DEPOSIT |
| Deposit | `ESCROW:{deal_id}` | 0 | amount | ESCROW_DEPOSIT |
| Release | `ESCROW:{deal_id}` | amount | 0 | ESCROW_RELEASE |
| Payout | `OWNER_PENDING:{owner_id}` | 0 | owner_payout | OWNER_PAYOUT |
| Commission | `COMMISSION:{deal_id}` | 0 | commission | PLATFORM_COMMISSION |
| Sweep | `COMMISSION:{deal_id}` | commission | 0 | COMMISSION_SWEEP |
| Sweep | `PLATFORM_TREASURY` | 0 | commission | COMMISSION_SWEEP |

---

## Commission Sweep

### Purpose

Transfer funds from per-deal commission accounts to the unified `PLATFORM_TREASURY`.

### Schedule

**Daily batch** at 02:00 UTC.

### Process

1. Find all `COMMISSION:{deal_id}` with positive balance (jOOQ query)
2. For each, create a pair of ledger entries (debit + credit)
3. Update `account_balances`
4. Write to `audit_log`

### Idempotency

Key: `sweep:{date}:{account_id}` -- prevents double sweep.

---

## Admin API for Tier Management

```
GET    /api/v1/admin/commission/tiers           -- All tiers
POST   /api/v1/admin/commission/tiers           -- Add tier
PUT    /api/v1/admin/commission/tiers/{id}      -- Update tier
DELETE /api/v1/admin/commission/tiers/{id}       -- Delete tier
```

**Authorization**: `@auth.isOperator()`

Tier changes only affect **new** deals. Existing deals retain their rate from `deals.commission_rate_bp`.

---

## Configuration

```yaml
commission:
  default-rate-bp: 1000    # 10%, fallback if no tier matches
  sweep:
    cron: "0 0 2 * * *"   # Daily at 02:00 UTC
    batch-size: 1000
```

---

## Related Documents

- [Commission Model](../07-financial-system/03-commission-model.md)
- [Double-Entry Ledger](../05-patterns-and-decisions/05-double-entry-ledger.md)
- [Account Types](../07-financial-system/05-account-types.md)
- [Channel Marketplace](../03-feature-specs/01-channel-marketplace.md)
