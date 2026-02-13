import { describe, expect, it } from 'vitest';
import { engagementRateColor } from '../engagement-rate';

describe('engagementRateColor', () => {
  it('returns success color for rate >= 5', () => {
    expect(engagementRateColor(5)).toBe('var(--color-state-success)');
    expect(engagementRateColor(10)).toBe('var(--color-state-success)');
  });

  it('returns primary color for rate >= 2 and < 5', () => {
    expect(engagementRateColor(2)).toBe('var(--color-foreground-primary)');
    expect(engagementRateColor(3.5)).toBe('var(--color-foreground-primary)');
    expect(engagementRateColor(4.9)).toBe('var(--color-foreground-primary)');
  });

  it('returns destructive color for rate < 2', () => {
    expect(engagementRateColor(0)).toBe('var(--color-state-destructive)');
    expect(engagementRateColor(1.9)).toBe('var(--color-state-destructive)');
    expect(engagementRateColor(1)).toBe('var(--color-state-destructive)');
  });
});
