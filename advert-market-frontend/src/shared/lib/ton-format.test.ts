import { formatTon, formatTonCompact, NANO_PER_TON, parseTonToNano } from '@/shared/lib/ton-format';

describe('ton-format', () => {
  describe('NANO_PER_TON', () => {
    it('equals 1 billion', () => {
      expect(NANO_PER_TON).toBe(1_000_000_000n);
    });
  });

  describe('formatTon', () => {
    it('formats 0 nanoTon as "0 TON"', () => {
      expect(formatTon(0n)).toBe('0 TON');
    });

    it('formats 1.5 TON correctly', () => {
      expect(formatTon(1_500_000_000n)).toBe('1.5 TON');
    });

    it('formats 0.1 TON correctly', () => {
      expect(formatTon(100_000_000n)).toBe('0.1 TON');
    });

    it('formats fractional nanoTon with full precision', () => {
      expect(formatTon(1_000_000_001n)).toBe('1.000000001 TON');
    });

    it('accepts number input', () => {
      expect(formatTon(1_500_000_000)).toBe('1.5 TON');
    });

    it('accepts string input', () => {
      expect(formatTon('1500000000')).toBe('1.5 TON');
    });

    it('formats negative values', () => {
      expect(formatTon(-1_500_000_000n)).toBe('-1.5 TON');
    });

    it('formats very large bigint', () => {
      const large = 999_999_999_000_000_000n;
      expect(formatTon(large)).toBe('999999999 TON');
    });
  });

  describe('formatTonCompact', () => {
    it('formats 0 nanoTon as "0"', () => {
      expect(formatTonCompact(0n)).toBe('0');
    });

    it('formats 1.5 TON as "1.5"', () => {
      expect(formatTonCompact(1_500_000_000n)).toBe('1.5');
    });

    it('formats 0.1 TON as "0.1"', () => {
      expect(formatTonCompact(100_000_000n)).toBe('0.1');
    });

    it('formats fractional nanoTon with full precision', () => {
      expect(formatTonCompact(1_000_000_001n)).toBe('1.000000001');
    });

    it('does not include TON suffix', () => {
      expect(formatTonCompact(1_000_000_000n)).not.toContain('TON');
    });

    it('formats negative values', () => {
      expect(formatTonCompact(-500_000_000n)).toBe('-0.5');
    });
  });

  describe('parseTonToNano', () => {
    it('parses "1.5" to 1500000000n', () => {
      expect(parseTonToNano('1.5')).toBe(1_500_000_000n);
    });

    it('parses "0" to 0n', () => {
      expect(parseTonToNano('0')).toBe(0n);
    });

    it('parses "100" to 100 TON in nano', () => {
      expect(parseTonToNano('100')).toBe(100_000_000_000n);
    });

    it('parses negative values', () => {
      expect(parseTonToNano('-2.5')).toBe(-2_500_000_000n);
    });

    it('round-trips with formatTonCompact', () => {
      const values = [0n, 1n, 1_500_000_000n, 1_000_000_001n, 999_999_999_999_999_999n];
      for (const nano of values) {
        expect(parseTonToNano(formatTonCompact(nano))).toBe(nano);
      }
    });

    it('throws on empty string', () => {
      expect(() => parseTonToNano('')).toThrow('Empty input');
    });

    it('throws on whitespace-only string', () => {
      expect(() => parseTonToNano('   ')).toThrow('Empty input');
    });
  });
});
