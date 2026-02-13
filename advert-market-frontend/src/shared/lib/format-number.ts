/**
 * Compact number formatting for display (e.g. 1.2M, 15K, 999).
 */
export function formatCompactNumber(count: number): string {
  if (count >= 1_000_000) return `${(count / 1_000_000).toFixed(1)}M`;
  if (count >= 1_000) return `${Math.round(count / 1_000)}K`;
  return String(count);
}

/**
 * Locale-formatted number (e.g. 1 234 567 in ru-RU).
 */
export function formatLocaleNumber(count: number): string {
  return count.toLocaleString('ru-RU');
}
