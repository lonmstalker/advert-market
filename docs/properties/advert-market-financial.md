# advert-market-financial

## Table of Contents

- [Commission Sweep](#commission-sweep)
- [Ledger](#ledger)
- [TON Blockchain](#ton-blockchain)
- [TON Resilience](#ton-resilience)
- [Wallet](#wallet)


---

## Commission Sweep

Scheduled sweep of accumulated commissions to treasury


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.financial.commission-sweep.cron` | `String` | Cron expression for sweep schedule (default: daily 02:00 UTC) |  | No |  |  |
| `app.financial.commission-sweep.dust-threshold-nano` | `long` | Minimum balance in nanoTON to trigger sweep (dust threshold) |  | No |  |  |
| `app.financial.commission-sweep.batch-size` | `int` | Max accounts per sweep batch |  | No |  |  |
| `app.financial.commission-sweep.lock-ttl` | `Duration` | Distributed lock TTL for sweep |  | No |  |  |

## Ledger

Double-entry bookkeeping configuration


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.ledger.cache-ttl` | `Duration` | Balance cache TTL in Redis |  | No |  |  |
| `app.ledger.default-page-size` | `int` | Default page size for entry queries |  | No |  |  |

## TON Blockchain

TON blockchain integration settings


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.ton.api.key` | `NonNull String` | TON Center API key |  | Yes |  |  |
| `app.ton.api.is-testnet` | `boolean` | Use testnet API endpoint |  | No |  |  |
| `app.ton.wallet.mnemonic` | `NonNull String` | BIP39 mnemonic for master wallet |  | Yes |  |  |
| `app.ton.wallet.allocation-size` | `Positive int` | Subwallet sequence prefetch batch size |  | No |  |  |
| `app.ton.deposit.poll-interval` | `Duration` | Interval between deposit polls |  | No |  |  |
| `app.ton.deposit.max-poll-duration` | `Duration` | Maximum polling duration for a deposit |  | No |  |  |
| `app.ton.deposit.batch-size` | `Positive int` | Number of pending deposits per poll batch |  | No |  |  |
| `app.ton.deposit.max-retries` | `Positive int` | Max retries before marking deposit as permanently failed |  | No |  |  |
| `app.ton.network` | `NonNull String` | Blockchain network: testnet or mainnet |  | Yes |  |  |
| `app.ton.confirmation.tiers` | `Tier>` | Ordered list of confirmation tiers |  | No |  |  |
| `app.ton.confirmation.t-i-e-r_-t-h-r-e-s-h-o-l-d_100_-t-o-n_-n-a-n-o` | `long` |  | `100000000000` | No |  |  |
| `app.ton.confirmation.t-i-e-r_-t-h-r-e-s-h-o-l-d_1000_-t-o-n_-n-a-n-o` | `long` |  | `1000000000000` | No |  |  |
| `app.ton.confirmation.d-e-f-a-u-l-t_-c-o-n-f-i-r-m-a-t-i-o-n-s_-t-i-e-r_1` | `int` |  | `1` | No |  |  |
| `app.ton.confirmation.d-e-f-a-u-l-t_-c-o-n-f-i-r-m-a-t-i-o-n-s_-t-i-e-r_2` | `int` |  | `3` | No |  |  |
| `app.ton.confirmation.d-e-f-a-u-l-t_-c-o-n-f-i-r-m-a-t-i-o-n-s_-t-i-e-r_3` | `int` |  | `5` | No |  |  |

## TON Resilience

Circuit breaker and bulkhead settings for TON Center API


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.ton.resilience.circuit-breaker.sliding-window-size` | `Positive int` | Count-based sliding window size |  | No |  |  |
| `app.ton.resilience.circuit-breaker.failure-rate-threshold` | `Positive int` | Failure rate percentage to open circuit |  | No |  |  |
| `app.ton.resilience.circuit-breaker.slow-call-duration` | `Duration` | Threshold duration for slow calls |  | No |  |  |
| `app.ton.resilience.circuit-breaker.wait-in-open-state` | `Duration` | Wait duration in open state before half-open |  | No |  |  |
| `app.ton.resilience.circuit-breaker.half-open-calls` | `Positive int` | Permitted calls in half-open state |  | No |  |  |
| `app.ton.resilience.circuit-breaker.minimum-calls` | `Positive int` | Minimum calls before evaluating failure rate |  | No |  |  |
| `app.ton.resilience.bulkhead.max-concurrent-calls` | `Positive int` | Max concurrent TON Center API calls |  | No |  |  |
| `app.ton.resilience.bulkhead.max-wait-duration` | `Duration` | Max wait duration for a bulkhead permit |  | No |  |  |

## Wallet

User wallet configuration


| Property | Type | Description | Default | Required | Constraints | Examples |
|----------|------|-------------|---------|----------|-------------|----------|
| `app.wallet.min-withdrawal-nano` | `long` | Minimum withdrawal amount in nanoTON |  | No |  |  |
| `app.wallet.daily-velocity-limit-nano` | `long` | Maximum cumulative auto-withdraw amount per 24h in nanoTON |  | No |  |  |
| `app.wallet.manual-approval-threshold-nano` | `long` | Single withdrawal threshold that requires manual review, nanoTON |  | No |  |  |
