/**
 * Returns a CSS color variable string based on engagement rate threshold.
 */
export function engagementRateColor(rate: number): string {
  if (rate >= 5) return 'var(--color-state-success)';
  if (rate >= 2) return 'var(--color-foreground-primary)';
  return 'var(--color-state-destructive)';
}
