/**
 * Resolves channel languages, falling back to the legacy single-language field.
 */
export function getChannelLanguages(channel: { languages?: string[] | null; language?: string | null }): string[] {
  if (channel.languages && channel.languages.length > 0) return channel.languages;
  if (channel.language) return [channel.language];
  return [];
}

/**
 * Returns the minimum price among a set of pricing rules, or null if empty.
 */
export function getMinPrice(rules: ReadonlyArray<{ priceNano: number }>): number | null {
  if (rules.length === 0) return null;
  return Math.min(...rules.map((r) => r.priceNano));
}
