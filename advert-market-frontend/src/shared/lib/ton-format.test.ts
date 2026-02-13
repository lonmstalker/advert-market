import { computeCpm, formatCpm, formatTon, formatTonCompact, NANO_PER_TON, parseTonToNano } from '@/shared/lib/ton-format';

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

    it('throws on non-numeric string', () => {
      expect(() => parseTonToNano('abc')).toThrow('Invalid TON format');
    });

    it('throws on double dot', () => {
      expect(() => parseTonToNano('1.2.3')).toThrow('Invalid TON format');
    });

    it('throws on dot only', () => {
      expect(() => parseTonToNano('.')).toThrow('Invalid TON format');
    });

    it('throws on leading dot without digit', () => {
      expect(() => parseTonToNano('.5')).toThrow('Invalid TON format');
    });

    it('throws on double negative', () => {
      expect(() => parseTonToNano('--5')).toThrow('Invalid TON format');
    });
  });

  describe('computeCpm', () => {
    it('computes CPM correctly', () => {
      // 5 TON for 45000 views -> 5/45000*1000 = 0.111...
      const cpm = computeCpm(5_000_000_000, 45000);
      expect(cpm).toBeCloseTo(0.1111, 3);
    });

    it('returns null for zero views', () => {
      expect(computeCpm(5_000_000_000, 0)).toBeNull();
    });

    it('returns null for negative views', () => {
      expect(computeCpm(5_000_000_000, -100)).toBeNull();
    });

    it('handles small price', () => {
      const cpm = computeCpm(100_000_000, 10000);
      expect(cpm).toBeCloseTo(0.01, 4);
    });

    it('handles large values', () => {
      const cpm = computeCpm(15_000_000_000, 180000);
      expect(cpm).toBeCloseTo(0.0833, 3);
    });
  });

  describe('formatCpm', () => {
    it('formats very small CPM as <0.01', () => {
      expect(formatCpm(0.005)).toBe('<0.01');
    });

    it('formats CPM less than 1 with 2 decimals', () => {
      expect(formatCpm(0.11)).toBe('0.11');
      expect(formatCpm(0.5)).toBe('0.50');
    });

    it('formats CPM between 1 and 10 with 1 decimal', () => {
      expect(formatCpm(3.14)).toBe('3.1');
      expect(formatCpm(9.99)).toBe('10.0');
    });

    it('formats CPM >= 10 as integer', () => {
      expect(formatCpm(15.7)).toBe('16');
      expect(formatCpm(100)).toBe('100');
    });

    it('handles boundary values', () => {
      expect(formatCpm(0.01)).toBe('0.01');
      expect(formatCpm(1)).toBe('1.0');
      expect(formatCpm(10)).toBe('10');
    });
  });

  describe('toBigInt (via formatTon)', () => {
    it('throws on float number input', () => {
      expect(() => formatTon(1.5)).toThrow('Expected integer nanoTON, got float');
    });
  });
});
