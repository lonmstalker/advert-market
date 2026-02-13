import { describe, expect, it } from 'vitest';
import { formatFiat } from '@/shared/lib/fiat-format';
import { NANO_PER_TON } from '@/shared/lib/ton-format';

describe('formatFiat', () => {
  it('formats 1 TON at default rate ($5.50)', () => {
    const result = formatFiat(NANO_PER_TON);
    expect(result).toBe('~$5.50');
  });

  it('formats 0 nanoTon as ~$0.00', () => {
    expect(formatFiat(0)).toBe('~$0.00');
  });

  it('formats bigint input correctly', () => {
    // 5 TON at default rate $5.50 = $27.50
    const result = formatFiat(5_000_000_000n);
    expect(result).toBe('~$27.50');
  });

  it('uses custom exchange rate', () => {
    // 1 TON at $10.00 rate
    const result = formatFiat(NANO_PER_TON, 10);
    expect(result).toBe('~$10.00');
  });

  it('formats fractional TON values', () => {
    // 0.5 TON at default rate $5.50 = $2.75
    const result = formatFiat(500_000_000);
    expect(result).toBe('~$2.75');
  });

  it('formats large nanoTon values', () => {
    // 100 TON at default rate $5.50 = $550.00
    const result = formatFiat(100_000_000_000n);
    expect(result).toBe('~$550.00');
  });
});
