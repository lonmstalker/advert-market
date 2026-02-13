import { describe, expect, it } from 'vitest';
import { getChannelLanguages, getMinPrice } from '../channel-utils';

describe('getChannelLanguages', () => {
  it('returns languages array when present', () => {
    expect(getChannelLanguages({ languages: ['en', 'ru'] })).toEqual(['en', 'ru']);
  });

  it('falls back to single language field', () => {
    expect(getChannelLanguages({ language: 'en' })).toEqual(['en']);
    expect(getChannelLanguages({ languages: [], language: 'fr' })).toEqual(['fr']);
  });

  it('returns empty array when no languages', () => {
    expect(getChannelLanguages({})).toEqual([]);
    expect(getChannelLanguages({ languages: null, language: null })).toEqual([]);
  });
});

describe('getMinPrice', () => {
  it('returns minimum price from rules', () => {
    expect(getMinPrice([{ priceNano: 300 }, { priceNano: 100 }, { priceNano: 200 }])).toBe(100);
  });

  it('returns the only price for a single rule', () => {
    expect(getMinPrice([{ priceNano: 500 }])).toBe(500);
  });

  it('returns null for empty rules', () => {
    expect(getMinPrice([])).toBeNull();
  });
});
