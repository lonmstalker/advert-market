import { describe, expect, it } from 'vitest';
import { buildOverlapLabel } from '../overlap-label';

const mockT = (key: string, opts?: Record<string, unknown>) => {
  if (key === 'catalog.channel.overlapFormat') return `${opts?.freq}/${opts?.dur}`;
  if (key === 'catalog.channel.onlyDuration') return `dur:${opts?.dur}`;
  if (key === 'catalog.channel.onlyFrequency') return `freq:${opts?.freq}`;
  return key;
};

describe('buildOverlapLabel', () => {
  it('returns formatted label with tooltip when both freq and dur present', () => {
    const result = buildOverlapLabel(4, 24, mockT);
    expect(result).toEqual({ label: '4/24', hasTooltip: true, freq: 4, dur: 24 });
  });

  it('returns duration-only label without tooltip', () => {
    const result = buildOverlapLabel(undefined, 48, mockT);
    expect(result).toEqual({ label: 'dur:48', hasTooltip: false });
  });

  it('returns frequency-only label without tooltip', () => {
    const result = buildOverlapLabel(6, undefined, mockT);
    expect(result).toEqual({ label: 'freq:6', hasTooltip: false });
  });

  it('returns null when neither is present', () => {
    expect(buildOverlapLabel(undefined, undefined, mockT)).toBeNull();
    expect(buildOverlapLabel(null, null, mockT)).toBeNull();
  });

  it('treats 0 as falsy (returns null)', () => {
    expect(buildOverlapLabel(0, 0, mockT)).toBeNull();
  });

  it('handles freq=0 with dur present', () => {
    const result = buildOverlapLabel(0, 24, mockT);
    expect(result).toEqual({ label: 'dur:24', hasTooltip: false });
  });
});
