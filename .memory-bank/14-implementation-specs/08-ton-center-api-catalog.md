# TON Center API Endpoint Catalog

## Base Configuration

| Environment | URL | Auth |
|-------------|-----|------|
| Testnet | `https://testnet.toncenter.com/api/v2/` | `X-API-Key` header |
| Mainnet | `https://toncenter.com/api/v2/` | `X-API-Key` header |

### Rate Limits

| Tier | Rate | Use Case |
|------|------|----------|
| No key | 1 req/sec | Local dev only |
| Basic | 10 req/sec | MVP |
| Advanced | 25 req/sec | Scaled |

---

## Endpoint Details

### GET /getAddressBalance

Check deposit address balance.

**Request**: `GET /api/v2/getAddressBalance?address={addr}`

**Response**:
```json
{"ok": true, "result": "1000000000"}
```

Result is balance in nanoTON as string.

---

### GET /getTransactions

Detect incoming deposits. Supports pagination via `lt` + `hash`.

**Request**: `GET /api/v2/getTransactions?address={addr}&limit=10[&lt={lt}&hash={hash}]`

**Response**:
```json
{
  "ok": true,
  "result": [{
    "transaction_id": {"lt": "12345", "hash": "abc..."},
    "in_msg": {
      "source": "EQA...",
      "destination": "EQB...",
      "value": "1000000000"
    },
    "fee": "1000000",
    "utime": 1700000000
  }]
}
```

**Key fields**:
- `transaction_id.lt` + `transaction_id.hash` -- pagination cursor
- `in_msg.source` -- sender address
- `in_msg.value` -- amount in nanoTON
- `utime` -- Unix timestamp

---

### GET /getAddressInformation

Wallet state and seqno (needed before sending transactions).

**Request**: `GET /api/v2/getAddressInformation?address={addr}`

**Response**:
```json
{
  "ok": true,
  "result": {
    "state": "active",
    "balance": "5000000000",
    "last_transaction_id": {"lt": "...", "hash": "..."}
  }
}
```

---

### POST /sendBoc

Broadcast signed transaction (payout/refund).

**Request**:
```json
POST /api/v2/sendBoc
{"boc": "<base64-encoded-bag-of-cells>"}
```

**Response**:
```json
{"ok": true, "result": {"hash": "<tx_hash>"}}
```

**Errors**: `400` (invalid BOC), `429` (rate limit), `500` (node error).

---

### GET /getMasterchainInfo

Current block height -- for confirmation counting.

**Request**: `GET /api/v2/getMasterchainInfo`

**Response**:
```json
{
  "ok": true,
  "result": {
    "last": {"seqno": 39000000, "workchain": -1}
  }
}
```

---

### POST /estimateFee

Estimate transaction fee before sending.

**Request**:
```json
POST /api/v2/estimateFee
{
  "address": "EQ...",
  "body": "<base64-boc>",
  "ignore_chksig": true
}
```

**Response**:
```json
{
  "ok": true,
  "result": {
    "source_fees": {
      "in_fwd_fee": 1,
      "storage_fee": 2,
      "gas_fee": 3,
      "fwd_fee": 4
    }
  }
}
```

---

## HTTP Client Configuration

```yaml
ton:
  api:
    url: ${TON_API_URL:https://testnet.toncenter.com/api/v2/}
    key: ${TON_API_KEY:}
    timeout:
      connect: 5s
      read: 30s
    retry:
      max-attempts: 3
      initial-backoff: 1s
      multiplier: 2
```

### Error Handling Strategy

| HTTP Code | Action | Retry? |
|-----------|--------|--------|
| 200 | Success | -- |
| 400 | Log, no retry | No |
| 401 | Alert, invalid API key | No |
| 429 | Backoff using `Retry-After` header | Yes |
| 500 | Exponential backoff | Yes, max 3 |
| 502/503 | Exponential backoff | Yes, max 3 |

### Rate Limiter

Token bucket implementation on client side:
- Bucket capacity matches API tier
- Refill rate: tier limit per second
- Block on acquire if bucket empty
- Separate limiter per API key

---

## Related Documents

- [TON SDK Integration](./01-ton-sdk-integration.md)
- [Confirmation Policy](./15-confirmation-policy.md)
- [Escrow Flow](../07-financial-system/02-escrow-flow.md)