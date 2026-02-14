import { describe, expect, it } from 'vitest';
import {
  TRANSACTION_DIRECTIONS,
  TRANSACTION_STATUSES,
  TRANSACTION_TYPES,
  transactionDetailSchema,
  transactionDirectionSchema,
  transactionSchema,
  transactionStatusSchema,
  transactionTypeSchema,
  walletSummarySchema,
} from './wallet';

function buildTransactionPayload(overrides: Record<string, unknown> = {}) {
  return {
    id: 'tx-1',
    type: 'payout',
    status: 'confirmed',
    amountNano: '5000000000',
    direction: 'income',
    dealId: null,
    channelTitle: null,
    description: 'Test transaction',
    createdAt: '2026-02-14T10:00:00Z',
    ...overrides,
  };
}

describe('walletSummarySchema', () => {
  it('parses valid summary', () => {
    const data = {
      earnedTotalNano: '15000000000',
      inEscrowNano: '5000000000',
      spentTotalNano: '0',
      activeEscrowNano: '0',
      activeDealsCount: 2,
      completedDealsCount: 5,
    };
    expect(walletSummarySchema.parse(data)).toEqual(data);
  });

  it('rejects missing required field', () => {
    const data = {
      earnedTotalNano: '0',
      inEscrowNano: '0',
      // spentTotalNano is missing
      activeEscrowNano: '0',
      activeDealsCount: 0,
      completedDealsCount: 0,
    };
    expect(() => walletSummarySchema.parse(data)).toThrow();
  });

  it('rejects numeric instead of string for nano fields', () => {
    const data = {
      earnedTotalNano: 15000000000,
      inEscrowNano: '0',
      spentTotalNano: '0',
      activeEscrowNano: '0',
      activeDealsCount: 0,
      completedDealsCount: 0,
    };
    expect(() => walletSummarySchema.parse(data)).toThrow();
  });
});

describe('transactionSchema', () => {
  it('parses valid transaction', () => {
    const payload = buildTransactionPayload();
    expect(transactionSchema.parse(payload)).toEqual(payload);
  });

  it('accepts null dealId and channelTitle', () => {
    const payload = buildTransactionPayload({ dealId: null, channelTitle: null });
    const parsed = transactionSchema.parse(payload);
    expect(parsed.dealId).toBeNull();
    expect(parsed.channelTitle).toBeNull();
  });

  it('rejects unknown type', () => {
    expect(() => transactionSchema.parse(buildTransactionPayload({ type: 'unknown_type' }))).toThrow();
  });

  it('rejects unknown status', () => {
    expect(() => transactionSchema.parse(buildTransactionPayload({ status: 'unknown_status' }))).toThrow();
  });

  it('rejects unknown direction', () => {
    expect(() => transactionSchema.parse(buildTransactionPayload({ direction: 'unknown' }))).toThrow();
  });
});

describe('transactionDetailSchema', () => {
  it('parses full detail with blockchain data', () => {
    const payload = {
      ...buildTransactionPayload(),
      txHash: 'abc123def456',
      fromAddress: 'EQBvW8Z5huBkMJYdnfAEFYpzHC2p0y3wR6Qf5eJYhS1eN0Yz',
      toAddress: 'EQAo92DYMokBh2HJiIXLfhE0qiG0mF3KxhNZ2W1ghT3xQ4Rn',
      commissionNano: '250000000',
    };
    const parsed = transactionDetailSchema.parse(payload);
    expect(parsed.txHash).toBe('abc123def456');
    expect(parsed.commissionNano).toBe('250000000');
  });

  it('parses with all nullable fields as null', () => {
    const payload = {
      ...buildTransactionPayload(),
      txHash: null,
      fromAddress: null,
      toAddress: null,
      commissionNano: null,
    };
    const parsed = transactionDetailSchema.parse(payload);
    expect(parsed.txHash).toBeNull();
    expect(parsed.fromAddress).toBeNull();
    expect(parsed.toAddress).toBeNull();
    expect(parsed.commissionNano).toBeNull();
  });
});

describe('enum schemas', () => {
  it('accepts all known values for transactionTypeSchema', () => {
    for (const val of TRANSACTION_TYPES) {
      expect(transactionTypeSchema.parse(val)).toBe(val);
    }
  });

  it('accepts all known values for transactionStatusSchema', () => {
    for (const val of TRANSACTION_STATUSES) {
      expect(transactionStatusSchema.parse(val)).toBe(val);
    }
  });

  it('accepts all known values for transactionDirectionSchema', () => {
    for (const val of TRANSACTION_DIRECTIONS) {
      expect(transactionDirectionSchema.parse(val)).toBe(val);
    }
  });
});
