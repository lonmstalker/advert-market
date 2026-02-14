import { describe, expect, it } from 'vitest';
import { TRANSACTION_TYPES, type TransactionType } from '../../types/wallet';
import { formatAmountWithSign, getAmountColor, getTransactionTypeConfig } from '../transaction-type';

describe('getTransactionTypeConfig', () => {
  it.each(TRANSACTION_TYPES)('returns config with Icon component and i18nKey for type "%s"', (type) => {
    const config = getTransactionTypeConfig(type);
    expect(config).toBeDefined();
    expect(config.Icon).toBeTruthy();
    expect(typeof config.Icon).toBe('function');
    expect(config.i18nKey).toBeTruthy();
  });

  it('i18nKey starts with wallet.txType. for all types', () => {
    for (const type of TRANSACTION_TYPES) {
      const config = getTransactionTypeConfig(type);
      expect(config.i18nKey).toMatch(/^wallet\.txType\./);
    }
  });
});

describe('getAmountColor', () => {
  it('returns var(--color-success) for income', () => {
    const color = getAmountColor('payout', 'income');
    expect(color).toBe('var(--color-success)');
  });

  it('returns var(--color-destructive) for expense', () => {
    const color = getAmountColor('escrow_deposit', 'expense');
    expect(color).toBe('var(--color-destructive)');
  });

  it('ignores transaction type — uses only direction', () => {
    const types: TransactionType[] = ['escrow_deposit', 'payout', 'refund', 'commission'];
    for (const type of types) {
      expect(getAmountColor(type, 'income')).toBe('var(--color-success)');
      expect(getAmountColor(type, 'expense')).toBe('var(--color-destructive)');
    }
  });
});

describe('formatAmountWithSign', () => {
  it('prepends + for income', () => {
    expect(formatAmountWithSign('5 TON', 'income')).toBe('+5 TON');
  });

  it('prepends U+2212 minus for expense', () => {
    const result = formatAmountWithSign('3 TON', 'expense');
    expect(result).toBe('\u22123 TON');
  });

  it('preserves original amount string', () => {
    expect(formatAmountWithSign('12.345', 'income')).toBe('+12.345');
    expect(formatAmountWithSign('0.001', 'expense')).toBe('\u22120.001');
  });
});
