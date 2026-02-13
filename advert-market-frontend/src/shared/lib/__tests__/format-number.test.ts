import { describe, expect, it } from 'vitest';
import { formatCompactNumber, formatLocaleNumber } from '../format-number';

describe('formatCompactNumber', () => {
  it('formats millions with one decimal', () => {
    expect(formatCompactNumber(1_500_000)).toBe('1.5M');
    expect(formatCompactNumber(1_000_000)).toBe('1.0M');
    expect(formatCompactNumber(12_345_678)).toBe('12.3M');
  });

  it('formats thousands as rounded K', () => {
    expect(formatCompactNumber(1_000)).toBe('1K');
    expect(formatCompactNumber(1_500)).toBe('2K');
    expect(formatCompactNumber(999_999)).toBe('1000K');
    expect(formatCompactNumber(50_000)).toBe('50K');
  });

  it('returns raw number below 1000', () => {
    expect(formatCompactNumber(0)).toBe('0');
    expect(formatCompactNumber(1)).toBe('1');
    expect(formatCompactNumber(999)).toBe('999');
    expect(formatCompactNumber(500)).toBe('500');
  });

  it('handles edge cases', () => {
    expect(formatCompactNumber(1_000_000_000)).toBe('1000.0M');
    expect(formatCompactNumber(10_000_000)).toBe('10.0M');
  });
});

describe('formatLocaleNumber', () => {
  it('formats with ru-RU locale separators', () => {
    const result = formatLocaleNumber(1234567);
    // ru-RU uses non-breaking space as thousands separator
    expect(result).toContain('1');
    expect(result).toContain('234');
    expect(result).toContain('567');
  });

  it('returns small numbers as-is', () => {
    expect(formatLocaleNumber(0)).toBe('0');
    expect(formatLocaleNumber(42)).toBe('42');
  });
});
