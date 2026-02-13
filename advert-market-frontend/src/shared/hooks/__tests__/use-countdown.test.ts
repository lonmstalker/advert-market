import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useCountdown } from '../use-countdown';

const mockT = (key: string, opts?: Record<string, unknown>) => {
  if (key === 'deals.detail.deadlineDays') return `${opts?.days}d ${opts?.hours}h`;
  if (key === 'deals.detail.deadlineHours') return `${opts?.hours}h ${opts?.minutes}m`;
  if (key === 'deals.detail.deadlineMinutes') return `${opts?.minutes}m`;
  return key;
};

describe('useCountdown', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-02-13T12:00:00Z'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('returns null when no deadline', () => {
    const { result } = renderHook(() => useCountdown(null, mockT));
    expect(result.current).toBeNull();
  });

  it('returns null when deadline passed', () => {
    const { result } = renderHook(() => useCountdown('2026-02-13T11:00:00Z', mockT));
    expect(result.current).toBeNull();
  });

  it('returns minutes format for < 1 hour', () => {
    const { result } = renderHook(() => useCountdown('2026-02-13T12:45:00Z', mockT));
    expect(result.current).toBe('45m');
  });

  it('returns hours format for < 24 hours', () => {
    const { result } = renderHook(() => useCountdown('2026-02-13T15:30:00Z', mockT));
    expect(result.current).toBe('3h 30m');
  });

  it('returns days format for >= 24 hours', () => {
    const { result } = renderHook(() => useCountdown('2026-02-15T18:00:00Z', mockT));
    expect(result.current).toBe('2d 6h');
  });

  it('updates after interval tick', () => {
    const { result } = renderHook(() => useCountdown('2026-02-13T12:02:00Z', mockT));
    expect(result.current).toBe('2m');

    act(() => {
      vi.advanceTimersByTime(60_000);
    });
    expect(result.current).toBe('1m');
  });
});
