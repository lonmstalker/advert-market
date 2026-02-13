import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { formatChannelAge, formatTimeUntil } from '../time-utils';

describe('formatTimeUntil', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-02-13T12:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('returns null for past dates', () => {
    expect(formatTimeUntil('2026-02-13T11:00:00Z')).toBeNull();
    expect(formatTimeUntil('2025-01-01T00:00:00Z')).toBeNull();
  });

  it('formats minutes when < 1 hour', () => {
    expect(formatTimeUntil('2026-02-13T12:30:00Z')).toBe('30m');
    expect(formatTimeUntil('2026-02-13T12:01:00Z')).toBe('1m');
  });

  it('formats hours and minutes', () => {
    expect(formatTimeUntil('2026-02-13T15:30:00Z')).toBe('3h 30m');
    expect(formatTimeUntil('2026-02-13T13:00:00Z')).toBe('1h 0m');
  });

  it('formats days and hours for > 24h', () => {
    expect(formatTimeUntil('2026-02-15T12:00:00Z')).toBe('2d 0h');
    expect(formatTimeUntil('2026-02-14T18:00:00Z')).toBe('1d 6h');
  });
});

describe('formatChannelAge', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-02-13T12:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  const mockT = (key: string, opts?: Record<string, unknown>) => {
    if (key === 'catalog.channel.addedToday') return 'Added today';
    if (key === 'catalog.channel.addedDaysAgo') return `${opts?.count} days ago`;
    if (key === 'catalog.channel.addedMonthsAgo') return `${opts?.count} months ago`;
    if (key === 'catalog.channel.onPlatformSince') return `Since ${opts?.date}`;
    return key;
  };

  it('returns "Added today" for same day', () => {
    expect(formatChannelAge('2026-02-13T10:00:00Z', mockT)).toBe('Added today');
  });

  it('returns days ago for < 30 days', () => {
    expect(formatChannelAge('2026-02-01T00:00:00Z', mockT)).toBe('12 days ago');
  });

  it('returns months ago for < 12 months', () => {
    expect(formatChannelAge('2025-11-13T00:00:00Z', mockT)).toBe('3 months ago');
  });

  it('returns platform since for >= 12 months', () => {
    const result = formatChannelAge('2024-06-15T00:00:00Z', mockT);
    expect(result).toBe('Since июня 2024');
  });
});
